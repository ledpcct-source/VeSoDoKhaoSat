package com.vesodo.khaosat.map

import androidx.compose.runtime.Composable

data class UserLocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val providerName: String = "",
    val errorMessage: String? = null
)

interface PlatformLocationHandler {
    /**
     * Cho biết nền tảng có cần hiển thị giải thích / xin quyền hệ thống hay không (ví dụ Android cần runtime permission)
     */
    val requiresPermissionRationale: Boolean

    /**
     * Kiểm tra xem quyền truy cập vị trí đã được cấp hay chưa
     */
    fun isPermissionGranted(): Boolean

    /**
     * Yêu cầu hệ điều hành cấp quyền vị trí (gọi popup hệ thống trên Android)
     */
    fun requestPermission(onResult: (Boolean) -> Unit)

    /**
     * Lấy tọa độ GPS / vị trí hiện tại của thiết bị
     */
    suspend fun getCurrentLocation(): UserLocationResult
}

@Composable
expect fun rememberPlatformLocationHandler(): PlatformLocationHandler
