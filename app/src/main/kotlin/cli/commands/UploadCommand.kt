import java.io.File
import java.io.FileInputStream
import java.io.ByteArrayOutputStream

import retrofit2.Call
import retrofit2.Response

import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

import org.json.JSONObject
import javax.inject.Inject
import com.google.gson.GsonBuilder

class UploadCommand @Inject constructor(
    private val apiService: DiscordApiService,
    private val database: LocalDatabaseManager
) : Command {
    override val name: String = "upload"

    private var totalSize: Long = 0
    private var totalBytesUploaded: Long = 0
    private var currentCellIndex: Int = 0
    private var currentBatchNumber: Int = 0
    private var currentCellUsed: Int = 0
    private val currentCellBuffer: ByteArrayOutputStream = ByteArrayOutputStream()
    private val metadata: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
    private var builder: MultipartBody.Builder = MultipartBody.Builder().setType(MultipartBody.FORM)

    private fun clearState() {
        totalSize = 0
        totalBytesUploaded = 0
        currentCellIndex = 0
        currentBatchNumber = 0
        currentCellUsed = 0
        currentCellBuffer.reset()
        builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        metadata.clear()
    }

    private fun buildProgressRequestBody(multipartBody: MultipartBody): ProgressRequestBody {
        return ProgressRequestBody(multipartBody, {
            totalBytesUploaded = (totalBytesUploaded + it).coerceAtMost(totalSize)
            displayProgressBar(totalBytesUploaded, totalSize, "Uploading")
        })
    }

    private fun upload(
        multipartBody: MultipartBody,
        retries: Int = 0
    ): String {
        val token: String = database.getFromConfig("token")
        val channelId: String = database.getFromConfig("channel_id")
        val progressBody: ProgressRequestBody = buildProgressRequestBody(multipartBody)
        val call: Call<Map<String, Any>> = apiService.uploadFile("Bot $token", channelId, progressBody)
        val response: Response<Map<String, Any>> = call.execute()
        val statusCode: Int = response.code()
        if (response.isSuccessful) {
            return (response.body()!!["id"]) as String
        } else if (statusCode == 429 && retries < 10) {
            val retryAfterSeconds: Double = response.errorBody()?.string()?.let {
                JSONObject(it).optDouble("retry_after", 1.0)
            } ?: 1.0
            Thread.sleep((retryAfterSeconds * 1000).toLong())
            upload(
                multipartBody,
                retries + 1
            )
        }
        throw Exception(
            when (statusCode) {
                429 -> "You are being rate limited."
                401 -> "Invalid token is being used."
                404 -> "Invalid channel id is being used."
                else -> statusCode.toString()
            }
        )
    }

    private fun buildFileMetadata(file: File, id: String, parentId: String): MutableMap<String, Any> {
        return mutableMapOf<String, Any>(
            "name" to file.name,
            "size" to file.length(),
            "type" to "FILE",
            "id" to id,
            "parent_id" to parentId,
            "data_ref" to mutableMapOf<String, MutableList<MutableMap<String, Any>>>(),
            "uploaded_at" to getCurrentDateTime()
        )
    }

    private fun buildDirectoryMetadata(file: File, id: String, parentId: String): MutableMap<String, Any> {
        return mutableMapOf<String, Any>(
            "name" to file.name,
            "type" to "DIRECTORY",
            "id" to id,
            "parent_id" to parentId,
            "created_at" to getCurrentDateTime(),
            "children" to mutableListOf<String>()
        )
    }
    
    private fun updateMetadata(
        fileId: String,
        startPos: Int
    ) {
        val currentFileMetadata: Map<String, Any> = metadata[fileId]!!
        val dataRef: MutableMap<String, MutableList<MutableMap<String, Any>>> = currentFileMetadata.getTypedValue("data_ref")!!
        dataRef.getOrPut(currentBatchNumber.toString()) { mutableListOf() }.apply {
            if (isNotEmpty() && last()["cell_index"] == currentCellIndex) {
                val prev: MutableMap<String, Any> = last()
                val position: MutableMap<String, Any> = prev.getTypedValue("position")!!
                position["end_position"] = currentCellUsed
            } else {
                addCurrentCellInfoToMetadata(startPos, this)
            }
        }
    }

    private fun addCurrentCellInfoToMetadata(
        startPos: Int, 
        cellInfoList: MutableList<MutableMap<String, Any>>
    ) {
        val cellInfo: MutableMap<String, Any> = mutableMapOf(
            "cell_index" to currentCellIndex,
            "position" to mapOf(
                "start_position" to startPos,
                "end_position" to currentCellUsed
            )
        )
        cellInfoList.add(cellInfo)
    }

    private fun flushCurrentCell() {
        val requestBody: RequestBody = currentCellBuffer.toByteArray()
            .toRequestBody("application/octet-stream".toMediaTypeOrNull())
        builder.addFormDataPart("batch-$currentCellIndex", "cell-$currentCellIndex", requestBody)
        currentCellUsed = 0
        currentCellIndex++
        currentCellBuffer.reset()
    }

    private fun uploadBatch() {
        val msgId: String = upload(builder.build())
        for (fileData: MutableMap<String, Any> in metadata.values) {
            if (fileData["type"] != "FILE") continue
            val dataRef: MutableMap<String, MutableList<MutableMap<String, Any>>> = fileData.getTypedValue("data_ref")!!
            val batchNo: String = currentBatchNumber.toString()
            if (dataRef.containsKey(batchNo)) {
                fileData["uploaded_at"] = getCurrentDateTime()
                val pending: MutableList<MutableMap<String, Any>> = dataRef.remove(currentBatchNumber.toString())!!
                dataRef[msgId] = pending
            }
        }
        builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        currentCellIndex = 0
        currentBatchNumber++
    }

    private fun processBytes(
        fileId: String,
        bytesRead: Int,
        buffer: ByteArray
    ) {
        var offset: Int = 0
        while (offset < bytesRead) {
            if (currentCellUsed == MAX_DATA_CELL_SIZE) {
                flushCurrentCell()
                if (currentCellIndex == MAX_CELLS_PER_BATCH) {
                    uploadBatch()
                }
            }
            val startPos: Int = currentCellUsed
            val available: Int = MAX_DATA_CELL_SIZE - currentCellUsed
            val bytesToWrite: Int = minOf(bytesRead - offset, available)
            currentCellBuffer.write(buffer, offset, bytesToWrite)
            currentCellUsed += bytesToWrite
            offset += bytesToWrite
            updateMetadata(fileId, startPos)  
        }
    }

    private fun processData(
        fileId: String,
        file: File
    ) {
        file.inputStream().use { 
            var bytesRead: Int
            val buffer: ByteArray = ByteArray(8 * 1024)
            while (it.read(buffer).also {bytesRead = it} != -1) {
                processBytes(fileId, bytesRead, buffer)
            }
        } 
    }

    private fun processRemainingData() {
        if (currentCellUsed == 0) return
        flushCurrentCell()
        uploadBatch()
    }

    private fun upload(file: File, parentId: String) {
        val previousParentIds: MutableMap<String, String> = mutableMapOf<String, String>()
        setConsole(clear = true)
        println("Preparing to upload...")
        val files: MutableList<File> = file.walkTopDown().mapTo(mutableListOf()) { currentFile ->
            if (currentFile.isFile) {
                totalSize += currentFile.length()
            }
            currentFile
        }
        setConsole(clear = true)
        displayProgressBar(totalBytesUploaded, totalSize, "Uploading")
        files.forEach{ currentFile ->
            val currentId: String = generateId()
            if (!(currentFile.exists() && currentFile.canRead())) {
                return@forEach
            }
            if (currentFile.isDirectory) {
                previousParentIds[currentFile.absolutePath] = currentId
            }
            val parent: String = previousParentIds[currentFile.parentFile?.absolutePath] ?: parentId
            metadata[parent]?.let { parentMeta ->
                val children: MutableList<String> = parentMeta.getTypedValue("children")!!
                children.add(currentId)
            }
            metadata[currentId] = if (currentFile.isDirectory) {
                buildDirectoryMetadata(currentFile, currentId, parent)
            } else {
                val fileMeta: MutableMap<String, Any> = buildFileMetadata(currentFile, currentId, parent)
                metadata[currentId] = fileMeta
                processData(currentId, currentFile)
                fileMeta
            }
        }
        processRemainingData()
        database.insertMetadata(metadata, parentId)
    }
    
    override fun execute(args: List<String>) {
        clearState()
        try {
            if (args.size > 1 || args.isEmpty()) {
                throw Exception("Invalid arguments. Usage: $name [path]")
            }
            val file: File = File(args.first()) 
            val path: String = file.absolutePath
            if (!file.exists()) {
                throw Exception("Path doesn't exist: $path")
            }
            setConsole(clear = true)
            val currentLocation: Map<String, String> = NavigationStack.getCurrentLocation()
            upload(file, currentLocation["id"]!!)
            println("\nSuccessfully added: $path")
        } catch (e: Exception) {
            println("\nError: ${e.message}")
        }
        waitForEnter()
    }
}