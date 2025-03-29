import java.io.File
import java.util.Base64
import java.io.FileOutputStream

import javax.inject.Inject
import org.json.JSONObject
import okhttp3.ResponseBody

import retrofit2.Call
import retrofit2.Response

class DownloadCommand @Inject constructor(
    private val apiService: DiscordApiService,
    private val database: LocalDatabaseManager
): Command {
    override val name: String = "download"

    private var totalBytesDownloaded: Long = 0L

    private fun writeDataToFile(
        responseBody: ResponseBody,
        tempFile: File,
        size: Long
    ) {
        responseBody.byteStream().use { inputStream ->
            FileOutputStream(tempFile, true).use { outputStream ->
                val buffer: ByteArray = ByteArray(8 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer.copyOf(bytesRead))
                    totalBytesDownloaded += bytesRead
                    displayProgressBar(totalBytesDownloaded, size, "Downloading")
                }
            }
        }
    }

    private fun fetchAndWriteData(
        url: String,
        startPosition: Long,
        endPosition: Long,
        tempFile: File,
        size: Long,
        retries: Int = 0
    ) {
        val call: Call<ResponseBody> = apiService.fetchData(url,"bytes=$startPosition-${endPosition - 1}")
        val response: Response<ResponseBody> = call.execute()
        val statusCode: Int = response.code()
        if (response.isSuccessful) {
            writeDataToFile(response.body()!!, tempFile, size)
            return
        } else if (statusCode == 429 && retries < 10) {
            val retryAfterSeconds: Double = response.errorBody()?.string()?.let {
                JSONObject(it).optDouble("retry_after", 1.0)
            } ?: 1.0
            Thread.sleep((retryAfterSeconds * 1000).toLong())
            fetchAndWriteData(url, startPosition, endPosition, tempFile, size, retries + 1)
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

    private fun fetchAttachmentUrls(messageId: String, retries: Int = 0): List<String> {
        val token: String = database.getFromConfig("token")
        val channelId: String = database.getFromConfig("channel_id")
        val call: Call<Map<String, Any>> = apiService.fetchMessage("Bot $token", channelId, messageId)
        val response: Response<Map<String, Any>> = call.execute()
        val statusCode: Int = response.code()
        if (response.isSuccessful) {
            val attachments: List<Map<String, Any>> = response.body()?.getTypedValue("attachments") ?: emptyList()
            return attachments.map { it["url"] as String }
        } else if (statusCode == 429 && retries < 10) {
            val retryAfterSeconds: Double = response.errorBody()?.string()?.let {
                JSONObject(it).optDouble("retry_after", 1.0)
            } ?: 1.0
            Thread.sleep((retryAfterSeconds * 1000).toLong())
            fetchAttachmentUrls(messageId, retries + 1)
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

    private fun fetchDataFromAttachments(
        filteredAttachmentUrls: List<Map<String, Any>>,
        tempFile: File,
        size: Long
    ) {
        for (cell: Map<String, Any> in filteredAttachmentUrls) {
            val positions: Map<String, Any> = cell.getTypedValue("position")!!
            fetchAndWriteData(
                cell["url"] as String,
                (positions["start_position"] as Number).toLong(),
                (positions["end_position"] as Number).toLong(),
                tempFile,
                size
            )
        }
    }

    private fun filterAttachmentUrls(attachmentUrls: List<String>, cellsList: List<Map<String, Any>>): List<Map<String, Any>> {
        return cellsList.map { cell ->
            val positions: Map<String, Any> = cell.getTypedValue("position")!!
            val cellIndex: Int = (cell["cell_index"] as Number).toInt()
            mapOf(
                "url" to attachmentUrls.get(cellIndex),
                "position" to positions
            )
        }
    }

    private fun downloadFile(
        node: Map<String, Any>,
        tempFile: File,
        finalFile: File
    ) {
        val size: Long = (node["size"] as Number).toLong()
        displayProgressBar(totalBytesDownloaded, size, "Downloading")
        val dataRef: Map<String, List<Map<String, Any>>> = node.getTypedValue("data_ref")!!
        for ((messageId: String, cellsList: List<Map<String, Any>>) in dataRef.entries) {
            val attachmentUrls: List<String> = fetchAttachmentUrls(messageId)
            val filteredAttachmentUrls: List<Map<String, Any>> = filterAttachmentUrls(attachmentUrls, cellsList)
            fetchDataFromAttachments(filteredAttachmentUrls, tempFile, size)
        }
        tempFile.renameTo(finalFile)
    }
    
    override fun execute(args: List<String>) {
        totalBytesDownloaded = 0
        try {
            if (args.size > 1 || args.isEmpty()) {
                throw Exception("Invalid arguments. Usage: $name [number]")
            }
            val index: Int? = args.first().toIntOrNull()
            if (index == null) {
                throw Exception("Given input must be a number.")
            }
            val node: Map<String, Any>? = GlobalStateManager.getNode(index - 1)
            if (node == null) {
                throw Exception("File not found.")
            }
            if (node["type"] == "DIRECTORY") {
                throw Exception("Given input must be a file.")
            }
            val destinationDirectory: File = File(takeInput("Destination Path: ").trim())
            if (!destinationDirectory.exists()) {
                throw Exception("Destination path doesn't exist: ${destinationDirectory.absolutePath}")
            }
            if (destinationDirectory.isFile) {
                throw Exception("Given path is not a directory: ${destinationDirectory.absolutePath}")
            }
            setConsole(clear = true)
            val fileName: String = node["name"] as String
            val tempFile: File = destinationDirectory.resolve("$fileName.incomplete")
            val finalFile: File = destinationDirectory.resolve(fileName)
            downloadFile(node, tempFile, finalFile)
            println("\nSuccessfully downloaded: $fileName")
        } catch (e: Exception) {
            println("\nError: ${e.message}")
        }
        waitForEnter()    
    }
}