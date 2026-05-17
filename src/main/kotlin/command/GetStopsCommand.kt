package scriptyyy.bd.cli.app.command

import scriptyyy.bd.cli.app.service.DatabaseService

class GetStopsCommand(private val dbService: DatabaseService) : Command {

    override fun execute(args: List<String>) {
        if (args.isEmpty()) {
            println("Ошибка: укажите название или номер маршрута")
            return
        }

        val routeQuery = args[0]
        val (route, variants) = dbService.findRouteByIdOrName(routeQuery)

        if (variants.isNotEmpty()) {
            println("Найдено несколько маршрутов:")
            for (r in variants) {
                println("  i${r.routeId} - ${r.shortName} - ${r.longName} (${r.transportType})")
            }
            println("Повторите команду, используя ID маршрута с префиксом 'i', например: get_stops i${variants[0].routeId}")
            return
        }

        if (route == null) {
            println("Маршрут не найден: $routeQuery")
            return
        }

        val stops = dbService.getStopsForRoute(route)
        if (stops.isEmpty()) {
            println("На маршруте нет остановок")
            return
        }

        val directions = stops.groupBy { it.direction }

        for ((dir, stopsInDir) in directions) {
            println("\nНаправление $dir:")
            for (rs in stopsInDir.sortedBy { it.stopNumber }) {
                println("  ${rs.stopNumber}. ${rs.stop?.name}")
            }
        }

        println("\nВсего остановок: ${route.totalStops}")
    }

    override fun getName(): String = "get_stops"

    override fun getHelp(): String = "список остановок на маршруте"
}