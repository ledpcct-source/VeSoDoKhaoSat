package com.vesodo.khaosat.map

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlin.math.*

@Serializable
data class GpsCoordinate(
    val latitude: Double,
    val longitude: Double,
    val name: String = ""
)


enum class MapTileProvider(
    val id: String,
    val title: String,
    val description: String,
    val urlTemplate: String,
    val isGrayscale: Boolean = false,
    val attribution: String = "© OpenStreetMap contributors"
) {
    ESRI_STREET_GRAY(
        id = "esri_street_gray",
        title = "ESRI Street Map (Đơn sắc Xám - Ổn định nhất ⭐)",
        description = "Server toàn cầu của ESRI, 100% Free không cần API Key, không bị lỗi 403, chi tiết đường sá ngõ hẻm",
        urlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}",
        isGrayscale = true,
        attribution = "Tiles © Esri — Source: Esri, USGS"
    ),
    ESRI_GRAY_BASE(
        id = "esri_gray",
        title = "ESRI Light Gray Base (Chuẩn Kỹ Thuật)",
        description = "Bản đồ kỹ thuật GIS của ESRI, sắc độ xám nhẹ, 100% Free không cần API Key",
        urlTemplate = "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}",
        isGrayscale = false,
        attribution = "Tiles © Esri — Source: Esri, DeLorme"
    ),
    OSM_GRAYSCALE(
        id = "osm_gray",
        title = "OpenStreetMap Thô (Đơn sắc Xám)",
        description = "OpenStreetMap cộng đồng, giới hạn 2 luồng tải tuân thủ Policy",
        urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        isGrayscale = true,
        attribution = "© OpenStreetMap contributors"
    ),
    CARTO_POSITRON_NO_LABELS(
        id = "carto_positron",
        title = "CARTO Positron (Nền Trắng Siêu Nhẹ)",
        description = "Nền trắng ngà và nét xám tối giản, 100% Free không cần API Key",
        urlTemplate = "https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
        isGrayscale = false,
        attribution = "© OpenStreetMap contributors, © CARTO"
    ),
    OSM_STANDARD(
        id = "osm_std",
        title = "OpenStreetMap Nguyên Bản (Có Màu)",
        description = "Bản đồ màu nguyên gốc, hiển thị rõ sông xanh và đường sá",
        urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        isGrayscale = false,
        attribution = "© OpenStreetMap contributors"
    )
}

object GpsBasemapManager {

    val PRESET_CAN_THO_NINH_KIEU = GpsCoordinate(10.034185, 105.787617, "TP. Cần Thơ (Bến Ninh Kiều)")
    val PRESET_CAN_THO_CAI_RANG = GpsCoordinate(10.002875, 105.753825, "TP. Cần Thơ (Cái Răng)")
    val PRESET_HCM_Q1 = GpsCoordinate(10.776889, 106.700806, "TP. Hồ Chí Minh (Bến Thành)")
    val PRESET_HA_NOI = GpsCoordinate(21.028511, 105.854444, "Hà Nội (Hồ Gươm)")

    val ALL_PRESETS = listOf(
        PRESET_CAN_THO_NINH_KIEU,
        PRESET_CAN_THO_CAI_RANG,
        PRESET_HCM_Q1,
        PRESET_HA_NOI
    )

    // Bộ nhớ đệm lưu tile bitmap đã tải
    val tileMemoryCache = mutableStateMapOf<String, ImageBitmap>()
    private val loadingKeys = mutableSetOf<String>()
    private val downloadSemaphore = kotlinx.coroutines.sync.Semaphore(2)

    data class TilePosition(
        val tileX: Int,
        val tileY: Int,
        val fetchZoom: Int,
        val screenOffset: Offset,
        val drawSize: Float = 256f
    )

    fun lon2tile(lon: Double, zoom: Int): Int {
        val n = 1 shl zoom
        return floor((lon + 180.0) / 360.0 * n).toInt()
    }

    fun lat2tile(lat: Double, zoom: Int): Int {
        val n = 1 shl zoom
        val latRad = lat * PI / 180.0
        return floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n).toInt()
    }

    /**
     * Dịch chuyển toạ độ GPS tâm khi người dùng kéo rê chuột (dx, dy tính bằng pixel)
     */
    fun panCenter(
        currentCenter: GpsCoordinate,
        dxPixels: Float,
        dyPixels: Float,
        zoom: Int
    ): GpsCoordinate {
        val n = 1 shl zoom
        val worldSize = 256.0 * n

        val centerWorldX = (currentCenter.longitude + 180.0) / 360.0 * worldSize
        val latRad = currentCenter.latitude * PI / 180.0
        val centerWorldY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * worldSize

        // Kéo chuột sang phải (dx > 0) -> bản đồ dịch phải -> toạ độ tâm dịch trái (-dx)
        val newWorldX = centerWorldX - dxPixels
        val newWorldY = centerWorldY - dyPixels

        val newLon = (newWorldX / worldSize) * 360.0 - 180.0
        val newLatRad = atan(sinh(PI * (1.0 - 2.0 * (newWorldY / worldSize))))
        val newLat = newLatRad * 180.0 / PI

        return GpsCoordinate(
            latitude = newLat.coerceIn(-85.05112878, 85.05112878),
            longitude = ((newLon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0,
            name = currentCenter.name
        )
    }

    /**
     * Tính toán danh sách các tile cần hiển thị quanh toạ độ GPS tâm
     * Hỗ trợ Overzoom (Phóng đại kỹ thuật số) vượt ngưỡng native zoom 19 lên đến 22x
     */
    fun calculateVisibleTiles(
        centerGps: GpsCoordinate,
        zoom: Int,
        canvasCenterScreen: Offset,
        canvasWidth: Float,
        canvasHeight: Float
    ): List<TilePosition> {
        val maxNativeZoom = 19
        val effectiveFetchZoom = zoom.coerceAtMost(maxNativeZoom)
        val overzoomFactor = 1 shl (zoom - effectiveFetchZoom).coerceAtLeast(0)
        val tileSize = 256f * overzoomFactor

        val n = 1 shl effectiveFetchZoom
        val centerTileXExact = (centerGps.longitude + 180.0) / 360.0 * n
        val latRad = centerGps.latitude * PI / 180.0
        val centerTileYExact = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n

        val centerTileX = floor(centerTileXExact).toInt()
        val centerTileY = floor(centerTileYExact).toInt()

        val fracX = centerTileXExact - centerTileX
        val fracY = centerTileYExact - centerTileY

        // Bán kính tile cần vẽ phủ kín màn hình
        val radiusX = ceil(canvasWidth / (tileSize * 2)).toInt() + 1
        val radiusY = ceil(canvasHeight / (tileSize * 2)).toInt() + 1

        val result = mutableListOf<TilePosition>()

        for (dx in -radiusX..radiusX) {
            for (dy in -radiusY..radiusY) {
                val tx = centerTileX + dx
                val ty = centerTileY + dy

                val screenX = canvasCenterScreen.x + (dx - fracX) * tileSize
                val screenY = canvasCenterScreen.y + (dy - fracY) * tileSize

                result.add(
                    TilePosition(
                        tileX = tx,
                        tileY = ty,
                        fetchZoom = effectiveFetchZoom,
                        screenOffset = Offset(screenX.toFloat(), screenY.toFloat()),
                        drawSize = tileSize
                    )
                )
            }
        }

        return result
    }

    /**
     * Xóa cache bản đồ hiện tại (được gọi khi đổi nhà cung cấp hoặc làm mới)
     */
    fun clearCache() {
        tileMemoryCache.clear()
        loadingKeys.clear()
    }

    /**
     * Tải bất đồng bộ các tile chưa có trong cache
     */
    fun preloadTiles(
        scope: CoroutineScope,
        tiles: List<TilePosition>,
        zoom: Int,
        provider: MapTileProvider
    ) {
        tiles.forEach { tile ->
            val cacheKey = "${provider.id}_${tile.fetchZoom}_${tile.tileX}_${tile.tileY}"
            if (!tileMemoryCache.containsKey(cacheKey) && !loadingKeys.contains(cacheKey)) {
                loadingKeys.add(cacheKey)
                val url = provider.urlTemplate
                    .replace("{z}", tile.fetchZoom.toString())
                    .replace("{x}", tile.tileX.toString())
                    .replace("{y}", tile.tileY.toString())

                scope.launch(Dispatchers.IO) {
                    try {
                        downloadSemaphore.withPermit {
                            val bytes = PlatformTileFetcher.fetchTileBytes(url)
                            if (bytes != null && bytes.size > 800) {
                                val bitmap = PlatformBitmapDecoder.decode(bytes)
                                if (bitmap != null) {
                                    tileMemoryCache[cacheKey] = bitmap
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        // Bỏ qua nếu lỗi mạng để không làm sập ứng dụng
                    } finally {
                        loadingKeys.remove(cacheKey)
                    }
                }
            }
        }
    }

    /**
     * Ghép và chụp các tile bản đồ trong khung nhìn thành một bức ảnh tĩnh ImageBitmap làm nền cho bản vẽ
     */
    fun captureMapSnapshot(
        width: Int,
        height: Int,
        centerGps: GpsCoordinate,
        zoom: Int,
        provider: MapTileProvider
    ): ImageBitmap {
        val safeWidth = width.coerceAtLeast(256)
        val safeHeight = height.coerceAtLeast(256)
        val bitmap = ImageBitmap(safeWidth, safeHeight)
        val canvas = Canvas(bitmap)
        val centerScreen = Offset(safeWidth / 2f, safeHeight / 2f)

        val tiles = calculateVisibleTiles(
            centerGps = centerGps,
            zoom = zoom,
            canvasCenterScreen = centerScreen,
            canvasWidth = safeWidth.toFloat(),
            canvasHeight = safeHeight.toFloat()
        )

        val paint = Paint()
        if (provider.isGrayscale) {
            paint.colorFilter = ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0f) }
            )
        }

        tiles.forEach { tile ->
            val cacheKey = "${provider.id}_${tile.fetchZoom}_${tile.tileX}_${tile.tileY}"
            val tileBmp = tileMemoryCache[cacheKey]
            if (tileBmp != null) {
                if (tile.drawSize == 256f) {
                    canvas.drawImage(tileBmp, tile.screenOffset, paint)
                } else {
                    canvas.drawImageRect(
                        image = tileBmp,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(tileBmp.width, tileBmp.height),
                        dstOffset = IntOffset(tile.screenOffset.x.toInt(), tile.screenOffset.y.toInt()),
                        dstSize = IntSize(tile.drawSize.toInt(), tile.drawSize.toInt()),
                        paint = paint
                    )
                }
            }
        }

        return bitmap
    }
}
