@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.vesodo.khaosat.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse

@Composable
actual fun rememberPlatformLocationHandler(): PlatformLocationHandler {
    return remember {
        IosLocationHandler()
    }
}

class IosLocationHandler : PlatformLocationHandler {
    private val locationManager = CLLocationManager()

    override val requiresPermissionRationale: Boolean = false

    override fun isPermissionGranted(): Boolean {
        val status = locationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                status == kCLAuthorizationStatusAuthorizedAlways
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        locationManager.requestWhenInUseAuthorization()
        onResult(isPermissionGranted())
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getCurrentLocation(): UserLocationResult {
        val loc = locationManager.location
        return if (loc != null) {
            val (lat, lon) = loc.coordinate.useContents { latitude to longitude }
            UserLocationResult(
                latitude = lat,
                longitude = lon,
                accuracyMeters = loc.horizontalAccuracy.toFloat(),
                providerName = "Apple CoreLocation"
            )
        } else {
            // Tọa độ mặc định (trung tâm Cần Thơ) nếu chưa có vị trí GPS thực
            UserLocationResult(
                latitude = 10.0333,
                longitude = 105.7833,
                errorMessage = "Đang chờ tín hiệu GPS từ thiết bị iOS..."
            )
        }
    }
}

