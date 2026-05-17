package scriptyyy.bd.cli.app.entity

import jakarta.persistence.*

@Entity
@Table(name = "route_stops")
class RouteStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", referencedColumnName = "id", nullable = false)
    var route: Route? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stop_id", referencedColumnName = "id", nullable = false)
    var stop: Stop? = null

    @Column(name = "stop_number", nullable = false)
    var stopNumber: Int = 0

    @Column(name = "direction", nullable = false)
    var direction: String = ""

    @Column(name = "distance")
    var distance: Double = 0.0

    @Column(name = "next_stop_id")
    var nextStopId: String? = null
}