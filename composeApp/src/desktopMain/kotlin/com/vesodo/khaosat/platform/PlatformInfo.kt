package com.vesodo.khaosat.platform

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual val isAndroidPlatform: Boolean = false
actual val isDesktopPlatform: Boolean = true
actual val isIosPlatform: Boolean = false

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

actual fun getFormattedCurrentTime(): String {
    return try {
        timeFormat.format(Date())
    } catch (_: Exception) {
        "00:00:00"
    }
}

actual fun currentEpochMillis(): Long = System.currentTimeMillis()
