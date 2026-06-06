package scriptyyy.bd.cli.app.command

import kotlin.system.exitProcess

class ExitCommand : Command {

    override suspend fun execute(args: List<String>) {
        println("Завершение работы")
        exitProcess(0)
    }

    override fun getName(): String = "exit"

    override fun getHelp(): String = "завершить работу"
}