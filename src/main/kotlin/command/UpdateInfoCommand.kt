package scriptyyy.bd.cli.app.command

import scriptyyy.bd.cli.app.service.ApiService

class UpdateInfoCommand(private val apiService: ApiService) : Command {

    override fun execute(args: List<String>) {
        println("Загружаю данные с API...")
        if (apiService.loadDataFromApi()) {
            println("Данные успешно загружены")
        } else {
            println("Ошибка при загрузке данных")
        }
    }

    override fun getName(): String = "update_info"

    override fun getHelp(): String = "загрузить/обновить данные с API"
}