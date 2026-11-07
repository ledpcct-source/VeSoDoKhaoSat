package com.vesodo.khaosat.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.vesodo.khaosat.model.Obj_CANVAS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual object PlatformTemplateStorage {

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }


    private var dbHelper: TemplateDbHelper? = null

    fun initialize(context: Context) {
        if (dbHelper == null) {
            dbHelper = TemplateDbHelper(context.applicationContext)
        }
    }

    private class TemplateDbHelper(context: Context) :
        SQLiteOpenHelper(context, "sodo_khaosat.db", null, 2) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
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
                    SCALE REAL DEFAULT 1,
                    DONG_BO INTEGER DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS app_gps_memory (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    LAT REAL NOT NULL,
                    LON REAL NOT NULL,
                    NAME TEXT,
                    ZOOM INTEGER DEFAULT 19
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS app_ui_preferences (
                    pref_key TEXT PRIMARY KEY,
                    pref_val TEXT NOT NULL
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            try {
                db.execSQL("ALTER TABLE obj_mau_canvas ADD COLUMN ROTATION REAL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE obj_mau_canvas ADD COLUMN SCALE REAL DEFAULT 1")
            } catch (_: Exception) {}
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_gps_memory (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        LAT REAL NOT NULL,
                        LON REAL NOT NULL,
                        NAME TEXT,
                        ZOOM INTEGER DEFAULT 19
                    )
                    """.trimIndent()
                )
            } catch (_: Exception) {}
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS app_ui_preferences (pref_key TEXT PRIMARY KEY, pref_val TEXT NOT NULL)")
            } catch (_: Exception) {}
        }

        override fun onOpen(db: SQLiteDatabase) {
            super.onOpen(db)
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS app_ui_preferences (pref_key TEXT PRIMARY KEY, pref_val TEXT NOT NULL)")
            } catch (_: Exception) {}
        }
    }

    actual suspend fun saveTemplate(
        tenMau: String,
        donVi: String,
        items: List<Obj_CANVAS>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.writableDatabase ?: return@withContext false
            db.beginTransaction()
            try {
                db.delete("obj_mau_canvas", "TEN_MAU = ?", arrayOf(tenMau))
                for (item in items) {
                    val values = ContentValues().apply {
                        put("TEN_MAU", tenMau)
                        put("MA_DVIQLY", donVi)
                        put("LENH", item.LENH)
                        put("X1", item.X1)
                        put("Y1", item.Y1)
                        put("X2", item.X2)
                        put("Y2", item.Y2)
                        put("X3", item.X3)
                        put("Y3", item.Y3)
                        put("CHU", item.TEN)
                        put("HINH", item.HINH)
                        put("THUOC_TINH", item.THUOC_TINH)
                        put("ROTATION", item.ROTATION)
                        put("SCALE", if (item.SCALE > 0f) item.SCALE else 1f)
                        put("DONG_BO", 0)
                    }
                    db.insert("obj_mau_canvas", null, values)
                }
                db.setTransactionSuccessful()
                true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual suspend fun getAllTemplates(): List<SavedTemplate> = withContext(Dispatchers.IO) {
        val result = mutableListOf<SavedTemplate>()
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext result
            val cursor = db.rawQuery(
                "SELECT DISTINCT TEN_MAU, MA_DVIQLY FROM obj_mau_canvas ORDER BY TEN_MAU ASC",
                null
            )
            val names = mutableListOf<Pair<String, String>>()
            cursor.use {
                while (it.moveToNext()) {
                    val ten = it.getString(0)
                    val dvi = it.getString(1) ?: ""
                    names.add(Pair(ten, dvi))
                }
            }

            for ((name, dvi) in names) {
                val items = getItemsByMauInternal(db, name)
                result.add(SavedTemplate(tenMau = name, donVi = dvi, items = items))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    actual suspend fun getTemplateByName(tenMau: String): List<Obj_CANVAS>? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext null
            val items = getItemsByMauInternal(db, tenMau)
            if (items.isNotEmpty()) items else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getItemsByMauInternal(db: SQLiteDatabase, tenMau: String): List<Obj_CANVAS> {
        val list = mutableListOf<Obj_CANVAS>()
        val cursor = db.rawQuery(
            "SELECT * FROM obj_mau_canvas WHERE TEN_MAU = ? ORDER BY id_MAU ASC",
            arrayOf(tenMau)
        )
        cursor.use {
            while (it.moveToNext()) {
                val rotIdx = it.getColumnIndex("ROTATION")
                val rot = if (rotIdx >= 0) it.getFloat(rotIdx) else 0f
                val scaleIdx = it.getColumnIndex("SCALE")
                val scale = if (scaleIdx >= 0) it.getFloat(scaleIdx).let { s -> if (s <= 0f) 1f else s } else 1f
                list.add(
                    Obj_CANVAS(
                        MA_DVIQLY = it.getString(it.getColumnIndexOrThrow("MA_DVIQLY")) ?: "",
                        STT = it.getLong(it.getColumnIndexOrThrow("id_MAU")),
                        TEN = it.getString(it.getColumnIndexOrThrow("CHU")) ?: "",
                        LENH = it.getInt(it.getColumnIndexOrThrow("LENH")),
                        X1 = it.getFloat(it.getColumnIndexOrThrow("X1")),
                        Y1 = it.getFloat(it.getColumnIndexOrThrow("Y1")),
                        X2 = it.getFloat(it.getColumnIndexOrThrow("X2")),
                        Y2 = it.getFloat(it.getColumnIndexOrThrow("Y2")),
                        X3 = it.getFloat(it.getColumnIndexOrThrow("X3")),
                        Y3 = it.getFloat(it.getColumnIndexOrThrow("Y3")),
                        HINH = it.getInt(it.getColumnIndexOrThrow("HINH")),
                        THUOC_TINH = it.getInt(it.getColumnIndexOrThrow("THUOC_TINH")),
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
            val db = dbHelper?.writableDatabase ?: return@withContext false
            db.delete("obj_mau_canvas", "TEN_MAU = ?", arrayOf(tenMau)) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual suspend fun saveLastGpsState(gpsState: SavedGpsState): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.writableDatabase ?: return@withContext
            val values = ContentValues().apply {
                put("id", 1)
                put("LAT", gpsState.latitude)
                put("LON", gpsState.longitude)
                put("NAME", gpsState.name)
                put("ZOOM", gpsState.zoom)
            }
            db.insertWithOnConflict("app_gps_memory", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadLastGpsState(): SavedGpsState? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext null
            val cursor = db.rawQuery("SELECT LAT, LON, NAME, ZOOM FROM app_gps_memory WHERE id = 1", null)
            cursor.use {
                if (it.moveToFirst()) {
                    val lat = it.getDouble(0)
                    val lon = it.getDouble(1)
                    val name = it.getString(2) ?: ""
                    val zoom = it.getInt(3).let { z -> if (z in 10..22) z else 19 }
                    if (lat != 0.0 && lon != 0.0) {
                        return@withContext SavedGpsState(lat, lon, name, zoom)
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
            val db = dbHelper?.writableDatabase ?: return@withContext
            val cv = ContentValues().apply {
                put("pref_key", "history_fab_pos")
                put("pref_val", "$xRatio,$yRatio")
            }
            db.insertWithOnConflict("app_ui_preferences", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadHistoryFabPosition(): Pair<Float, Float>? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext null
            val cursor = db.rawQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'history_fab_pos'", null)
            cursor.use {
                if (it.moveToFirst()) {
                    val raw = it.getString(0)
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    actual suspend fun saveLastCanvasSession(session: LastCanvasSession): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.writableDatabase ?: return@withContext
            val rawJson = jsonHelper.encodeToString(session)
            val cv = ContentValues().apply {
                put("pref_key", "last_canvas_session")
                put("pref_val", rawJson)
            }
            db.insertWithOnConflict("app_ui_preferences", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadLastCanvasSession(): LastCanvasSession? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext null
            val cursor = db.rawQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'last_canvas_session'", null)
            cursor.use {
                if (it.moveToFirst()) {
                    val raw = it.getString(0)
                    if (!raw.isNullOrBlank()) {
                        return@withContext jsonHelper.decodeFromString<LastCanvasSession>(raw)
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
            val db = dbHelper?.writableDatabase ?: return@withContext
            val rawJson = jsonHelper.encodeToString(counts)
            val cv = ContentValues().apply {
                put("pref_key", "tool_usage_counts")
                put("pref_val", rawJson)
            }
            db.insertWithOnConflict("app_ui_preferences", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadToolUsageCounts(): Map<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext emptyMap()
            val cursor = db.rawQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'tool_usage_counts'", null)
            cursor.use {
                if (it.moveToFirst()) {
                    val raw = it.getString(0)
                    if (!raw.isNullOrBlank()) {
                        return@withContext jsonHelper.decodeFromString<Map<Int, Int>>(raw)
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
            val db = dbHelper?.writableDatabase ?: return@withContext
            val rawJson = jsonHelper.encodeToString(zones)
            val cv = ContentValues().apply {
                put("pref_key", "cached_vector_road_zones")
                put("pref_val", rawJson)
            }
            db.insertWithOnConflict("app_ui_preferences", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadCachedVectorRoadZones(): List<com.vesodo.khaosat.map.CachedVectorRoadZone> = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext emptyList()
            val cursor = db.rawQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'cached_vector_road_zones'", null)
            cursor.use {
                if (it.moveToFirst()) {
                    val raw = it.getString(0)
                    if (!raw.isNullOrBlank()) {
                        return@withContext jsonHelper.decodeFromString<List<com.vesodo.khaosat.map.CachedVectorRoadZone>>(raw)
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
            val db = dbHelper?.writableDatabase ?: return@withContext
            val rawJson = jsonHelper.encodeToString(config)
            val cv = ContentValues().apply {
                put("pref_key", "qr_config")
                put("pref_val", rawJson)
            }
            db.insertWithOnConflict("app_ui_preferences", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadQrConfig(): SavedQrConfig? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext null
            val cursor = db.rawQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'qr_config'", null)
            cursor.use {
                if (it.moveToFirst()) {
                    val raw = it.getString(0)
                    if (!raw.isNullOrBlank()) {
                        return@withContext jsonHelper.decodeFromString<SavedQrConfig>(raw)
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
            val db = dbHelper?.writableDatabase ?: return@withContext
            val cv = ContentValues().apply {
                put("pref_key", "default_object_scale")
                put("pref_val", scale.toString())
            }
            db.insertWithOnConflict("app_ui_preferences", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadDefaultObjectScale(): Float? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext null
            val cursor = db.rawQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'default_object_scale'", null)
            cursor.use {
                if (it.moveToFirst()) {
                    val raw = it.getString(0)
                    return@withContext raw?.toFloatOrNull()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    actual suspend fun saveUnitConfig(config: SavedSurveyUnitConfig): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.writableDatabase ?: return@withContext
            val rawJson = jsonHelper.encodeToString(config)
            val cv = ContentValues().apply {
                put("pref_key", "survey_unit_config")
                put("pref_val", rawJson)
            }
            db.insertWithOnConflict("app_ui_preferences", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun loadUnitConfig(): SavedSurveyUnitConfig? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper?.readableDatabase ?: return@withContext null
            val cursor = db.rawQuery("SELECT pref_val FROM app_ui_preferences WHERE pref_key = 'survey_unit_config'", null)
            cursor.use {
                if (it.moveToFirst()) {
                    val raw = it.getString(0)
                    if (!raw.isNullOrBlank()) {
                        return@withContext jsonHelper.decodeFromString<SavedSurveyUnitConfig>(raw)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}


