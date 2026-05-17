package scriptyyy.bd.cli.app.entity

import jakarta.persistence.*

@Entity
@Table(name = "routes")
class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "route_id", nullable = false, unique = true)
    var routeId: String = ""

    @Column(name = "short_name", nullable = false)
    var shortName: String = ""

    @Column(name = "route_long_name", nullable = false)
    var longName: String = ""

    @Column(name = "transport_type", nullable = false)
    var transportType: String = ""

    @Column(name = "total_stops")
    var totalStops: Int = 0

    @OneToMany(mappedBy = "route", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var stops: MutableList<RouteStop> = mutableListOf()

    override fun toString(): String {
        return "$shortName - $longName ($transportType)"
    }
}