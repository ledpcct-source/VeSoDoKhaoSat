package com.vesodo.khaosat.database

import com.vesodo.khaosat.map.CachedVectorRoadZone
import com.vesodo.khaosat.map.VectorBasemap
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Obj_MAU_CANVAS
import kotlinx.serialization.Serializable

@Serializable
data class SavedTemplate(
    val tenMau: String,
    val donVi: String = "",
    val items: List<Obj_CANVAS>
)

@Serializable
data class SavedGpsState(
    val latitude: Double,
    val longitude: Double,
    val name: String = "",
    val zoom: Int = 19
)

@Serializable
data class LastCanvasSession(
    val items: List<Obj_CANVAS> = emptyList(),
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val zoomScale: Float = 1f,
    val timestamp: Long = 0L,
    val vectorBasemap: VectorBasemap? = null
)

@Serializable
data class SavedQrConfig(
    val scalePercent: Int = 100,
    val positionName: String = "TOP_RIGHT",
    val lastMaTram: String = ""
)

@Serializable
data class SavedSurveyUnitConfig(
    val maDvi: String = "PK",
    val tenDvi: String = "CÔNG TY ĐIỆN LỰC CẦN THƠ",
    val nguoiKhaoSat: String = "KTV Khảo sát",
    val nguoiKiemTra: String = "Tổ trưởng"
)

expect object PlatformTemplateStorage {
    suspend fun saveTemplate(tenMau: String, donVi: String, items: List<Obj_CANVAS>): Boolean
    suspend fun getAllTemplates(): List<SavedTemplate>
    suspend fun getTemplateByName(tenMau: String): List<Obj_CANVAS>?
    suspend fun deleteTemplate(tenMau: String): Boolean
    suspend fun saveLastGpsState(gpsState: SavedGpsState)
    suspend fun loadLastGpsState(): SavedGpsState?
    suspend fun saveHistoryFabPosition(xRatio: Float, yRatio: Float)
    suspend fun loadHistoryFabPosition(): Pair<Float, Float>?
    suspend fun saveLastCanvasSession(session: LastCanvasSession)
    suspend fun loadLastCanvasSession(): LastCanvasSession?
    suspend fun saveToolUsageCounts(counts: Map<Int, Int>)
    suspend fun loadToolUsageCounts(): Map<Int, Int>
    suspend fun saveCachedVectorRoadZones(zones: List<CachedVectorRoadZone>)
    suspend fun loadCachedVectorRoadZones(): List<CachedVectorRoadZone>
    suspend fun saveQrConfig(config: SavedQrConfig)
    suspend fun loadQrConfig(): SavedQrConfig?
    suspend fun saveDefaultObjectScale(scale: Float)
    suspend fun loadDefaultObjectScale(): Float?
    suspend fun saveUnitConfig(config: SavedSurveyUnitConfig)
    suspend fun loadUnitConfig(): SavedSurveyUnitConfig?
}


