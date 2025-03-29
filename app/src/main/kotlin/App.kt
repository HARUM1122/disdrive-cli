import java.io.File
import java.io.PrintStream

import com.sun.jna.Function
import com.sun.jna.NativeLibrary

import javax.inject.Inject

class App @Inject constructor(
    private val database: LocalDatabaseManager,
    private val commandProcessor: CommandProcessor
) {
    private fun display() {
        setConsole(clear = true, title = "Disdrive")
        GlobalStateManager.clearState()
        var totalFiles: Int = 0
        var totalFolders: Int = 0
        val currentLocation: String = NavigationStack.getCurrentLocation()["id"]!!
        val rootNode: Map<String, Any> = database.getNode("root")!!
        val childNodes: List<Map<String, Any>> = database.getChildNodes(currentLocation)
        println("📂 Current Directory: ${NavigationStack.getFullPath()}\n")
        if (childNodes.isNotEmpty()) {
            childNodes.forEachIndexed { index, node ->
                val name: String = node["name"] as String
                val type: String = node["type"] as String
                val size: Long = (node["size"] as? Number)?.toLong() ?: 0L
                val prefix: String = when (type) {
                    "DIRECTORY" -> {
                        totalFolders++
                        "📂"
                    }
                    else -> {
                        totalFiles++
                        "📄"
                    }
                }
                val suffix: String = when (type) {
                    "FILE" -> "(${formatBytes(size)})"
                    else -> ""
                }
                println("  [${index + 1}] $prefix $name $suffix")
                GlobalStateManager.addNode(node)
            }
        } else {
            println("  No files or folders uploaded.")
        }
        println("\n💾 Total Storage Used: ${formatBytes((rootNode["total_storage_used"] as Number).toLong())} 📄 Files: $totalFiles 📂 Folders: $totalFolders")
    }

    fun run() {
        while (true) {
            display()
            val command: String = takeInput("\nCommand: ")
            commandProcessor.processCommand(command) 
        }
    }
}

fun main() {
    val kernel32Lib: NativeLibrary = NativeLibrary.getInstance("kernel32")
    val setConsoleOutputCP: Function = kernel32Lib.getFunction("SetConsoleOutputCP")
    setConsoleOutputCP.invoke(Boolean::class.java, arrayOf<Any>(65001))
    System.setOut(PrintStream(System.out, true, "UTF-8"))

    val appComponent: AppComponent = DaggerAppComponent.create()
    val app: App = appComponent.getApplication()
    app.run()
}