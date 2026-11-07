package com.vesodo.khaosat.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume

@Composable
actual fun rememberPlatformLocationHandler(): PlatformLocationHandler {
    val context = LocalContext.current
    var permissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val granted = fineGranted || coarseGranted
        permissionCallback?.invoke(granted)
        permissionCallback = null
    }

    return remember(context, launcher) {
        object : PlatformLocationHandler {
            override val requiresPermissionRationale: Boolean = true

            override fun isPermissionGranted(): Boolean {
                val fine = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarse = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                return fine || coarse
            }

            override fun requestPermission(onResult: (Boolean) -> Unit) {
                if (isPermissionGranted()) {
                    onResult(true)
                    return
                }
                permissionCallback = onResult
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            override suspend fun getCurrentLocation(): UserLocationResult {
                return fetchAndroidLocation(context)
            }
        }
    }
}

private val jsonParser = Json { ignoreUnknownKeys = true }

@SuppressLint("MissingPermission")
private suspend fun fetchAndroidLocation(context: Context): UserLocationResult = withContext(Dispatchers.IO) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext fallbackIpLocation("Không tìm thấy dịch vụ định vị LocationManager")

        val isGpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Throwable) { false }
        val isNetworkEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Throwable) { false }

        // 1. Kiểm tra vị trí đã lưu gần nhất (Last Known Location)
        var bestLocation: Location? = null
        try {
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation!!.accuracy) {
                    bestLocation = loc
                }
            }
        } catch (_: SecurityException) {
            return@withContext UserLocationResult(
                latitude = 0.0,
                longitude = 0.0,
                errorMessage = "Thiếu quyền truy cập vị trí GPS. Vui lòng cấp quyền trong Cài đặt."
            )
        }

        // Nếu có vị trí gần nhất còn mới (< 30 giây) và độ chính xác tốt (< 40m), trả về ngay tức thì
        if (bestLocation != null) {
            val ageMs = System.currentTimeMillis() - bestLocation!!.time
            if (ageMs < 30_000 && bestLocation!!.accuracy <= 40f) {
                return@withContext UserLocationResult(
                    latitude = bestLocation!!.latitude,
                    longitude = bestLocation!!.longitude,
                    accuracyMeters = bestLocation!!.accuracy,
                    providerName = if (bestLocation!!.provider == LocationManager.GPS_PROVIDER) "GPS Vệ tinh" else "Mạng Di động / WiFi"
                )
            }
        }

        // 2. Nếu GPS và Network đều tắt và không có lastKnownLocation, báo người dùng bật GPS
        if (!isGpsEnabled && !isNetworkEnabled && bestLocation == null) {
            return@withContext fallbackIpLocation("Dịch vụ GPS trên điện thoại đang tắt. Vui lòng bật vị trí trong Cài đặt máy.")
        }

        // 3. Yêu cầu cập nhật vị trí trực tiếp từ GPS / Network với thời gian chờ 7 giây
        val freshLocation = withTimeoutOrNull(7000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        try {
                            locationManager.removeUpdates(this)
                        } catch (_: Throwable) {}
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                cont.invokeOnCancellation {
                    try {
                        locationManager.removeUpdates(listener)
                    } catch (_: Throwable) {}
                }

                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post {
                    var requested = false
                    try {
                        if (isGpsEnabled) {
                            locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                0L,
                                0f,
                                listener,
                                Looper.getMainLooper()
                            )
                            requested = true
                        }
                        if (isNetworkEnabled) {
                            locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                0L,
                                0f,
                                listener,
                                Looper.getMainLooper()
                            )
                            requested = true
                        }
                    } catch (e: SecurityException) {
                        if (cont.isActive) cont.resume(null)
                        return@post
                    }

                    if (!requested && cont.isActive) {
                        cont.resume(null)
                    }
                }
            }
        }

        val finalLoc = freshLocation ?: bestLocation
        if (finalLoc != null) {
            UserLocationResult(
                latitude = finalLoc.latitude,
                longitude = finalLoc.longitude,
                accuracyMeters = finalLoc.accuracy,
                providerName = if (finalLoc.provider == LocationManager.GPS_PROVIDER) "GPS Vệ tinh" else "Mạng Di động / WiFi"
            )
        } else {
            // Không bắt được sóng GPS (ví dụ ở trong nhà), tự động dự phòng IP Geolocation
            fallbackIpLocation("Không nhận được tín hiệu GPS vệ tinh")
        }
    } catch (e: Throwable) {
        fallbackIpLocation("Lỗi định vị GPS: ${e.message}")
    }
}

private suspend fun fallbackIpLocation(reason: String): UserLocationResult {
    // 1. ipwho.is (hỗ trợ phân dải Tỉnh/Thành Việt Nam chính xác hơn)
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
                return UserLocationResult(
                    latitude = lat,
                    longitude = lon,
                    accuracyMeters = 3000f,
                    providerName = if (locName.isNotBlank()) "Mạng Internet ($locName)" else "Vị trí mạng"
                )
            }
        }
    } catch (_: Throwable) {}

    // 2. Dự phòng ip-api.com
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
                return UserLocationResult(
                    latitude = lat,
                    longitude = lon,
                    accuracyMeters = 5000f,
                    providerName = if (locName.isNotBlank()) "Mạng Internet ($locName)" else "Vị trí mạng"
                )
            }
        }
    } catch (_: Throwable) {}

    return UserLocationResult(
        latitude = 0.0,
        longitude = 0.0,
        errorMessage = "$reason. Vui lòng bật vị trí GPS hoặc kiểm tra kết nối mạng."
    )
}
