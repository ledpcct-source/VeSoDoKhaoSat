package com.vesodo.khaosat.database

import com.vesodo.khaosat.map.CachedVectorRoadZone
import com.vesodo.khaosat.model.Obj_CANVAS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

actual object PlatformTemplateStorage {

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storageDir: String by lazy {
        val fileManager = NSFileManager.defaultManager
        val docsUrl = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL
        val basePath = docsUrl?.path ?: ""
        val dirPath = "$basePath/VeSoDoData"
        fileManager.createDirectoryAtPath(dirPath, true, null, null)
        dirPath
    }

    private fun readTextFile(fileName: String): String? {
        val filePath = "$storageDir/$fileName"
        return try {
            NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)
        } catch (_: Throwable) {
            null
        }
    }

    private fun writeTextFile(fileName: String, content: String): Boolean {
        val filePath = "$storageDir/$fileName"
        return try {
            (content as NSString).writeToFile(filePath, true, NSUTF8StringEncoding, null)
        } catch (_: Throwable) {
            false
        }
    }

    actual suspend fun saveTemplate(tenMau: String, donVi: String, items: List<Obj_CANVAS>): Boolean = withContext(Dispatchers.IO) {
        try {
            val templates = getAllTemplates().toMutableList()
            templates.removeAll { it.tenMau.equals(tenMau, ignoreCase = true) }
            templates.add(SavedTemplate(tenMau = tenMau, donVi = donVi, items = items))
            val json = jsonHelper.encodeToString(templates)
            writeTextFile("templates.json", json)
        } catch (_: Throwable) {
            false
        }
    }

    actual suspend fun getAllTemplates(): List<SavedTemplate> = withContext(Dispatchers.IO) {
        try {
            val json = readTextFile("templates.json") ?: return@withContext emptyList()
            jsonHelper.decodeFromString<List<SavedTemplate>>(json)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    actual suspend fun getTemplateByName(tenMau: String): List<Obj_CANVAS>? = withContext(Dispatchers.IO) {
        val templates = getAllTemplates()
        templates.firstOrNull { it.tenMau.equals(tenMau, ignoreCase = true) }?.items
    }

    actual suspend fun deleteTemplate(tenMau: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val templates = getAllTemplates().toMutableList()
            val removed = templates.removeAll { it.tenMau.equals(tenMau, ignoreCase = true) }
            if (removed) {
                val json = jsonHelper.encodeToString(templates)
                writeTextFile("templates.json", json)
            }
            removed
        } catch (_: Throwable) {
            false
        }
    }

    actual suspend fun saveLastGpsState(gpsState: SavedGpsState): Unit = withContext(Dispatchers.IO) {
        try {
            val json = jsonHelper.encodeToString(gpsState)
            writeTextFile("last_gps.json", json)
        } catch (_: Throwable) { }
    }

    actual suspend fun loadLastGpsState(): SavedGpsState? = withContext(Dispatchers.IO) {
        try {
            val json = readTextFile("last_gps.json") ?: return@withContext null
            jsonHelper.decodeFromString<SavedGpsState>(json)
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun saveHistoryFabPosition(xRatio: Float, yRatio: Float): Unit = withContext(Dispatchers.IO) {
        try {
            val str = "$xRatio,$yRatio"
            writeTextFile("fab_pos.txt", str)
        } catch (_: Throwable) { }
    }

    actual suspend fun loadHistoryFabPosition(): Pair<Float, Float>? = withContext(Dispatchers.IO) {
        try {
            val str = readTextFile("fab_pos.txt") ?: return@withContext null
            val parts = str.split(",")
            if (parts.size >= 2) {
                Pair(parts[0].toFloat(), parts[1].toFloat())
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun saveLastCanvasSession(session: LastCanvasSession): Unit = withContext(Dispatchers.IO) {
        try {
            val json = jsonHelper.encodeToString(session)
            writeTextFile("last_session.json", json)
        } catch (_: Throwable) { }
    }

    actual suspend fun loadLastCanvasSession(): LastCanvasSession? = withContext(Dispatchers.IO) {
        try {
            val json = readTextFile("last_session.json") ?: return@withContext null
            jsonHelper.decodeFromString<LastCanvasSession>(json)
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun saveToolUsageCounts(counts: Map<Int, Int>): Unit = withContext(Dispatchers.IO) {
        try {
            val json = jsonHelper.encodeToString(counts)
            writeTextFile("tool_usage.json", json)
        } catch (_: Throwable) { }
    }

    actual suspend fun loadToolUsageCounts(): Map<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val json = readTextFile("tool_usage.json") ?: return@withContext emptyMap()
            jsonHelper.decodeFromString<Map<Int, Int>>(json)
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    actual suspend fun saveCachedVectorRoadZones(zones: List<CachedVectorRoadZone>): Unit = withContext(Dispatchers.IO) {
        try {
            val json = jsonHelper.encodeToString(zones)
            writeTextFile("cached_road_zones.json", json)
        } catch (_: Throwable) { }
    }

    actual suspend fun loadCachedVectorRoadZones(): List<CachedVectorRoadZone> = withContext(Dispatchers.IO) {
        try {
            val json = readTextFile("cached_road_zones.json") ?: return@withContext emptyList()
            jsonHelper.decodeFromString<List<CachedVectorRoadZone>>(json)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    actual suspend fun saveQrConfig(config: SavedQrConfig): Unit = withContext(Dispatchers.IO) {
        try {
            val json = jsonHelper.encodeToString(config)
            writeTextFile("qr_config.json", json)
        } catch (_: Throwable) { }
    }

    actual suspend fun loadQrConfig(): SavedQrConfig? = withContext(Dispatchers.IO) {
        try {
            val json = readTextFile("qr_config.json") ?: return@withContext null
            jsonHelper.decodeFromString<SavedQrConfig>(json)
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun saveDefaultObjectScale(scale: Float): Unit = withContext(Dispatchers.IO) {
        try {
            writeTextFile("default_scale.txt", scale.toString())
        } catch (_: Throwable) { }
    }

    actual suspend fun loadDefaultObjectScale(): Float? = withContext(Dispatchers.IO) {
        try {
            readTextFile("default_scale.txt")?.toFloatOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun saveUnitConfig(config: SavedSurveyUnitConfig): Unit = withContext(Dispatchers.IO) {
        try {
            val json = jsonHelper.encodeToString(config)
            writeTextFile("unit_config.json", json)
        } catch (_: Throwable) { }
    }

    actual suspend fun loadUnitConfig(): SavedSurveyUnitConfig? = withContext(Dispatchers.IO) {
        try {
            val json = readTextFile("unit_config.json") ?: return@withContext null
            jsonHelper.decodeFromString<SavedSurveyUnitConfig>(json)
        } catch (_: Throwable) {
            null
        }
    }
}

