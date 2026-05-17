package scriptyyy.bd.cli.app.entity

import jakarta.persistence.*

@Entity
@Table(name = "stops")
class Stop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "stop_id", nullable = false, unique = true)
    var stopId: String = ""

    @Column(name = "name", nullable = false)
    var name: String = ""

    @Column(name = "coordinates")
    var coordinates: String = ""

    @OneToMany(mappedBy = "stop", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var routeStops: MutableList<RouteStop> = mutableListOf()

    override fun toString(): String = name
}