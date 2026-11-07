package com.vesodo.khaosat.export

import java.awt.Desktop
import java.io.File
import java.net.URI

actual object PlatformFileOpener {

    actual fun createTempDxfFile(dxfContent: String, prefix: String): String? {
        return try {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "VeSoDoCAD")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, "${prefix}${System.currentTimeMillis()}.dxf")
            tempFile.writeText(dxfContent, Charsets.UTF_8)
            tempFile.deleteOnExit()
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun createTempHtmlFile(htmlContent: String, prefix: String): String? {
        return try {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "VeSoDoCAD")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, "${prefix}${System.currentTimeMillis()}.html")
            tempFile.writeText(htmlContent, Charsets.UTF_8)
            tempFile.deleteOnExit()
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun openFile(filePath: String, mimeType: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file)
                true
            } else {
                // Windows fallback
                val os = System.getProperty("os.name").lowercase()
                if (os.contains("win")) {
                    ProcessBuilder("cmd", "/c", "start", "", file.absolutePath).start()
                    true
                } else if (os.contains("mac")) {
                    ProcessBuilder("open", file.absolutePath).start()
                    true
                } else {
                    ProcessBuilder("xdg-open", file.absolutePath).start()
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun openUrl(url: String): Boolean {
        return try {
            val uri = if (url.startsWith("http://") || url.startsWith("https://")) {
                URI(url)
            } else {
                File(url).toURI()
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri)
                true
            } else {
                val os = System.getProperty("os.name").lowercase()
                if (os.contains("win")) {
                    ProcessBuilder("cmd", "/c", "start", "", uri.toString()).start()
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun openFolder(folderOrFilePath: String): Boolean {
        return try {
            val file = File(folderOrFilePath)
            val os = System.getProperty("os.name").lowercase()
            if (os.contains("win")) {
                if (file.isFile && file.exists()) {
                    // Mở Windows File Explorer và bôi sáng (highlight/select) trực tiếp file vừa lưu
                    ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
                } else {
                    val folder = if (file.isDirectory) file else file.parentFile ?: file
                    ProcessBuilder("explorer.exe", folder.absolutePath).start()
                }
                true
            } else if (os.contains("mac")) {
                if (file.isFile && file.exists()) {
                    ProcessBuilder("open", "-R", file.absolutePath).start()
                } else {
                    val folder = if (file.isDirectory) file else file.parentFile ?: file
                    ProcessBuilder("open", folder.absolutePath).start()
                }
                true
            } else {
                val folder = if (file.isDirectory) file else file.parentFile ?: file
                ProcessBuilder("xdg-open", folder.absolutePath).start()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
