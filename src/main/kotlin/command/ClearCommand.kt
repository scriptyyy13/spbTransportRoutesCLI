package scriptyyy.bd.cli.app.command

import scriptyyy.bd.cli.app.service.DatabaseService

class ClearCommand(private val dbService: DatabaseService) : Command {

    override fun execute(args: List<String>) {
        print("Вы уверены? База данных будет полностью очищена (y/n): ")
        val confirmation = readlnOrNull()?.trim()?.lowercase()

        if (confirmation?.lowercase() == "y") {
            try {
                dbService.clear()
                println("База данных очищена")
            } catch (e: Exception) {
                println("Ошибка при очистке БД: ${e.message}")
            }
        } else {
            println("Отменено")
        }
    }

    override fun getName(): String = "clear"

    override fun getHelp(): String = "очистить базу данных"
}