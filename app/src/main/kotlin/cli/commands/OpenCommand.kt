import javax.inject.Inject

class OpenCommand @Inject constructor(): Command {
    override val name: String = "open"
    
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
                throw Exception("Directory not found.")
            }
            if (node["type"] == "FILE") {
                throw Exception("Given input must be a directory.")
            }
            NavigationStack.navigateTo(
                node["name"] as String,
                node["id"] as String
            )
        } catch (e: Exception) {
            println("\nError: ${e.message}")
            waitForEnter()
        }
    }
}


