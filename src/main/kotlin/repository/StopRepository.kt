package scriptyyy.bd.cli.app.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import scriptyyy.bd.cli.app.entity.Stop

@Repository
interface StopRepository : JpaRepository<Stop, Long> {
    fun findByStopId(stopId: String): Stop?
    fun findByNameIgnoreCase(name: String): List<Stop>
}