package scriptyyy.bd.cli.app

import org.springframework.stereotype.Component
import scriptyyy.bd.cli.app.command.*
import scriptyyy.bd.cli.app.service.ApiService
import scriptyyy.bd.cli.app.service.DatabaseService
import java.util.Scanner

@Component
class ConsoleCli(
    private val dbService: DatabaseService,
    private val apiService: ApiService
) {

    private lateinit var commands: List<Command>

    fun run() {
        initCommands()

        println("Введите help для списка команд")

        val scanner = Scanner(System.`in`)
        while (true) {
            print("> ")
            val input = scanner.nextLine().trim()

            if (input.isEmpty()) continue

            val parts = input.split(" ")
            val cmdName = parts[0]
            val args = parts.drop(1)

            val cmd = commands.find { it.getName() == cmdName }

            if (cmd != null) {
                cmd.execute(args)
            } else {
                println("Неизвестная команда: $cmdName")
            }
        }
    }

    private fun initCommands() {
        commands = listOf(
            GetStopsCommand(dbService),
            GetRouteInfoCommand(dbService),
            GetRoutesCommand(dbService),
            UpdateInfoCommand(apiService),
            ClearCommand(dbService),
            ExitCommand()
        )

        commands = commands + HelpCommand(commands)
    }
}