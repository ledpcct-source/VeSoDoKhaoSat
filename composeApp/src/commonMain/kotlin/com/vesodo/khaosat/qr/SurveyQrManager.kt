package com.vesodo.khaosat.qr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vesodo.khaosat.database.PlatformTemplateStorage
import com.vesodo.khaosat.database.SavedQrConfig
import com.vesodo.khaosat.drawing.DrawingRenderer
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.map.PlatformBitmapDecoder
import com.vesodo.khaosat.math.Function_tinhtoan_ve
import com.vesodo.khaosat.math.Point2D
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import qrcode.QRCode
import kotlin.math.roundToInt

@Serializable
enum class QrPosition(val label: String, val shortName: String) {
    TOP_RIGHT("Phía trên - Bên phải (Mặc định)", "Trên - Phải"),
    TOP_LEFT("Phía trên - Bên trái", "Trên - Trái"),
    BOTTOM_RIGHT("Phía dưới - Bên phải", "Dưới - Phải"),
    BOTTOM_LEFT("Phía dưới - Bên trái", "Dưới - Trái");

    companion object {
        fun fromName(name: String?): QrPosition {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: TOP_RIGHT
        }
    }
}

@Serializable
data class SurveyQrData(
    val ma_tram: String,
    val ten_khach_hang: String,
    val lat: Double,
    val lon: Double,
    val so_tru: String
)

object SurveyQrManager {

    private val jsonHelper = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private val jsonPrettyHelper = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val qrCache = mutableMapOf<String, ImageBitmap>()

    /**
     * Tạo đối tượng SurveyQrData từ DrawingState hiện tại.
     * Số trụ được tự động nội suy từ nhãn của trụ điện có dây nối liên kết với icon Khách hàng hoặc Công tơ.
     */
    fun createQrData(state: DrawingState): SurveyQrData {
        val maTram = state.customer.MA_TRAM.ifBlank { "TRAM_01" }
        val tenKh = state.customer.TEN_KHANG_KS.ifBlank { "Khách Hàng Khảo Sát" }
        val lat = (state.gpsCenter.latitude * 1000000.0).roundToInt() / 1000000.0
        val lon = (state.gpsCenter.longitude * 1000000.0).roundToInt() / 1000000.0

        // Ưu tiên:
        // 1. Nếu người dùng nhập customSoTru thì dùng customSoTru
        // 2. Nội suy tìm nhãn của trụ nối với Khách hàng / Công tơ qua dây
        // 3. Dự phòng tổng số lượng trụ
        val soTru = if (state.customSoTru.isNotBlank()) {
            state.customSoTru
        } else {
            interpolateConnectedPole(state.listCanvas).ifBlank {
                val poleCount = state.getPolesCount()
                if (poleCount > 0) poleCount.toString() else "Chưa có trụ"
            }
        }

        return SurveyQrData(
            ma_tram = maTram,
            ten_khach_hang = tenKh,
            lat = lat,
            lon = lon,
            so_tru = soTru
        )
    }

    /**
     * Thuật toán tự động tìm trụ điện có liên kết dây dẫn với Khách hàng hoặc Công tơ.
     * Duyệt qua danh sách các đường nối (Line, Arrow, 2Line) để truy vết điểm mút nối với Nhà/Công tơ,
     * sau đó tìm trụ điện nằm tại đầu mút còn lại.
     */
    fun interpolateConnectedPole(items: List<Obj_CANVAS>): String {
        val customerOrMeter = items.firstOrNull { cv ->
            cv.LENH == Variables.VE_NHA ||
            cv.LENH == Variables.VE_HINH_CONGTO ||
            cv.LENH == Variables.VE_CONGTO_TREO_TRU_BTLT_12_HHUU ||
            cv.LENH == Variables.VE_CONGTO_TREO_TRU_BTV_65_HHUU ||
            cv.LENH == Variables.VE_CONGTO_TREO_TRU_BTV_65_SLAP
        } ?: return ""

        val custPos = Point2D(customerOrMeter.X3, customerOrMeter.Y3)
        val lines = items.filter { DrawingRenderer.isLineCommand(it.LENH) }
        val poles = items.filter { IconRegistry.isTru(it.LENH) }
        if (poles.isEmpty()) return ""

        val threshold = 35f // Bán kính tiếp xúc kết nối giữa đầu mút dây và tâm icon

        // Lần lượt kiểm tra các đường dây
        for (line in lines) {
            val p1 = Point2D(line.X1, line.Y1)
            val p2 = Point2D(line.X3, line.Y3)

            val connectsP1ToCust = Function_tinhtoan_ve.TINH_KC(p1.x, p1.y, custPos.x, custPos.y) <= threshold
            val connectsP2ToCust = Function_tinhtoan_ve.TINH_KC(p2.x, p2.y, custPos.x, custPos.y) <= threshold

            if (connectsP1ToCust || connectsP2ToCust) {
                val poleEnd = if (connectsP1ToCust) p2 else p1
                val nearestPole = poles.minByOrNull { pole ->
                    Function_tinhtoan_ve.TINH_KC(poleEnd.x, poleEnd.y, pole.X3, pole.Y3)
                }
                if (nearestPole != null) {
                    val dist = Function_tinhtoan_ve.TINH_KC(poleEnd.x, poleEnd.y, nearestPole.X3, nearestPole.Y3)
                    if (dist <= threshold) {
                        return nearestPole.TEN.ifBlank { "Trụ gần nhất" }
                    }
                }
            }
        }

        // Nếu không có dây liên kết trực tiếp, chọn trụ điện có cự ly gần nhất với khách hàng
        val closestPole = poles.minByOrNull { pole ->
            Function_tinhtoan_ve.TINH_KC(custPos.x, custPos.y, pole.X3, pole.Y3)
        }
        return closestPole?.TEN?.ifBlank { "Trụ gần nhất" } ?: ""
    }

    fun toJsonString(data: SurveyQrData, pretty: Boolean = false): String {
        return if (pretty) {
            jsonPrettyHelper.encodeToString(SurveyQrData.serializer(), data)
        } else {
            jsonHelper.encodeToString(SurveyQrData.serializer(), data)
        }
    }

    /**
     * Tạo ImageBitmap mã QR từ chuỗi JSON.
     */
    fun generateQrBitmap(dataString: String): ImageBitmap? {
        if (dataString.isBlank()) return null
        qrCache[dataString]?.let { return it }

        return try {
            val qr = QRCode.ofSquares().build(dataString)
            val bytes = qr.renderToBytes()
            val bitmap = PlatformBitmapDecoder.decode(bytes)
            if (bitmap != null) {
                if (qrCache.size > 20) qrCache.clear()
                qrCache[dataString] = bitmap
            }
            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Vẽ mã QR trực tiếp lên 1 trong 4 góc của bảng vẽ kỹ thuật theo tỷ lệ (100% - 150%).
     */
    fun renderQrOnCanvas(
        drawScope: DrawScope,
        state: DrawingState,
        canvasSize: Size
    ) {
        if (!state.showSurveyQrCode) return

        val qrData = createQrData(state)
        val jsonStr = toJsonString(qrData, pretty = false)
        val bitmap = generateQrBitmap(jsonStr) ?: return

        with(drawScope) {
            val baseSize = 105f
            val scale = (state.qrScalePercent.coerceIn(100, 150) / 100f)
            val qrBoxSize = baseSize * scale
            val pad = 5f * scale
            val margin = 16f

            val (boxX, boxY) = when (state.qrPosition) {
                QrPosition.TOP_RIGHT -> Pair(canvasSize.width - qrBoxSize - margin, margin)
                QrPosition.TOP_LEFT -> Pair(margin, margin)
                QrPosition.BOTTOM_RIGHT -> Pair(canvasSize.width - qrBoxSize - margin, canvasSize.height - qrBoxSize - margin)
                QrPosition.BOTTOM_LEFT -> Pair(margin, canvasSize.height - qrBoxSize - margin)
            }

            // Khung nền trắng bo góc nhẹ làm nổi bật mã QR trên nền bản vẽ
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(boxX, boxY),
                size = Size(qrBoxSize, qrBoxSize),
                cornerRadius = CornerRadius(6f * scale, 6f * scale)
            )
            // Viền chỉ mảnh kỹ thuật màu xanh tinh tế
            drawRoundRect(
                color = Color(0xFF0284C7).copy(alpha = 0.5f),
                topLeft = Offset(boxX, boxY),
                size = Size(qrBoxSize, qrBoxSize),
                cornerRadius = CornerRadius(6f * scale, 6f * scale),
                style = Stroke(width = 1f * scale)
            )

            // Vẽ ảnh mã QR Code chính giữa khung
            val imgSize = qrBoxSize - pad * 2f
            drawImage(
                image = bitmap,
                dstOffset = IntOffset((boxX + pad).toInt(), (boxY + pad).toInt()),
                dstSize = IntSize(imgSize.toInt(), imgSize.toInt())
            )
        }
    }
}

/**
 * Hộp thoại cài đặt thông tin mã trạm, kích thước phóng to 100%-150% và chọn vị trí hiển thị 1 trong 4 góc.
 * Có cơ chế tự động ghi nhớ (persist) cấu hình cho các lần vẽ tiếp theo.
 */
@Composable
fun SurveyQrDetailDialog(
    state: DrawingState,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    val initialQrData = remember { SurveyQrManager.createQrData(state) }

    var editMaTram by remember { mutableStateOf(state.customer.MA_TRAM.ifBlank { "TRAM_01" }) }
    var editTenKh by remember { mutableStateOf(state.customer.TEN_KHANG_KS.ifBlank { "Khách Hàng Khảo Sát" }) }
    var editSoTru by remember { mutableStateOf(initialQrData.so_tru) }
    var editScalePercent by remember { mutableIntStateOf(state.qrScalePercent.coerceIn(100, 150)) }
    var editPosition by remember { mutableStateOf(state.qrPosition) }

    val qrData = remember(editMaTram, editTenKh, editSoTru, state.gpsCenter.latitude, state.gpsCenter.longitude) {
        SurveyQrData(
            ma_tram = editMaTram,
            ten_khach_hang = editTenKh,
            lat = (state.gpsCenter.latitude * 1000000.0).roundToInt() / 1000000.0,
            lon = (state.gpsCenter.longitude * 1000000.0).roundToInt() / 1000000.0,
            so_tru = editSoTru
        )
    }

    val jsonCompact = remember(qrData) {
        SurveyQrManager.toJsonString(qrData, pretty = false)
    }

    val jsonPretty = remember(qrData) {
        SurveyQrManager.toJsonString(qrData, pretty = true)
    }

    val qrBitmap = remember(jsonCompact) {
        SurveyQrManager.generateQrBitmap(jsonCompact)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 520.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Tiêu đề hộp thoại
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Cài Đặt Mã QR Khảo Sát",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tuỳ chỉnh mã trạm, kích thước (100-150%) & vị trí 4 góc",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                // 2. Hình xem trước mã QR Code
                Box(
                    modifier = Modifier
                        .size((140 * (editScalePercent / 100f).coerceAtMost(1.35f)).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "Mã QR thông tin khảo sát",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = Color(0xFF0284C7))
                    }
                }

                // 3. MỤC 1: NHẬP THÔNG TIN MÃ TRẠM
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "1. Thông tin trạm biến áp cấp điện:",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                    OutlinedTextField(
                        value = editMaTram,
                        onValueChange = { editMaTram = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mã trạm (MA_TRAM)", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        placeholder = { Text("Ví dụ: TRAM_01, T.CHOMOI...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF020617),
                            unfocusedContainerColor = Color(0xFF020617)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // 4. MỤC 2: CHỌN KÍCH THƯỚC QR (100% - 150%)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Kích thước mã QR:",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "$editScalePercent%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Slider 100% - 150%
                    Slider(
                        value = editScalePercent.toFloat(),
                        onValueChange = { editScalePercent = it.roundToInt() },
                        valueRange = 100f..150f,
                        steps = 9, // Mỗi nấc 5% (100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150)
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF0284C7),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    // Các nút chọn nhanh kích thước
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100 to "100% (Gốc)", 115 to "115%", 130 to "130%", 150 to "150% (Tối đa)").forEach { (pct, txt) ->
                            val isSelected = editScalePercent == pct
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editScalePercent = pct }
                            ) {
                                Text(
                                    text = txt,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 5. MỤC 3: CHỌN VỊ TRÍ HIỂN THỊ (1 TRONG 4 GÓC BẢN VẼ)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "3. Vị trí hiển thị trên bản vẽ (1 trong 4 góc):",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )

                    // Bố cục lưới 2x2 mô phỏng 4 góc bản vẽ
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF020617))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hàng trên: Trên - Trái | Trên - Phải (Mặc định)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CornerSelectionCard(
                                title = "↖ Trên - Trái",
                                isSelected = editPosition == QrPosition.TOP_LEFT,
                                modifier = Modifier.weight(1f),
                                onClick = { editPosition = QrPosition.TOP_LEFT }
                            )
                            CornerSelectionCard(
                                title = "↗ Trên - Phải ★",
                                subtitle = "(Mặc định)",
                                isSelected = editPosition == QrPosition.TOP_RIGHT,
                                modifier = Modifier.weight(1f),
                                onClick = { editPosition = QrPosition.TOP_RIGHT }
                            )
                        }

                        // Hàng dưới: Dưới - Trái | Dưới - Phải
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CornerSelectionCard(
                                title = "↙ Dưới - Trái",
                                isSelected = editPosition == QrPosition.BOTTOM_LEFT,
                                modifier = Modifier.weight(1f),
                                onClick = { editPosition = QrPosition.BOTTOM_LEFT }
                            )
                            CornerSelectionCard(
                                title = "↘ Dưới - Phải",
                                isSelected = editPosition == QrPosition.BOTTOM_RIGHT,
                                modifier = Modifier.weight(1f),
                                onClick = { editPosition = QrPosition.BOTTOM_RIGHT }
                            )
                        }
                    }
                }

                // 6. MỤC 4: KHUNG MÃ JSON & NÚT SAO CHÉP
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF020617))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nội dung JSON khảo sát:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(jsonPretty))
                                    isCopied = true
                                    state.statusMessage = "Đã sao chép chuỗi JSON vào Clipboard!"
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = if (isCopied) Color(0xFF4ADE80) else Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isCopied) "Đã sao chép!" else "Sao chép",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCopied) Color(0xFF4ADE80) else Color(0xFF38BDF8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = jsonCompact,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        maxLines = 3,
                        lineHeight = 15.sp
                    )
                }

                // 7. CÁC NÚT ĐIỀU HƯỚNG / LƯU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("Đóng", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            state.customer = state.customer.copy(
                                MA_TRAM = editMaTram,
                                TEN_KHANG_KS = editTenKh
                            )
                            state.customSoTru = editSoTru
                            state.qrScalePercent = editScalePercent
                            state.qrPosition = editPosition

                            // Lưu nhớ vào bộ nhớ bền vững SQLite cho các lần vẽ tiếp theo
                            scope.launch {
                                PlatformTemplateStorage.saveQrConfig(
                                    SavedQrConfig(
                                        scalePercent = editScalePercent,
                                        positionName = editPosition.name,
                                        lastMaTram = editMaTram
                                    )
                                )
                            }
                            state.statusMessage = "Đã lưu cấu hình QR ($editScalePercent%, ${editPosition.shortName})!"
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.6f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lưu & Áp dụng", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CornerSelectionCard(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFF1E293B),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF38BDF8) else Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
