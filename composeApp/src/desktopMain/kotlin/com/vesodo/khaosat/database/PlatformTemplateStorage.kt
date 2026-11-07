package com.vesodo.khaosat.database

import com.vesodo.khaosat.model.Obj_CANVAS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.DriverManager

actual object PlatformTemplateStorage {

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }


    private val dbFile: File by lazy {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".vesodokhaosat")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "mau_canvas.db")
    }

    private fun getConnection() = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

    init {
        try {
            Class.forName("org.sqlite.JDBC")
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS obj_mau_canvas (
                            id_MAU INTEGER PRIMARY KEY AUTOINCREMENT,
                            TEN_MAU TEXT NOT NULL,
                            MA_DVIQLY TEXT,
                            LENH INTEGER,
                            X1 REAL,
                            Y1 REAL,
                            X2 REAL,
                            Y2 REAL,
                            X3 REAL,
                            Y3 REAL,
                            CHU TEXT,
                            HINH INTEGER,
                            THUOC_TINH INTEGER,
                            ROTATION REAL DEFAULT 0,
                            DONG_BO INTEGER DEFAULT 0
                        );
                        """.trimIndent()
                    )
                    try {
                        stmt.executeUpdate("ALTER TABLE obj_mau_canvas ADD COLUMN ROTATION REAL DEFAULT 0;")
                    } catch (_: Exception) {
                        // Column already exists
                    }
                    try {
                        stmt.executeUpdate("ALTER TABLE obj_mau_canvas ADD COLUMN SCALE REAL DEFAULT 1;")
                    } catch (_: Exception) {
                        // Column already exists
                    }
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS app_gps_memory (
                            id INTEGER PRIMARY KEY CHECK (id = 1),
                            LAT REAL NOT NULL,
                            LON REAL NOT NULL,
                            NAME TEXT,
                            ZOOM INTEGER DEFAULT 19
                        );
                        """.trimIndent()
                    )
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS app_ui_preferences (
                            pref_key TEXT PRIMARY KEY,
                            pref_val TEXT NOT NULL
                        );
                        """.trimIndent()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun saveTemplate(
        tenMau: String,
        donVi: String,
        items: List<Obj_CANVAS>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.autoCommit = false
                // Xoá mẫu cũ nếu trùng tên (giống MauCV_Control.delete_mau_canvas_by_mau)
                conn.prepareStatement("DELETE FROM obj_mau_canvas WHERE TEN_MAU = ?").use { ps ->
                    ps.setString(1, tenMau)
                    ps.executeUpdate()
                }

                val sql = """
                    INSERT INTO obj_mau_canvas (TEN_MAU, MA_DVIQLY, LENH, X1, Y1, X2, Y2, X3, Y3, CHU, HINH, THUOC_TINH, ROTATION, SCALE, DONG_BO)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    for (item in items) {
                        ps.setString(1, tenMau)
                        ps.setString(2, donVi)
                        ps.setInt(3, item.LENH)
                        ps.setFloat(4, item.X1)
                        ps.setFloat(5, item.Y1)
                        ps.setFloat(6, item.X2)
                        ps.setFloat(7, item.Y2)
                        ps.setFloat(8, item.X3)
                        ps.setFloat(9, item.Y3)
                        ps.setString(10, item.TEN)
                        ps.setInt(11, item.HINH)
                        ps.setInt(12, item.THUOC_TINH)
                        ps.setFloat(13, item.ROTATION)
                        ps.setFloat(14, if (item.SCALE > 0f) item.SCALE else 1f)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
                conn.commit()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual suspend fun getAllTemplates(): List<SavedTemplate> = withContext(Dispatchers.IO) {
        val result = mutableListOf<SavedTemplate>()
        try {
            getConnection().use { conn ->
                val sqlNames = "SELECT DISTINCT TEN_MAU, MA_DVIQLY FROM obj_mau_canvas ORDER BY TEN_MAU ASC"
                val names = mutableListOf<Pair<String, String>>()
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sqlNames)
                    while (rs.next()) {
                        names.add(Pair(rs.getString("TEN_MAU"), rs.getString("MA_DVIQLY") ?: ""))
                    }
                }

                for ((name, dvi) in names) {
                    val items = getTemplateItemsInternal(conn, name)
                    result.add(SavedTemplate(tenMau = name, donVi = dvi, items = items))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    actual suspend fun getTemplateByName(tenMau: String): List<Obj_CANVAS>? = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val items = getTemplateItemsInternal(conn, tenMau)
                if (items.isNotEmpty()) items else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getTemplateItemsInternal(conn: java.sql.Connection, tenMau: String): List<Obj_CANVAS> {
        val list = mutableListOf<Obj_CANVAS>()
        conn.prepareStatement("SELECT * FROM obj_mau_canvas WHERE TEN_MAU = ? ORDER BY id_MAU ASC").use { ps ->
            ps.setString(1, tenMau)
            val rs = ps.executeQuery()
            while (rs.next()) {
                val rot = try { rs.getFloat("ROTATION") } catch (_: Exception) { 0f }
                val scale = try { rs.getFloat("SCALE") } catch (_: Exception) { 1f }.let { if (it <= 0f) 1f else it }
                list.add(
                    Obj_CANVAS(
                        MA_DVIQLY = rs.getString("MA_DVIQLY") ?: "",
                        STT = rs.getLong("id_MAU"),
                        TEN = rs.getString("CHU") ?: "",
                        LENH = rs.getInt("LENH"),
                        X1 = rs.getFloat("X1"),
                        Y1 = rs.getFloat("Y1"),
                        X2 = rs.getFloat("X2"),
                        Y2 = rs.getFloat("Y2"),
                        X3 = rs.getFloat("X3"),
                        Y3 = rs.getFloat("Y3"),
                        HINH = rs.getInt("HINH"),
                        THUOC_TINH = rs.getInt("THUOC_TINH"),
                        ROTATION = rot,
                        SCALE = scale
                    )
                )
            }
        }
        return list
    }

    actual suspend fun deleteTemplate(tenMau: String): Boolean = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.prepareStatement("DELETE FROM obj_mau_canvas WHERE TEN_MAU = ?").use { ps ->
                    ps.setString(1, tenMau)
                    ps.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual suspend fun saveLastGpsState(gpsState: SavedGpsState): Unit = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val sql = """
                    INSERT OR REPLACE INTO app_gps_memory (id, LAT, LON, NAME, ZOOM)
                    VALUES (1, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setDouble(1, gpsState.latitude)
                    ps.setDouble(2, gpsState.longitude)
                    ps.setString(3, gpsState.name)
                    ps.setInt(4, gpsState.zoom)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadLastGpsState(): SavedGpsState? = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT LAT, LON, NAME, ZOOM FROM app_gps_memory WHERE id = 1")
                    if (rs.next()) {
                        val lat = rs.getDouble("LAT")
                        val lon = rs.getDouble("LON")
                        val name = rs.getString("NAME") ?: ""
                        val zoom = rs.getInt("ZOOM").let { if (it in 10..22) it else 19 }
                        if (lat != 0.0 && lon != 0.0) {
                            return@withContext SavedGpsState(lat, lon, name, zoom)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    actual suspend fun saveHistoryFabPosition(xRatio: Float, yRatio: Float): Unit = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val sql = "INSERT OR REPLACE INTO app_ui_preferences (pref_key, pref_val) VALUES ('history_fab_pos', ?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, "$xRatio,$yRatio")
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadHistoryFabPosition(): Pair<Float, Float>? = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'history_fab_pos'")
                    if (rs.next()) {
                        val raw = rs.getString("pref_val")
                        val parts = raw.split(",")
                        if (parts.size == 2) {
                            val x = parts[0].toFloatOrNull()
                            val y = parts[1].toFloatOrNull()
                            if (x != null && y != null) {
                                return@withContext Pair(x, y)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    actual suspend fun saveLastCanvasSession(session: LastCanvasSession): Unit = withContext(Dispatchers.IO) {
        try {
            val rawJson = jsonHelper.encodeToString(session)
            getConnection().use { conn ->
                val sql = "INSERT OR REPLACE INTO app_ui_preferences (pref_key, pref_val) VALUES ('last_canvas_session', ?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, rawJson)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadLastCanvasSession(): LastCanvasSession? = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'last_canvas_session'")
                    if (rs.next()) {
                        val raw = rs.getString("pref_val")
                        if (!raw.isNullOrBlank()) {
                            return@withContext jsonHelper.decodeFromString<LastCanvasSession>(raw)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    actual suspend fun saveToolUsageCounts(counts: Map<Int, Int>): Unit = withContext(Dispatchers.IO) {
        try {
            val rawJson = jsonHelper.encodeToString(counts)
            getConnection().use { conn ->
                val sql = "INSERT OR REPLACE INTO app_ui_preferences (pref_key, pref_val) VALUES ('tool_usage_counts', ?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, rawJson)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadToolUsageCounts(): Map<Int, Int> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'tool_usage_counts'")
                    if (rs.next()) {
                        val raw = rs.getString("pref_val")
                        if (!raw.isNullOrBlank()) {
                            return@withContext jsonHelper.decodeFromString<Map<Int, Int>>(raw)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyMap()
    }

    actual suspend fun saveCachedVectorRoadZones(zones: List<com.vesodo.khaosat.map.CachedVectorRoadZone>): Unit = withContext(Dispatchers.IO) {
        try {
            val rawJson = jsonHelper.encodeToString(zones)
            getConnection().use { conn ->
                val sql = "INSERT OR REPLACE INTO app_ui_preferences (pref_key, pref_val) VALUES ('cached_vector_road_zones', ?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, rawJson)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadCachedVectorRoadZones(): List<com.vesodo.khaosat.map.CachedVectorRoadZone> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'cached_vector_road_zones'")
                    if (rs.next()) {
                        val raw = rs.getString("pref_val")
                        if (!raw.isNullOrBlank()) {
                            return@withContext jsonHelper.decodeFromString<List<com.vesodo.khaosat.map.CachedVectorRoadZone>>(raw)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    actual suspend fun saveQrConfig(config: SavedQrConfig): Unit = withContext(Dispatchers.IO) {
        try {
            val rawJson = jsonHelper.encodeToString(config)
            getConnection().use { conn ->
                val sql = "INSERT OR REPLACE INTO app_ui_preferences (pref_key, pref_val) VALUES ('qr_config', ?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, rawJson)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadQrConfig(): SavedQrConfig? = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'qr_config'")
                    if (rs.next()) {
                        val raw = rs.getString("pref_val")
                        if (!raw.isNullOrBlank()) {
                            return@withContext jsonHelper.decodeFromString<SavedQrConfig>(raw)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    actual suspend fun saveDefaultObjectScale(scale: Float): Unit = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val sql = "INSERT OR REPLACE INTO app_ui_preferences (pref_key, pref_val) VALUES ('default_object_scale', ?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, scale.toString())
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadDefaultObjectScale(): Float? = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'default_object_scale'")
                    if (rs.next()) {
                        val raw = rs.getString("pref_val")
                        return@withContext raw?.toFloatOrNull()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    actual suspend fun saveUnitConfig(config: SavedSurveyUnitConfig): Unit = withContext(Dispatchers.IO) {
        try {
            val rawJson = jsonHelper.encodeToString(config)
            getConnection().use { conn ->
                val sql = "INSERT OR REPLACE INTO app_ui_preferences (pref_key, pref_val) VALUES ('survey_unit_config', ?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, rawJson)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadUnitConfig(): SavedSurveyUnitConfig? = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'survey_unit_config'")
                    if (rs.next()) {
                        val raw = rs.getString("pref_val")
                        if (!raw.isNullOrBlank()) {
                            return@withContext jsonHelper.decodeFromString<SavedSurveyUnitConfig>(raw)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}


