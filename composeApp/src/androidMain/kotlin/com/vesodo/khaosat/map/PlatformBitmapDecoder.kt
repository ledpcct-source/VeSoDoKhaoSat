package com.vesodo.khaosat.map

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual object PlatformBitmapDecoder {
    actual fun decode(bytes: ByteArray): ImageBitmap? {
        return try {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bmp?.asImageBitmap()
        } catch (e: Throwable) {
            null
        }
    }
}
