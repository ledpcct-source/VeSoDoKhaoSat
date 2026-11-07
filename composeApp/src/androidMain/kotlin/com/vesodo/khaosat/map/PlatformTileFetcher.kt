package com.vesodo.khaosat.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

actual object PlatformTileFetcher {
    actual suspend fun fetchTileBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "VeSoDoKhaoSatApp/1.0 (https://vesodo.com; contact@vesodo.com)")
            conn.connectTimeout = 7000
            conn.readTimeout = 9000
            if (conn.responseCode == 200) {
                if (conn.getHeaderField("x-blocked") != null) {
                    return@withContext null
                }
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.size > 800) bytes else null
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }

    actual suspend fun fetchText(url: String, postBody: String?): String? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "VeSoDoKhaoSatApp/1.0 (https://vesodo.com; contact@vesodo.com)")
            conn.connectTimeout = 6000
            conn.readTimeout = 12000
            if (postBody != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                conn.outputStream.use { os ->
                    os.write(postBody.toByteArray(Charsets.UTF_8))
                }
            }
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }
}
