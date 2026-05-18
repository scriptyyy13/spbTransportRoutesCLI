package scriptyyy.bd.cli.app.service

import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import scriptyyy.bd.cli.app.entity.Route
import scriptyyy.bd.cli.app.entity.RouteStop
import scriptyyy.bd.cli.app.entity.Stop
import scriptyyy.bd.cli.app.repository.RouteRepository
import scriptyyy.bd.cli.app.repository.RouteStopRepository
import scriptyyy.bd.cli.app.repository.StopRepository
import javax.sql.DataSource

@Service
@Transactional
@Component
class DatabaseService(
    private val routeRepository: RouteRepository,
    private val stopRepository: StopRepository,
    private val routeStopRepository: RouteStopRepository,
    private val dataSource: DataSource
) {

    fun saveRoute(route: Route) {
        routeRepository.save(route)
    }

    fun saveStop(stop: Stop) {
        stopRepository.save(stop)
    }

    fun saveRouteStops(items: List<RouteStop>) {
        routeStopRepository.saveAll(items)
    }

    fun saveRouteStop(routeStop: RouteStop) {
        routeStopRepository.save(routeStop)
    }

    fun getAllRoutes(): List<Route> {
        return routeRepository.findAll()
    }

    fun getAllStops(): List<Stop> {
        return stopRepository.findAll()
    }

    fun getAllRouteStops(): List<RouteStop> {
        return routeStopRepository.findAll()
    }

    /**
     * Проверяет существует ли уже такой RouteStop
     */
    fun routeStopExists(route: Route?, stop: Stop?, stopNumber: Int, direction: String): Boolean {
        if (route?.id == null || stop?.id == null) {
            return false
        }
        return try {
            routeStopRepository.findByRouteIdAndStopIdAndStopNumberAndDirection(
                route.id,
                stop.id,
                stopNumber,
                direction
            ) != null
        } catch (e: Exception) {
            false
        }
    }

    fun findRouteByIdOrName(query: String): Pair<Route?, List<Route>> {
        // если i в начале аргумента, то ищем по айди
        val isIdSearch = query.startsWith("i") && query.length > 1
        val searchQuery = if (isIdSearch) query.substring(1) else query

        val byId = routeRepository.findByRouteId(searchQuery)
        if (byId != null) {
            return Pair(byId, emptyList())
        }

        if (!isIdSearch) {
            val byName = routeRepository.findByShortNameIgnoreCase(searchQuery)
            return when {
                byName.isEmpty() -> Pair(null, emptyList())
                byName.size == 1 -> Pair(byName[0], emptyList())
                else -> Pair(null, byName)
            }
        }

        return Pair(null, emptyList())
    }

    fun findStopByIdOrName(query: String): Pair<Stop?, List<Stop>> {
        // если i в начале аргумента, то ищем по айди
        val isIdSearch = query.startsWith("i") && query.length > 1
        val searchQuery = if (isIdSearch) query.substring(1) else query

        val byId = stopRepository.findByStopId(searchQuery)
        if (byId != null) {
            return Pair(byId, emptyList())
        }

        if (!isIdSearch) {
            val byName = stopRepository.findByNameIgnoreCase(searchQuery)
            return when {
                byName.isEmpty() -> Pair(null, emptyList())
                byName.size == 1 -> Pair(byName[0], emptyList())
                else -> Pair(null, byName)
            }
        }

        return Pair(null, emptyList())
    }

    fun getStopsForRoute(route: Route): List<RouteStop> {
        return try {
            routeStopRepository.findByRouteOrderByStopNumber(route)
        } catch (e: Exception) {
            println("Ошибка при получении остановок: ${e.message}")
            emptyList()
        }
    }

    fun getRoutesForStop(stop: Stop): List<RouteStop> {
        return try {
            routeStopRepository.findByStop(stop)
        } catch (e: Exception) {
            println("Ошибка при получении маршрутов: ${e.message}")
            emptyList()
        }
    }

    @Transactional
    fun clear() {
        routeStopRepository.deleteAll()
        routeRepository.deleteAll()
        stopRepository.deleteAll()
    }

    /**
     * Триггер для подсчета остановок у маршрута
     * */
    fun createTrigger() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->

                statement.execute("""
                CREATE OR REPLACE FUNCTION update_stops_count()
                RETURNS TRIGGER AS $$
                BEGIN
                    UPDATE routes
                    SET total_stops = (
                        SELECT COUNT(*)
                        FROM route_stops
                        WHERE route_id = NEW.route_id
                    )
                    WHERE id = NEW.route_id;

                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql;
            """.trimIndent())

                statement.execute("""
                DROP TRIGGER IF EXISTS update_route_stops_count
                ON route_stops
            """.trimIndent())

                statement.execute("""
                CREATE TRIGGER update_route_stops_count
                AFTER INSERT ON route_stops
                FOR EACH ROW
                EXECUTE FUNCTION update_stops_count()
            """.trimIndent())
            }
        }
    }
}