package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.export.ExportFormat
import com.vesodo.khaosat.export.ExportResult
import com.vesodo.khaosat.export.PlatformDrawingExporter
import com.vesodo.khaosat.platform.isAndroidPlatform
import com.vesodo.khaosat.platform.isDesktopPlatform
import com.vesodo.khaosat.platform.isIosPlatform
import kotlinx.coroutines.launch

@Composable
fun ExportDialog(
    state: DrawingState,
    defaultFileName: String,
    fontSizeSp: Float,
    onDismiss: () -> Unit,
    onExportComplete: (ExportResult) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(if (isDesktopPlatform) ExportFormat.DXF else ExportFormat.JPG) }
    var fileName by remember { mutableStateOf(defaultFileName.ifBlank { "sodo_khaosat" }) }
    var isExporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = {
            Text(
                text = if (isDesktopPlatform) "Xuất Bản Vẽ Khảo Sát (Desktop)" else if (isIosPlatform) "Lưu Sơ Đồ Vào Thư Viện (iOS)" else "Lưu Sơ Đồ Vào Bộ Sưu Tập",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isDesktopPlatform)
                        "Chọn định dạng muốn xuất và lưu trữ (Hỗ trợ bản vẽ CAD .DXF, hình ảnh và PDF):"
                    else
                        "Ảnh sơ đồ sẽ được lưu trực tiếp với chất lượng cao chuẩn khổ A4 vào Thư viện của thiết bị:",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (!isDesktopPlatform) {
                    // Mobile (Android & Apple iOS): Không có CAD, chỉ hiển thị tùy chọn Lưu ảnh JPG chuẩn A4
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFormat = ExportFormat.JPG },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Hình ảnh JPEG (.jpg) - Chuẩn khổ A4",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    if (isIosPlatform)
                                        "Tỷ lệ chuẩn khổ A4 lưu trực tiếp vào Thư viện ảnh (Photos) của iPhone/iPad"
                                    else
                                        "Tỷ lệ chuẩn khổ giấy A4 (297x210 mm) lưu vào Bộ sưu tập",
                                    fontSize = 11.5.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    // Trên Desktop: Options: CAD (.dxf) - JPG - PDF
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. CAD DXF Option
                        val isDxf = (selectedFormat == ExportFormat.DXF)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedFormat = ExportFormat.DXF },
                            color = if (isDxf) Color(0xFF0284C7).copy(alpha = 0.14f) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Polyline,
                                    contentDescription = null,
                                    tint = if (isDxf) Color(0xFF0284C7) else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Bản vẽ CAD\n(.dxf)",
                                    fontWeight = if (isDxf) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isDxf) Color(0xFF0284C7) else Color.Black,
                                    lineHeight = 15.sp,
                                    maxLines = 2
                                )
                            }
                        }

                        // 2. JPG Option
                        val isJpg = (selectedFormat == ExportFormat.JPG)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedFormat = ExportFormat.JPG },
                            color = if (isJpg) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (isJpg) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Hình ảnh A4\n(.jpg)",
                                    fontWeight = if (isJpg) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isJpg) MaterialTheme.colorScheme.primary else Color.Black,
                                    lineHeight = 15.sp,
                                    maxLines = 2
                                )
                            }
                        }

                        // 3. PDF Option
                        val isPdf = (selectedFormat == ExportFormat.PDF)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedFormat = ExportFormat.PDF },
                            color = if (isPdf) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isPdf) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Tài liệu A4\n(.pdf)",
                                    fontWeight = if (isPdf) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isPdf) MaterialTheme.colorScheme.primary else Color.Black,
                                    lineHeight = 15.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                // Gợi ý thông tin khi chọn CAD DXF (Chỉ dành cho Desktop)
                if (!isAndroidPlatform && selectedFormat == ExportFormat.DXF) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFE0F2FE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚡ Tự động thay thế các điểm vẽ bằng CAD Blocks chuyên ngành, phân lớp Layer, khung tên A4 nền xám nhẹ chống xuyên và mã QR nền trắng tương thích 100% AutoCAD.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0369A1),
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Đơn vị: ${state.unitConfig.tenDvi.ifBlank { state.customer.MA_DVIQLY }} | KTV: ${state.unitConfig.nguoiKhaoSat}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1
                                )
                                TextButton(
                                    onClick = { state.showUnitConfigDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Đổi...", fontSize = 11.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Nút Xem trước bản vẽ CAD A4
                            Button(
                                onClick = { state.showCadPreviewDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF38BDF8)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Xem trước bản vẽ CAD (Khổ A4)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Tên tập tin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (exportError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exportError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Đang kết xuất và lưu tệp...", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isExporting && fileName.isNotBlank(),
                onClick = {
                    isExporting = true
                    exportError = null
                    scope.launch {
                        val capturedBitmap = try {
                            state.captureCanvasBitmap?.invoke()
                        } catch (e: Throwable) {
                            null
                        }
                        val screenW = if (state.canvasSize.width > 0f) state.canvasSize.width else 1920f
                        val screenH = if (state.canvasSize.height > 0f) state.canvasSize.height else 1080f
                        val isPortrait = screenW < screenH
                        val exportW = if (isPortrait) {
                            kotlin.math.round(maxOf(screenH.toInt(), 1920) * 210.0 / 297.0).toInt()
                        } else {
                            maxOf(screenW.toInt(), 1920)
                        }
                        val exportH = if (isPortrait) {
                            maxOf(screenH.toInt(), 1920)
                        } else {
                            kotlin.math.round(exportW * 210.0 / 297.0).toInt()
                        }
                        val qrData = com.vesodo.khaosat.qr.SurveyQrManager.createQrData(state)
                        val qrJson = com.vesodo.khaosat.qr.SurveyQrManager.toJsonString(qrData, pretty = false)
                        val result = PlatformDrawingExporter.exportDiagram(
                            capturedBitmap = capturedBitmap,
                            items = state.listCanvas,
                            format = selectedFormat,
                            fileName = fileName,
                            width = exportW,
                            height = exportH,
                            fontSizeSp = fontSizeSp,
                            customer = state.customer,
                            vectorBasemap = state.vectorBasemap,
                            showQrCode = state.showSurveyQrCode,
                            qrDataString = qrJson,
                            qrScalePercent = state.qrScalePercent,
                            qrPosition = state.qrPosition,
                            unitConfig = state.unitConfig
                        )
                        isExporting = false
                        if (result.success) {
                            if (!isAndroidPlatform && result.filePath.isNotBlank()) {
                                com.vesodo.khaosat.export.PlatformFileOpener.openFolder(result.filePath)
                            }
                            onExportComplete(result)
                        } else {
                            exportError = result.message
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isAndroidPlatform) "Lưu vào Bộ sưu tập" else "Lưu tệp")
            }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !isExporting,
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
