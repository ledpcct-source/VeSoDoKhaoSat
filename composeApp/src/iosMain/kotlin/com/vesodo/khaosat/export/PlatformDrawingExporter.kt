@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.vesodo.khaosat.export

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.vesodo.khaosat.cad.DxfExporter
import com.vesodo.khaosat.map.VectorBasemap
import com.vesodo.khaosat.model.MOBILE_KHAOSAT_KHANG
import com.vesodo.khaosat.model.Obj_CANVAS
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import kotlin.coroutines.resume

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

            // 1. Trường hợp xuất CAD (.dxf)
            if (format == ExportFormat.DXF) {
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
                val fileUrl = exportsDir.URLByAppendingPathComponent("$sanitizedName.dxf")
                val filePath = fileUrl?.path ?: ""
                if (filePath.isNotBlank()) {
                    (dxfText as NSString).writeToFile(filePath, true, NSUTF8StringEncoding, null)
                }
                return@withContext ExportResult(
                    success = true,
                    filePath = filePath,
                    message = "Đã lưu bản vẽ CAD DXF vào tệp ứng dụng: $sanitizedName.dxf"
                )
            }

            // 2. Trường hợp xuất hình ảnh (.jpg / .png) -> Lưu vào thư viện ảnh iOS (Photos / Cuộn camera)
            if (capturedBitmap == null) {
                return@withContext ExportResult(
                    success = false,
                    message = "Không thể chụp hình sơ đồ khảo sát để xuất ảnh. Vui lòng thử lại!"
                )
            }

            // Chuyển đổi ImageBitmap (Skiko) sang dữ liệu ảnh nén JPEG
            val skiaBitmap = capturedBitmap.asSkiaBitmap()
            val skiaImage = Image.makeFromBitmap(skiaBitmap)
            val encodedData = skiaImage.encodeToData(EncodedImageFormat.JPEG, 95)
                ?: return@withContext ExportResult(false, message = "Không thể nén dữ liệu hình ảnh sơ đồ")

            val bytes = encodedData.bytes

            // Chuyển sang NSData của iOS
            val nsData: NSData = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }

            // Lưu 1 bản sao vào Documents/VeSoDoKhaoSat_Exports để có thể xem lại trong ứng dụng Files (Tệp)
            val fileUrl = exportsDir.URLByAppendingPathComponent("$sanitizedName.${format.extension}")
            val filePath = fileUrl?.path ?: ""
            if (filePath.isNotBlank()) {
                nsData.writeToFile(filePath, true)
            }

            // Khởi tạo đối tượng UIImage
            val uiImage = UIImage.imageWithData(nsData)
                ?: return@withContext ExportResult(false, message = "Không thể tạo đối tượng hình ảnh UIImage từ dữ liệu")

            // Lưu trực tiếp vào Thư viện ảnh iOS (Photos / Gallery)
            val (saveSuccess, saveError) = saveImageToPhotoLibrary(uiImage)
            if (saveSuccess) {
                ExportResult(
                    success = true,
                    filePath = filePath,
                    message = "Đã lưu ảnh sơ đồ vào Thư viện ảnh (Photos) thành công!"
                )
            } else {
                ExportResult(
                    success = false,
                    filePath = filePath,
                    message = saveError ?: "Không thể lưu ảnh vào Thư viện ảnh của thiết bị"
                )
            }
        } catch (e: Throwable) {
            ExportResult(false, message = "Lỗi xuất file: ${e.message}")
        }
    }

    private suspend fun saveImageToPhotoLibrary(image: UIImage): Pair<Boolean, String?> =
        suspendCancellableCoroutine { continuation ->
            val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly)

            fun doPerformSave() {
                PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                    changeBlock = {
                        PHAssetChangeRequest.creationRequestForAssetFromImage(image)
                    },
                    completionHandler = { success, error ->
                        if (success) {
                            if (continuation.isActive) {
                                continuation.resume(Pair(true, null))
                            }
                        } else {
                            // Thử phương thức dự phòng UIImageWriteToSavedPhotosAlbum
                            try {
                                UIImageWriteToSavedPhotosAlbum(image, null, null, null)
                                if (continuation.isActive) {
                                continuation.resume(Pair(true, null))
                            }
                        } catch (_: Throwable) {
                            if (continuation.isActive) {
                                continuation.resume(Pair(false, error?.localizedDescription))
                            }
                        }
                    }
                }
            )
        }

        when (status) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> {
                doPerformSave()
            }
            PHAuthorizationStatusNotDetermined -> {
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { newStatus ->
                    if (newStatus == PHAuthorizationStatusAuthorized || newStatus == PHAuthorizationStatusLimited) {
                        doPerformSave()
                    } else {
                        if (continuation.isActive) {
                            continuation.resume(
                                Pair(false, "Quyền lưu ảnh vào Thư viện đã bị từ chối")
                            )
                        }
                    }
                }
            }
            PHAuthorizationStatusDenied, PHAuthorizationStatusRestricted -> {
                if (continuation.isActive) {
                    continuation.resume(
                        Pair(
                            false,
                            "Ứng dụng chưa được cấp quyền lưu ảnh. Vui lòng mở Cài đặt (Settings) -> Quyền riêng tư -> Ảnh để cấp quyền."
                        )
                    )
                }
            }
            else -> {
                doPerformSave()
            }
        }
    }
}
