@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.vesodo.khaosat.map

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

actual object PlatformTileFetcher {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun fetchTileBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val nsUrl = NSURL.URLWithString(url) ?: return@withContext null
            val data = NSData.dataWithContentsOfURL(nsUrl) ?: return@withContext null
            if (data.length.toInt() <= 800) return@withContext null
            val size = data.length.toInt()
            val byteArray = ByteArray(size)
            byteArray.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
            byteArray
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun fetchText(url: String, postBody: String?): String? = withContext(Dispatchers.IO) {
        try {
            val nsUrl = NSURL.URLWithString(url) ?: return@withContext null
            val data = NSData.dataWithContentsOfURL(nsUrl) ?: return@withContext null
            NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        } catch (_: Throwable) {
            null
        }
    }
}

