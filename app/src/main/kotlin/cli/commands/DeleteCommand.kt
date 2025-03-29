import javax.inject.Inject

class DeleteCommand @Inject constructor(
    private val database: LocalDatabaseManager
): Command {
    override val name: String = "delete"

    override fun execute(args: List<String>) {
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
                throw Exception("Node not found.")
            }
            if (takeInput("Delete ‘${node["name"]}’? (y/N): ") != "y") return
            setConsole(clear = true)
            println("Deleting ${node["name"]} ...")
            if (node["type"] == "FILE") {
                database.deleteFile(node["id"] as String)
            } else {
                database.deleteDirectory(node["id"] as String)
            }
        } catch(e: Exception) {
            println("\nError: ${e.message}")
            waitForEnter()
        }
    }
}