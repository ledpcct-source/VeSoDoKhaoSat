package com.vesodo.khaosat.map

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.*

@Serializable
data class VectorRoadPoint(
    val lat: Double,
    val lon: Double
)

@Serializable
data class VectorRoad(
    val id: Long,
    val name: String,
    val roadType: String,
    val points: List<VectorRoadPoint>
)

@Serializable
data class VectorBasemap(
    val centerGps: GpsCoordinate,
    val zoom: Int,
    val roads: List<VectorRoad>
)

@Serializable
data class CachedVectorRoadZone(
    val id: String = "",
    val centerLat: Double,
    val centerLon: Double,
    val zoom: Int,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val radiusMeters: Double,
    val roads: List<VectorRoad>,
    val timestamp: Long = 0L
)


enum class FetchStatus {
    CONNECTING,
    SUCCESS,
    EMPTY,
    ERROR
}

data class VectorFetchReport(
    val status: FetchStatus,
    val endpoint: String = "",
    val message: String = "",
    val roads: List<VectorRoad> = emptyList(),
    val rawElementCount: Int = 0,
    val queryBBox: String = "",
    val errorMessage: String? = null
)

object GpsVectorRoadManager {

    private val OVERPASS_MIRRORS = listOf(
        "https://overpass.private.coffee/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
        "https://overpass-api.de/api/interpreter",
        "https://lz4.overpass-api.de/api/interpreter"
    )

    // Kho bộ nhớ đệm các vùng đường vector đã tải
    private val cachedZones = mutableListOf<CachedVectorRoadZone>()

    var onZoneCachedListener: ((CachedVectorRoadZone) -> Unit)? = null

    /**
     * Tính toán khoảng cách cự ly theo công thức Haversine (đơn vị: mét)
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Bán kính trái đất (m)
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = sin(dLat / 2.0).pow(2) + cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * sin(dLon / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }

    /**
     * Kiểm tra xem toạ độ targetGps có nằm trong phạm vi của bất kỳ vùng đường nào đã load trước đó hay không.
     * Trả về vùng đường đã cache nếu vị trí nằm trong phạm vi rất gần (cự ly quét thực tế <= 75m).
     * Tuyệt đối không dùng bounding box thô vì các tuyến đường dài (quốc lộ, vành đai) có thể kéo dài hàng chục km
     * làm sai lệch vị trí khảo sát mới.
     */
    fun findMatchingCachedZone(targetGps: GpsCoordinate, zoom: Int): CachedVectorRoadZone? {
        val lat = targetGps.latitude
        val lon = targetGps.longitude
        if (lat == 0.0 && lon == 0.0) return null

        // Chỉ coi là khớp cache nếu khoảng cách từ toạ độ đích đến tâm vùng đã lưu rất gần (<= 75 mét)
        // và mức zoom chênh lệch không quá 1 (để đảm bảo mật độ chi tiết đường sá tương thích)
        for (zone in cachedZones) {
            if (kotlin.math.abs(zone.zoom - zoom) > 1) continue
            val dist = calculateDistanceMeters(lat, lon, zone.centerLat, zone.centerLon)
            if (dist <= 75.0 && zone.roads.isNotEmpty()) {
                return zone
            }
        }

        return null
    }

    /**
     * Lưu vùng đường mới tải vào bộ nhớ đệm
     */
    fun saveZoneToCache(centerGps: GpsCoordinate, zoom: Int, roads: List<VectorRoad>): CachedVectorRoadZone {
        val latDelta = when {
            zoom >= 20 -> 0.0025
            zoom == 19 -> 0.0035
            zoom == 18 -> 0.0050
            else -> 0.0070
        }
        val cosLat = cos(centerGps.latitude * PI / 180.0).coerceAtLeast(0.1)
        val lonDelta = latDelta / cosLat

        val defaultSouth = centerGps.latitude - latDelta
        val defaultNorth = centerGps.latitude + latDelta
        val defaultWest = centerGps.longitude - lonDelta
        val defaultEast = centerGps.longitude + lonDelta

        val radius = when {
            zoom >= 20 -> 300.0
            zoom == 19 -> 400.0
            zoom == 18 -> 550.0
            else -> 800.0
        }

        val zone = CachedVectorRoadZone(
            id = "zone_${com.vesodo.khaosat.platform.currentEpochMillis()}",
            centerLat = centerGps.latitude,
            centerLon = centerGps.longitude,
            zoom = zoom,
            minLat = defaultSouth,
            maxLat = defaultNorth,
            minLon = defaultWest,
            maxLon = defaultEast,
            radiusMeters = radius,
            roads = roads,
            timestamp = com.vesodo.khaosat.platform.currentEpochMillis()
        )

        // Tránh trùng lặp zone có toạ độ gần (< 60m)
        cachedZones.removeAll { existing ->
            calculateDistanceMeters(existing.centerLat, existing.centerLon, centerGps.latitude, centerGps.longitude) < 60.0
        }
        cachedZones.add(0, zone)

        if (cachedZones.size > 30) {
            cachedZones.removeAt(cachedZones.lastIndex)
        }

        onZoneCachedListener?.invoke(zone)
        return zone
    }

    fun initCache(zones: List<CachedVectorRoadZone>) {
        cachedZones.clear()
        cachedZones.addAll(zones)
    }

    fun getAllCachedZones(): List<CachedVectorRoadZone> = cachedZones.toList()

    /**
     * Tải dữ liệu vector đường sá kèm báo cáo chi tiết cho Dialog tiến trình kết nối.
     * Tự động kiểm tra bộ nhớ đệm: Nếu toạ độ đã nằm trong phạm vi các tuyến đường đã load trước đó
     * thì tái sử dụng tức thì, không cần tải lại hay gửi truy vấn mạng.
     */
    suspend fun fetchVectorRoadsWithReport(
        centerGps: GpsCoordinate,
        zoom: Int,
        forceRefresh: Boolean = false,
        onProgress: (VectorFetchReport) -> Unit = {}
    ): VectorFetchReport {
        // --- KIỂM TRA BỘ NHỚ ĐỆM PHẠM VI TRƯỚC TIÊN ---
        if (!forceRefresh) {
            val cachedZone = findMatchingCachedZone(centerGps, zoom)
            if (cachedZone != null && cachedZone.roads.isNotEmpty()) {
                val report = VectorFetchReport(
                    status = FetchStatus.SUCCESS,
                    endpoint = "Bộ nhớ đệm (Cache phạm vi)",
                    message = "⚡ Tự động nạp ${cachedZone.roads.size} tuyến đường từ bộ nhớ đã lưu (vị trí nằm trong phạm vi đã tải trước đó, không tốn lưu lượng mạng)!",
                    roads = cachedZone.roads,
                    rawElementCount = cachedZone.roads.size,
                    queryBBox = "Tâm cache: (${cachedZone.centerLat.toString().take(7)}, ${cachedZone.centerLon.toString().take(7)})"
                )
                onProgress(report)
                return report
            }
        }

        // Tính toán bán kính quét (Bounding Box) theo mức zoom
        val latDelta = when {
            zoom >= 20 -> 0.0025 // ~280m
            zoom == 19 -> 0.0035 // ~390m
            zoom == 18 -> 0.0050 // ~550m
            else -> 0.0070       // ~780m
        }
        val cosLat = cos(centerGps.latitude * PI / 180.0).coerceAtLeast(0.1)
        val lonDelta = latDelta / cosLat

        val south = centerGps.latitude - latDelta
        val north = centerGps.latitude + latDelta
        val west = centerGps.longitude - lonDelta
        val east = centerGps.longitude + lonDelta
        val bboxStr = "(${south.toString().take(8)}, ${west.toString().take(8)}) -> (${north.toString().take(8)}, ${east.toString().take(8)})"

        var responseText: String? = null
        var usedEndpoint = ""
        var lastError: String? = null

        // --- BƯỚC 1: Ưu tiên OpenStreetMap Official Map API (XML, tốc độ cao, không bị chặn ở VN) ---
        val osmOfficialUrl = "https://api.openstreetmap.org/api/0.6/map?bbox=$west,$south,$east,$north"
        usedEndpoint = "api.openstreetmap.org"
        onProgress(
            VectorFetchReport(
                status = FetchStatus.CONNECTING,
                endpoint = usedEndpoint,
                message = "Đang kết nối OpenStreetMap API chính thức...",
                queryBBox = bboxStr
            )
        )


        try {
            val xmlResponse = PlatformTileFetcher.fetchText(osmOfficialUrl, null)
            if (!xmlResponse.isNullOrBlank() && xmlResponse.contains("<osm")) {
                responseText = xmlResponse
            }
        } catch (e: Throwable) {
            lastError = e.message ?: "Không thể kết nối OSM API"
        }

        // --- BƯỚC 2: Nếu máy chủ chính không phản hồi, thử lần lượt các Overpass API Mirrors ---
        if (responseText == null) {
            val query = "[out:json][timeout:15];way[highway]($south,$west,$north,$east);out geom;"
            val encodedQuery = "data=" + urlEncode(query)

            for (endpoint in OVERPASS_MIRRORS) {
                usedEndpoint = endpoint
                onProgress(
                    VectorFetchReport(
                        status = FetchStatus.CONNECTING,
                        endpoint = endpoint,
                        message = "Đang kết nối máy chủ Overpass: $endpoint...",
                        queryBBox = bboxStr
                    )
                )

                try {
                    val resp = PlatformTileFetcher.fetchText(endpoint, encodedQuery)
                    if (!resp.isNullOrBlank() && resp.contains("\"elements\"")) {
                        responseText = resp
                        break
                    } else if (!resp.isNullOrBlank() && resp.contains("rate_limited")) {
                        lastError = "Máy chủ bận (Rate limit), chuyển máy chủ tiếp theo..."
                    }
                } catch (e: Throwable) {
                    lastError = e.message ?: "Không thể kết nối"
                }
            }
        }

        if (responseText.isNullOrBlank()) {
            val errReport = VectorFetchReport(
                status = FetchStatus.ERROR,
                endpoint = usedEndpoint,
                message = lastError ?: "Không thể kết nối tới các máy chủ OpenStreetMap API. Vui lòng kiểm tra lại mạng Internet.",
                queryBBox = bboxStr,
                errorMessage = lastError
            )
            onProgress(errReport)
            return errReport
        }

        // --- BƯỚC 3: Xử lý dữ liệu trả về (hỗ trợ cả XML từ OSM và JSON từ Overpass) ---
        return try {
            val trimmed = responseText.trimStart()
            val roads = if (trimmed.startsWith("<")) {
                parseOsmXmlRoads(responseText)
            } else {
                parseOverpassJsonRoads(responseText)
            }

            if (roads.isNotEmpty()) {
                // Tự động lưu vào bộ nhớ đệm phạm vi để các lần sau không cần tải lại
                saveZoneToCache(centerGps, zoom, roads)

                val report = VectorFetchReport(
                    status = FetchStatus.SUCCESS,
                    endpoint = usedEndpoint,
                    message = "Đã tải thành công ${roads.size} tuyến đường từ OpenStreetMap (đã lưu vào cache)!",
                    roads = roads,
                    rawElementCount = roads.size,
                    queryBBox = bboxStr
                )
                onProgress(report)
                report
            } else {

                val report = VectorFetchReport(
                    status = FetchStatus.EMPTY,
                    endpoint = usedEndpoint,
                    message = "Khu vực toạ độ này không có tuyến đường (highway) nào trong OpenStreetMap.",
                    rawElementCount = 0,
                    queryBBox = bboxStr
                )
                onProgress(report)
                report
            }
        } catch (e: Throwable) {
            val errReport = VectorFetchReport(
                status = FetchStatus.ERROR,
                endpoint = usedEndpoint,
                message = "Lỗi xử lý dữ liệu: ${e.message}",
                queryBBox = bboxStr,
                errorMessage = e.message
            )
            onProgress(errReport)
            errReport
        }
    }

    /**
     * Phân tích cú pháp dữ liệu XML chuẩn từ OpenStreetMap API 0.6
     */
    private fun parseOsmXmlRoads(xml: String): List<VectorRoad> {
        val nodes = mutableMapOf<Long, VectorRoadPoint>()
        val roads = mutableListOf<VectorRoad>()

        var inWay = false
        var currentWayId = 0L
        var currentWayHighway: String? = null
        var currentWayName = ""
        val currentWayNodes = mutableListOf<Long>()

        xml.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("<node ")) {
                val id = extractXmlAttr(line, "id")?.toLongOrNull()
                val lat = extractXmlAttr(line, "lat")?.toDoubleOrNull()
                val lon = extractXmlAttr(line, "lon")?.toDoubleOrNull()
                if (id != null && lat != null && lon != null) {
                    nodes[id] = VectorRoadPoint(lat, lon)
                }
            } else if (line.startsWith("<way ")) {
                inWay = true
                currentWayId = extractXmlAttr(line, "id")?.toLongOrNull() ?: 0L
                currentWayHighway = null
                currentWayName = ""
                currentWayNodes.clear()
            } else if (inWay) {
                if (line.startsWith("<nd ")) {
                    val ref = extractXmlAttr(line, "ref")?.toLongOrNull()
                    if (ref != null) currentWayNodes.add(ref)
                } else if (line.startsWith("<tag ")) {
                    val k = extractXmlAttr(line, "k")
                    val v = extractXmlAttr(line, "v") ?: ""
                    if (k == "highway") {
                        currentWayHighway = v
                    } else if (k == "name") {
                        currentWayName = v
                    }
                } else if (line.startsWith("</way>")) {
                    inWay = false
                    val hw = currentWayHighway
                    if (hw != null && hw != "no" && currentWayNodes.size >= 2) {
                        val pts = currentWayNodes.mapNotNull { nodes[it] }
                        if (pts.size >= 2) {
                            roads.add(
                                VectorRoad(
                                    id = currentWayId,
                                    name = currentWayName,
                                    roadType = hw,
                                    points = pts
                                )
                            )
                        }
                    }
                }
            }
        }
        return roads
    }

    private fun extractXmlAttr(line: String, attrName: String): String? {
        val pattern = "$attrName=\""
        val start = line.indexOf(pattern)
        if (start == -1) return null
        val valStart = start + pattern.length
        val end = line.indexOf('"', valStart)
        if (end == -1) return null
        return line.substring(valStart, end)
    }

    /**
     * Phân tích cú pháp dữ liệu JSON từ Overpass API
     */
    private fun parseOverpassJsonRoads(jsonText: String): List<VectorRoad> {
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(jsonText).jsonObject
        val elements = root["elements"]?.jsonArray ?: return emptyList()

        val roads = mutableListOf<VectorRoad>()
        for (elem in elements) {
            val obj = elem.jsonObject
            if (obj["type"]?.jsonPrimitive?.content == "way") {
                val id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                val tags = obj["tags"]?.jsonObject
                val highway = tags?.get("highway")?.jsonPrimitive?.content ?: continue
                val name = tags["name"]?.jsonPrimitive?.content ?: ""

                val geomArray = obj["geometry"]?.jsonArray
                if (geomArray != null && geomArray.size >= 2) {
                    val points = geomArray.mapNotNull { gElem ->
                        val gObj = gElem.jsonObject
                        val lat = gObj["lat"]?.jsonPrimitive?.content?.toDoubleOrNull()
                        val lon = gObj["lon"]?.jsonPrimitive?.content?.toDoubleOrNull()
                        if (lat != null && lon != null) VectorRoadPoint(lat, lon) else null
                    }
                    if (points.size >= 2) {
                        roads.add(
                            VectorRoad(
                                id = id,
                                name = name,
                                roadType = highway,
                                points = points
                            )
                        )
                    }
                }
            }
        }
        return roads
    }

    /**
     * URL encoding thuần Kotlin (Multiplatform)
     */
    private fun urlEncode(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when (ch) {
                in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.', '~' -> sb.append(ch)
                ' ' -> sb.append('+')
                else -> {
                    val bytes = ch.toString().encodeToByteArray()
                    for (b in bytes) {
                        val hex = (b.toInt() and 0xFF).toString(16).uppercase()
                        sb.append('%')
                        if (hex.length == 1) sb.append('0')
                        sb.append(hex)
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Tải dữ liệu vector đường sá thực tế quanh toạ độ GPS từ OpenStreetMap
     */
    suspend fun fetchVectorRoads(
        centerGps: GpsCoordinate,
        zoom: Int
    ): List<VectorRoad> {
        val report = fetchVectorRoadsWithReport(centerGps, zoom)
        return report.roads
    }

    /**
     * Chuyển đổi toạ độ GPS (lat, lon) sang toạ độ hiển thị trên màn hình Canvas CAD
     */
    fun gpsToScreen(
        lat: Double,
        lon: Double,
        centerGps: GpsCoordinate,
        zoom: Int,
        canvasWidth: Float,
        canvasHeight: Float,
        panOffsetX: Float,
        panOffsetY: Float
    ): Offset {
        val n = 1 shl zoom
        val worldSize = 256.0 * n

        val centerWorldX = (centerGps.longitude + 180.0) / 360.0 * worldSize
        val centerLatRad = centerGps.latitude * PI / 180.0
        val centerWorldY = (1.0 - ln(tan(centerLatRad) + 1.0 / cos(centerLatRad)) / PI) / 2.0 * worldSize

        val pointWorldX = (lon + 180.0) / 360.0 * worldSize
        val pointLatRad = lat * PI / 180.0
        val pointWorldY = (1.0 - ln(tan(pointLatRad) + 1.0 / cos(pointLatRad)) / PI) / 2.0 * worldSize

        val anchorX = canvasWidth / 2f + panOffsetX
        val anchorY = canvasHeight / 2f + panOffsetY

        val screenX = anchorX + (pointWorldX - centerWorldX).toFloat()
        val screenY = anchorY + (pointWorldY - centerWorldY).toFloat()

        return Offset(screenX, screenY)
    }

    /**
     * Vẽ các tuyến đường vector và tên đường sắc nét trực tiếp lên Canvas CAD
     */
    fun renderVectorRoads(
        drawScope: DrawScope,
        vectorBasemap: VectorBasemap,
        canvasSize: Size,
        panOffsetX: Float,
        panOffsetY: Float,
        textMeasurer: TextMeasurer
    ) {
        try {
            val roads = vectorBasemap.roads
            if (roads.isEmpty()) return

            val zoom = vectorBasemap.zoom
            val centerGps = vectorBasemap.centerGps

            // Chuyển toàn bộ toạ độ sang screen coordinates
            val roadScreenPaths = roads.map { road ->
                road to road.points.map { pt ->
                    gpsToScreen(
                        lat = pt.lat,
                        lon = pt.lon,
                        centerGps = centerGps,
                        zoom = zoom,
                        canvasWidth = canvasSize.width,
                        canvasHeight = canvasSize.height,
                        panOffsetX = panOffsetX,
                        panOffsetY = panOffsetY
                    )
                }
            }

            // --- LƯỢT 1: VẼ VIỀN ĐƯỜNG (ROAD CASING) ---
            roadScreenPaths.forEach { (road, pts) ->
                val isMajor = road.roadType in listOf("primary", "secondary", "trunk", "motorway")
                val isService = road.roadType in listOf("service", "footway", "path", "living_street")
                val casingWidth = if (isMajor) 15f else if (isService) 8f else 11f
                val casingColor = if (isMajor) Color(0xFF94A3B8) else Color(0xFFCBD5E1)

                for (i in 0 until pts.size - 1) {
                    val p1 = pts[i]
                    val p2 = pts[i + 1]
                    // Vẽ các đoạn trong phạm vi hiển thị mở rộng của Canvas
                    if ((p1.x in -5000f..(canvasSize.width + 5000f) && p1.y in -5000f..(canvasSize.height + 5000f)) ||
                        (p2.x in -5000f..(canvasSize.width + 5000f) && p2.y in -5000f..(canvasSize.height + 5000f))
                    ) {
                        drawScope.drawLine(
                            color = casingColor,
                            start = p1,
                            end = p2,
                            strokeWidth = casingWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // --- LƯỢT 2: VẼ LÒNG ĐƯỜNG NỘI DUNG (ROAD SURFACE) ---
            roadScreenPaths.forEach { (road, pts) ->
                val isMajor = road.roadType in listOf("primary", "secondary", "trunk", "motorway")
                val isService = road.roadType in listOf("service", "footway", "path", "living_street")
                val innerWidth = if (isMajor) 12f else if (isService) 5.5f else 8.5f
                val innerColor = if (isMajor) Color(0xFFFFFBEB) else Color.White

                for (i in 0 until pts.size - 1) {
                    val p1 = pts[i]
                    val p2 = pts[i + 1]
                    if ((p1.x in -5000f..(canvasSize.width + 5000f) && p1.y in -5000f..(canvasSize.height + 5000f)) ||
                        (p2.x in -5000f..(canvasSize.width + 5000f) && p2.y in -5000f..(canvasSize.height + 5000f))
                    ) {
                        drawScope.drawLine(
                            color = innerColor,
                            start = p1,
                            end = p2,
                            strokeWidth = innerWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // --- LƯỢT 3: VẼ TIM ĐƯỜNG NÉT ĐỨT (ROAD CENTERLINE) CHO ĐƯỜNG LỚN ---
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            roadScreenPaths.forEach { (road, pts) ->
                val isMajor = road.roadType in listOf("primary", "secondary", "trunk", "motorway")
                if (isMajor) {
                    for (i in 0 until pts.size - 1) {
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        if ((p1.x in -5000f..(canvasSize.width + 5000f) && p1.y in -5000f..(canvasSize.height + 5000f)) ||
                            (p2.x in -5000f..(canvasSize.width + 5000f) && p2.y in -5000f..(canvasSize.height + 5000f))
                        ) {
                            drawScope.drawLine(
                                color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                                start = p1,
                                end = p2,
                                strokeWidth = 1.6f,
                                pathEffect = dashEffect
                            )
                        }
                    }
                }
            }

            // --- LƯỢT 4: VẼ TÊN ĐƯỜNG SẮC NÉT (ROAD NAMES) ---
            val drawnNames = mutableSetOf<String>()
            roadScreenPaths.forEach { (road, pts) ->
                if (road.name.isNotBlank() && !drawnNames.contains(road.name)) {
                    // Tìm đoạn dài nhất trên màn hình để đặt tên đường
                    var bestP1 = pts.first()
                    var bestP2 = pts.last()
                    var maxLen = 0f

                    for (i in 0 until pts.size - 1) {
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        val len = hypot(p2.x - p1.x, p2.y - p1.y)
                        if (len > maxLen) {
                            maxLen = len
                            bestP1 = p1
                            bestP2 = p2
                        }
                    }

                    // Chỉ vẽ nhãn nếu đoạn đường đủ dài (>= 70px)
                    if (maxLen >= 70f) {
                        val mid = Offset((bestP1.x + bestP2.x) / 2f, (bestP1.y + bestP2.y) / 2f)

                        // Chỉ vẽ nếu tâm nhãn nằm trong vùng nhìn thấy mở rộng của Canvas
                        if (mid.x in -5000f..(canvasSize.width + 5000f) && mid.y in -5000f..(canvasSize.height + 5000f)) {
                            drawnNames.add(road.name)
                            var angleDeg = (atan2(bestP2.y - bestP1.y, bestP2.x - bestP1.x) * 180.0 / PI).toFloat()

                            // Giữ chữ luôn xuôi mắt, không bị lộn ngược
                            if (angleDeg > 90f) angleDeg -= 180f
                            else if (angleDeg < -90f) angleDeg += 180f

                            val textLayout = textMeasurer.measure(
                                text = road.name,
                                style = TextStyle(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            )

                            val padH = 6f
                            val padV = 2.5f
                            val badgeW = textLayout.size.width + padH * 2
                            val badgeH = textLayout.size.height + padV * 2

                            drawScope.withTransform({
                                rotate(degrees = angleDeg, pivot = mid)
                            }) {
                                // Nền trắng viền xám nhẹ sau tên đường
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.92f),
                                    topLeft = Offset(mid.x - badgeW / 2f, mid.y - badgeH / 2f),
                                    size = Size(badgeW, badgeH),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                                drawRoundRect(
                                    color = Color(0xFF94A3B8).copy(alpha = 0.6f),
                                    topLeft = Offset(mid.x - badgeW / 2f, mid.y - badgeH / 2f),
                                    size = Size(badgeW, badgeH),
                                    cornerRadius = CornerRadius(4f, 4f),
                                    style = Stroke(width = 0.8f)
                                )
                                // Vẽ chữ an toàn tuyệt đối với textLayoutResult đã đo sẵn
                                drawText(
                                    textLayoutResult = textLayout,
                                    topLeft = Offset(mid.x - badgeW / 2f + padH, mid.y - badgeH / 2f + padV)
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            // Không bao giờ để lỗi vẽ làm crash Compose AWT thread
        }
    }
}
