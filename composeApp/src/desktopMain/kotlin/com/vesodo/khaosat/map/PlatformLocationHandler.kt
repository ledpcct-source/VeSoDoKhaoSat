package com.vesodo.khaosat.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64
import java.util.concurrent.TimeUnit

@Composable
actual fun rememberPlatformLocationHandler(): PlatformLocationHandler {
    return remember {
        object : PlatformLocationHandler {
            override val requiresPermissionRationale: Boolean = false

            override fun isPermissionGranted(): Boolean = true

            override fun requestPermission(onResult: (Boolean) -> Unit) {
                onResult(true)
            }

            override suspend fun getCurrentLocation(): UserLocationResult {
                return fetchDesktopLocation()
            }
        }
    }
}

private val jsonParser = Json { ignoreUnknownKeys = true }

private suspend fun fetchDesktopLocation(): UserLocationResult = withContext(Dispatchers.IO) {
    var specificError = ""

    // 1. Ưu tiên 1: Định vị chính xác qua Windows Location Service (Wi-Fi Triangulation / GPS) hoặc IPv6 Native
    val winLoc = fetchWindowsNativeLocation()
    if (winLoc != null) {
        if (winLoc.latitude != 0.0 && winLoc.longitude != 0.0) {
            return@withContext winLoc
        }
        if (!winLoc.errorMessage.isNullOrBlank()) {
            specificError = winLoc.errorMessage
        }
    }

    // 2. Ưu tiên 2: Định vị qua HTTP ipwho.is (trả về đúng Tỉnh/Thành thực tế)
    try {
        val text = PlatformTileFetcher.fetchText("https://ipwho.is/")
        if (text != null && text.contains("\"success\":true")) {
            val root = jsonParser.parseToJsonElement(text).jsonObject
            val lat = root["latitude"]?.jsonPrimitive?.doubleOrNull
            val lon = root["longitude"]?.jsonPrimitive?.doubleOrNull
            val city = root["city"]?.jsonPrimitive?.content ?: ""
            val region = root["region"]?.jsonPrimitive?.content ?: ""
            val locName = listOf(city, region).filter { it.isNotBlank() }.joinToString(", ")
            if (lat != null && lon != null) {
                return@withContext UserLocationResult(
                    latitude = lat,
                    longitude = lon,
                    accuracyMeters = 3000f,
                    providerName = if (locName.isNotBlank()) "Mạng Internet ($locName)" else "IP Geolocation"
                )
            }
        }
    } catch (e: Throwable) {
        if (specificError.isBlank()) specificError = "Lỗi kết nối định vị mạng: ${e.message}"
    }

    // 3. Dự phòng 3: ip-api.com
    try {
        val text = PlatformTileFetcher.fetchText("http://ip-api.com/json/?fields=status,message,country,city,regionName,lat,lon")
        if (text != null && text.contains("\"status\":\"success\"")) {
            val root = jsonParser.parseToJsonElement(text).jsonObject
            val lat = root["lat"]?.jsonPrimitive?.doubleOrNull
            val lon = root["lon"]?.jsonPrimitive?.doubleOrNull
            val city = root["city"]?.jsonPrimitive?.content ?: ""
            val region = root["regionName"]?.jsonPrimitive?.content ?: ""
            val locName = listOf(city, region).filter { it.isNotBlank() }.joinToString(", ")
            if (lat != null && lon != null) {
                return@withContext UserLocationResult(
                    latitude = lat,
                    longitude = lon,
                    accuracyMeters = 5000f,
                    providerName = if (locName.isNotBlank()) "Mạng Internet ($locName)" else "IP Geolocation"
                )
            }
        }
    } catch (_: Throwable) {}

    val errorDetail = if (specificError.isNotBlank()) {
        specificError
    } else {
        "Không thể xác định vị trí máy tính. Vui lòng bật 'Location services' trong Windows Settings hoặc kiểm tra kết nối Internet."
    }

    UserLocationResult(
        latitude = 0.0,
        longitude = 0.0,
        errorMessage = errorDetail
    )
}

/**
 * Gọi Windows Native Location Service qua PowerShell GeoCoordinateWatcher
 */
private fun fetchWindowsNativeLocation(): UserLocationResult? {
    val osName = System.getProperty("os.name")?.lowercase() ?: ""
    if (!osName.contains("win")) return null

    return try {
        val d = '$'
        val psScript = buildString {
            append("${d}ProgressPreference = 'SilentlyContinue'; ")
            append("Add-Type -AssemblyName System.Device; ")
            append("${d}w = New-Object System.Device.Location.GeoCoordinateWatcher(1); ")
            append("${d}w.Start(); ")
            append("for (${d}i=0; ${d}i -lt 50; ${d}i++) { ")
            append("  if (${d}w.Status.ToString() -eq 'Ready') { break }; ")
            append("  Start-Sleep -Milliseconds 100 ")
            append("}; ")
            append("${d}loc = ${d}w.Position.Location; ")
            append("${d}status = ${d}w.Status.ToString(); ")
            append("${d}w.Stop(); ")
            append("if (!${d}loc.IsUnknown -and ${d}loc.Latitude -ne 0) { ")
            append("  Write-Output ('SUCCESS:' + ${d}loc.Latitude + ',' + ${d}loc.Longitude + ',' + ${d}loc.HorizontalAccuracy + ',Windows Wi-Fi/GPS') ")
            append("} elseif (${d}status -eq 'Denied' -or ${d}status -eq 'Disabled') { ")
            append("  Write-Output ('STATUS:' + ${d}status) ")
            append("} else { ")
            append("  try { ")
            append("    ${d}r = Invoke-RestMethod -Uri 'https://ipwho.is/' -TimeoutSec 4; ")
            append("    if (${d}r.success) { ")
            append("      Write-Output ('SUCCESS:' + ${d}r.latitude + ',' + ${d}r.longitude + ',3000,Mạng Internet (' + ${d}r.city + ' - ' + ${d}r.region + ')') ")
            append("    } else { ")
            append("      Write-Output ('STATUS:' + ${d}status) ")
            append("    } ")
            append("  } catch { ")
            append("    Write-Output ('STATUS:' + ${d}status) ")
            append("  } ")
            append("}")
        }

        val encoded = Base64.getEncoder().encodeToString(psScript.toByteArray(Charsets.UTF_16LE))
        val process = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded)
            .redirectErrorStream(true)
            .start()

        val reader = process.inputStream.bufferedReader(Charsets.UTF_8)
        var line: String?
        var successResult: UserLocationResult? = null
        var statusStr = ""

        while (reader.readLine().also { line = it } != null) {
            val text = line?.trim().orEmpty()
            if (text.startsWith("SUCCESS:")) {
                val parts = text.substringAfter("SUCCESS:").split(",")
                val lat = parts.getOrNull(0)?.toDoubleOrNull()
                val lon = parts.getOrNull(1)?.toDoubleOrNull()
                val acc = parts.getOrNull(2)?.toFloatOrNull()
                val provider = parts.getOrNull(3)?.trim()?.ifBlank { null } ?: "Windows Location (Wi-Fi/GPS)"
                if (lat != null && lon != null) {
                    successResult = UserLocationResult(
                        latitude = lat,
                        longitude = lon,
                        accuracyMeters = acc ?: 500f,
                        providerName = provider
                    )
                }
            } else if (text.startsWith("STATUS:")) {
                statusStr = text.substringAfter("STATUS:").trim()
            }
        }
        process.waitFor(8, TimeUnit.SECONDS)

        if (successResult != null) {
            successResult
        } else if (statusStr.equals("Denied", ignoreCase = true) || statusStr.equals("Disabled", ignoreCase = true)) {
            UserLocationResult(
                latitude = 0.0,
                longitude = 0.0,
                errorMessage = "Dịch vụ định vị Windows đang bị TẮT (Trạng thái: $statusStr). Vui lòng vào Windows Settings > Privacy & security > Location và Bật 'Location services'."
            )
        } else {
            null
        }
    } catch (_: Throwable) {
        null
    }
}
