package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vesodo.khaosat.cad.CadHtmlViewerGenerator
import com.vesodo.khaosat.cad.DxfExporter
import com.vesodo.khaosat.drawing.DrawingRenderer
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.export.PlatformFileOpener
import com.vesodo.khaosat.math.Function_tinhtoan_ve
import com.vesodo.khaosat.map.GpsVectorRoadManager
import com.vesodo.khaosat.model.Variables
import com.vesodo.khaosat.qr.QrPosition
import qrcode.QRCode
import kotlin.math.*

/**
 * Hộp thoại Xem trước Bản vẽ CAD (AutoCAD DXF Preview Mode) khổ A4 chuẩn kỹ thuật EVN & TCVN.
 *
 * Tính năng nâng cấp:
 * 1. Tự động tạo tệp DXF thật và tệp HTML Vector CAD tạm thời.
 * 2. Cung cấp nút gọi thư viện / ứng dụng CAD chuyên nghiệp trên máy (AutoCAD, DWG FastView, CAD Pockets)
 *    để xem trực tiếp tệp CAD thật 100%.
 * 3. Cung cấp nút mở xem trên Trình duyệt Web (Vector SVG A4) với đầy đủ zoom/pan và nút in ấn.
 * 4. Tinh chỉnh kích thước Icon trong Canvas: Giảm từ kích cỡ khổng lồ xuống tỷ lệ chuẩn CAD (bán kính 2.2mm),
 *    mạng lưới đường giao thông bo góc mềm mại, không bị vẽ lung tung, có nút bật/tắt nhanh lớp đường.
 */
@Composable
fun CadPreviewDialog(
    state: DrawingState,
    onDismiss: () -> Unit,
    onExportCad: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showRoads by remember { mutableStateOf(true) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    val qrJson = remember {
        val qrData = com.vesodo.khaosat.qr.SurveyQrManager.createQrData(state)
        com.vesodo.khaosat.qr.SurveyQrManager.toJsonString(qrData, pretty = false)
    }

    // Tạo nội dung DXF và HTML tạm thời một lần khi mở dialog
    val dxfContent = remember {
        val w = if (state.canvasSize.width > 0) state.canvasSize.width.toInt() else 1920
        val h = if (state.canvasSize.height > 0) state.canvasSize.height.toInt() else 1080
        DxfExporter.generateDxf(
            items = state.listCanvas,
            width = w,
            height = h,
            fontSizeSp = state.fontSize.sizeSp,
            customer = state.customer,
            vectorBasemap = state.vectorBasemap,
            showQrCode = state.showSurveyQrCode,
            qrDataString = qrJson,
            qrScalePercent = state.qrScalePercent,
            qrPosition = state.qrPosition,
            unitConfig = state.unitConfig
        )
    }

    val htmlContent = remember {
        val w = if (state.canvasSize.width > 0) state.canvasSize.width.toInt() else 1920
        val h = if (state.canvasSize.height > 0) state.canvasSize.height.toInt() else 1080
        CadHtmlViewerGenerator.generateHtml(
            items = state.listCanvas,
            width = w,
            height = h,
            fontSizeSp = state.fontSize.sizeSp,
            customer = state.customer,
            vectorBasemap = state.vectorBasemap,
            showQrCode = state.showSurveyQrCode,
            qrDataString = qrJson,
            qrScalePercent = state.qrScalePercent,
            qrPosition = state.qrPosition,
            unitConfig = state.unitConfig
        )
    }

    val tempDxfPath = remember { PlatformFileOpener.createTempDxfFile(dxfContent) }
    val tempHtmlPath = remember { PlatformFileOpener.createTempHtmlFile(htmlContent) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        var topBarHeightPx by remember { mutableIntStateOf(0) }
        var footerHeightPx by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current

        val topPaddingDp = with(density) {
            if (topBarHeightPx > 0) topBarHeightPx.toDp() else 76.dp
        }
        val bottomPaddingDp = with(density) {
            if (footerHeightPx > 0) footerHeightPx.toDp() else 32.dp
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16)) // Nền tối sâu CAD Studio
        ) {
            // ==============================================================
            // 2. VÙNG CANVAS HIỂN THỊ BẢN VẼ CAD (INTERACTIVE CANVAS)
            // Có margin 1 khoảng cách các cạnh để bản vẽ trông thoáng đãng, dễ nhìn
            // ==============================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPaddingDp, bottom = bottomPaddingDp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1420))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.25f, 6.0f)
                            panOffset += pan
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height
                    if (canvasW <= 0f || canvasH <= 0f) return@Canvas

                    // Khổ A4 Landscape: 297mm x 210mm (tỷ lệ 1.4142)
                    val a4Ratio = 297f / 210f
                    val basePaperW: Float
                    val basePaperH: Float

                    val paddingViewport = 28f
                    val availW = canvasW - paddingViewport * 2
                    val availH = canvasH - paddingViewport * 2

                    if (availW / availH > a4Ratio) {
                        basePaperH = availH
                        basePaperW = basePaperH * a4Ratio
                    } else {
                        basePaperW = availW
                        basePaperH = basePaperW / a4Ratio
                    }

                    // Tọa độ trang giấy A4 (căn giữa màn hình)
                    val centerScreen = Offset(canvasW / 2f + panOffset.x, canvasH / 2f + panOffset.y)
                    val paperW = basePaperW * scale
                    val paperH = basePaperH * scale
                    val paperLeft = centerScreen.x - paperW / 2f
                    val paperTop = centerScreen.y - paperH / 2f

                    val a4Scale = paperW / 297f // Hệ số quy đổi 1mm trên giấy A4 sang pixel màn hình

                    // 1. Bóng đổ trang giấy
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.55f),
                        topLeft = Offset(paperLeft + 6f, paperTop + 6f),
                        size = Size(paperW, paperH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                    )

                    // 2. Nền giấy A4 CAD: Màu đen AutoCAD thuần túy
                    drawRect(
                        color = Color(0xFF12141A),
                        topLeft = Offset(paperLeft, paperTop),
                        size = Size(paperW, paperH)
                    )

                    // 3. Mép ngoài giấy A4
                    drawRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(paperLeft, paperTop),
                        size = Size(paperW, paperH),
                        style = Stroke(width = maxOf(1f, 1.0f * a4Scale))
                    )

                    // 4. Vạch định vị tâm 4 cạnh giấy A4 (Centering Marks - 5mm)
                    val tick = 5f * a4Scale
                    val midPaperX = paperLeft + paperW / 2f
                    val midPaperY = paperTop + paperH / 2f
                    val tickColor = Color(0xFF94A3B8)
                    val tickWidth = maxOf(1f, 1.2f * a4Scale)
                    drawLine(tickColor, Offset(paperLeft, midPaperY), Offset(paperLeft + tick, midPaperY), tickWidth)
                    drawLine(tickColor, Offset(paperLeft + paperW - tick, midPaperY), Offset(paperLeft + paperW, midPaperY), tickWidth)
                    drawLine(tickColor, Offset(midPaperX, paperTop), Offset(midPaperX, paperTop + tick), tickWidth)
                    drawLine(tickColor, Offset(midPaperX, paperTop + paperH - tick), Offset(midPaperX, paperTop + paperH), tickWidth)

                    // 5. Khung viền đôi trang trí song song A4 (2 đường kẻ song song tuyệt đẹp)
                    val decorGap = 2.0f * a4Scale // Khoảng cách 2mm song song giữa 2 viền
                    val borderLeft = paperLeft + 20f * a4Scale
                    val borderRight = paperLeft + paperW - 10f * a4Scale
                    val borderTopActual = paperTop + 10f * a4Scale
                    val borderBottomActual = paperTop + paperH - 10f * a4Scale
                    val borderW = borderRight - borderLeft
                    val borderH = borderBottomActual - borderTopActual

                    // Đường 1: Viền ngoài trang trí
                    drawRect(
                        color = Color(0xFF64748B),
                        topLeft = Offset(borderLeft - decorGap, borderTopActual - decorGap),
                        size = Size(borderW + decorGap * 2, borderH + decorGap * 2),
                        style = Stroke(width = maxOf(1f, 1.2f * a4Scale))
                    )

                    // Đường 2: Viền trong bản vẽ chính (Khung nét đậm kỹ thuật)
                    drawRect(
                        color = Color(0xFFE2E8F0),
                        topLeft = Offset(borderLeft, borderTopActual),
                        size = Size(borderW, borderH),
                        style = Stroke(width = maxOf(1.6f, 2.0f * a4Scale))
                    )

                    // 6. Tính toán kích thước Khung tên A4 & Mã QR để bảo vệ
                    val titleW = 140f * a4Scale
                    val titleH = 38f * a4Scale
                    val titleLeft = borderRight - titleW
                    val titleTop = borderBottomActual - titleH

                    val qrScaleMult = (state.qrScalePercent.coerceIn(100, 150) / 100f)
                    val qrBoxSize = 24f * a4Scale * qrScaleMult
                    val qrMargin = 5f * a4Scale
                    val qrLabelH = 4.0f * a4Scale

                    val (qrBoxLeft, qrBoxTop) = when (state.qrPosition) {
                        QrPosition.TOP_RIGHT -> Pair(borderRight - qrBoxSize - qrMargin, borderTopActual + qrMargin)
                        QrPosition.TOP_LEFT -> Pair(borderLeft + qrMargin, borderTopActual + qrMargin)
                        QrPosition.BOTTOM_LEFT -> Pair(borderLeft + qrMargin, borderBottomActual - qrBoxSize - qrMargin - qrLabelH)
                        QrPosition.BOTTOM_RIGHT -> Pair(borderRight - qrBoxSize - qrMargin, titleTop - qrBoxSize - qrMargin - qrLabelH)
                    }

                    // Tỷ lệ chuyển đổi tọa độ thiết kế của người dùng sang hệ tọa độ bản vẽ CAD A4
                    val designW = if (state.canvasSize.width > 0f) state.canvasSize.width else 1920f
                    val designH = if (state.canvasSize.height > 0f) state.canvasSize.height else 1080f
                    val designScale = minOf(borderW / designW, borderH / designH) * 0.94f
                    val contentOffsetX = borderLeft + (borderW - designW * designScale) / 2f
                    val contentOffsetY = borderTopActual + (borderH - designH * designScale) / 2f

                    fun mapPoint(x: Float, y: Float): Offset {
                        return Offset(contentOffsetX + x * designScale, contentOffsetY + y * designScale)
                    }

                    // 7. VẼ MẠNG LƯỚI ĐƯỜNG GIAO THÔNG (Nếu bật lớp đường, khống chế 100% trong khung định sẵn)
                    val vectorBasemap = state.vectorBasemap
                    if (showRoads && vectorBasemap != null && vectorBasemap.roads.isNotEmpty()) {
                        clipRect(borderLeft, borderTopActual, borderRight, borderBottomActual) {
                            val majorRoadColor = Color(0xFF475569)
                            val minorRoadColor = Color(0xFF334155)
                            val centerlineColor = Color(0xFF94A3B8)
                            val dashedCenterline = PathEffect.dashPathEffect(floatArrayOf(6f * a4Scale, 4f * a4Scale), 0f)

                            for (road in vectorBasemap.roads) {
                                val pts = road.points.map { pt ->
                                    val scr = GpsVectorRoadManager.gpsToScreen(
                                        lat = pt.lat,
                                        lon = pt.lon,
                                        centerGps = vectorBasemap.centerGps,
                                        zoom = vectorBasemap.zoom,
                                        canvasWidth = designW,
                                        canvasHeight = designH,
                                        panOffsetX = 0f,
                                        panOffsetY = 0f
                                    )
                                    mapPoint(scr.x, scr.y)
                                }

                                if (pts.size >= 2) {
                                    val isMajor = road.roadType in listOf("primary", "secondary", "trunk", "motorway")
                                    val roadWidth = if (isMajor) 3.5f * a4Scale else 2.4f * a4Scale
                                    val roadColor = if (isMajor) majorRoadColor else minorRoadColor

                                    // Vẽ lòng đường bằng polyline liên tục với bo tròn góc (Cap & Join Round)
                                    val roadPath = Path().apply {
                                        moveTo(pts[0].x, pts[0].y)
                                        for (i in 1 until pts.size) {
                                            lineTo(pts[i].x, pts[i].y)
                                        }
                                    }

                                    drawPath(
                                        path = roadPath,
                                        color = roadColor,
                                        style = Stroke(
                                            width = roadWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )

                                    // Tim đường nét đứt thanh mảnh cho đường lớn
                                    if (isMajor) {
                                        drawPath(
                                            path = roadPath,
                                            color = centerlineColor,
                                            style = Stroke(
                                                width = maxOf(0.8f, 0.6f * a4Scale),
                                                pathEffect = dashedCenterline,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 8. VẼ CÁC ĐƯỜNG DÂY DẪN ĐIỆN (Line, 2Line, Dashed)
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(5f * a4Scale, 3.5f * a4Scale), 0f)

                    for (item in state.listCanvas) {
                        if (DrawingRenderer.isLineCommand(item.LENH)) {
                            val p1 = mapPoint(item.X1, item.Y1)
                            val p2 = mapPoint(item.X3, item.Y3)

                            when (item.LENH) {
                                Variables.VE_DUONG_THANG -> {
                                    // Dây chính: Xanh lá CAD (#22C55E)
                                    drawLine(
                                        color = Color(0xFF22C55E),
                                        start = p1,
                                        end = p2,
                                        strokeWidth = maxOf(1.4f, 1.2f * a4Scale)
                                    )
                                }
                                Variables.VE_LINE_DUT_NET -> {
                                    // Dây nhánh: Tím Magenta CAD (#E879F9)
                                    drawLine(
                                        color = Color(0xFFE879F9),
                                        start = p1,
                                        end = p2,
                                        strokeWidth = maxOf(1.2f, 1.0f * a4Scale),
                                        pathEffect = dashedEffect
                                    )
                                }
                                Variables.VE_2LINE -> {
                                    val dx = p2.x - p1.x
                                    val dy = p2.y - p1.y
                                    val len = hypot(dx, dy)
                                    val off = 1.0f * a4Scale
                                    val nx = if (len > 0f) -dy / len * off else 0f
                                    val ny = if (len > 0f) dx / len * off else 0f
                                    drawLine(Color(0xFF22C55E), Offset(p1.x + nx, p1.y + ny), Offset(p2.x + nx, p2.y + ny), maxOf(1.2f, 1.0f * a4Scale))
                                    drawLine(Color(0xFF22C55E), Offset(p1.x - nx, p1.y - ny), Offset(p2.x - nx, p2.y - ny), maxOf(1.2f, 1.0f * a4Scale))
                                }
                                Variables.VE_MUI_TEN, Variables.VE_ARROW -> {
                                    val isDashed = (item.THUOC_TINH == 1)
                                    val effect = if (isDashed) dashedEffect else null
                                    val strokeCol = if (isDashed) Color(0xFFE879F9) else Color(0xFF22C55E)
                                    drawLine(
                                        color = strokeCol,
                                        start = p1,
                                        end = p2,
                                        strokeWidth = maxOf(1.2f, 1.0f * a4Scale),
                                        pathEffect = effect
                                    )
                                    val arrow = Function_tinhtoan_ve.TINH_MUI_TEN(p1.x, p1.y, p2.x, p2.y, (14 * a4Scale).toInt().coerceAtLeast(10))
                                    drawLine(
                                        color = strokeCol,
                                        start = Offset(arrow.wing1.x, arrow.wing1.y),
                                        end = p2,
                                        strokeWidth = maxOf(1.2f, 1.0f * a4Scale),
                                        pathEffect = effect
                                    )
                                    drawLine(
                                        color = strokeCol,
                                        start = Offset(arrow.wing2.x, arrow.wing2.y),
                                        end = p2,
                                        strokeWidth = maxOf(1.2f, 1.0f * a4Scale),
                                        pathEffect = effect
                                    )
                                }
                                else -> {
                                    drawLine(Color(0xFF22C55E), p1, p2, maxOf(1.4f, 1.2f * a4Scale))
                                }
                            }

                            // Nhãn cự ly / tên dây
                            if (item.TEN.isNotBlank()) {
                                val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                                val textResult = textMeasurer.measure(
                                    text = item.TEN,
                                    style = TextStyle(fontSize = maxOf(7.5f, 2.0f * a4Scale).sp, color = Color.White)
                                )
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = item.TEN,
                                    topLeft = Offset(mid.x - textResult.size.width / 2f, mid.y - textResult.size.height - 2f * a4Scale),
                                    style = TextStyle(fontSize = maxOf(7.5f, 2.0f * a4Scale).sp, color = Color.White)
                                )
                            }
                        }
                    }

                    // 9. VẼ BIỂU TƯỢNG ICON / TRỤ ĐIỆN / TRẠM / CÔNG TƠ (Kích thước nhỏ gọn chuẩn CAD)
                    for (item in state.listCanvas) {
                        if (!DrawingRenderer.isLineCommand(item.LENH)) {
                            val pt = mapPoint(item.X3, item.Y3)
                            val itemScale = if (item.SCALE > 0f) item.SCALE else 1f
                            // Bán kính chuẩn CAD: 2.2mm trên khổ giấy A4
                            val nodeRadius = 2.2f * a4Scale * itemScale

                            val isTru = IconRegistry.isTru(item.LENH)
                            val isTram = IconRegistry.TRAM_TOOLS.any { it.command == item.LENH }
                            val isCongTo = item.LENH == Variables.VE_HINH_CONGTO ||
                                    item.LENH == Variables.VE_CONGTO_TREO_NHA ||
                                    item.LENH == Variables.VE_CONGTO_TREO_TRU_BTLT_105_HHUU ||
                                    item.LENH == Variables.VE_CONGTO_TREO_TRU_BTLT_105_SLAP ||
                                    item.LENH == Variables.VE_CONGTO_TREO_TRU_BTLT_12_HHUU ||
                                    item.LENH == Variables.VE_CONGTO_TREO_TRU_BTV_65_HHUU ||
                                    item.LENH == Variables.VE_CONGTO_TREO_TRU_BTV_65_SLAP

                            if (isTram) {
                                // Trạm biến áp: Hình vuông đỏ nhỏ gọn 5x5mm
                                val subW = 5.5f * a4Scale * itemScale
                                drawRect(
                                    color = Color(0xFFEF4444),
                                    topLeft = Offset(pt.x - subW / 2f, pt.y - subW / 2f),
                                    size = Size(subW, subW)
                                )
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(pt.x - subW / 2f, pt.y - subW / 2f),
                                    size = Size(subW, subW),
                                    style = Stroke(width = maxOf(0.8f, 0.6f * a4Scale))
                                )
                            } else if (isCongTo) {
                                // Công tơ: Vòng tròn vàng có tâm đen (2.0mm)
                                drawCircle(
                                    color = Color(0xFFFACC15),
                                    radius = nodeRadius,
                                    center = pt
                                )
                                drawCircle(
                                    color = Color.Black,
                                    radius = nodeRadius * 0.45f,
                                    center = pt
                                )
                            } else if (isTru) {
                                // Trụ điện: Vòng tròn đôi Cyan CAD (2.2mm)
                                drawCircle(
                                    color = Color(0xFF12141A),
                                    radius = nodeRadius,
                                    center = pt
                                )
                                drawCircle(
                                    color = Color(0xFF22D3EE),
                                    radius = nodeRadius,
                                    center = pt,
                                    style = Stroke(width = maxOf(1.0f, 0.7f * a4Scale))
                                )
                                drawCircle(
                                    color = Color(0xFF22D3EE),
                                    radius = nodeRadius * 0.45f,
                                    center = pt
                                )
                            } else if (item.LENH == Variables.VE_TEXT) {
                                // Text ghi chú
                                if (item.TEN.isNotBlank()) {
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = item.TEN,
                                        topLeft = pt,
                                        style = TextStyle(
                                            fontSize = maxOf(8f, 2.4f * a4Scale * itemScale).sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            } else {
                                // Địa vật / Nhà KH: Hình vuông cam nhỏ
                                val diaVatSize = 4f * a4Scale * itemScale
                                drawRect(
                                    color = Color(0xFFFB923C),
                                    topLeft = Offset(pt.x - diaVatSize / 2f, pt.y - diaVatSize / 2f),
                                    size = Size(diaVatSize, diaVatSize)
                                )
                            }

                            // Tên đối tượng (Số trụ, tên trạm)
                            if (item.TEN.isNotBlank() && item.LENH != Variables.VE_TEXT) {
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = item.TEN,
                                    topLeft = Offset(pt.x + nodeRadius + 3f * a4Scale, pt.y - 2.5f * a4Scale),
                                    style = TextStyle(
                                        fontSize = maxOf(7.5f, 2.0f * a4Scale * itemScale).sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // 10. VẼ KHUNG TÊN TIÊU CHUẨN A4 EVN (Góc dưới bên phải - Nền xám nhẹ chống xuyên)
                    drawRect(
                        color = Color(0xFFE2E8F0),
                        topLeft = Offset(titleLeft, titleTop),
                        size = Size(titleW, titleH)
                    )
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(titleLeft, titleTop),
                        size = Size(titleW, titleH),
                        style = Stroke(width = maxOf(1.2f, 1.4f * a4Scale))
                    )

                    // Các đường kẻ phân chia khung tên
                    val line1Y = titleTop + 9f * a4Scale
                    val line2Y = titleTop + 18f * a4Scale
                    val line3Y = titleTop + 27f * a4Scale
                    val midColX = titleLeft + titleW / 2f

                    val strokeGrid = Stroke(width = maxOf(0.8f, 0.8f * a4Scale))
                    val colorGrid = Color(0xFF0F172A)
                    drawLine(colorGrid, Offset(titleLeft, line1Y), Offset(borderRight, line1Y), strokeWidth = maxOf(0.8f, 0.8f * a4Scale))
                    drawLine(colorGrid, Offset(titleLeft, line2Y), Offset(borderRight, line2Y), strokeWidth = maxOf(0.8f, 0.8f * a4Scale))
                    drawLine(colorGrid, Offset(titleLeft, line3Y), Offset(borderRight, line3Y), strokeWidth = maxOf(0.8f, 0.8f * a4Scale))
                    drawLine(colorGrid, Offset(midColX, line2Y), Offset(midColX, borderBottomActual), strokeWidth = maxOf(0.8f, 0.8f * a4Scale))

                    // Chữ trong khung tên
                    val tenDvi = state.unitConfig.tenDvi.ifBlank { state.customer.MA_DVIQLY }.ifBlank { "ĐIỆN LỰC" }
                    val nguoiKs = state.unitConfig.nguoiKhaoSat.ifBlank { "KTV Khảo Sát" }
                    val nguoiKt = state.unitConfig.nguoiKiemTra.ifBlank { "Tổ Trưởng" }
                    val tenKh = state.customer.TEN_KHANG_KS.ifBlank { "Khách hàng" }
                    val tram = state.customer.MA_TRAM.ifBlank { "Trạm hạ thế" }

                    val titleTextSize = maxOf(8f, 2.6f * a4Scale).sp
                    val bodyTextSize = maxOf(7f, 2.0f * a4Scale).sp

                    val t1 = textMeasurer.measure(tenDvi.uppercase(), style = TextStyle(fontSize = titleTextSize, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)))
                    drawText(textMeasurer, tenDvi.uppercase(), Offset(titleLeft + (titleW - t1.size.width) / 2f, titleTop + 2.5f * a4Scale), style = TextStyle(fontSize = titleTextSize, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)))

                    val t2 = textMeasurer.measure("SƠ ĐỒ KHẢO SÁT CẤP ĐIỆN HẠ THẾ", style = TextStyle(fontSize = titleTextSize, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1)))
                    drawText(textMeasurer, "SƠ ĐỒ KHẢO SÁT CẤP ĐIỆN HẠ THẾ", Offset(titleLeft + (titleW - t2.size.width) / 2f, line1Y + 2.5f * a4Scale), style = TextStyle(fontSize = titleTextSize, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1)))

                    drawText(textMeasurer, "KHÁCH HÀNG: $tenKh", Offset(titleLeft + 3f * a4Scale, line2Y + 2f * a4Scale), style = TextStyle(fontSize = bodyTextSize, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium))
                    drawText(textMeasurer, "ĐỊA CHỈ: ${state.customer.DIA_CHI}", Offset(titleLeft + 3f * a4Scale, line2Y + 5f * a4Scale), style = TextStyle(fontSize = bodyTextSize, color = Color(0xFF475569)))

                    drawText(textMeasurer, "TRẠM: $tram", Offset(midColX + 3f * a4Scale, line2Y + 2f * a4Scale), style = TextStyle(fontSize = bodyTextSize, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium))
                    drawText(textMeasurer, "TỶ LỆ: 1/500 | KHỔ A4", Offset(midColX + 3f * a4Scale, line2Y + 5f * a4Scale), style = TextStyle(fontSize = bodyTextSize, color = Color(0xFF475569)))

                    drawText(textMeasurer, "KTV KHẢO SÁT: $nguoiKs", Offset(titleLeft + 3f * a4Scale, line3Y + 3f * a4Scale), style = TextStyle(fontSize = bodyTextSize, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold))
                    drawText(textMeasurer, "NGƯỜI KIỂM TRA: $nguoiKt", Offset(midColX + 3f * a4Scale, line3Y + 3f * a4Scale), style = TextStyle(fontSize = bodyTextSize, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold))

                    // 11. VẼ MÃ QR KHẢO SÁT (Nền trắng che nhẹ tên đường)
                    if (state.showSurveyQrCode) {
                        val qrText = qrJson
                        if (qrText.isNotBlank()) {
                            try {
                                val qrCode = QRCode.ofSquares().build(qrText)
                                val rawData = qrCode.rawData
                                val rowCount = rawData.size
                                val cellSize = qrBoxSize / rowCount

                                // Nền trắng
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(qrBoxLeft, qrBoxTop),
                                    size = Size(qrBoxSize, qrBoxSize),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * a4Scale)
                                )
                                drawRoundRect(
                                    color = Color(0xFF94A3B8),
                                    topLeft = Offset(qrBoxLeft, qrBoxTop),
                                    size = Size(qrBoxSize, qrBoxSize),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * a4Scale),
                                    style = Stroke(width = maxOf(0.8f, 0.8f * a4Scale))
                                )

                                for (r in 0 until rowCount) {
                                    val row = rawData[r]
                                    for (c in 0 until row.size) {
                                        if (row[c].dark) {
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset(qrBoxLeft + c * cellSize, qrBoxTop + r * cellSize),
                                                size = Size(cellSize, cellSize)
                                            )
                                        }
                                    }
                                }

                                val qrLabel = "MÃ QR KHẢO SÁT"
                                val qMeas = textMeasurer.measure(qrLabel, style = TextStyle(fontSize = maxOf(6.5f, 1.8f * a4Scale).sp, color = Color.White, fontWeight = FontWeight.Bold))
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = qrLabel,
                                    topLeft = Offset(qrBoxLeft + (qrBoxSize - qMeas.size.width) / 2f, qrBoxTop + qrBoxSize + 1.5f * a4Scale),
                                    style = TextStyle(fontSize = maxOf(6.5f, 1.8f * a4Scale).sp, color = Color.White, fontWeight = FontWeight.Bold)
                                )
                            } catch (e: Exception) {
                                // Bỏ qua nếu lỗi sinh QR
                            }
                        }
                    }
                }
            }


            // ==============================================================
            // 1. THANH ĐIỀU KHIỂN PHÍA TRÊN (CAD TOPBAR DÀNH CHO MOBILE)
            // Thiết kế 2 tầng: Tầng 1 tiêu đề + nút đóng; Tầng 2 toolbar cuộn ngang
            // ==============================================================
            Surface(
                color = Color(0xFF1E293B),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .onSizeChanged { topBarHeightPx = it.height }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 6.dp)
                ) {
                    // TẦNG 1: TIÊU ĐỀ BẢN VẼ & NÚT ĐÓNG
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0284C7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Polyline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CAD A4",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = "Xem Trước Bản Vẽ CAD A4",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Chuẩn TCVN 3821:2008 & EVN | Tỷ lệ 100% Vector",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }

                        // NÚT ĐÓNG DIALOG
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // TẦNG 2: THANH CÔNG CỤ CUỘN NGANG (HORIZONTAL SCROLL TOOLBAR CHO ĐIỆN THOẠI)
                    val toolbarScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(toolbarScrollState)
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // NÚT 1: Mở bằng app CAD trên máy (AutoCAD / DWG FastView)
                        Button(
                            onClick = {
                                if (tempDxfPath != null) {
                                    val opened = PlatformFileOpener.openFile(tempDxfPath, "application/dxf")
                                    statusFeedback = if (opened) {
                                        "Đã mở file DXF tạm bằng ứng dụng CAD trên máy!"
                                    } else {
                                        "Không tìm thấy phần mềm CAD mặc định. Hãy thử xem bằng trình duyệt Web."
                                    }
                                } else {
                                    statusFeedback = "Không thể tạo tệp DXF tạm."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 34.dp)
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Mở app CAD", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // NÚT 2: Xem trên Trình duyệt Web (Vector CAD)
                        Button(
                            onClick = {
                                if (tempHtmlPath != null) {
                                    val opened = PlatformFileOpener.openFile(tempHtmlPath, "text/html")
                                    statusFeedback = if (opened) {
                                        "Đang mở bản vẽ Vector trên Trình duyệt Web..."
                                    } else {
                                        "Không thể mở Trình duyệt Web."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)), // Teal
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 34.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Xem Web (A4)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // NÚT 3: Xuất DXF chính thức
                        Button(
                            onClick = onExportCad,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)), // Green
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 34.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xuất DXF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // NÚT 4: Vừa màn hình
                        Button(
                            onClick = {
                                scale = 1f
                                panOffset = Offset.Zero
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 34.dp)
                        ) {
                            Icon(Icons.Default.FitScreen, contentDescription = "Vừa màn hình", tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vừa khung", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }

                        // NÚT 5: Bật/Tắt lớp đường giao thông
                        Button(
                            onClick = { showRoads = !showRoads },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showRoads) Color(0xFF1E3A8A) else Color(0xFF334155)
                            ),
                            border = if (showRoads) BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 34.dp)
                        ) {
                            Icon(
                                Icons.Default.AltRoute,
                                contentDescription = "Ẩn/Hiện đường",
                                tint = if (showRoads) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showRoads) "Đường: Bật" else "Đường: Tắt",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (showRoads) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        }

                        // NÚT 6, 7, 8: Nhóm phóng to / thu nhỏ / hiển thị tỷ lệ zoom
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.heightIn(min = 34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { scale = (scale / 1.25f).coerceAtLeast(0.3f) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Thu nhỏ", tint = Color.White, modifier = Modifier.size(15.dp))
                                }

                                Text(
                                    text = "${(scale * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = { scale = (scale * 1.25f).coerceAtMost(6.0f) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Phóng to", tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }


            // ==============================================================
            // 3. THANH THÔNG TIN BÊN DƯỚI (FOOTER STATUS)
            // ==============================================================
            Surface(
                color = Color(0xFF0F172A),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { footerHeightPx = it.height }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    if (statusFeedback != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusFeedback!!,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Khổ A4 (297x210 mm) | Đối tượng: ${state.listCanvas.size}",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Text(
                            text = "Zoom: ${(scale * 100).toInt()}% • Chạm 2 ngón để zoom/pan",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}
