package com.vesodo.khaosat.export

import androidx.compose.ui.graphics.ImageBitmap
import com.vesodo.khaosat.map.VectorBasemap
import com.vesodo.khaosat.model.MOBILE_KHAOSAT_KHANG
import com.vesodo.khaosat.model.Obj_CANVAS

enum class ExportFormat(val extension: String, val displayName: String) {
    JPG("jpg", "Hình ảnh JPEG (.jpg)"),
    PDF("pdf", "Tài liệu PDF (.pdf)"),
    DXF("dxf", "Bản vẽ CAD (.dxf - AutoCAD)")
}

data class ExportResult(
    val success: Boolean,
    val filePath: String = "",
    val message: String = ""
)

expect object PlatformDrawingExporter {
    suspend fun exportDiagram(
        capturedBitmap: ImageBitmap?,
        items: List<Obj_CANVAS>,
        format: ExportFormat,
        fileName: String,
        width: Int,
        height: Int,
        fontSizeSp: Float,
        customer: MOBILE_KHAOSAT_KHANG? = null,
        vectorBasemap: VectorBasemap? = null,
        showQrCode: Boolean = true,
        qrDataString: String? = null,
        qrScalePercent: Int = 100,
        qrPosition: com.vesodo.khaosat.qr.QrPosition = com.vesodo.khaosat.qr.QrPosition.TOP_RIGHT,
        unitConfig: com.vesodo.khaosat.database.SavedSurveyUnitConfig? = null
    ): ExportResult
}
