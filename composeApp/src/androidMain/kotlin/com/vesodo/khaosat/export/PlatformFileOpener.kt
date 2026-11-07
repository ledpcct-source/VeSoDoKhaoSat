package com.vesodo.khaosat.export

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

actual object PlatformFileOpener {

    actual fun createTempDxfFile(dxfContent: String, prefix: String): String? {
        return try {
            val context = AndroidAppContext.appContext
            val cacheDir = context?.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
            val targetDir = File(cacheDir, "VeSoDoCAD")
            if (!targetDir.exists()) targetDir.mkdirs()

            val tempFile = File(targetDir, "${prefix}${System.currentTimeMillis()}.dxf")
            tempFile.writeText(dxfContent, Charsets.UTF_8)
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun createTempHtmlFile(htmlContent: String, prefix: String): String? {
        return try {
            val context = AndroidAppContext.appContext
            val cacheDir = context?.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
            val targetDir = File(cacheDir, "VeSoDoCAD")
            if (!targetDir.exists()) targetDir.mkdirs()

            val tempFile = File(targetDir, "${prefix}${System.currentTimeMillis()}.html")
            tempFile.writeText(htmlContent, Charsets.UTF_8)
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun openFile(filePath: String, mimeType: String): Boolean {
        val context = AndroidAppContext.appContext ?: return false
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val targetMime = if (file.name.endsWith(".dxf", ignoreCase = true)) {
                "application/dxf"
            } else if (file.name.endsWith(".html", ignoreCase = true) || file.name.endsWith(".htm", ignoreCase = true)) {
                "text/html"
            } else {
                mimeType
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, targetMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Mở bản vẽ bằng...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            // Thử fallback sang MIME type */* nếu application/dxf không có handler
            try {
                val file = File(filePath)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Mở tệp bằng...").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                true
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }
    }

    actual fun openUrl(url: String): Boolean {
        val context = AndroidAppContext.appContext ?: return false
        return try {
            val uri = if (url.startsWith("http://") || url.startsWith("https://")) {
                Uri.parse(url)
            } else {
                val file = File(url)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Mở xem bản vẽ...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun openFolder(folderOrFilePath: String): Boolean {
        // Trên Android không mở File Explorer của PC, ảnh đã được lưu vào Gallery
        return false
    }
}
