package scriptyyy.bd.cli.app.command

import scriptyyy.bd.cli.app.service.DatabaseService

class GetRouteInfoCommand(private val dbService: DatabaseService) : Command {

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
            println("Повторите команду, используя ID маршрута с префиксом 'i', например: get_route_info i${variants[0].routeId}")
            return
        }

        if (route == null) {
            println("Маршрут не найден: $routeQuery")
            return
        }

        val stops = dbService.getStopsForRoute(route)
        val directions = stops.groupBy { it.direction }

        println("Маршрут: ${route.shortName} - ${route.longName}")
        println("Тип: ${route.transportType}")
        println("id маршрута: ${route.routeId}")

        for ((dir, stopsInDir) in directions) {
            val totalDistance = stopsInDir.sumOf { it.distance }
            println("Направление $dir - Расстояние: $totalDistance км")
        }

        println("Всего остановок: ${route.totalStops}")
    }

    override fun getName(): String = "get_route_info"

    override fun getHelp(): String = "информация о маршруте"
}