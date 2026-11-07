package com.vesodo.khaosat.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.*

/**
 * Bộ ước tính và tra cứu Tỉnh/Thành phố dựa trên toạ độ GPS.
 * Hỗ trợ 2 tầng:
 * 1. Offline (tức thì 0ms): Tính khoảng cách tới tâm 63 Tỉnh/Thành Việt Nam.
 * 2. Online (chính xác cấp Quận/Huyện/Xã): Gọi Reverse Geocode OpenStreetMap Nominatim.
 */
object GpsProvinceEstimator {

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val addressCache = mutableMapOf<String, String>()

    data class ProvinceCentroid(
        val name: String,
        val lat: Double,
        val lon: Double
    )

    // Bảng toạ độ trọng tâm 63 Tỉnh / Thành phố Việt Nam
    private val VIETNAM_PROVINCES = listOf(
        // Miền Tây Nam Bộ
        ProvinceCentroid("Tỉnh Vĩnh Long", 10.2537, 105.9722),
        ProvinceCentroid("TP. Cần Thơ", 10.0342, 105.7876),
        ProvinceCentroid("Tỉnh Hậu Giang", 9.7844, 105.4701),
        ProvinceCentroid("Tỉnh Đồng Tháp", 10.4578, 105.6322),
        ProvinceCentroid("Tỉnh Tiền Giang", 10.4284, 106.3421),
        ProvinceCentroid("Tỉnh Bến Tre", 10.2415, 106.3756),
        ProvinceCentroid("Tỉnh Trà Vinh", 9.9347, 106.3455),
        ProvinceCentroid("Tỉnh Sóc Trăng", 9.6033, 105.9800),
        ProvinceCentroid("Tỉnh An Giang", 10.5216, 105.1259),
        ProvinceCentroid("Tỉnh Kiên Giang", 10.0125, 105.0809),
        ProvinceCentroid("Tỉnh Bạc Liêu", 9.2941, 105.7278),
        ProvinceCentroid("Tỉnh Cà Mau", 9.1769, 105.1524),
        ProvinceCentroid("Tỉnh Long An", 10.5364, 106.4116),

        // Đông Nam Bộ
        ProvinceCentroid("TP. Hồ Chí Minh", 10.8231, 106.6297),
        ProvinceCentroid("Tỉnh Bình Dương", 11.1603, 106.6521),
        ProvinceCentroid("Tỉnh Đồng Nai", 10.9575, 106.8426),
        ProvinceCentroid("Tỉnh Bà Rịa - Vũng Tàu", 10.4966, 107.1685),
        ProvinceCentroid("Tỉnh Tây Ninh", 11.3134, 106.0989),
        ProvinceCentroid("Tỉnh Bình Phước", 11.7512, 106.9067),

        // Tây Nguyên
        ProvinceCentroid("Tỉnh Lâm Đồng", 11.9404, 108.4376),
        ProvinceCentroid("Tỉnh Đắk Lắk", 12.6667, 108.0383),
        ProvinceCentroid("Tỉnh Đắk Nông", 12.0042, 107.6875),
        ProvinceCentroid("Tỉnh Gia Lai", 13.9833, 108.0000),
        ProvinceCentroid("Tỉnh Kon Tum", 14.3500, 108.0000),

        // Duyên hải Nam Trung Bộ & Bắc Trung Bộ
        ProvinceCentroid("Tỉnh Bình Thuận", 10.9333, 108.1000),
        ProvinceCentroid("Tỉnh Ninh Thuận", 11.5667, 108.9833),
        ProvinceCentroid("Tỉnh Khánh Hòa", 12.2451, 109.1943),
        ProvinceCentroid("Tỉnh Phú Yên", 13.0882, 109.3075),
        ProvinceCentroid("Tỉnh Bình Định", 13.7830, 109.2197),
        ProvinceCentroid("Tỉnh Quảng Ngãi", 15.1205, 108.7923),
        ProvinceCentroid("Tỉnh Quảng Nam", 15.5667, 108.4833),
        ProvinceCentroid("TP. Đà Nẵng", 16.0544, 108.2022),
        ProvinceCentroid("Tỉnh Thừa Thiên Huế", 16.4637, 107.5909),
        ProvinceCentroid("Tỉnh Quảng Trị", 16.8167, 107.1000),
        ProvinceCentroid("Tỉnh Quảng Bình", 17.4833, 106.6000),
        ProvinceCentroid("Tỉnh Hà Tĩnh", 18.3429, 105.9056),
        ProvinceCentroid("Tỉnh Nghệ An", 18.6734, 105.6813),
        ProvinceCentroid("Tỉnh Thanh Hóa", 19.8067, 105.7852),

        // Đồng bằng Sông Hồng & Bắc Bộ
        ProvinceCentroid("Thủ đô Hà Nội", 21.0285, 105.8542),
        ProvinceCentroid("TP. Hải Phòng", 20.8449, 106.6881),
        ProvinceCentroid("Tỉnh Quảng Ninh", 21.0069, 107.2925),
        ProvinceCentroid("Tỉnh Hải Dương", 20.9373, 106.3146),
        ProvinceCentroid("Tỉnh Hưng Yên", 20.6464, 106.0511),
        ProvinceCentroid("Tỉnh Bắc Ninh", 21.1861, 106.0763),
        ProvinceCentroid("Tỉnh Bắc Giang", 21.2731, 106.1946),
        ProvinceCentroid("Tỉnh Vĩnh Phúc", 21.3089, 105.6049),
        ProvinceCentroid("Tỉnh Phú Thọ", 21.3228, 105.2155),
        ProvinceCentroid("Tỉnh Thái Bình", 20.4463, 106.3366),
        ProvinceCentroid("Tỉnh Nam Định", 20.4200, 106.1683),
        ProvinceCentroid("Tỉnh Hà Nam", 20.5453, 105.9122),
        ProvinceCentroid("Tỉnh Ninh Bình", 20.2506, 105.9745),
        ProvinceCentroid("Tỉnh Thái Nguyên", 21.5942, 105.8481),
        ProvinceCentroid("Tỉnh Tuyên Quang", 21.8233, 105.2144),
        ProvinceCentroid("Tỉnh Lạng Sơn", 21.8533, 106.7617),
        ProvinceCentroid("Tỉnh Cao Bằng", 22.6667, 106.2500),
        ProvinceCentroid("Tỉnh Bắc Kạn", 22.1472, 105.8347),
        ProvinceCentroid("Tỉnh Hà Giang", 22.8233, 104.9839),
        ProvinceCentroid("Tỉnh Lào Cai", 22.4856, 103.9707),
        ProvinceCentroid("Tỉnh Yên Bái", 21.7167, 104.9000),
        ProvinceCentroid("Tỉnh Hòa Bình", 20.8133, 105.3383),
        ProvinceCentroid("Tỉnh Sơn La", 21.3256, 103.9189),
        ProvinceCentroid("Tỉnh Điện Biên", 21.3869, 103.0231),
        ProvinceCentroid("Tỉnh Lai Châu", 22.3964, 103.4589)
    )

    /**
     * Ước tính nhanh Tỉnh/Thành phố bằng thuật toán khoảng cách ngắn nhất (Offline 0ms).
     */
    fun estimateProvinceOffline(latitude: Double, longitude: Double): String {
        if (latitude == 0.0 && longitude == 0.0) return ""

        var minDistance = Double.MAX_VALUE
        var bestProvince = ""

        for (p in VIETNAM_PROVINCES) {
            val dLat = (latitude - p.lat) * 111.0
            val dLon = (longitude - p.lon) * 111.0 * cos(latitude * PI / 180.0)
            val distKm = sqrt(dLat * dLat + dLon * dLon)
            if (distKm < minDistance) {
                minDistance = distKm
                bestProvince = p.name
            }
        }

        return if (minDistance <= 120.0) {
            bestProvince
        } else {
            "Việt Nam"
        }
    }

    /**
     * Tra cứu địa chỉ chi tiết trực tuyến qua OSM Nominatim Reverse Geocoding.
     * Trả về chuỗi ngắn gọn dạng: "Tỉnh Vĩnh Long" hoặc "Huyện Bình Minh, Tỉnh Vĩnh Long".
     */
    suspend fun fetchOnlineAddress(latitude: Double, longitude: Double): String? {
        if (latitude == 0.0 && longitude == 0.0) return null

        val cacheKey = "${(latitude * 100).roundToInt()}_${(longitude * 100).roundToInt()}"
        addressCache[cacheKey]?.let { return it }

        return try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json&zoom=10"
            val text = PlatformTileFetcher.fetchText(url) ?: return null
            val root = jsonParser.parseToJsonElement(text).jsonObject
            val addressObj = root["address"]?.jsonObject

            val state = addressObj?.get("state")?.jsonPrimitive?.content
            val county = addressObj?.get("county")?.jsonPrimitive?.content
            val city = addressObj?.get("city")?.jsonPrimitive?.content

            val parts = listOfNotNull(
                county ?: city,
                state
            ).filter { it.isNotBlank() }.distinct()

            val result = if (parts.isNotEmpty()) {
                parts.joinToString(", ")
            } else {
                root["display_name"]?.jsonPrimitive?.content?.split(",")?.take(2)?.joinToString(",")?.trim()
            }

            if (!result.isNullOrBlank()) {
                addressCache[cacheKey] = result
                result
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
