package com.vesodo.khaosat.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual object PlatformBitmapDecoder {
    actual fun decode(bytes: ByteArray): ImageBitmap? {
        return try {
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Throwable) {
            null
        }
    }
}

