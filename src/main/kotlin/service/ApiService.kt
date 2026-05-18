package scriptyyy.bd.cli.app.service

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import scriptyyy.bd.cli.app.config.EnvConfig
import scriptyyy.bd.cli.app.entity.Route
import scriptyyy.bd.cli.app.entity.RouteStop
import scriptyyy.bd.cli.app.entity.Stop
import java.net.URL
import org.springframework.stereotype.Service
import javax.net.ssl.*
import java.security.SecureRandom

@Service
class ApiService(
    private val dbService: DatabaseService,
    private val gson: Gson,
    private val routeCache: MutableMap<String, Route> = mutableMapOf<String, Route>(),
    private val stopCache: MutableMap<String, Stop> = mutableMapOf<String, Stop>(),
    private val routeStopCache: HashSet<String> = hashSetOf<String>()
) {
    init {
        disableSslVerification()
    }

    fun loadDataFromApi(): Boolean {
        return try {
            preloadCaches()
            val token = getToken() ?: return false
            val baseUrl = EnvConfig.getApiUrl()

            var page = 1
            var totalLoaded = 0
            var totalSkipped = 0
            var timeSpent = 0L
            var totalRecords = 0

            while (true) {
                val startTime = System.currentTimeMillis()
                val json = fetchPage(baseUrl, page, token)
                    ?: return false
                if (page == 1) {
                    totalRecords = json.get("count")?.asInt ?: 0
                }
                val results = json.getAsJsonArray("results")
                if (results.isEmpty) break

                val (loaded, skipped) = processResults(results)

                totalLoaded += loaded
                totalSkipped += skipped

                val elapsed = System.currentTimeMillis() - startTime
                timeSpent += elapsed

                val avgPageTime = timeSpent / page
                val remainingPages = (totalRecords / 100) - page
                print("\rСтраница $page: загружено $loaded записей, пропущено $skipped дубликатов (всего: $totalLoaded)" +
                "\nОсталось примерно: ${formatTime(avgPageTime * remainingPages)}")

                if (page == 1) {
                    dbService.createTrigger() // создаем триггер после записи первой страницы
                }

                page++
            }

            println("Данные загружены: $totalLoaded новых записей, $totalSkipped дубликатов пропущено")
            true
        } catch (e: Exception) {
            println("Ошибка загрузки: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            if (hours > 0) append("${hours}ч ")
            if (minutes > 0 || hours > 0) append("${minutes}м ")
            append("${seconds}с")
        }
    }

    private fun preloadCaches() {
        dbService.getAllRoutes().forEach {
            routeCache[it.routeId] = it
        }

        dbService.getAllStops().forEach {
            stopCache[it.stopId] = it
        }

        dbService.getAllRouteStops().forEach {
            routeStopCache.add(
                buildRouteStopKey(
                    it.route?.routeId ?: "",
                    it.stop?.stopId ?: "",
                    it.stopNumber,
                    it.direction
                )
            )
        }
    }

    private fun getToken(): String? {
        val token = EnvConfig.getApiToken()
        if (token.isBlank()) {
            println("Ошибка: токен не установлен в .env")
            return null
        }
        return token
    }

    private fun fetchPage(baseUrl: String, page: Int, token: String): JsonObject? {
        val url = "$baseUrl?page=$page&per_page=100"
        return fetchJson(url, token).also {
            if (it == null) {
                println("Ошибка при загрузке страницы $page")
            }
        }
    }

    private fun fetchJson(urlString: String, token: String): JsonObject? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection()
            conn.setRequestProperty("Authorization", "Token $token")
            conn.setRequestProperty("Accept", "application/json")

            val response = conn.getInputStream().bufferedReader().use { it.readText() }

            gson.fromJson(response, JsonObject::class.java)
        } catch (e: Exception) {
            println("Ошибка при получении данных (закончились данные или ошибка соединения)")
            null
        }
    }

    private fun disableSslVerification() {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers() = arrayOf<java.security.cert.X509Certificate>()
                override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
            }
        )

        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    }

    private fun buildRouteStopKey(
        routeId: String,
        stopId: String,
        number: Int,
        direction: String
    ): String {
        return "$routeId|$stopId|$number|$direction"
    }

    /**
     * Обрабатывает результаты и возвращает количество загруженных и пропущенных записей
     */
    private fun processResults(results: JsonArray): Pair<Int, Int> {
        var loaded = 0
        var skipped = 0

        val batch = mutableListOf<RouteStop>()

        for (item in results) {
            try {
                val obj = item.asJsonObject

                val route = getOrCreateRoute(obj)
                val stop = getOrCreateStop(obj)

                val number = obj["number"].asInt
                val direction = obj["direction"].asString

                val key = buildRouteStopKey(
                    route.routeId,
                    stop.stopId,
                    number,
                    direction
                )

                if (key in routeStopCache) {
                    skipped++
                    continue
                }

                routeStopCache.add(key)

                batch.add(
                    RouteStop().apply {
                        this.route = route
                        this.stop = stop
                        this.direction = direction
                        this.stopNumber = number
                        this.distance =
                            obj["stop_distance"]
                                .asString
                                .toDoubleOrNull() ?: 0.0

                        this.nextStopId = obj["next_stop"].asString
                    }
                )

                loaded++

            } catch (e: Exception) {
                println("Ошибка записи: ${e.message}")
            }
        }

        if (batch.isNotEmpty()) {
            dbService.saveRouteStops(batch)
        }

        return loaded to skipped
    }

    private fun getOrCreateRoute(obj: JsonObject): Route {
        val routeId = obj["route_id"].asString

        routeCache[routeId]?.let {
            return it
        }

        val route = Route().apply {
            this.routeId = routeId
            this.shortName = obj["route_short_name"].asString
            this.longName = obj["route_long_name"].asString
            this.transportType = obj["transport_type"].asString
        }

        dbService.saveRoute(route)
        routeCache[routeId] = route
        return route
    }

    private fun getOrCreateStop(obj: JsonObject): Stop {
        val stopId = obj["stop_id"].asString

        stopCache[stopId]?.let {
            return it
        }

        val stop = Stop().apply {
            this.stopId = stopId
            this.name = obj["stop_name"].asString
            this.coordinates = obj["coordinates"].asString
        }

        dbService.saveStop(stop)
        stopCache[stopId] = stop
        return stop
    }
}