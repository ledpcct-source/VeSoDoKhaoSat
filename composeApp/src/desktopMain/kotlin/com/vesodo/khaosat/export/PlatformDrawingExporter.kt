@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.vesodo.khaosat.export

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import com.vesodo.khaosat.drawing.DrawingRenderer
import com.vesodo.khaosat.math.Function_tinhtoan_ve
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import com.vesodo.khaosat.cad.DxfExporter
import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.map.VectorBasemap
import com.vesodo.khaosat.model.MOBILE_KHAOSAT_KHANG
import vesodokhaosat.composeapp.generated.resources.Res
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

actual object PlatformDrawingExporter {

    actual suspend fun exportDiagram(
        capturedBitmap: ImageBitmap?,
        items: List<Obj_CANVAS>,
        format: ExportFormat,
        fileName: String,
        width: Int,
        height: Int,
        fontSizeSp: Float,
        customer: MOBILE_KHAOSAT_KHANG?,
        vectorBasemap: VectorBasemap?,
        showQrCode: Boolean,
        qrDataString: String?,
        qrScalePercent: Int,
        qrPosition: com.vesodo.khaosat.qr.QrPosition,
        unitConfig: com.vesodo.khaosat.database.SavedSurveyUnitConfig?
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val userHome = System.getProperty("user.home")
            val targetDir = File(userHome, "VeSoDoKhaoSat_Exports")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val sanitizedName = if (fileName.isBlank()) "sodo_${System.currentTimeMillis()}" else fileName.replace(" ", "_")

            // 1. Xuất bản vẽ kỹ thuật CAD (.dxf) tương thích 100% AutoCAD
            if (format == ExportFormat.DXF) {
                val outFile = File(targetDir, "$sanitizedName.dxf")
                val dxfText = DxfExporter.generateDxf(
                    items = items,
                    width = if (width > 0) width else 1920,
                    height = if (height > 0) height else 1080,
                    fontSizeSp = fontSizeSp,
                    customer = customer,
                    vectorBasemap = vectorBasemap,
                    showQrCode = showQrCode,
                    qrDataString = qrDataString,
                    qrScalePercent = qrScalePercent,
                    qrPosition = qrPosition,
                    unitConfig = unitConfig
                )
                outFile.writeText(dxfText, Charsets.UTF_8)

                // Sao chép các file ảnh icon vào thư mục icons/ cùng cấp với file CAD
                copyIconsForCadExport(targetDir)

                // Tự động mở thư mục chứa file vừa lưu trên Desktop (bôi sáng file)
                PlatformFileOpener.openFolder(outFile.absolutePath)
                return@withContext ExportResult(
                    success = true,
                    filePath = outFile.absolutePath,
                    message = "Lưu bản vẽ CAD DXF thành công (kèm thư mục icons): ${outFile.absolutePath}"
                )
            }

            val image: BufferedImage
            val exportWidth: Int
            val exportHeight: Int


            if (capturedBitmap != null) {
                // Xuất trực tiếp ảnh chụp từ GraphicsLayer (WYSIWYG: Bao gồm nền bản đồ, vector, toạ độ, icon, dây điện...)
                val awtImage = capturedBitmap.toAwtImage()
                exportWidth = awtImage.width
                exportHeight = awtImage.height

                image = BufferedImage(exportWidth, exportHeight, BufferedImage.TYPE_INT_RGB)
                val g2d = image.createGraphics()
                g2d.color = Color.WHITE
                g2d.fillRect(0, 0, exportWidth, exportHeight)
                g2d.drawImage(awtImage, 0, 0, null)
                g2d.dispose()
            } else {
                exportWidth = if (width > 0) width else 1920
                exportHeight = if (height > 0) height else kotlin.math.round(exportWidth * 210.0 / 297.0).toInt()

                image = BufferedImage(exportWidth, exportHeight, BufferedImage.TYPE_INT_RGB)
                val g2d = image.createGraphics()

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

                g2d.color = Color.WHITE
                g2d.fillRect(0, 0, exportWidth, exportHeight)

                val solidStroke = BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                val dashedStroke = BasicStroke(
                    3.5f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    10f,
                    floatArrayOf(16f, 12f),
                    0f
                )

                g2d.stroke = solidStroke
                g2d.font = Font("SansSerif", Font.BOLD, (fontSizeSp * 1.5f).toInt())

            // Vẽ lines
            for (cv in items) {
                g2d.color = Color.BLACK
                when (cv.LENH) {
                    Variables.VE_DUONG_THANG -> {
                        g2d.stroke = solidStroke
                        g2d.drawLine(cv.X1.toInt(), cv.Y1.toInt(), cv.X3.toInt(), cv.Y3.toInt())
                        drawAwtLabel(g2d, cv.TEN, cv.X1, cv.Y1, cv.X3, cv.Y3)
                    }
                    Variables.VE_LINE_DUT_NET -> {
                        g2d.stroke = dashedStroke
                        g2d.drawLine(cv.X1.toInt(), cv.Y1.toInt(), cv.X3.toInt(), cv.Y3.toInt())
                        drawAwtLabel(g2d, cv.TEN, cv.X1, cv.Y1, cv.X3, cv.Y3)
                    }
                    Variables.VE_2LINE -> {
                        g2d.stroke = solidStroke
                        if (abs(cv.Y3 - cv.Y1) < abs(cv.X3 - cv.X1)) {
                            g2d.drawLine(cv.X1.toInt(), (cv.Y1 - 12).toInt(), cv.X3.toInt(), (cv.Y3 - 12).toInt())
                            g2d.drawLine(cv.X1.toInt(), (cv.Y1 + 12).toInt(), cv.X3.toInt(), (cv.Y3 + 12).toInt())
                        } else {
                            g2d.drawLine((cv.X1 - 12).toInt(), cv.Y1.toInt(), (cv.X3 - 12).toInt(), cv.Y3.toInt())
                            g2d.drawLine((cv.X1 + 12).toInt(), cv.Y1.toInt(), (cv.X3 + 12).toInt(), cv.Y3.toInt())
                        }
                        drawAwtLabel(g2d, cv.TEN, cv.X1, cv.Y1, cv.X3, cv.Y3)
                    }
                    Variables.VE_MUI_TEN -> {
                        g2d.stroke = solidStroke
                        g2d.drawLine(cv.X1.toInt(), cv.Y1.toInt(), cv.X3.toInt(), cv.Y3.toInt())
                        val arrow = Function_tinhtoan_ve.TINH_MUI_TEN(cv.X1, cv.Y1, cv.X3, cv.Y3, 22)
                        g2d.drawLine(arrow.wing1.x.toInt(), arrow.wing1.y.toInt(), cv.X3.toInt(), cv.Y3.toInt())
                        g2d.drawLine(arrow.wing2.x.toInt(), arrow.wing2.y.toInt(), cv.X3.toInt(), cv.Y3.toInt())
                        drawAwtLabel(g2d, cv.TEN, cv.X1, cv.Y1, cv.X3, cv.Y3)
                    }
                }
            }

            // Vẽ icons / nodes
            for (cv in items) {
                if (cv.LENH == Variables.VE_TEXT) {
                    g2d.color = Color.BLACK
                    g2d.drawString(cv.TEN, cv.X3.toInt(), cv.Y3.toInt())
                } else if (!DrawingRenderer.isLineCommand(cv.LENH)) {
                    g2d.color = Color(33, 150, 243)
                    g2d.fillOval((cv.X3 - 8).toInt(), (cv.Y3 - 8).toInt(), 16, 16)
                    g2d.color = Color.DARK_GRAY
                    g2d.drawOval((cv.X3 - 8).toInt(), (cv.Y3 - 8).toInt(), 16, 16)
                    if (cv.TEN.isNotBlank()) {
                        g2d.drawString(cv.TEN, (cv.X3 - 15).toInt(), (cv.Y3 + 28).toInt())
                    }
                }
            }

                // Đường viền bo cong trang trí nghệ thuật
                val borderInset = 8
                val cornerRad = 20
                g2d.stroke = BasicStroke(2.5f)
                g2d.color = Color(30, 41, 59) // Slate-800
                g2d.drawRoundRect(borderInset, borderInset, exportWidth - borderInset * 2, exportHeight - borderInset * 2, cornerRad, cornerRad)

                val innerInset = borderInset + 4
                val innerRad = cornerRad - 6
                g2d.stroke = BasicStroke(1.0f)
                g2d.color = Color(148, 163, 184) // Slate-400
                g2d.drawRoundRect(innerInset, innerInset, exportWidth - innerInset * 2, exportHeight - innerInset * 2, innerRad, innerRad)

                g2d.dispose()
            }

            if (format == ExportFormat.JPG) {
                val outFile = File(targetDir, "$sanitizedName.jpg")
                ImageIO.write(image, "JPEG", outFile)
                PlatformFileOpener.openFolder(outFile.absolutePath)
                ExportResult(
                    success = true,
                    filePath = outFile.absolutePath,
                    message = "Lưu file JPG thành công: ${outFile.absolutePath}"
                )
            } else {
                val outFile = File(targetDir, "$sanitizedName.pdf")
                val document = PDDocument()
                val page = PDPage(PDRectangle(exportWidth.toFloat(), exportHeight.toFloat()))
                document.addPage(page)

                val baos = ByteArrayOutputStream()
                ImageIO.write(image, "JPEG", baos)
                val pdImage = JPEGFactory.createFromByteArray(document, baos.toByteArray())

                val contentStream = PDPageContentStream(document, page)
                contentStream.drawImage(pdImage, 0f, 0f, exportWidth.toFloat(), exportHeight.toFloat())
                contentStream.close()

                document.save(outFile)
                document.close()
                PlatformFileOpener.openFolder(outFile.absolutePath)
                ExportResult(
                    success = true,
                    filePath = outFile.absolutePath,
                    message = "Lưu file PDF thành công: ${outFile.absolutePath}"
                )
            }
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = "",
                message = "Lỗi khi lưu file: ${e.message}"
            )
        }
    }

    private fun drawAwtLabel(
        g2d: java.awt.Graphics2D,
        text: String,
        x1: Float,
        y1: Float,
        x3: Float,
        y3: Float
    ) {
        if (text.isBlank()) return
        val midX = (x1 + x3) / 2f
        val midY = (y1 + y3) / 2f
        g2d.color = Color.RED
        if (abs(y3 - y1) > abs(x3 - x1)) {
            g2d.drawString(text, (midX + 15).toInt(), midY.toInt())
        } else {
            g2d.drawString(text, midX.toInt(), (midY - 15).toInt())
        }
    }

    private suspend fun copyIconsForCadExport(targetDir: File) {
        try {
            val iconsDir = File(targetDir, "icons")
            if (!iconsDir.exists()) {
                iconsDir.mkdirs()
            }

            // Sao chép toàn bộ biểu tượng icon sang thư mục icons/ cùng cấp với file DXF
            val iconFiles = IconRegistry.ALL_TOOLS.mapNotNull { it.iconFileName }.distinct()
            for (fileName in iconFiles) {
                val destFile = File(iconsDir, fileName)
                if (!destFile.exists() || destFile.length() == 0L) {
                    val bytes: ByteArray? = try {
                        Res.readBytes("drawable/$fileName")
                    } catch (_: Throwable) {
                        PlatformDrawingExporter::class.java.classLoader
                            ?.getResourceAsStream("composeResources/vesodokhaosat.composeapp.generated.resources/drawable/$fileName")
                            ?.use { it.readBytes() }
                    }
                    if (bytes != null && bytes.isNotEmpty()) {
                        destFile.writeBytes(bytes)
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
