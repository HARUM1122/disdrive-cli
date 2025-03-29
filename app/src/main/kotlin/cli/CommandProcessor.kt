import javax.inject.Inject

class CommandProcessor @Inject constructor(private val registry: CommandRegistry) {
    private fun showAvailableCommands() {
        var count: Int = 1
        registry.getAvailableCommands().forEach { println("${count++}: $it") }
    }

    private fun handleEmptyCommands() {
        println("No command provided. Available:")
        showAvailableCommands()
    }

    private fun handleUnknownCommand(rawCommand: String) {
        println("Command `$rawCommand` not found. Available:")
        showAvailableCommands()
    }

    private fun executeCommand(rawCommand: String, commandArgs: List<String>) {
        val command: Command? = registry.getCommand(rawCommand)
        if (command == null) {
            handleUnknownCommand(rawCommand)
            waitForEnter()
            return
        }
        command.execute(commandArgs)
    }

    fun parseCommand(command: String): List<String> {
        val pattern: Regex = Regex("\"([^\"]*)\"|(\\S+)")
        val matches: Sequence<MatchResult> = pattern.findAll(command)
        return matches.map { matchResult ->
            val quotedText = matchResult.groupValues[1] 
            val unquotedText = matchResult.groupValues[2]
            if (quotedText.isNotEmpty()) quotedText else unquotedText
        }.toList()
    }
    
    fun processCommand(command: String) {
        val parsedCommand: List<String> = parseCommand(command)
        if (parsedCommand.isEmpty()) {
            handleEmptyCommands()
            waitForEnter()
            return
        }
        val rawCommand: String = parsedCommand.first().lowercase()
        val commandArgs: List<String> = parsedCommand.drop(1).map { it.trim() }
        executeCommand(rawCommand, commandArgs)
    }
}