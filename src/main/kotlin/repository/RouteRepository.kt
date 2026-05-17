package scriptyyy.bd.cli.app.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import scriptyyy.bd.cli.app.entity.Route

@Repository
interface RouteRepository : JpaRepository<Route, Long> {
    fun findByRouteId(routeId: String): Route?
    fun findByShortNameIgnoreCase(shortName: String): List<Route>
}