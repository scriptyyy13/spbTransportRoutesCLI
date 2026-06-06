package scriptyyy.bd.cli.app.command

interface Command {
    suspend fun execute(args: List<String>)
    fun getName(): String
    fun getHelp(): String
}