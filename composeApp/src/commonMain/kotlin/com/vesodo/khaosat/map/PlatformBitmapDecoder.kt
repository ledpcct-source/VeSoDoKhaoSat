package com.vesodo.khaosat.map

import androidx.compose.ui.graphics.ImageBitmap

expect object PlatformBitmapDecoder {
    fun decode(bytes: ByteArray): ImageBitmap?
}
