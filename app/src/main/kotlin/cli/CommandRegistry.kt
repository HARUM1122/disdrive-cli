import javax.inject.Inject

class CommandRegistry @Inject constructor(
    commands: Set<@JvmSuppressWildcards Command>
) {
    private val commandMap: Map<String, Command> = commands.associateBy { it.name.lowercase() }

    fun getAvailableCommands(): List<String> = commandMap.keys.toList()
    
    fun getCommand(name: String): Command? = commandMap[name.lowercase()]
}
