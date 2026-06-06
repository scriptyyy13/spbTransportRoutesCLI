package scriptyyy.bd.cli.app.service

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import scriptyyy.bd.cli.app.config.EnvConfig
import scriptyyy.bd.cli.app.entity.Route
import scriptyyy.bd.cli.app.entity.RouteStop
import scriptyyy.bd.cli.app.entity.Stop
import java.net.URL
import org.springframework.stereotype.Service
import javax.net.ssl.*
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger

@Service
class ApiService(
    private val dbService: DatabaseService,
    private val gson: Gson,
    private val routeCache: MutableMap<String, Route> = mutableMapOf(),
    private val stopCache: MutableMap<String, Stop> = mutableMapOf(),
    private val routeStopCache: HashSet<String> = hashSetOf()
) {

    init {
        disableSslVerification()
    }

    suspend fun loadDataFromApi(): Boolean = coroutineScope {
        try {
            preloadCaches()
            val token = getToken() ?: return@coroutineScope false
            val baseUrl = EnvConfig.getApiUrl()

            // данные о страницах
            val firstPageJson = withContext(Dispatchers.IO) { fetchPage(baseUrl, 1, token) }
                ?: return@coroutineScope false

            val totalRecords = firstPageJson.get("count")?.asInt ?: 0
            dbService.createTrigger()

            if (totalRecords == 0) return@coroutineScope true

            val perPage = 100
            val totalPages = (totalRecords + perPage - 1) / perPage

            var totalLoaded = 0
            var totalSkipped = 0
            val startTime = System.currentTimeMillis()

            // храним полученные данные
            val pageChannel = Channel<JsonObject>(capacity = Channel.BUFFERED)
            var elemetsCount = AtomicInteger(0)

            println("Запуск получения данных с API...")

            // асинхронное получение данных
            val producerJob = launch(Dispatchers.IO) {
                (1..totalPages).map { page ->
                    async {
                        val json = fetchPage(baseUrl, page, token)
                        if (json != null) {
                            pageChannel.send(json) // полученную страницу в очередь
                            elemetsCount.incrementAndGet()
                        }
                    }
                }.awaitAll()

                println("Получено ${elemetsCount.get()} страниц.\nНачинается загрузка в бд.")
                // после завершения чтения закрываем канал
                pageChannel.close()
            }


            // последовательно загружаем данные
            for (json in pageChannel) {
                val results = json.getAsJsonArray("results")
                if (results != null && !results.isEmpty) {
                    val (loaded, skipped) = processResults(results)
                    totalLoaded += loaded
                    totalSkipped += skipped
                }
            }

            producerJob.join()

            val elapsedTime = System.currentTimeMillis() - startTime
            println("\nДанные загружены за ${formatTime(elapsedTime)}: $totalLoaded новых записей, $totalSkipped дубликатов пропущено")
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
        dbService.getAllRoutes().forEach { routeCache[it.routeId] = it }
        dbService.getAllStops().forEach { stopCache[it.stopId] = it }
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

    private fun buildRouteStopKey(routeId: String, stopId: String, number: Int, direction: String): String {
        return "$routeId|$stopId|$number|$direction"
    }

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

                val key = buildRouteStopKey(route.routeId, stop.stopId, number, direction)

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
                        this.distance = obj["stop_distance"].asString.toDoubleOrNull() ?: 0.0
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
        routeCache[routeId]?.let { return it }

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
        stopCache[stopId]?.let { return it }

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