import javax.inject.Inject

class SetChannelIdCommand @Inject constructor(
    private val database: LocalDatabaseManager
): Command {
    override val name: String = "set-channel-id"

    override fun execute(args: List<String>) {
        try {
            if (args.size > 1 || args.isEmpty()) {
                throw Exception("Invalid arguments. Usage: $name [your-channel-id]")
            }
            database.updateConfig(channelId = args.first())
        } catch (e: Exception) {
            println("\nError: ${e.message}")
            waitForEnter()
        }
    }
}