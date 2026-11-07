package com.vesodo.khaosat.platform

expect val isAndroidPlatform: Boolean
expect val isDesktopPlatform: Boolean
expect val isIosPlatform: Boolean

expect fun getFormattedCurrentTime(): String

expect fun currentEpochMillis(): Long
