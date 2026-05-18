package scriptyyy.bd.cli.app.command

class HelpCommand(private val commands: List<Command>) : Command {

    override fun execute(args: List<String>) {
        println("Список команд:")
        for (cmd in commands) {
            println("   ${cmd.getName()} - ${cmd.getHelp()}")
        }
    }

    override fun getName(): String = "help"

    override fun getHelp(): String = "вывести все команды"
}