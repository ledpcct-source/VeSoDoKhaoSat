package com.vesodo.khaosat.drawing

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.math.Function_tinhtoan_ve
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DrawingRenderer {

    private val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)

    fun renderAll(
        drawScope: DrawScope,
        items: List<Obj_CANVAS>,
        selectedItem: Obj_CANVAS?,
        textMeasurer: TextMeasurer,
        fontSizeSp: Float,
        iconBitmaps: Map<Int, ImageBitmap>,
        showSelection: Boolean = true
    ) {
        with(drawScope) {
            val activeSelected = if (showSelection) selectedItem else null

            // 1. Vẽ các đường trước (lines, dashed lines, double lines, arrows)
            items.forEach { cv ->
                if (isLineCommand(cv.LENH)) {
                    val isSelected = (activeSelected != null && cv == activeSelected)
                    renderLineElement(
                        cv = cv,
                        isSelected = isSelected,
                        textMeasurer = textMeasurer,
                        fontSizeSp = fontSizeSp
                    )
                }
            }

            // 2. Vẽ các đối tượng icon / trạm / trụ / thiết bị
            items.forEach { cv ->
                if (!isLineCommand(cv.LENH)) {
                    val isSelected = (activeSelected != null && cv == activeSelected)
                    renderIconElement(
                        cv = cv,
                        isSelected = isSelected,
                        textMeasurer = textMeasurer,
                        fontSizeSp = fontSizeSp,
                        iconBitmap = iconBitmaps[cv.LENH]
                    )
                }
            }
        }
    }

    fun isLineCommand(command: Int): Boolean {
        return command == Variables.VE_DUONG_THANG ||
                command == Variables.VE_LINE_DUT_NET ||
                command == Variables.VE_2LINE ||
                command == Variables.VE_MUI_TEN ||
                command == Variables.VE_ARROW ||
                command == Variables.VE_TU_DO
    }

    private fun DrawScope.renderLineElement(
        cv: Obj_CANVAS,
        isSelected: Boolean,
        textMeasurer: TextMeasurer,
        fontSizeSp: Float
    ) {
        val lineColor = if (isSelected) Color(0xFFE53935) else Color(0xFF0F172A)
        val strokeWidth = if (isSelected) 5f else 3.5f
        val haloColor = Color.White.copy(alpha = 0.85f)
        val haloWidth = strokeWidth + 3.2f

        val start = Offset(cv.X1, cv.Y1)
        val end = Offset(cv.X3, cv.Y3)

        when (cv.LENH) {
            Variables.VE_DUONG_THANG -> {
                drawLine(
                    color = haloColor,
                    start = start,
                    end = end,
                    strokeWidth = haloWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lineColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                renderLineLabel(cv.TEN, start, end, textMeasurer, fontSizeSp, isSelected)
            }

            Variables.VE_LINE_DUT_NET -> {
                drawLine(
                    color = haloColor,
                    start = start,
                    end = end,
                    strokeWidth = haloWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lineColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    pathEffect = dashedEffect,
                    cap = StrokeCap.Round
                )
                renderLineLabel(cv.TEN, start, end, textMeasurer, fontSizeSp, isSelected)
            }

            Variables.VE_2LINE -> {
                // Thuật toán vẽ 2 đường song song cách nhau 15px từ Function_ve.java
                if (abs(cv.Y3 - cv.Y1) < abs(cv.X3 - cv.X1)) {
                    drawLine(
                        color = haloColor,
                        start = Offset(cv.X1, cv.Y1 - 12f),
                        end = Offset(cv.X3, cv.Y3 - 12f),
                        strokeWidth = haloWidth
                    )
                    drawLine(
                        color = haloColor,
                        start = Offset(cv.X1, cv.Y1 + 12f),
                        end = Offset(cv.X3, cv.Y3 + 12f),
                        strokeWidth = haloWidth
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(cv.X1, cv.Y1 - 12f),
                        end = Offset(cv.X3, cv.Y3 - 12f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(cv.X1, cv.Y1 + 12f),
                        end = Offset(cv.X3, cv.Y3 + 12f),
                        strokeWidth = strokeWidth
                    )
                } else {
                    drawLine(
                        color = haloColor,
                        start = Offset(cv.X1 - 12f, cv.Y1),
                        end = Offset(cv.X3 - 12f, cv.Y3),
                        strokeWidth = haloWidth
                    )
                    drawLine(
                        color = haloColor,
                        start = Offset(cv.X1 + 12f, cv.Y1),
                        end = Offset(cv.X3 + 12f, cv.Y3),
                        strokeWidth = haloWidth
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(cv.X1 - 12f, cv.Y1),
                        end = Offset(cv.X3 - 12f, cv.Y3),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(cv.X1 + 12f, cv.Y1),
                        end = Offset(cv.X3 + 12f, cv.Y3),
                        strokeWidth = strokeWidth
                    )
                }
                renderLineLabel(cv.TEN, start, end, textMeasurer, fontSizeSp, isSelected)
            }

            Variables.VE_MUI_TEN, Variables.VE_ARROW -> {
                val arrow = Function_tinhtoan_ve.TINH_MUI_TEN(cv.X1, cv.Y1, cv.X3, cv.Y3, 22)
                val isDashed = (cv.THUOC_TINH == 1)
                val effect = if (isDashed) dashedEffect else null

                // Viền sáng halo bảo vệ độ tương phản trên nền bản đồ
                drawLine(
                    color = haloColor,
                    start = start,
                    end = end,
                    strokeWidth = haloWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = haloColor,
                    start = Offset(arrow.wing1.x, arrow.wing1.y),
                    end = end,
                    strokeWidth = haloWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = haloColor,
                    start = Offset(arrow.wing2.x, arrow.wing2.y),
                    end = end,
                    strokeWidth = haloWidth,
                    cap = StrokeCap.Round
                )
                // Thân đường mũi tên
                drawLine(
                    color = lineColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    pathEffect = effect,
                    cap = StrokeCap.Round
                )
                // Đầu mũi tên theo thuật toán ThtCanvas.ve_mui_ten
                drawLine(
                    color = lineColor,
                    start = Offset(arrow.wing1.x, arrow.wing1.y),
                    end = end,
                    strokeWidth = strokeWidth,
                    pathEffect = effect,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lineColor,
                    start = Offset(arrow.wing2.x, arrow.wing2.y),
                    end = end,
                    strokeWidth = strokeWidth,
                    pathEffect = effect,
                    cap = StrokeCap.Round
                )

                renderLineLabel(cv.TEN, start, end, textMeasurer, fontSizeSp, isSelected)
            }
        }

        // Nếu được chọn, vẽ các điểm chốt ở đầu, cuối và trung điểm để người dùng dễ kéo thả
        if (isSelected) {
            val mid = Offset((cv.X1 + cv.X3) / 2f, (cv.Y1 + cv.Y3) / 2f)

            // Điểm mút P1 (Đầu 1): Vòng tròn xanh biển (Chỉ di chuyển đầu mút P1)
            drawCircle(color = Color(0xFF0284C7), radius = 9f, center = start)
            drawCircle(color = Color.White, radius = 4.5f, center = start)

            // Điểm mút P2 (Đầu 2): Vòng tròn xanh biển (Chỉ di chuyển đầu mút P2)
            drawCircle(color = Color(0xFF0284C7), radius = 9f, center = end)
            drawCircle(color = Color.White, radius = 4.5f, center = end)

            // Điểm trung tâm (Midpoint): Vòng tròn vàng cam lớn hơn để kéo di chuyển cả khối
            drawCircle(color = Color(0xFFD97706), radius = 10f, center = mid)
            drawCircle(color = Color.White, radius = 5f, center = mid)
            drawCircle(color = Color(0xFFB45309), radius = 2.5f, center = mid)
        }
    }

    private fun DrawScope.renderLineLabel(
        text: String,
        start: Offset,
        end: Offset,
        textMeasurer: TextMeasurer,
        fontSizeSp: Float,
        isSelected: Boolean
    ) {
        if (text.isBlank()) return

        // 1. Toạ độ trung điểm chính xác của đoạn thẳng
        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f

        // 2. Vector pháp tuyến đơn vị vuông góc với đoạn thẳng
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = sqrt(dx * dx + dy * dy)

        var nx = 0f
        var ny = -1f
        if (len > 0.001f) {
            nx = -dy / len
            ny = dx / len
            // Luôn ưu tiên hướng lên trên (ny < 0) hoặc hướng sang phải (nx > 0)
            if (ny > 0.05f || (abs(ny) <= 0.05f && nx < 0f)) {
                nx = -nx
                ny = -ny
            }
        }

        // 3. Đo kích thước chữ
        val textLayoutResult = textMeasurer.measure(
            text = text,
            style = TextStyle(
                fontSize = fontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFFDC2626) else Color(0xFFB91C1C)
            )
        )

        // 4. Kích thước khung bo cong (rounded pill badge)
        val padH = 8f
        val padV = 3.5f
        val badgeW = textLayoutResult.size.width + padH * 2
        val badgeH = textLayoutResult.size.height + padV * 2

        // 5. Đặt tâm nhãn lệch khỏi đường thẳng theo vector pháp tuyến (tuyệt đối không che dây)
        val clearance = 10f + (badgeH / 2f)
        val badgeCenterX = midX + nx * clearance
        val badgeCenterY = midY + ny * clearance
        val badgeLeft = badgeCenterX - badgeW / 2f
        val badgeTop = badgeCenterY - badgeH / 2f

        // Đổ bóng mờ nhẹ tạo chiều sâu nổi bật
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.12f),
            topLeft = Offset(badgeLeft, badgeTop + 1.5f),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Nền trắng bo góc tinh tế (hơi cong)
        drawRoundRect(
            color = Color(0xFFFFFFFF).copy(alpha = 0.95f),
            topLeft = Offset(badgeLeft, badgeTop),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Đường viền bo mỏng trang nhã
        drawRoundRect(
            color = if (isSelected) Color(0xFFEF4444) else Color(0xFFCBD5E1),
            topLeft = Offset(badgeLeft, badgeTop),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 1f)
        )
        // Vẽ chữ căn giữa hoàn hảo
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(badgeLeft + padH, badgeTop + padV)
        )
    }

    private fun DrawScope.renderIconElement(
        cv: Obj_CANVAS,
        isSelected: Boolean,
        textMeasurer: TextMeasurer,
        fontSizeSp: Float,
        iconBitmap: ImageBitmap?
    ) {
        val center = Offset(cv.X3, cv.Y3)

        if (cv.LENH == Variables.VE_TEXT) {
            // Vẽ chuỗi text độc lập: Bao bọc bởi hình chữ nhật nét đứt, tối đa 1/3 bề ngang màn hình, tự động xuống dòng
            val maxBoxW = if (size.width > 100f) size.width / 3f else 320f
            val padH = 10f
            val padV = 6f
            val maxTextW = maxOf(40f, maxBoxW - padH * 2)
            val text = cv.TEN.ifBlank { "Văn bản" }
            val layout = textMeasurer.measure(
                text = text,
                style = TextStyle(
                    fontSize = fontSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                ),
                constraints = Constraints(maxWidth = maxTextW.toInt())
            )
            val tagW = layout.size.width.toFloat() + padH * 2
            val tagH = layout.size.height.toFloat() + padV * 2
            val tagLeft = cv.X3
            val tagTop = cv.Y3
            val rect = Rect(tagLeft, tagTop, tagLeft + tagW, tagTop + tagH)
            val pivot = Offset(rect.left + rect.width / 2f, rect.top + rect.height / 2f)

            val drawTextBox: DrawScope.() -> Unit = {
                // 1. Bóng mờ nhẹ
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.08f),
                    topLeft = Offset(tagLeft, tagTop + 1.5f),
                    size = Size(tagW, tagH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // 2. Nền trắng/xanh nhạt
                drawRoundRect(
                    color = if (isSelected) Color(0xFFEFF6FF).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.92f),
                    topLeft = Offset(tagLeft, tagTop),
                    size = Size(tagW, tagH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // 3. Hình chữ nhật nét đứt bao bọc text theo yêu cầu
                drawRoundRect(
                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                    topLeft = Offset(tagLeft, tagTop),
                    size = Size(tagW, tagH),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(
                        width = if (isSelected) 2f else 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                )
                // 4. Nội dung text tự động xuống dòng
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(tagLeft + padH, tagTop + padV)
                )
            }

            val currentScale = if (cv.SCALE > 0f) cv.SCALE else 1f
            val drawScaledTextBox: DrawScope.() -> Unit = {
                if (currentScale != 1f) {
                    scale(scale = currentScale, pivot = pivot) {
                        drawTextBox()
                    }
                } else {
                    drawTextBox()
                }
            }

            if (cv.ROTATION != 0f) {
                rotate(degrees = cv.ROTATION, pivot = pivot) {
                    drawScaledTextBox()
                }
            } else {
                drawScaledTextBox()
            }
            return
        }

        // Vẽ icon đối tượng
        if (iconBitmap != null) {
            val imgW = iconBitmap.width.toFloat()
            val imgH = iconBitmap.height.toFloat()
            val currentScale = if (cv.SCALE > 0f) cv.SCALE else 1f
            val topLeft = Offset(center.x - imgW / 2f, center.y - imgH / 2f)

            if (isSelected) {
                // Viền chọn nổi bật xung quanh icon (thích ứng theo SCALE)
                val selRadius = ((maxOf(imgW, imgH) * currentScale) / 2f) + 12f
                drawCircle(
                    color = Color(0xFFE53935).copy(alpha = 0.20f),
                    radius = selRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFE53935),
                    radius = selRadius,
                    center = center,
                    style = Stroke(width = 2f, pathEffect = dashedEffect)
                )

                // Huy hiệu tỉ lệ thu phóng (%) khi khác chuẩn 1.0x
                if (currentScale != 1f) {
                    val zoomText = "${(currentScale * 100).toInt()}%"
                    val badgeLayout = textMeasurer.measure(
                        text = zoomText,
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    val bPadH = 4f
                    val bPadV = 2f
                    val bW = badgeLayout.size.width + bPadH * 2
                    val bH = badgeLayout.size.height + bPadV * 2
                    val bLeft = center.x + selRadius - bW / 2f
                    val bTop = center.y - selRadius - bH / 2f
                    drawRoundRect(
                        color = Color(0xFF0284C7),
                        topLeft = Offset(bLeft, bTop),
                        size = Size(bW, bH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawText(
                        textLayoutResult = badgeLayout,
                        topLeft = Offset(bLeft + bPadH, bTop + bPadV)
                    )
                }
            }

            // Xoay icon theo góc ROTATION và phóng to/thu nhỏ theo SCALE
            val drawIconBody: DrawScope.() -> Unit = {
                if (cv.ROTATION != 0f) {
                    rotate(degrees = cv.ROTATION, pivot = center) {
                        drawImage(
                            image = iconBitmap,
                            topLeft = topLeft
                        )
                    }
                } else {
                    drawImage(
                        image = iconBitmap,
                        topLeft = topLeft
                    )
                }
            }

            if (currentScale != 1f) {
                scale(scale = currentScale, pivot = center) {
                    drawIconBody()
                }
            } else {
                drawIconBody()
            }

            // Vẽ nhãn văn bản của đối tượng trên nền bo góc hơi cong (nằm bên dưới, KHÔNG che icon)
            if (cv.TEN.isNotBlank()) {
                val label = cv.TEN
                val textLayout = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        fontSize = (fontSizeSp - 1f).coerceAtLeast(10.5f).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFFDC2626) else Color(0xFF1E293B)
                    )
                )

                val padH = 7f
                val padV = 3f
                val tagW = textLayout.size.width + padH * 2
                val tagH = textLayout.size.height + padV * 2

                // Đặt nhãn nằm an toàn dưới chân icon (tính theo bán kính bao quanh đã scale để khi xoay/zoom không che icon)
                val safeBoundingR = (maxOf(imgW, imgH) * currentScale) / 2f
                val tagLeft = center.x - (tagW / 2f)
                val tagTop = center.y + safeBoundingR + 6f

                // Bóng mờ nhẹ
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.12f),
                    topLeft = Offset(tagLeft, tagTop + 1.2f),
                    size = Size(tagW, tagH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Nền bo cong nhẹ thanh lịch
                drawRoundRect(
                    color = Color(0xFFFFFFFF).copy(alpha = 0.95f),
                    topLeft = Offset(tagLeft, tagTop),
                    size = Size(tagW, tagH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Viền bo mỏng tinh tế
                drawRoundRect(
                    color = if (isSelected) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                    topLeft = Offset(tagLeft, tagTop),
                    size = Size(tagW, tagH),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1f)
                )
                // Chữ căn giữa
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(tagLeft + padH, tagTop + padV)
                )
            }
        } else {
            // Fallback nếu bitmap chưa load: vẽ vòng tròn biểu tượng
            val currentScale = if (cv.SCALE > 0f) cv.SCALE else 1f
            val baseRadius = 16f * currentScale
            drawCircle(
                color = if (isSelected) Color(0xFFE53935) else Color(0xFF00897B),
                radius = baseRadius,
                center = center
            )
            if (cv.ROTATION != 0f) {
                rotate(degrees = cv.ROTATION, pivot = center) {
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = Offset(center.x, center.y - 12f * currentScale),
                        strokeWidth = 2.5f * currentScale,
                        cap = StrokeCap.Round
                    )
                }
            }
            if (cv.TEN.isNotBlank()) {
                val fallbackLayout = textMeasurer.measure(
                    text = cv.TEN,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                )
                val padH = 6f
                val padV = 3f
                val tagW = fallbackLayout.size.width + padH * 2
                val tagH = fallbackLayout.size.height + padV * 2
                val tagLeft = center.x - (tagW / 2f)
                val tagTop = center.y + 16f + 6f

                drawRoundRect(
                    color = Color(0xFFFFFFFF).copy(alpha = 0.95f),
                    topLeft = Offset(tagLeft, tagTop),
                    size = Size(tagW, tagH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = if (isSelected) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                    topLeft = Offset(tagLeft, tagTop),
                    size = Size(tagW, tagH),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1f)
                )
                drawText(
                    textLayoutResult = fallbackLayout,
                    topLeft = Offset(tagLeft + padH, tagTop + padV)
                )
            }
        }
    }

    /**
     * Vẽ bản xem trước khi người dùng đang kéo ngón tay trên màn hình
     */
    fun renderLivePreview(
        drawScope: DrawScope,
        command: Int,
        start: Offset,
        current: Offset,
        snappedCurrent: Offset,
        iconBitmap: ImageBitmap?
    ) {
        with(drawScope) {
            when (command) {
                Variables.VE_DUONG_THANG -> {
                    drawLine(
                        color = Color(0xFF1976D2).copy(alpha = 0.7f),
                        start = start,
                        end = snappedCurrent,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                Variables.VE_LINE_DUT_NET -> {
                    drawLine(
                        color = Color(0xFF1976D2).copy(alpha = 0.7f),
                        start = start,
                        end = snappedCurrent,
                        strokeWidth = 3f,
                        pathEffect = dashedEffect,
                        cap = StrokeCap.Round
                    )
                }
                Variables.VE_2LINE -> {
                    drawLine(
                        color = Color(0xFF1976D2).copy(alpha = 0.7f),
                        start = Offset(start.x, start.y - 10f),
                        end = Offset(snappedCurrent.x, snappedCurrent.y - 10f),
                        strokeWidth = 2.5f
                    )
                    drawLine(
                        color = Color(0xFF1976D2).copy(alpha = 0.7f),
                        start = Offset(start.x, start.y + 10f),
                        end = Offset(snappedCurrent.x, snappedCurrent.y + 10f),
                        strokeWidth = 2.5f
                    )
                }
                Variables.VE_MUI_TEN, Variables.VE_ARROW -> {
                    drawLine(
                        color = Color(0xFF1976D2).copy(alpha = 0.7f),
                        start = start,
                        end = snappedCurrent,
                        strokeWidth = 3f
                    )
                    val arrow = Function_tinhtoan_ve.TINH_MUI_TEN(start.x, start.y, snappedCurrent.x, snappedCurrent.y, 20)
                    drawLine(
                        color = Color(0xFF1976D2).copy(alpha = 0.7f),
                        start = Offset(arrow.wing1.x, arrow.wing1.y),
                        end = snappedCurrent,
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color(0xFF1976D2).copy(alpha = 0.7f),
                        start = Offset(arrow.wing2.x, arrow.wing2.y),
                        end = snappedCurrent,
                        strokeWidth = 3f
                    )
                }
                Variables.MOVE -> {
                    // Hiển thị đường gióng di chuyển
                    drawLine(
                        color = Color(0xFFFB8C00).copy(alpha = 0.6f),
                        start = start,
                        end = current,
                        strokeWidth = 2f,
                        pathEffect = dashedEffect
                    )
                    drawCircle(
                        color = Color(0xFFFB8C00),
                        radius = 8f,
                        center = current
                    )
                }
                else -> {
                    // Preview icon tại vị trí snap
                    if (iconBitmap != null) {
                        drawImage(
                            image = iconBitmap,
                            topLeft = Offset(
                                snappedCurrent.x - iconBitmap.width / 2f,
                                snappedCurrent.y - iconBitmap.height / 2f
                            ),
                            alpha = 0.65f
                        )
                    } else {
                        drawCircle(
                            color = Color(0xFF1976D2).copy(alpha = 0.5f),
                            radius = 16f,
                            center = snappedCurrent
                        )
                    }
                }
            }

            // Điểm nam châm snap guide
            drawCircle(
                color = Color(0xFF4CAF50),
                radius = 6f,
                center = snappedCurrent
            )
        }
    }

    /**
     * Tính toán hình chữ nhật bao quanh đối tượng văn bản (giới hạn tối đa 1/3 bề ngang màn hình, tự động xuống dòng)
     */
    fun getTextBounds(
        cv: Obj_CANVAS,
        textMeasurer: TextMeasurer,
        fontSizeSp: Float,
        canvasWidth: Float
    ): Rect {
        val maxBoxW = if (canvasWidth > 100f) canvasWidth / 3f else 320f
        val padH = 10f
        val padV = 6f
        val maxTextW = maxOf(40f, maxBoxW - padH * 2)
        val text = cv.TEN.ifBlank { "Văn bản" }
        val layout = textMeasurer.measure(
            text = text,
            style = TextStyle(
                fontSize = fontSizeSp.sp,
                fontWeight = FontWeight.Bold
            ),
            constraints = Constraints(maxWidth = maxTextW.toInt())
        )
        val tagW = layout.size.width.toFloat() + padH * 2
        val tagH = layout.size.height.toFloat() + padV * 2
        return Rect(cv.X3, cv.Y3, cv.X3 + tagW, cv.Y3 + tagH)
    }

    /**
     * Kiểm tra một điểm chạm (touch point) có nằm trong vùng hình chữ nhật của khối văn bản hay không
     */
    fun isPointInTextItem(
        point: Offset,
        cv: Obj_CANVAS,
        textMeasurer: TextMeasurer,
        fontSizeSp: Float,
        canvasWidth: Float,
        extraPadding: Float = 14f
    ): Boolean {
        val rect = getTextBounds(cv, textMeasurer, fontSizeSp, canvasWidth)
        val scale = if (cv.SCALE > 0f) cv.SCALE else 1f
        val pivotX = rect.left + rect.width / 2f
        val pivotY = rect.top + rect.height / 2f
        val scaledW = rect.width * scale
        val scaledH = rect.height * scale
        val expandedRect = Rect(
            left = pivotX - scaledW / 2f - extraPadding,
            top = pivotY - scaledH / 2f - extraPadding,
            right = pivotX + scaledW / 2f + extraPadding,
            bottom = pivotY + scaledH / 2f + extraPadding
        )
        if (cv.ROTATION == 0f) {
            return expandedRect.contains(point)
        }
        // Nếu văn bản có góc xoay, xoay ngược toạ độ điểm chạm quanh tâm pivot
        val rad = -cv.ROTATION * (PI / 180.0)
        val cosA = cos(rad)
        val sinA = sin(rad)
        val dx = point.x - pivotX
        val dy = point.y - pivotY
        val unrotX = (dx * cosA - dy * sinA + pivotX).toFloat()
        val unrotY = (dx * sinA + dy * cosA + pivotY).toFloat()
        return expandedRect.contains(Offset(unrotX, unrotY))
    }
}
