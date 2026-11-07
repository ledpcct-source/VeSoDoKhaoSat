package com.vesodo.khaosat.export

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.vesodo.khaosat.drawing.DrawingRenderer
import com.vesodo.khaosat.math.Function_tinhtoan_ve
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import com.vesodo.khaosat.cad.DxfExporter
import com.vesodo.khaosat.map.VectorBasemap
import com.vesodo.khaosat.model.MOBILE_KHAOSAT_KHANG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
            val sanitizedName = if (fileName.isBlank()) "sodo_${System.currentTimeMillis()}" else fileName.replace(" ", "_")
            val context = AndroidAppContext.appContext

            val bitmap: Bitmap
            val exportWidth: Int
            val exportHeight: Int

            if (capturedBitmap != null) {
                bitmap = capturedBitmap.asAndroidBitmap()
                exportWidth = bitmap.width
                exportHeight = bitmap.height
            } else {
                exportWidth = if (width > 0) width else 1920
                exportHeight = if (height > 0) height else kotlin.math.round(exportWidth * 210.0 / 297.0).toInt()

                // 1. Render elements onto an Android Bitmap
                bitmap = Bitmap.createBitmap(exportWidth, exportHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                val linePaint = Paint().apply {
                    color = Color.BLACK
                    strokeWidth = 4f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }

                val dashedPaint = Paint().apply {
                    color = Color.BLACK
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
                    isAntiAlias = true
                }

                val textPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = fontSizeSp * 1.8f
                    isAntiAlias = true
                }

                val nodePaint = Paint().apply {
                    color = Color.rgb(2, 132, 199)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                for (cv in items) {
                    when {
                        cv.LENH == Variables.VE_DUONG_THANG -> {
                            canvas.drawLine(cv.X1, cv.Y1, cv.X3, cv.Y3, linePaint)
                            drawTextLabel(canvas, cv.TEN, cv.X1, cv.Y1, cv.X3, cv.Y3, textPaint)
                        }
                        cv.LENH == Variables.VE_LINE_DUT_NET -> {
                            canvas.drawLine(cv.X1, cv.Y1, cv.X3, cv.Y3, dashedPaint)
                            drawTextLabel(canvas, cv.TEN, cv.X1, cv.Y1, cv.X3, cv.Y3, textPaint)
                        }
                        cv.LENH == Variables.VE_2LINE -> {
                            val offset = 10f
                            canvas.drawLine(cv.X1, cv.Y1 - offset, cv.X3, cv.Y3 - offset, linePaint)
                            canvas.drawLine(cv.X1, cv.Y1 + offset, cv.X3, cv.Y3 + offset, linePaint)
                            drawTextLabel(canvas, cv.TEN, cv.X1, cv.Y1, cv.X3, cv.Y3, textPaint)
                        }
                        cv.LENH == Variables.VE_MUI_TEN || cv.LENH == Variables.VE_ARROW -> {
                            canvas.drawLine(cv.X1, cv.Y1, cv.X3, cv.Y3, linePaint)
                            val arrow = Function_tinhtoan_ve.TINH_MUI_TEN(cv.X1, cv.Y1, cv.X3, cv.Y3, 24)
                            canvas.drawLine(arrow.wing1.x, arrow.wing1.y, cv.X3, cv.Y3, linePaint)
                            canvas.drawLine(arrow.wing2.x, arrow.wing2.y, cv.X3, cv.Y3, linePaint)
                            drawTextLabel(canvas, cv.TEN, cv.X1, cv.Y1, cv.X3, cv.Y3, textPaint)
                        }
                        cv.LENH == Variables.VE_TEXT -> {
                            val lines = cv.TEN.split("\n")
                            var curY = cv.Y3
                            for (line in lines) {
                                canvas.drawText(line, cv.X3, curY, textPaint)
                                curY += textPaint.textSize * 1.25f
                            }
                        }
                        else -> {
                            // Icon biểu tượng 1 toạ độ
                            val scale = if (cv.SCALE > 0f) cv.SCALE else 1f
                            val radius = 18f * scale
                            canvas.drawCircle(cv.X3, cv.Y3, radius, nodePaint)
                            if (cv.TEN.isNotBlank()) {
                                canvas.drawText(cv.TEN, cv.X3 - 15f, cv.Y3 + 28f, textPaint)
                            }
                        }
                    }
                }

                // Đường viền bo cong trang trí nghệ thuật bên ngoài
                val borderInset = 8f
                val cornerRad = 18f
                val borderPaint = Paint().apply {
                    color = Color.rgb(30, 41, 59)
                    strokeWidth = 2.5f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                val innerBorderPaint = Paint().apply {
                    color = Color.rgb(148, 163, 184)
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawRoundRect(borderInset, borderInset, exportWidth - borderInset, exportHeight - borderInset, cornerRad, cornerRad, borderPaint)
                val innerInset = borderInset + 4f
                canvas.drawRoundRect(innerInset, innerInset, exportWidth - innerInset, exportHeight - innerInset, cornerRad - 4f, cornerRad - 4f, innerBorderPaint)
            }

            // TRƯỜNG HỢP A: XUẤT HÌNH ẢNH (JPG) -> LƯU VÀO GALLERY QUA MEDIASTORE (ZERO PERMISSION)
            if (format == ExportFormat.JPG) {
                val resolver = context?.contentResolver
                if (resolver != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "$sanitizedName.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VeSoDoKhaoSat")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)

                        return@withContext ExportResult(
                            success = true,
                            filePath = "Bộ sưu tập (Pictures/VeSoDoKhaoSat/$sanitizedName.jpg)",
                            message = "Đã lưu ảnh sơ đồ vào Bộ sưu tập (Gallery) thành công!"
                        )
                    }
                }

                // Fallback cho Android cũ hoặc khi không có MediaStore Q
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val outDir = File(picturesDir, "VeSoDoKhaoSat")
                if (!outDir.exists()) outDir.mkdirs()
                val outFile = File(outDir, "$sanitizedName.jpg")
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                }
                if (context != null) {
                    MediaScannerConnection.scanFile(context, arrayOf(outFile.absolutePath), arrayOf("image/jpeg"), null)
                }
                return@withContext ExportResult(
                    success = true,
                    filePath = outFile.absolutePath,
                    message = "Đã lưu ảnh sơ đồ vào Bộ sưu tập (Gallery) thành công!"
                )
            }

            // TRƯỜNG HỢP B: NẾU GỌI DXF HOẶC PDF (Lưu an toàn vào app files dir, không cần quyền)
            val appDocDir = context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: File(context?.filesDir, "export")
            if (!appDocDir.exists()) appDocDir.mkdirs()

            if (format == ExportFormat.DXF) {
                val outFile = File(appDocDir, "$sanitizedName.dxf")
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
                return@withContext ExportResult(
                    success = true,
                    filePath = outFile.absolutePath,
                    message = "Lưu bản vẽ CAD DXF thành công: ${outFile.name}"
                )
            } else {
                val outFile = File(appDocDir, "$sanitizedName.pdf")
                val pdfDoc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(exportWidth, exportHeight, 1).create()
                val page = pdfDoc.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDoc.finishPage(page)
                FileOutputStream(outFile).use { fos ->
                    pdfDoc.writeTo(fos)
                }
                pdfDoc.close()
                return@withContext ExportResult(
                    success = true,
                    filePath = outFile.absolutePath,
                    message = "Lưu file PDF thành công: ${outFile.name}"
                )
            }
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = "",
                message = "Lỗi lưu file: ${e.message}"
            )
        }
    }

    private fun drawTextLabel(
        canvas: Canvas,
        text: String,
        x1: Float,
        y1: Float,
        x3: Float,
        y3: Float,
        textPaint: Paint
    ) {
        if (text.isBlank()) return
        val midX = (x1 + x3) / 2f
        val midY = (y1 + y3) / 2f
        if (abs(y3 - y1) > abs(x3 - x1)) {
            canvas.drawText(text, midX + 15f, midY, textPaint)
        } else {
            canvas.drawText(text, midX, midY - 15f, textPaint)
        }
    }
}
