@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.vesodo.khaosat.export

import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication

actual object PlatformFileOpener {

    actual fun createTempDxfFile(dxfContent: String, prefix: String): String? {
        return try {
            val tempDir = NSTemporaryDirectory()
            val fileName = "${prefix}${com.vesodo.khaosat.platform.currentEpochMillis()}.dxf"
            val filePath = tempDir + fileName
            (dxfContent as NSString).writeToFile(filePath, true, NSUTF8StringEncoding, null)
            filePath
        } catch (_: Throwable) {
            null
        }
    }

    actual fun createTempHtmlFile(htmlContent: String, prefix: String): String? {
        return try {
            val tempDir = NSTemporaryDirectory()
            val fileName = "${prefix}${com.vesodo.khaosat.platform.currentEpochMillis()}.html"
            val filePath = tempDir + fileName
            (htmlContent as NSString).writeToFile(filePath, true, NSUTF8StringEncoding, null)
            filePath
        } catch (_: Throwable) {
            null
        }
    }

    actual fun openFile(filePath: String, mimeType: String): Boolean {
        return try {
            val url = NSURL.fileURLWithPath(filePath)
            UIApplication.sharedApplication.openURL(url)
            true
        } catch (_: Throwable) {
            false
        }
    }

    actual fun openUrl(url: String): Boolean {
        return try {
            val nsUrl = NSURL.URLWithString(url) ?: return false
            UIApplication.sharedApplication.openURL(nsUrl)
            true
        } catch (_: Throwable) {
            false
        }
    }

    actual fun openFolder(folderOrFilePath: String): Boolean {
        return true
    }
}

