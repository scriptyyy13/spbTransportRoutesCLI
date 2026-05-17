package scriptyyy.bd.cli.app.command

import scriptyyy.bd.cli.app.service.DatabaseService

class GetRoutesCommand(private val dbService: DatabaseService) : Command {

    override fun execute(args: List<String>) {
        if (args.isEmpty()) {
            println("Ошибка: укажите id или название остановки")
            return
        }

        val stopQuery = args.joinToString(" ")
        val (stop, variants) = dbService.findStopByIdOrName(stopQuery)

        if (variants.isNotEmpty()) {
            println("Найдено несколько остановок:")
            for (s in variants) {
                println("  i${s.stopId} - ${s.name}")
            }
            println("Повторите команду, используя ID остановки с префиксом 'i', например: get_routes i${variants[0].stopId}")
            return
        }

        if (stop == null) {
            println("Остановка не найдена: $stopQuery")
            return
        }

        val routeStops = dbService.getRoutesForStop(stop)
        if (routeStops.isEmpty()) {
            println("На остановке нет маршрутов")
            return
        }

        val routes = routeStops.map { it.route }.distinctBy { it?.id }

        println("Остановка: ${stop.name}")
        println("Маршруты:")
        for (route in routes) {
            println("  ${route?.shortName} - ${route?.longName} (${route?.transportType})")
        }
        println("\nВсего маршрутов: ${routes.size}")
    }

    override fun getName(): String = "get_routes"

    override fun getHelp(): String = "маршруты на остановке"
}