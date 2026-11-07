package com.vesodo.khaosat.platform

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.timeIntervalSince1970

actual val isAndroidPlatform: Boolean = false
actual val isDesktopPlatform: Boolean = false
actual val isIosPlatform: Boolean = true

private val timeFormatter: NSDateFormatter by lazy {
    NSDateFormatter().apply {
        dateFormat = "HH:mm:ss.SSS"
        locale = NSLocale.currentLocale
    }
}

actual fun getFormattedCurrentTime(): String {
    return try {
        timeFormatter.stringFromDate(NSDate())
    } catch (_: Throwable) {
        "00:00:00"
    }
}

actual fun currentEpochMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}
