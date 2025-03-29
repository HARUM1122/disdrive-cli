import javax.inject.Inject

class BackCommand @Inject constructor(): Command {
    override val name: String = "back"
    
    override fun execute(args: List<String>) {
        try {
            if (args.size > 1) {
                throw Exception("Invalid arguments. Usage: $name [Optional: N]")
            }
            if (args.size == 1 && args.first().toIntOrNull() == null) {
                throw Exception("Given input must be a number.")
            }
            repeat(args.firstOrNull()?.toIntOrNull() ?: 1) {
                if (!NavigationStack.navigateBack()) return
            }
        } catch (e: Exception) {
            println("\nError: ${e.message}")
            waitForEnter()
        } 
    }
}
