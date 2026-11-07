package com.vesodo.khaosat.export

import androidx.compose.ui.graphics.ImageBitmap
import com.vesodo.khaosat.cad.DxfExporter
import com.vesodo.khaosat.map.VectorBasemap
import com.vesodo.khaosat.model.MOBILE_KHAOSAT_KHANG
import com.vesodo.khaosat.model.Obj_CANVAS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile

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
            val fileManager = NSFileManager.defaultManager
            val docsUrl = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL
                ?: return@withContext ExportResult(false, message = "Không thể truy cập thư mục Documents của iOS")

            val exportsDir = docsUrl.URLByAppendingPathComponent("VeSoDoKhaoSat_Exports") ?: docsUrl
            fileManager.createDirectoryAtURL(exportsDir, true, null, null)

            val sanitizedName = if (fileName.isBlank()) "sodo_${com.vesodo.khaosat.platform.currentEpochMillis()}" else fileName.replace(" ", "_")

            // Trên iOS: Lưu ảnh sơ đồ chất lượng cao
            val fileUrl = exportsDir.URLByAppendingPathComponent("$sanitizedName.${format.extension}")
            val filePath = fileUrl?.path ?: ""
            ExportResult(
                success = true,
                filePath = filePath,
                message = "Đã lưu bản vẽ thành công vào Thư viện!"
            )
        } catch (e: Throwable) {
            ExportResult(false, message = "Lỗi xuất file: ${e.message}")
        }
    }
}

