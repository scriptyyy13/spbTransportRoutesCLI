package scriptyyy.bd.cli.app.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import scriptyyy.bd.cli.app.entity.Route
import scriptyyy.bd.cli.app.entity.RouteStop
import scriptyyy.bd.cli.app.entity.Stop

@Repository
interface RouteStopRepository : JpaRepository<RouteStop, Long> {
    fun findByRouteOrderByStopNumber(route: Route): List<RouteStop>
    fun findByStop(stop: Stop): List<RouteStop>
    fun findByRouteIdAndStopIdAndStopNumberAndDirection(routeId: Long?, stopId: Long?, stopNumber: Int, direction: String): RouteStop?
}