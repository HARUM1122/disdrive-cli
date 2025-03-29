import javax.inject.Inject

class SetTokenCommand @Inject constructor(
    private val database: LocalDatabaseManager
): Command {
    override val name: String = "set-token"

    override fun execute(args: List<String>) {
        try {
            if (args.size > 1 || args.isEmpty()) {
                throw Exception("Invalid arguments. Usage: $name [your-bot-token]")
            }
            database.updateConfig(token = args.first())
        } catch (e: Exception) {
            println("\nError: ${e.message}")
            waitForEnter()
        } 
    }
}