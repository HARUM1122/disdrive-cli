interface Command {
    val name: String
    fun execute(args: List<String>)
}