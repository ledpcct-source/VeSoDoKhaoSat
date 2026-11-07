package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.ai.DiagramRuleEngine
import com.vesodo.khaosat.debug.DebugLogger
import com.vesodo.khaosat.debug.LogType
import com.vesodo.khaosat.drawing.DragHandleType
import com.vesodo.khaosat.drawing.DrawingRenderer
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.map.GpsBasemapManager
import com.vesodo.khaosat.map.GpsVectorRoadManager
import com.vesodo.khaosat.math.Function_tinhtoan_ve
import com.vesodo.khaosat.math.Point2D
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import com.vesodo.khaosat.qr.SurveyQrDetailDialog
import com.vesodo.khaosat.qr.SurveyQrManager
import org.jetbrains.compose.resources.imageResource
import kotlin.math.abs
import kotlin.math.roundToInt


@Composable
fun CanvasArea(
    state: DrawingState,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val coroutineScope = rememberCoroutineScope()

    // Tải trước ImageBitmap của các icon vẽ
    val iconBitmaps = remember { mutableStateMapOf<Int, ImageBitmap>() }

    // Nạp toàn bộ icon bitmap vào map để vẽ siêu tốc
    IconRegistry.ALL_TOOLS.forEach { tool ->
        val bitmap = imageResource(tool.canvasIconResource)
        iconBitmaps[tool.command] = bitmap
    }

    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentOffset by remember { mutableStateOf<Offset?>(null) }
    var snappedCurrentOffset by remember { mutableStateOf<Offset?>(null) }
    var itemBeingMoved by remember { mutableStateOf<Obj_CANVAS?>(null) }
    var activeHandleType by remember { mutableStateOf(DragHandleType.WHOLE_ITEM) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current
    val touchSlop = viewConfiguration.touchSlop

    // Kích thước vùng chạm thích ứng màn hình Android (tối thiểu 44dp theo chuẩn Material)
    val handleRadiusPx = with(density) { 36.dp.toPx() }.toDouble()
    val iconRadiusPx = with(density) { 44.dp.toPx() }.toDouble()
    val lineMidRadiusPx = with(density) { 36.dp.toPx() }.toDouble()
    val lineSegmentRadiusPx = with(density) { 24.dp.toPx() }.toDouble()

    val selItem = state.selectedItem
    val isSinglePointSelected = selItem != null &&
            !DrawingRenderer.isLineCommand(selItem.LENH) &&
            !state.showExportDialog
    val isObjectControlActive = selItem != null && !state.showExportDialog

    var rotationPanelBoundsInCanvas by remember { mutableStateOf<Rect?>(null) }
    var customerSuggestionBoundsInCanvas by remember { mutableStateOf<Rect?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }
    var lastClickedItemId by remember { mutableStateOf<Long?>(null) }
    var lastClickedItemRef by remember { mutableStateOf<Obj_CANVAS?>(null) }

    LaunchedEffect(isObjectControlActive) {
        if (!isObjectControlActive) {
            rotationPanelBoundsInCanvas = null
            customerSuggestionBoundsInCanvas = null
            showDeleteConfirmDialog = false
        }
    }

    // Hàm xuất chụp toàn bộ bản vẽ thành ImageBitmap theo đúng tỷ lệ chuẩn khổ giấy A4 (297mm x 210mm = 1.4142)
    LaunchedEffect(density, layoutDirection, textMeasurer, iconBitmaps) {
        state.captureCanvasBitmap = {
            val screenW = if (state.canvasSize.width > 0f) state.canvasSize.width else 1920f
            val screenH = if (state.canvasSize.height > 0f) state.canvasSize.height else 1080f

            val isPortrait = screenW < screenH
            val a4Width: Int
            val a4Height: Int
            if (isPortrait) {
                // Khổ A4 đứng (210 x 297)
                a4Height = maxOf(screenH.toInt(), 1920)
                a4Width = kotlin.math.round(a4Height * 210.0 / 297.0).toInt()
            } else {
                // Khổ A4 ngang (297 x 210 - Chuẩn hồ sơ kỹ thuật khảo sát)
                a4Width = maxOf(screenW.toInt(), 1920)
                a4Height = kotlin.math.round(a4Width * 210.0 / 297.0).toInt()
            }

            val deltaX = (a4Width - screenW) / 2f
            val deltaY = (a4Height - screenH) / 2f

            val bitmap = ImageBitmap(a4Width, a4Height)
            val canvas = Canvas(bitmap)
            val drawScope = CanvasDrawScope()
            drawScope.draw(density, layoutDirection, canvas, Size(a4Width.toFloat(), a4Height.toFloat())) {
                renderDiagramContent(
                    drawScope = this,
                    state = state,
                    size = Size(a4Width.toFloat(), a4Height.toFloat()),
                    textMeasurer = textMeasurer,
                    iconBitmaps = iconBitmaps,
                    isExporting = true,
                    exportOffsetX = deltaX,
                    exportOffsetY = deltaY,
                    origScreenWidth = screenW,
                    origScreenHeight = screenH
                )
            }
            bitmap
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .onGloballyPositioned { coords ->
                state.canvasBoundsInRoot = coords.boundsInRoot()
            }
            // Thu phóng toàn bộ bản vẽ / bản đồ khi giữ Ctrl + lăn chuột trên Desktop
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val isCtrl = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                            if (isCtrl) {
                                val change = event.changes.firstOrNull()
                                val deltaY = change?.scrollDelta?.y ?: 0f
                                if (deltaY != 0f) {
                                    val mousePos = change?.position ?: Offset(state.canvasSize.width / 2f, state.canvasSize.height / 2f)
                                    val zoomFactor = if (deltaY < 0f) 1.15f else (1f / 1.15f)
                                    state.zoomAllCanvas(zoomFactor, mousePos.x, mousePos.y)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    DebugLogger.liveCursorX = startPos.x
                    DebugLogger.liveCursorY = startPos.y

                    // 0. Bỏ qua nếu chạm vào thanh công cụ xoay hoặc control UI đã tiêu thụ sự kiện
                    val inRotationPanel = rotationPanelBoundsInCanvas?.contains(startPos) == true
                    val inCustomerSuggestion = customerSuggestionBoundsInCanvas?.contains(startPos) == true
                    if (inRotationPanel || inCustomerSuggestion || down.isConsumed) {
                        return@awaitEachGesture
                    }

                    val currentZ = if (state.zoomScale > 0.001f) state.zoomScale else 1f
                    val canvasStartPos = Offset(
                        (startPos.x - state.panOffsetX) / currentZ,
                        (startPos.y - state.panOffsetY) / currentZ
                    )
                    fun toCanvas(pos: Offset): Offset {
                        val z = if (state.zoomScale > 0.001f) state.zoomScale else 1f
                        return Offset((pos.x - state.panOffsetX) / z, (pos.y - state.panOffsetY) / z)
                    }

                    // 1. Phân biệt chạm đầu mút P1, đầu mút P2 hay thân/trung điểm (Hit-test)
                    val isDrawingIcon = !DrawingRenderer.isLineCommand(state.currentCommand) &&
                            state.currentCommand != Variables.MOVE &&
                            state.currentCommand != Variables.KO_VE

                    var hitItem: Obj_CANVAS? = null
                    var handleType = DragHandleType.WHOLE_ITEM

                    // Ngưỡng chạm ngón tay chọn đối tượng: giới hạn nghiêm ngặt trong khoảng 1-2 cm trên màn hình (~50dp)
                    val maxTouchThresholdDp = 50.dp
                    val maxTouchThresholdPx = with(density) { maxTouchThresholdDp.toPx() }.toDouble()

                    // Cho phép tìm và kéo di chuyển đối tượng khi ở chế độ MOVE hoặc khi đang có đối tượng được chọn (tắt khi đang ở chế độ Rải cột AI)
                    val canHitTest = (state.currentCommand == Variables.MOVE || state.selectedItem != null) && !state.isAutoSpacingActive
                    if (canHitTest) {
                        val sel = state.selectedItem

                        // --- BƯỚC 1: ƯU TIÊN SỐ 1 - TÌM ĐỐI TƯỢNG TRỤ ĐIỆN GẦN VỊ TRÍ CHẠM NHẤT TRONG PHẠM VI 1-2CM ---
                        var bestPole: Obj_CANVAS? = null
                        var minPoleDistPx = Double.MAX_VALUE

                        for (i in state.listCanvas.indices.reversed()) {
                            val cv = state.listCanvas[i]
                            if (IconRegistry.isTru(cv.LENH) || DiagramRuleEngine.isPoleCommand(cv.LENH)) {
                                val cvScreenX = cv.X3 * currentZ + state.panOffsetX
                                val cvScreenY = cv.Y3 * currentZ + state.panOffsetY
                                val screenDist = Function_tinhtoan_ve.TINH_KC(startPos.x, startPos.y, cvScreenX, cvScreenY)
                                val isCurrentlySelected = (sel != null && (sel === cv || (sel.STT != 0L && sel.STT == cv.STT)))
                                val radiusCap = if (isCurrentlySelected) maxTouchThresholdPx * 1.25 else maxTouchThresholdPx

                                if (screenDist <= radiusCap && screenDist < minPoleDistPx) {
                                    minPoleDistPx = screenDist
                                    bestPole = cv
                                }
                            }
                        }

                        if (bestPole != null) {
                            hitItem = bestPole
                            handleType = DragHandleType.WHOLE_ITEM
                        }

                        // --- BƯỚC 2: NẾU KHÔNG CÓ TRỤ ĐIỆN, TÌM CÁC ICON 1 TOẠ ĐỘ KHÁC GẦN NHẤT (Trạm, Công tơ, Nhà, Text...) ---
                        if (hitItem == null) {
                            var bestIcon: Obj_CANVAS? = null
                            var minIconDistPx = Double.MAX_VALUE

                            for (i in state.listCanvas.indices.reversed()) {
                                val cv = state.listCanvas[i]
                                if (!DrawingRenderer.isLineCommand(cv.LENH) && !IconRegistry.isTru(cv.LENH) && !DiagramRuleEngine.isPoleCommand(cv.LENH)) {
                                    if (cv.LENH == Variables.VE_TEXT) {
                                        val inBox = DrawingRenderer.isPointInTextItem(
                                            point = canvasStartPos,
                                            cv = cv,
                                            textMeasurer = textMeasurer,
                                            fontSizeSp = state.fontSize.sizeSp,
                                            canvasWidth = state.canvasSize.width,
                                            extraPadding = (maxTouchThresholdPx / currentZ).toFloat()
                                        )
                                        if (inBox) {
                                            val cvScreenX = cv.X3 * currentZ + state.panOffsetX
                                            val cvScreenY = cv.Y3 * currentZ + state.panOffsetY
                                            val screenDist = Function_tinhtoan_ve.TINH_KC(startPos.x, startPos.y, cvScreenX, cvScreenY)
                                            if (screenDist < minIconDistPx) {
                                                minIconDistPx = screenDist
                                                bestIcon = cv
                                            }
                                        }
                                    } else {
                                        val cvScreenX = cv.X3 * currentZ + state.panOffsetX
                                        val cvScreenY = cv.Y3 * currentZ + state.panOffsetY
                                        val screenDist = Function_tinhtoan_ve.TINH_KC(startPos.x, startPos.y, cvScreenX, cvScreenY)
                                        val isCurrentlySelected = (sel != null && (sel === cv || (sel.STT != 0L && sel.STT == cv.STT)))
                                        val radiusCap = if (isCurrentlySelected) maxTouchThresholdPx * 1.25 else maxTouchThresholdPx

                                        if (screenDist <= radiusCap && screenDist < minIconDistPx) {
                                            minIconDistPx = screenDist
                                            bestIcon = cv
                                        }
                                    }
                                }
                            }

                            if (bestIcon != null) {
                                hitItem = bestIcon
                                handleType = DragHandleType.WHOLE_ITEM
                            }
                        }

                        // --- BƯỚC 3: CHỈ KHI KHÔNG TRÚNG BẤT KỲ TRỤ ĐIỆN HAY ICON NÀO, MỚI XÉT ĐỐI TƯỢNG DÂY (2 TOẠ ĐỘ) ---
                        if (hitItem == null) {
                            var bestLine: Obj_CANVAS? = null
                            var bestLineHandle = DragHandleType.WHOLE_ITEM
                            var minLineDistPx = Double.MAX_VALUE

                            // Quét danh sách dây, ưu tiên dây đang chọn nếu có
                            val candidateLines = if (sel != null && DrawingRenderer.isLineCommand(sel.LENH)) {
                                listOf(sel) + state.listCanvas.filter { it !== sel && DrawingRenderer.isLineCommand(it.LENH) }
                            } else {
                                state.listCanvas.filter { DrawingRenderer.isLineCommand(it.LENH) }
                            }

                            for (cv in candidateLines) {
                                val p1ScreenX = cv.X1 * currentZ + state.panOffsetX
                                val p1ScreenY = cv.Y1 * currentZ + state.panOffsetY
                                val p2ScreenX = cv.X3 * currentZ + state.panOffsetX
                                val p2ScreenY = cv.Y3 * currentZ + state.panOffsetY

                                val d1Px = Function_tinhtoan_ve.TINH_KC(startPos.x, startPos.y, p1ScreenX, p1ScreenY)
                                val d2Px = Function_tinhtoan_ve.TINH_KC(startPos.x, startPos.y, p2ScreenX, p2ScreenY)
                                val dsegPx = Function_tinhtoan_ve.DIST_POINT_TO_SEGMENT(startPos.x, startPos.y, p1ScreenX, p1ScreenY, p2ScreenX, p2ScreenY)

                                if (d1Px <= maxTouchThresholdPx && d1Px < minLineDistPx) {
                                    minLineDistPx = d1Px
                                    bestLine = cv
                                    bestLineHandle = DragHandleType.LINE_START
                                } else if (d2Px <= maxTouchThresholdPx && d2Px < minLineDistPx) {
                                    minLineDistPx = d2Px
                                    bestLine = cv
                                    bestLineHandle = DragHandleType.LINE_END
                                } else if (dsegPx <= with(density) { 26.dp.toPx() } && dsegPx < minLineDistPx) {
                                    minLineDistPx = dsegPx
                                    bestLine = cv
                                    bestLineHandle = DragHandleType.WHOLE_ITEM
                                }
                            }

                            if (bestLine != null) {
                                hitItem = bestLine
                                handleType = bestLineHandle
                            }
                        }
                    }

                    activeHandleType = handleType

                    // Chế độ Pan map: Khi ở chế độ MOVE và không bấm trúng đối tượng nào (chạm ngoài khoảng 1-2cm vào nền bản đồ)
                    val isPanMode = (state.currentCommand == Variables.MOVE && hitItem == null && !state.isAutoSpacingActive)
                    var isPanningMap = isPanMode

                    var totalDx = 0f
                    var totalDy = 0f
                    var lastPos = startPos

                    if (hitItem != null) {
                        itemBeingMoved = hitItem
                        state.selectItem(hitItem)
                        // Nếu là đối tượng 1 toạ độ (icon: trụ, trạm, công tơ, nhà) và đang nằm ở vùng nửa dưới màn hình (dễ bị thanh xoay che):
                        val screenY = hitItem.Y3 * currentZ + state.panOffsetY
                        val screenH = if (state.canvasSize.height > 0f) state.canvasSize.height else 1080f
                        if (!DrawingRenderer.isLineCommand(hitItem.LENH) && screenY > screenH * 0.65f) {
                            state.centerViewportOn(hitItem.X3, hitItem.Y3)
                        }
                        if (handleType != DragHandleType.NONE) {
                            state.startDragTranslate(hitItem, handleType)
                        }
                        val desc = "${hitItem.TEN.ifBlank { "Mục #" + hitItem.STT }} (Lệnh ${hitItem.LENH})"
                        val hLabel = when (handleType) {
                            DragHandleType.LINE_START -> "Đầu mút P1 (chỉ di chuyển đầu này)"
                            DragHandleType.LINE_END -> "Đầu mút P2 (chỉ di chuyển đầu này)"
                            DragHandleType.WHOLE_ITEM -> "Toàn khối (di chuyển đối tượng)"
                            DragHandleType.NONE -> "Thân đường"
                        }
                        DebugLogger.logSelect("$desc", hitItem.X3, hitItem.Y3)
                        DebugLogger.logDragStart("$desc - $hLabel", startPos.x, startPos.y, isExistingItem = true)
                    } else {
                        itemBeingMoved = null
                        val snappedStart = if (DrawingRenderer.isLineCommand(state.currentCommand) || state.isAutoSpacingActive) {
                            Function_tinhtoan_ve.KT_DIEM_GAN_NHAT(
                                state.listCanvas,
                                canvasStartPos.x,
                                canvasStartPos.y,
                                state.snapDistance / currentZ
                            )
                        } else {
                            Point2D(canvasStartPos.x, canvasStartPos.y)
                        }
                        dragStartOffset = Offset(snappedStart.x, snappedStart.y)
                        dragCurrentOffset = Offset(snappedStart.x, snappedStart.y)
                        snappedCurrentOffset = Offset(snappedStart.x, snappedStart.y)
                        DebugLogger.logDragStart(
                            if (state.isAutoSpacingActive) "Rải cột AI"
                            else if (isPanMode) "Kéo bản đồ GPS"
                            else "Lệnh ${state.currentCommand}",
                            startPos.x, startPos.y, isExistingItem = false
                        )
                    }

                    // 2. Vòng lặp kéo di chuyển & phát hiện cử chỉ 2 ngón tay thu phóng (Pinch-to-zoom)
                    var isPinchingItem = false
                    var initialPinchSpan = 0f
                    var initialItemScale = state.selectedItemScale

                    var isPinchingCanvas = false
                    var lastCanvasPinchSpan = 0f
                    var lastCanvasPinchCenter = Offset.Zero

                    do {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }

                        // CỬ CHỈ 2 NGÓN TAY THU PHÓNG (PINCH TO ZOOM) TRÊN MOBILE
                        val currentSel = state.selectedItem
                        val canPinchZoomItem = currentSel != null && !DrawingRenderer.isLineCommand(currentSel.LENH)

                        if (pressedChanges.size >= 2) {
                            val p1 = pressedChanges[0].position
                            val p2 = pressedChanges[1].position
                            val currentSpan = Function_tinhtoan_ve.TINH_KC(p1.x, p1.y, p2.x, p2.y).toFloat()
                            val currentCenter = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                            if (canPinchZoomItem) {
                                // TRƯỜNG HỢP A: Đang chọn 1 đối tượng -> Thu phóng riêng đối tượng đó
                                if (!isPinchingItem) {
                                    isPinchingItem = true
                                    initialPinchSpan = currentSpan
                                    initialItemScale = state.selectedItemScale
                                    itemBeingMoved = null // Hủy kéo di chuyển để tránh lệch toạ độ
                                    isPanningMap = false
                                    dragStartOffset = null
                                    dragCurrentOffset = null
                                    snappedCurrentOffset = null
                                } else if (initialPinchSpan > 15f) {
                                    val factor = currentSpan / initialPinchSpan
                                    val targetScale = (initialItemScale * factor).coerceIn(0.4f, 3.5f)
                                    state.scaleSelectedItem(targetScale, saveHistory = false)
                                }
                            } else {
                                // TRƯỜNG HỢP B: KHÔNG chọn đối tượng nào -> Thu phóng & trượt toàn bộ bảng vẽ
                                if (!isPinchingCanvas) {
                                    isPinchingCanvas = true
                                    lastCanvasPinchSpan = currentSpan
                                    lastCanvasPinchCenter = currentCenter
                                    itemBeingMoved = null
                                    isPanningMap = false
                                    dragStartOffset = null
                                    dragCurrentOffset = null
                                    snappedCurrentOffset = null
                                } else if (lastCanvasPinchSpan > 15f && currentSpan > 15f) {
                                    val spanFactor = currentSpan / lastCanvasPinchSpan
                                    val panDx = currentCenter.x - lastCanvasPinchCenter.x
                                    val panDy = currentCenter.y - lastCanvasPinchCenter.y

                                    val newZoom = (state.zoomScale * spanFactor).coerceIn(0.25f, 4.0f)
                                    val actualFactor = newZoom / state.zoomScale

                                    // Dịch chuyển pan và zoom mượt mà quanh tâm 2 ngón tay currentCenter
                                    state.panOffsetX = currentCenter.x - (currentCenter.x - state.panOffsetX) * actualFactor + panDx
                                    state.panOffsetY = currentCenter.y - (currentCenter.y - state.panOffsetY) * actualFactor + panDy
                                    state.zoomScale = newZoom
                                    state.renderVersion++

                                    lastCanvasPinchSpan = currentSpan
                                    lastCanvasPinchCenter = currentCenter
                                }
                            }

                            pressedChanges.forEach { it.consume() }
                            continue
                        }

                        if (isPinchingItem || isPinchingCanvas) {
                            state.saveHistorySnapshot()
                            isPinchingItem = false
                            isPinchingCanvas = false
                            itemBeingMoved = null
                            dragStartOffset = null
                            dragCurrentOffset = null
                            snappedCurrentOffset = null
                            pressedChanges.forEach { it.consume() }
                            var curEv = event
                            while (curEv.changes.any { it.pressed }) {
                                curEv = awaitPointerEvent()
                                curEv.changes.forEach { it.consume() }
                            }
                            break
                        }

                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) break

                        val current = pointer.position
                        val dx = current.x - lastPos.x
                        val dy = current.y - lastPos.y

                        // Chuột giữa hoặc chuột phải kéo bất cứ lúc nào cũng là Pan Map (trừ khi đang khóa nền hoặc đang rải cột)
                        val isSecondaryOrTertiary = event.buttons.isSecondaryPressed || event.buttons.isTertiaryPressed
                        if (isSecondaryOrTertiary && !isObjectControlActive && !state.isAutoSpacingActive) {
                            isPanningMap = true
                        }

                        if (dx != 0f || dy != 0f) {
                            pointer.consume()
                            totalDx += dx
                            totalDy += dy

                            if (isPanningMap) {
                                state.panOffsetX += dx
                                state.panOffsetY += dy
                                state.renderVersion++
                                DebugLogger.logDrag("Kéo di chuyển bản vẽ", current.x, current.y, dx, dy)
                            } else {
                                val moving = itemBeingMoved
                                val currentZ = if (state.zoomScale > 0.001f) state.zoomScale else 1f
                                if (moving != null) {
                                    val canvasDx = dx / currentZ
                                    val canvasDy = dy / currentZ
                                    val updated = state.translateItemHandle(moving, activeHandleType, canvasDx, canvasDy)
                                    itemBeingMoved = updated
                                    val desc = updated.TEN.ifBlank { "Mục #" + updated.STT }
                                    val hLabel = when (activeHandleType) {
                                        DragHandleType.LINE_START -> "P1"
                                        DragHandleType.LINE_END -> "P2"
                                        DragHandleType.WHOLE_ITEM -> "Khối"
                                        DragHandleType.NONE -> "Thân"
                                    }
                                    DebugLogger.logDrag("$desc ($hLabel)", current.x, current.y, dx, dy)
                                } else {
                                    val canvasCurrent = toCanvas(current)
                                    dragCurrentOffset = canvasCurrent
                                    val start = dragStartOffset ?: canvasCurrent
                                    val nearPt = Function_tinhtoan_ve.KT_DIEM_GAN_NHAT(
                                        state.listCanvas,
                                        canvasCurrent.x,
                                        canvasCurrent.y,
                                        state.snapDistance / currentZ
                                    )
                                    if (DrawingRenderer.isLineCommand(state.currentCommand) || state.isAutoSpacingActive) {
                                        if (nearPt.x == canvasCurrent.x && nearPt.y == canvasCurrent.y) {
                                            val straight = Function_tinhtoan_ve.LAM_THANG(
                                                start.x, start.y, canvasCurrent.x, canvasCurrent.y, state.snapDistance / currentZ
                                            )
                                            snappedCurrentOffset = Offset(straight.x, straight.y)
                                        } else {
                                            snappedCurrentOffset = Offset(nearPt.x, nearPt.y)
                                        }
                                    } else {
                                        // Với icon 1 điểm: Không snap đè lên icon khác, áp dụng phân giải va chạm
                                        val safePt = Function_tinhtoan_ve.PHAN_GIAI_VA_CHAM_NODE(
                                            listCanvas = state.listCanvas,
                                            targetX = canvasCurrent.x,
                                            targetY = canvasCurrent.y,
                                            minDistance = 42f / currentZ
                                        )
                                        snappedCurrentOffset = Offset(safePt.x, safePt.y)
                                    }
                                    DebugLogger.liveCursorX = current.x
                                    DebugLogger.liveCursorY = current.y
                                }
                            }
                            lastPos = current
                        }
                    } while (event.changes.any { it.pressed })

                    // 3. Kết thúc chạm / thả chuột (Up)
                    if (isPinchingItem || isPinchingCanvas) {
                        state.saveHistorySnapshot()
                        isPinchingItem = false
                        isPinchingCanvas = false
                        itemBeingMoved = null
                        dragStartOffset = null
                        dragCurrentOffset = null
                        snappedCurrentOffset = null
                        return@awaitEachGesture
                    }

                    val moving = itemBeingMoved
                    val moveDistSquared = totalDx * totalDx + totalDy * totalDy
                    val minDragDistSquared = (touchSlop * touchSlop).coerceAtLeast(16f)

                    if (isPanningMap) {
                        if (moveDistSquared < minDragDistSquared) {
                            // Chỉ là click chọn tại chỗ vào khoảng trống canvas -> Bỏ chọn đối tượng
                            state.selectedItem = null
                            DebugLogger.log(
                                LogType.SELECT,
                                "Bỏ chọn",
                                "Click vào khoảng trống canvas (${startPos.x.toInt()}, ${startPos.y.toInt()})"
                            )
                        } else {
                            DebugLogger.log(
                                LogType.DRAG_MOVE,
                                "Kéo bản đồ GPS",
                                "Tâm mới: (${state.gpsCenter.latitude.toString().take(7)}, ${state.gpsCenter.longitude.toString().take(8)})"
                            )
                            state.autoSaveLastSession()
                        }
                        itemBeingMoved = null
                        dragStartOffset = null
                        dragCurrentOffset = null
                        snappedCurrentOffset = null
                    } else if (moving != null) {

                        val desc = moving.TEN.ifBlank { "Mục #" + moving.STT }
                        if (moveDistSquared > minDragDistSquared) {
                            // Đã thực sự kéo rê -> kiểm tra snap theo handle tương ứng
                            when (activeHandleType) {
                                DragHandleType.LINE_START -> {
                                    val near = Function_tinhtoan_ve.KT_DIEM_GAN_NHAT(
                                        state.listCanvas.filter { it != moving },
                                        moving.X1,
                                        moving.Y1,
                                        state.snapDistance / currentZ
                                    )
                                    if (near.x != moving.X1 || near.y != moving.Y1) {
                                        val snapDx = near.x - moving.X1
                                        val snapDy = near.y - moving.Y1
                                        state.translateItemHandle(moving, DragHandleType.LINE_START, snapDx, snapDy)
                                        DebugLogger.logSnap("Đầu P1 vào node", moving.X1, moving.Y1, near.x, near.y)
                                    }
                                }
                                DragHandleType.LINE_END -> {
                                    val near = Function_tinhtoan_ve.KT_DIEM_GAN_NHAT(
                                        state.listCanvas.filter { it != moving },
                                        moving.X3,
                                        moving.Y3,
                                        state.snapDistance / currentZ
                                    )
                                    if (near.x != moving.X3 || near.y != moving.Y3) {
                                        val snapDx = near.x - moving.X3
                                        val snapDy = near.y - moving.Y3
                                        state.translateItemHandle(moving, DragHandleType.LINE_END, snapDx, snapDy)
                                        DebugLogger.logSnap("Đầu P2 vào node", moving.X3, moving.Y3, near.x, near.y)
                                    }
                                }
                                DragHandleType.WHOLE_ITEM -> {
                                    if (DrawingRenderer.isLineCommand(moving.LENH)) {
                                        val near = Function_tinhtoan_ve.KT_DIEM_GAN_NHAT(
                                            state.listCanvas.filter { it != moving },
                                            moving.X3,
                                            moving.Y3,
                                            state.snapDistance / currentZ
                                        )
                                        if (near.x != moving.X3 || near.y != moving.Y3) {
                                            val snapDx = near.x - moving.X3
                                            val snapDy = near.y - moving.Y3
                                            state.translateItemHandle(moving, DragHandleType.WHOLE_ITEM, snapDx, snapDy)
                                            DebugLogger.logSnap("Node lân cận", moving.X3, moving.Y3, near.x, near.y)
                                        }
                                    } else {
                                        // Với đối tượng 1 điểm (cột, trạm...): Tự động phân giải va chạm, không để trùng khít hoặc quá gần cột khác
                                        val resolved = Function_tinhtoan_ve.PHAN_GIAI_VA_CHAM_NODE(
                                            listCanvas = state.listCanvas,
                                            targetX = moving.X3,
                                            targetY = moving.Y3,
                                            minDistance = 42f / currentZ,
                                            excludeItem = moving
                                        )
                                        if (resolved.x != moving.X3 || resolved.y != moving.Y3) {
                                            val shiftX = resolved.x - moving.X3
                                            val shiftY = resolved.y - moving.Y3
                                            state.translateItemHandle(moving, DragHandleType.WHOLE_ITEM, shiftX, shiftY)
                                        }
                                    }
                                }
                                DragHandleType.NONE -> {}
                            }

                            // Tự động tính toán lại khoảng cách trên dây (GPS * 1.2 độ võng) khi di chuyển trụ hoặc đầu mút dây nối
                            state.updateConnectedWireDistances(moving)

                            state.autoSaveLastSession()
                        } else {

                            // Cú click/chạm tại chỗ (không kéo rê)
                            val now = com.vesodo.khaosat.platform.currentEpochMillis()
                            val isSameItem = (lastClickedItemRef === moving || (moving.STT != 0L && lastClickedItemId == moving.STT))
                            if (isSameItem && (now - lastClickTime) < 400L) {
                                // Double click / Chạm đúp vào đối tượng hoặc văn bản -> Mở hộp thoại sửa nhãn/ghi chú
                                state.editingItemForNote = moving
                                state.showNoteDialog = true
                                lastClickTime = 0L
                                lastClickedItemId = null
                                lastClickedItemRef = null
                                DebugLogger.log(
                                    LogType.SELECT,
                                    "Click đúp: $desc",
                                    "Mở hộp thoại chỉnh sửa nhãn/ghi chú"
                                )
                            } else {
                                lastClickTime = now
                                lastClickedItemId = moving.STT
                                lastClickedItemRef = moving
                                DebugLogger.log(
                                    LogType.SELECT,
                                    "Click chọn: $desc",
                                    "Toạ độ đối tượng: (${moving.X3.toInt()}, ${moving.Y3.toInt()})"
                                )
                            }
                        }
                        state.endDragTranslate()
                        itemBeingMoved = null
                    } else {
                        // Thao tác vẽ mới hoặc click vào vùng trống
                        val start = dragStartOffset
                        val end = snappedCurrentOffset ?: dragCurrentOffset
                        if (start != null && end != null) {
                            val dist = Function_tinhtoan_ve.TINH_KC(start.x, start.y, end.x, end.y)
                            val isLine = DrawingRenderer.isLineCommand(state.currentCommand)

                            if (isDrawingIcon) {
                                // ĐỐI TƯỢNG 1 TOẠ ĐỘ (Cột, Công tơ, Trạm...):
                                val cmd = state.currentCommand
                                val tool = IconRegistry.findTool(cmd)
                                val isPoleOrSub = IconRegistry.isTru(cmd) || DiagramRuleEngine.isSubstationCommand(cmd) || DiagramRuleEngine.isPoleCommand(cmd)
                                val defaultTitle = if (isPoleOrSub) "" else (tool?.name ?: "")
                                val safePt = Function_tinhtoan_ve.PHAN_GIAI_VA_CHAM_NODE(
                                    listCanvas = state.listCanvas,
                                    targetX = end.x,
                                    targetY = end.y,
                                    minDistance = 42f / currentZ
                                )
                                state.addItem(
                                    x1 = 0f,
                                    y1 = 0f,
                                    x3 = safePt.x,
                                    y3 = safePt.y,
                                    command = cmd,
                                    title = defaultTitle,
                                    imageRes = cmd
                                )
                                val newlyAdded = state.listCanvas.lastOrNull()
                                if (newlyAdded != null) {
                                    state.selectItem(newlyAdded)
                                }
                                state.currentCommand = Variables.MOVE // Chuyển sang MOVE để click là di chuyển được ngay
                                DebugLogger.logDrawNew(tool?.name ?: "Icon #$cmd", 0f, 0f, safePt.x, safePt.y)
                            } else if (dist < touchSlop) {
                                // Cú chạm/click tại chỗ trên canvas khi ở chế độ MOVE hoặc LINE:
                                state.deselectAll()
                                lastClickTime = 0L
                                lastClickedItemId = null
                                lastClickedItemRef = null
                                DebugLogger.log(
                                    LogType.SELECT,
                                    "Bỏ chọn",
                                    "Click vào khoảng trống canvas (${start.x.toInt()}, ${start.y.toInt()})"
                                )
                            } else {
                                // Hoàn tất vẽ nét mới kéo dài hoặc rải cột AI
                                if (state.isAutoSpacingActive) {
                                    state.applyAutoRoute(
                                        Point2D(start.x, start.y),
                                        Point2D(end.x, end.y)
                                    )
                                    DebugLogger.log(
                                        LogType.DRAW_NEW,
                                        "Rải cột tự động AI",
                                        "Từ (${start.x.toInt()}, ${start.y.toInt()}) -> (${end.x.toInt()}, ${end.y.toInt()})"
                                    )
                                } else {
                                    val cmd = state.currentCommand
                                    val tool = IconRegistry.findTool(cmd)
                                    when (cmd) {
                                        Variables.MOVE -> {
                                            // Chế độ MOVE khi bấm vào khoảng trống canvas: Bỏ chọn đối tượng
                                            state.selectedItem = null
                                            state.listCanvas.forEach { it.CHON = 0 }
                                        }
                                        Variables.VE_DUONG_THANG,
                                        Variables.VE_LINE_DUT_NET,
                                        Variables.VE_2LINE,
                                        Variables.VE_MUI_TEN -> {
                                            if (dist > state.snapDistance / currentZ) {
                                                fun findConnectedPole(x: Float, y: Float, threshold: Float = 45f): Obj_CANVAS? {
                                                    return state.listCanvas.firstOrNull { item ->
                                                        (IconRegistry.isTru(item.LENH) ||
                                                         DiagramRuleEngine.isPoleCommand(item.LENH) ||
                                                         DiagramRuleEngine.isSubstationCommand(item.LENH)) &&
                                                        Function_tinhtoan_ve.TINH_KC(x, y, item.X3, item.Y3) <= threshold
                                                    }
                                                }
                                                val startPole = findConnectedPole(start.x, start.y)
                                                val endPole = findConnectedPole(end.x, end.y)
                                                val isConnectingTwoPoles = startPole != null && endPole != null && startPole !== endPole

                                                // Khoảng cách dây có tính hệ số độ võng 1.2 hiển thị ở trung điểm của dây
                                                val wireTitle = if (isConnectingTwoPoles) {
                                                    val meters = state.calculateWireDistanceWithSag(start.x, start.y, end.x, end.y)
                                                    if (meters > 0) "${meters}m" else ""
                                                } else {
                                                    ""
                                                }

                                                state.addItem(
                                                    x1 = start.x,
                                                    y1 = start.y,
                                                    x3 = end.x,
                                                    y3 = end.y,
                                                    command = cmd,
                                                    title = wireTitle
                                                )
                                                val newLine = state.listCanvas.lastOrNull()
                                                if (newLine != null) {
                                                    state.selectItem(newLine)
                                                }
                                                state.currentCommand = Variables.MOVE
                                                DebugLogger.logDrawNew(tool?.name ?: "Đường dây", start.x, start.y, end.x, end.y)
                                            }
                                        }
                                        Variables.VE_TEXT -> {
                                            state.addItem(
                                                x1 = 0f,
                                                y1 = 0f,
                                                x3 = end.x,
                                                y3 = end.y,
                                                command = cmd,
                                                title = "Ghi chú"
                                            )
                                            val newItem = state.listCanvas.lastOrNull()
                                            if (newItem != null) {
                                                state.editingItemForNote = newItem
                                                state.showNoteDialog = true
                                            }
                                            state.currentCommand = Variables.MOVE
                                            DebugLogger.logDrawNew("Ghi chú văn bản", 0f, 0f, end.x, end.y)
                                        }
                                        else -> {
                                            val isPoleOrSub = IconRegistry.isTru(cmd) || DiagramRuleEngine.isSubstationCommand(cmd) || DiagramRuleEngine.isPoleCommand(cmd)
                                            val defaultTitle = if (isPoleOrSub) "" else (tool?.name ?: "")
                                            val safePt = Function_tinhtoan_ve.PHAN_GIAI_VA_CHAM_NODE(
                                                listCanvas = state.listCanvas,
                                                targetX = end.x,
                                                targetY = end.y,
                                                minDistance = 42f / currentZ
                                            )
                                            state.addItem(
                                                x1 = 0f,
                                                y1 = 0f,
                                                x3 = safePt.x,
                                                y3 = safePt.y,
                                                command = cmd,
                                                title = defaultTitle,
                                                imageRes = cmd
                                            )
                                            val newItem = state.listCanvas.lastOrNull()
                                            if (newItem != null) {
                                                state.selectItem(newItem)
                                            }
                                            state.currentCommand = Variables.MOVE
                                            DebugLogger.logDrawNew(tool?.name ?: "Icon #$cmd", 0f, 0f, safePt.x, safePt.y)
                                        }
                                    }
                                }
                            }
                        }
                        dragStartOffset = null
                        dragCurrentOffset = null
                        snappedCurrentOffset = null
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val _renderVersion = state.renderVersion // Đảm bảo DrawScope quan sát renderVersion để Canvas vẽ lại ngay lập tức khi kéo thả
            state.canvasSize = size
            renderDiagramContent(
                drawScope = this,
                state = state,
                size = size,
                textMeasurer = textMeasurer,
                iconBitmaps = iconBitmaps,
                isExporting = state.showExportDialog
            )

            // Vẽ bản xem trước khi đang kéo thả
            val start = dragStartOffset
            val curr = dragCurrentOffset
            val snapped = snappedCurrentOffset ?: curr
            if (start != null && curr != null && snapped != null) {
                withTransform({
                    translate(state.panOffsetX, state.panOffsetY)
                    scale(state.zoomScale, state.zoomScale, pivot = Offset.Zero)
                }) {
                    val previewCmd = if (state.isAutoSpacingActive) state.autoSpacingConfig.wireCmd else state.currentCommand
                    DrawingRenderer.renderLivePreview(
                        drawScope = this,
                        command = previewCmd,
                        start = start,
                        current = curr,
                        snappedCurrent = snapped,
                        iconBitmap = iconBitmaps[previewCmd]
                    )
                }
            }
        }

        // Banner thông báo chế độ rải cột tự động của AI
        if (state.isAutoSpacingActive) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Polyline,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Rải Cột: Hãy kéo từ điểm đầu đến điểm cuối (${state.autoSpacingConfig.spanMeters.toInt()}m/nhịp)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { state.isAutoSpacingActive = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hủy",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Vùng chạm mở chi tiết mã QR khi người dùng nhấp vào góc bảng vẽ
        if (state.showSurveyQrCode) {
            val qrScale = (state.qrScalePercent.coerceIn(100, 150) / 100f)
            val boxSizeDp = (105 * qrScale).dp
            val boxAlignment = when (state.qrPosition) {
                com.vesodo.khaosat.qr.QrPosition.TOP_RIGHT -> Alignment.TopEnd
                com.vesodo.khaosat.qr.QrPosition.TOP_LEFT -> Alignment.TopStart
                com.vesodo.khaosat.qr.QrPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                com.vesodo.khaosat.qr.QrPosition.BOTTOM_LEFT -> Alignment.BottomStart
            }
            Box(
                modifier = Modifier
                    .align(boxAlignment)
                    .padding(16.dp)
                    .size(boxSizeDp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { state.showQrDetailDialog = true }
            )
        }

        // Hộp thoại phóng to và xem / chỉnh sửa / sao chép JSON mã QR
        if (state.showQrDetailDialog) {
            SurveyQrDetailDialog(
                state = state,
                onDismiss = { state.showQrDetailDialog = false }
            )
        }

        // Huy hiệu hiển thị trạng thái Ảnh nền / Sơ đồ đường Vector ở góc dưới bên trái
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 14.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.90f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mục 1: Sơ đồ đường Vector (Phương án 2)
                if (state.vectorBasemap != null) {
                    Icon(
                        Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Vector: ${state.vectorBasemap!!.roads.size} đường",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { state.showVectorRoads = !state.showVectorRoads },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (state.showVectorRoads) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Bật/Tắt Vector đường",
                            tint = if (state.showVectorRoads) Color(0xFF10B981) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (state.capturedMapBackground != null) {
                        Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color(0xFF475569)))
                    }
                }

                // Mục 2: Ảnh nền chụp (Phương án 1)
                if (state.capturedMapBackground != null) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ảnh nền (${state.capturedMapZoom}x)",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { state.showCapturedMap = !state.showCapturedMap },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (state.showCapturedMap) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Bật/Tắt ảnh nền",
                            tint = if (state.showCapturedMap) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (state.vectorBasemap != null || state.capturedMapBackground != null) {
                    // Nút Mở bản đồ để đổi vùng hoặc lấy lại
                    IconButton(
                        onClick = { state.showGpsExplorerDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Chọn vùng GPS khác",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    // Chưa có nền nào
                    Row(
                        modifier = Modifier.clickable { state.showGpsExplorerDialog = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Bấm để chọn bản đồ hoặc lấy sơ đồ đường Vector",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Huy hiệu chỉ báo Thu phóng bảng vẽ & Nút Reset về gốc 100%
                if (abs(state.zoomScale - 1f) > 0.02f || abs(state.panOffsetX) > 3f || abs(state.panOffsetY) > 3f) {
                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color(0xFF475569)))
                    Surface(
                        onClick = { state.resetCanvasZoom() },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${(state.zoomScale * 100).toInt()}% (1:1)",
                                color = Color(0xFFE0F2FE),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- NÚT TRÔI TRÊN BẢNG VẼ NẰM PHÍA TRÊN ICON VỪA VẼ (NHÀ, CÔNG TƠ, KHÁCH HÀNG): ĐỀ XUẤT THÊM KHÁCH HÀNG BÊN TRÁI / PHẢI ---
        val isHouseOrMeterOrCustomer = selItem != null && (
            selItem.LENH == Variables.VE_NHA ||
            selItem.LENH == Variables.VE_HINH_CONGTO ||
            selItem.LENH in 701..706 ||
            selItem.LENH == Variables.VE_HOP ||
            selItem.LENH == Variables.VE_HOP_NGANG ||
            selItem.TEN.contains("công tơ", ignoreCase = true) ||
            selItem.TEN.contains("khách hàng", ignoreCase = true) ||
            selItem.TEN.contains("nhà", ignoreCase = true)
        ) && (selItem.LENH != Variables.VE_TEXT)

        // Rà soát danh sách thông tin khách hàng xung quanh đối tượng đang chọn
        val attachedCustomerTexts = remember(state.listCanvas.size, state.renderVersion, selItem?.STT) {
            if (selItem == null) emptyList()
            else {
                state.listCanvas.filter { other ->
                    other.LENH == Variables.VE_TEXT &&
                    Function_tinhtoan_ve.TINH_KC(selItem.X3, selItem.Y3, other.X3, other.Y3) <= 260.0 &&
                    (other.TEN.contains("Mã:", ignoreCase = true) ||
                     other.TEN.contains("Tên:", ignoreCase = true) ||
                     other.TEN.contains("Khách hàng", ignoreCase = true) ||
                     other.TEN.contains("KH", ignoreCase = true))
                }
            }
        }

        val hasLeftCustomer = attachedCustomerTexts.any { it.X3 < (selItem?.X3 ?: 0f) - 15f || it.TEN.contains("trái", ignoreCase = true) }
        val hasRightCustomer = attachedCustomerTexts.any { it.X3 > (selItem?.X3 ?: 0f) + 15f || it.TEN.contains("phải", ignoreCase = true) }
        val hasBothCustomerSides = hasLeftCustomer && hasRightCustomer

        val shouldShowCustomerSuggestion = isHouseOrMeterOrCustomer &&
            !hasBothCustomerSides &&
            (state.dismissedCustomerSuggestionItemId != selItem?.STT) &&
            !state.showExportDialog

        if (shouldShowCustomerSuggestion && selItem != null) {
            val iconScreenX = selItem.X3 * state.zoomScale + state.panOffsetX
            val iconScreenY = selItem.Y3 * state.zoomScale + state.panOffsetY

            val popupWidthDp = 276.dp
            val popupHeightDp = 70.dp
            val gapDp = 12.dp

            val iconScreenXDp = with(density) { iconScreenX.toDp() }
            val iconScreenYDp = with(density) { iconScreenY.toDp() }

            // Bán kính hiển thị an toàn của icon (bao gồm cả viền và hào quang chọn)
            val iconVisualRadiusDp = with(density) {
                ((32f * selItem.SCALE.coerceAtLeast(0.8f) * state.zoomScale).coerceIn(24f, 60f)).toDp()
            }

            val canvasWidthDp = with(density) {
                (if (state.canvasSize.width > 0f) state.canvasSize.width else 1080f).toDp()
            }

            // Trục X: Căn giữa popup theo icon, clamp an toàn không tràn 2 mép màn hình
            val widgetLeftDp = (iconScreenXDp - popupWidthDp / 2f).coerceIn(
                12.dp,
                (canvasWidthDp - popupWidthDp - 12.dp).coerceAtLeast(12.dp)
            )

            // Trục Y: Đặt popup nằm PHÍA TRÊN icon (đáy popup cách đỉnh icon khoảng gapDp)
            val desiredTopDp = iconScreenYDp - iconVisualRadiusDp - gapDp - popupHeightDp

            // Nếu quá sát đỉnh màn hình (< 56dp do vướng thanh Toolbar) thì tự động đảo xuống phía dưới icon
            val widgetTopDp = if (desiredTopDp >= 56.dp) {
                desiredTopDp
            } else {
                iconScreenYDp + iconVisualRadiusDp + gapDp + 18.dp
            }

            Surface(
                modifier = Modifier
                    .offset(x = widgetLeftDp, y = widgetTopDp)
                    .width(popupWidthDp)
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(14.dp))
                    .onGloballyPositioned { coords ->
                        customerSuggestionBoundsInCanvas = coords.boundsInParent()
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                border = BorderStroke(1.2.dp, Color(0xFF818CF8).copy(alpha = 0.85f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Header của widget trôi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Gợi ý Khách hàng (AI)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE0E7FF)
                            )
                        }

                        IconButton(
                            onClick = {
                                customerSuggestionBoundsInCanvas = null
                                state.dismissedCustomerSuggestionItemId = selItem.STT
                            },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tắt gợi ý",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // 2 nút đề xuất thêm khách hàng bên trái / bên phải
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = { state.openCustomerAttachmentDialog(selItem, com.vesodo.khaosat.ai.CustomerSide.LEFT) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasLeftCustomer) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (hasLeftCustomer) Color(0xFF10B981).copy(alpha = 0.85f) else Color(0xFF38BDF8).copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (hasLeftCustomer) "✓ KH Trái" else "👈 + KH Trái",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasLeftCustomer) Color(0xFF34D399) else Color(0xFF7DD3FC)
                                )
                            }
                        }

                        Surface(
                            onClick = { state.openCustomerAttachmentDialog(selItem, com.vesodo.khaosat.ai.CustomerSide.RIGHT) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasRightCustomer) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (hasRightCustomer) Color(0xFF10B981).copy(alpha = 0.85f) else Color(0xFF818CF8).copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (hasRightCustomer) "✓ KH Phải" else "👉 + KH Phải",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasRightCustomer) Color(0xFF34D399) else Color(0xFFA5B4FC)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bảng điều khiển đối tượng khi được chọn: Xoay (bên trái) & Dịch chuyển vị trí (bên phải)
        if (selItem != null && !state.showExportDialog) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(0.96f)
                    .widthIn(max = 660.dp)
                    .onGloballyPositioned { coords ->
                        rotationPanelBoundsInCanvas = coords.boundsInParent()
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) break
                            }
                        }
                    },
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- PHẦN BÊN TRÁI: ĐIỀU CHỈNH XOAY (Nếu là đối tượng 1 toạ độ) ---
                    if (isSinglePointSelected) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Hàng 1: Tiêu đề đối tượng + Nút sửa/copy + Badge góc xoay hiện tại
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        Icons.Default.RotateRight,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = selItem.TEN.ifBlank { "Đối tượng #${selItem.STT}" },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    // Nút sửa thông tin / ghi chú đối tượng
                                    IconButton(
                                        onClick = {
                                            state.editingItemForNote = selItem
                                            state.showNoteDialog = true
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Sửa tên/ghi chú",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    // Nút nhân bản nhanh đối tượng (Duplicate)
                                    IconButton(
                                        onClick = {
                                            state.duplicateSelectedItem()
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Nhân bản đối tượng",
                                            tint = Color(0xFF4ADE80),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // Badge hiển thị góc xoay hiện tại (Bấm để reset về 0°)
                                Surface(
                                    onClick = { state.rotateSelectedItem(0f, saveHistory = true) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.RotateRight,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${state.selectedItemRotation.toInt()}°",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Hàng 2: Thanh trượt xoay chuyên dụng kèm Nút [-] và [+]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Nút [-] giảm góc xoay 5°
                                Surface(
                                    onClick = {
                                        state.rotateSelectedItem(state.selectedItemRotation - 5f, saveHistory = true)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Giảm góc xoay",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "0°",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = state.selectedItemRotation,
                                    onValueChange = { angle ->
                                        state.rotateSelectedItem(angle, saveHistory = false)
                                    },
                                    onValueChangeFinished = {
                                        state.saveHistorySnapshot()
                                    },
                                    valueRange = 0f..360f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF38BDF8),
                                        activeTrackColor = Color(0xFF0284C7),
                                        inactiveTrackColor = Color(0xFF334155)
                                    )
                                )
                                Text(
                                    text = "360°",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )

                                // Nút [+] tăng góc xoay 5°
                                Surface(
                                    onClick = {
                                        state.rotateSelectedItem(state.selectedItemRotation + 5f, saveHistory = true)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Tăng góc xoay",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Hàng 3: Các góc xoay nhanh (Presets)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QuickAngleChip("-45°") {
                                    state.rotateSelectedItem(state.selectedItemRotation - 45f, saveHistory = true)
                                }
                                QuickAngleChip("0°", isSelected = (state.selectedItemRotation.toInt() == 0)) {
                                    state.rotateSelectedItem(0f, saveHistory = true)
                                }
                                QuickAngleChip("90°", isSelected = (state.selectedItemRotation.toInt() == 90)) {
                                    state.rotateSelectedItem(90f, saveHistory = true)
                                }
                                QuickAngleChip("180°", isSelected = (state.selectedItemRotation.toInt() == 180)) {
                                    state.rotateSelectedItem(180f, saveHistory = true)
                                }
                                QuickAngleChip("270°", isSelected = (state.selectedItemRotation.toInt() == 270)) {
                                    state.rotateSelectedItem(270f, saveHistory = true)
                                }
                                QuickAngleChip("+45°") {
                                    state.rotateSelectedItem(state.selectedItemRotation + 45f, saveHistory = true)
                                }
                            }
                        }

                        // Vách ngăn giữa Xoay và Thu Phóng
                        VerticalDivider(
                            modifier = Modifier
                                .height(72.dp)
                                .padding(horizontal = 4.dp),
                            color = Color(0xFF334155),
                            thickness = 1.dp
                        )

                        // --- PHẦN RIÊNG BIỆT THU PHÓNG (ZOOM) CẠNH MỤC DỊCH CHUYỂN: Nút [-] [% zoom] [+] ---
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            // Tiêu đề Thu phóng
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Thu phóng",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFCBD5E1)
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            // Hàng phím: [-]  [% Zoom Badge]  [+]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Nút [-] giảm tỉ lệ 10%
                                Surface(
                                    onClick = { state.zoomOutSelectedItem(0.1f) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Thu nhỏ đối tượng",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                // Badge % Zoom hiện tại (Click để reset về 100%)
                                val currentScalePercent = (state.selectedItemScale * 100).roundToInt()
                                val isNonDefaultScale = abs(state.selectedItemScale - 1.0f) > 0.02f
                                Surface(
                                    onClick = { state.resetSelectedItemScale() },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isNonDefaultScale) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isNonDefaultScale) Color(0xFF38BDF8) else Color(0xFF475569)),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    ) {
                                        Text(
                                            text = "$currentScalePercent%",
                                            color = if (isNonDefaultScale) Color(0xFF38BDF8) else Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Nút [+] tăng tỉ lệ 10%
                                Surface(
                                    onClick = { state.zoomInSelectedItem(0.1f) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Phóng to đối tượng",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            // Nút reset nhanh về 1.0x (100%)
                            Surface(
                                onClick = { state.resetSelectedItemScale() },
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(0.5.dp, Color(0xFF475569)),
                                modifier = Modifier.height(18.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = "100% (1.0x)",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Vách ngăn giữa Thu Phóng và Dịch Chuyển
                        VerticalDivider(
                            modifier = Modifier
                                .height(72.dp)
                                .padding(horizontal = 4.dp),
                            color = Color(0xFF334155),
                            thickness = 1.dp
                        )
                    } else {
                        // Đối tượng dạng đường (Line / Poly)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Gesture,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selItem.TEN.ifBlank { "Nét vẽ #${selItem.STT}" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                // Nút sửa thông tin / ghi chú nét vẽ
                                IconButton(
                                    onClick = {
                                        state.editingItemForNote = selItem
                                        state.showNoteDialog = true
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Sửa tên/ghi chú",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                // Nút nhân bản nhanh nét vẽ (Duplicate)
                                IconButton(
                                    onClick = {
                                        state.duplicateSelectedItem()
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Nhân bản nét vẽ",
                                        tint = Color(0xFF4ADE80),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sử dụng cụm phím bên phải để dịch chuyển nét vẽ hoặc kéo trực tiếp 2 đầu mút",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(60.dp)
                                .padding(horizontal = 4.dp),
                            color = Color(0xFF334155),
                            thickness = 1.dp
                        )
                    }

                    // --- PHẦN BÊN PHẢI: CỤM PHÍM DỊCH CHUYỂN VỊ TRÍ (NUDGE CONTROL PAD) ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        // Tiêu đề Dịch chuyển
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.width(108.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenWith,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Dịch chuyển",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Nút [ ↑ Lên ]
                        NudgeDirectionButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Lên",
                            onClick = { state.nudgeSelected(Variables.LEN, 6f) }
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Hàng: [ ← Trái ] [ ↓ Xuống ] [ → Phải ]
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NudgeDirectionButton(
                                icon = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Trái",
                                onClick = { state.nudgeSelected(Variables.TRAI, 6f) }
                            )

                            NudgeDirectionButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Xuống",
                                onClick = { state.nudgeSelected(Variables.XUONG, 6f) }
                            )

                            NudgeDirectionButton(
                                icon = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Phải",
                                onClick = { state.nudgeSelected(Variables.PHAI, 6f) }
                            )
                        }
                    }

                    // Vách ngăn đứng phân cách cụm phím và góc chức năng đóng / xóa
                    VerticalDivider(
                        modifier = Modifier
                            .height(72.dp)
                            .padding(horizontal = 8.dp),
                        color = Color(0xFF334155),
                        thickness = 1.dp
                    )

                    // --- PHẦN GÓC PHẢI RIÊNG BIỆT: NÚT ĐÓNG & NÚT XÓA (NẰM XA ĐỂ TRÁNH BẤM NHẦM) ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .height(82.dp)
                            .padding(start = 4.dp, end = 12.dp) // Margin rộng rãi bên góc phải
                    ) {
                        // Nút Đóng / Bỏ chọn
                        IconButton(
                            onClick = {
                                state.deselectAll()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng tùy chỉnh",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Nút Xóa đối tượng (nằm tách biệt ở góc dưới cùng)
                        IconButton(
                            onClick = {
                                showDeleteConfirmDialog = true
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFFDC2626).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Xóa đối tượng",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hộp thoại xác nhận xoá đối tượng an toàn
        if (showDeleteConfirmDialog && selItem != null) {
            val itemName = selItem.TEN.ifBlank {
                if (DrawingRenderer.isLineCommand(selItem.LENH)) "Nét vẽ #${selItem.STT}" else "Đối tượng #${selItem.STT}"
            }
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444)
                        )
                        Text(
                            text = "Xác nhận xóa đối tượng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Text(
                        text = "Bạn có chắc chắn muốn xóa '$itemName' khỏi bản vẽ?",
                        fontSize = 14.sp,
                        color = Color(0xFF334155)
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        onClick = {
                            state.removeItem(selItem)
                            showDeleteConfirmDialog = false
                        }
                    ) {
                        Text("Xóa đối tượng", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = false }
                    ) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickAngleChip(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
        )
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun NudgeDirectionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Hàm kết xuất toàn bộ nội dung bản vẽ CAD, bao gồm cả nền bản đồ, đường vector, toạ độ GPS và các đối tượng điện thoại/trụ/trạm...
 * Dùng chung cho cả hiển thị trực tiếp và xuất file JPG/PDF (WYSIWYG: Bản vẽ thế nào xuất ra y như vậy)
 */
fun renderDiagramContent(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    state: DrawingState,
    size: Size,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    iconBitmaps: Map<Int, ImageBitmap>,
    isExporting: Boolean,
    exportOffsetX: Float = 0f,
    exportOffsetY: Float = 0f,
    origScreenWidth: Float = size.width,
    origScreenHeight: Float = size.height
) {
    with(drawScope) {
        if (isExporting) {
            // Nền trắng mặc định khi xuất file
            drawRect(Color.White, size = size)
        }

        // Toàn bộ các đối tượng bản vẽ (nền bản đồ, đường vector, GPS, lưới điểm, và các nét vẽ CAD)
        // được thu phóng và dịch chuyển đồng bộ theo Viewport Matrix Transform
        withTransform({
            translate(state.panOffsetX + exportOffsetX, state.panOffsetY + exportOffsetY)
            scale(state.zoomScale, state.zoomScale, pivot = Offset.Zero)
        }) {
            // 0. Vẽ ảnh PNG nền bản đồ đã chụp tràn full toàn bộ bản vẽ CAD (giữ nguyên tỷ lệ, không bị kéo giãn)
            if (state.showCapturedMap && state.capturedMapBackground != null) {
                val bg = state.capturedMapBackground!!
                val bgScale = maxOf(size.width / bg.width.toFloat(), size.height / bg.height.toFloat())
                val dstW = (bg.width * bgScale).toInt()
                val dstH = (bg.height * bgScale).toInt()
                val dstX = ((size.width - dstW) / 2f).toInt()
                val dstY = ((size.height - dstH) / 2f).toInt()
                drawImage(
                    image = bg,
                    dstOffset = IntOffset(dstX, dstY),
                    dstSize = IntSize(dstW, dstH),
                    alpha = state.mapOpacity
                )
            }

            // 0.1 Vẽ SƠ ĐỒ ĐƯỜNG DẠNG VECTOR SIÊU NÉT (PHƯƠNG ÁN 2)
            if (state.showVectorRoads && state.vectorBasemap != null) {
                try {
                    GpsVectorRoadManager.renderVectorRoads(
                        drawScope = this,
                        vectorBasemap = state.vectorBasemap!!,
                        canvasSize = Size(origScreenWidth, origScreenHeight),
                        panOffsetX = 0f,
                        panOffsetY = 0f,
                        textMeasurer = textMeasurer
                    )
                } catch (e: Throwable) {
                    DebugLogger.log(LogType.SELECT, "Lỗi vẽ vector", e.message ?: "")
                }
            }

            // Vẽ CHẤM XANH ĐỊNH VỊ TỌA ĐỘ GPS KHẢO SÁT ĐÃ CHỌN (khi có ảnh nền HOẶC có sơ đồ vector)
            val hasMapActive = (state.showCapturedMap && state.capturedMapBackground != null) ||
                               (state.showVectorRoads && state.vectorBasemap != null)

            if (hasMapActive) {
                try {
                    val pinCenter = Offset(
                        origScreenWidth / 2f,
                        origScreenHeight / 2f
                    )

                    // 1. Quầng hào quang phát sáng lan toả (Soft radar pulse)
                    drawCircle(
                        color = Color(0x33007AFF),
                        radius = 22f,
                        center = pinCenter
                    )
                    // 2. Vòng tròn viền định vị chuẩn xác
                    drawCircle(
                        color = Color(0xFF007AFF),
                        radius = 13f,
                        center = pinCenter,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                    // 3. Chấm xanh toạ độ chính (Bright Blue Pin)
                    drawCircle(
                        color = Color(0xFF007AFF),
                        radius = 7f,
                        center = pinCenter
                    )
                    // 4. Điểm trắng tâm chính xác
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = pinCenter
                    )

                    // 5. Thước ngắm chữ thập định vị nhỏ
                    drawLine(
                        color = Color(0xFF007AFF),
                        start = Offset(pinCenter.x - 20f, pinCenter.y),
                        end = Offset(pinCenter.x - 9f, pinCenter.y),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color(0xFF007AFF),
                        start = Offset(pinCenter.x + 9f, pinCenter.y),
                        end = Offset(pinCenter.x + 20f, pinCenter.y),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color(0xFF007AFF),
                        start = Offset(pinCenter.x, pinCenter.y - 20f),
                        end = Offset(pinCenter.x, pinCenter.y - 9f),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color(0xFF007AFF),
                        start = Offset(pinCenter.x, pinCenter.y + 9f),
                        end = Offset(pinCenter.x, pinCenter.y + 20f),
                        strokeWidth = 1.5f
                    )

                    // 6. Thẻ nhãn toạ độ định vị nổi bật ngay trên chấm xanh
                    val activeCoord = state.vectorBasemap?.centerGps ?: state.capturedMapCenter
                    val latStr = activeCoord.latitude.toString().take(8)
                    val lonStr = activeCoord.longitude.toString().take(9)
                    val coordTag = "📍 GPS: ($latStr, $lonStr)"
                    val layout = textMeasurer.measure(
                        text = coordTag,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE0F2FE)
                        )
                    )
                    val tagPadH = 8f
                    val tagPadV = 4f
                    val tagW = layout.size.width + tagPadH * 2
                    val tagH = layout.size.height + tagPadV * 2
                    val tagLeft = pinCenter.x - tagW / 2f
                    val tagTop = pinCenter.y - 30f - tagH

                    // Khung nền đen mờ bo góc cho toạ độ
                    drawRoundRect(
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        topLeft = Offset(tagLeft, tagTop),
                        size = Size(tagW, tagH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFF38BDF8).copy(alpha = 0.75f),
                        topLeft = Offset(tagLeft, tagTop),
                        size = Size(tagW, tagH),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                    )
                    // Vẽ chữ toạ độ GPS an toàn bằng textLayoutResult
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(tagLeft + tagPadH, tagTop + tagPadV)
                    )
                } catch (e: Throwable) {
                    DebugLogger.log(LogType.SELECT, "Lỗi vẽ định vị GPS", e.message ?: "")
                }
            }

            // Vẽ các chấm lưới định vị nhẹ (Grid dots) khi hiển thị trực tiếp
            if (!isExporting) {
                val step = 40f
                var gx = 0f
                while (gx < size.width) {
                    var gy = 0f
                    while (gy < size.height) {
                        drawCircle(
                            color = Color(0xFFE2E8F0),
                            radius = 1.2f,
                            center = Offset(gx, gy)
                        )
                        gy += step
                    }
                    gx += step
                }
            }

            // Vẽ toàn bộ các phần tử hiện có
            val showSelection = !isExporting && !state.showExportDialog
            DrawingRenderer.renderAll(
                drawScope = this,
                items = state.listCanvas,
                selectedItem = if (showSelection) state.selectedItem else null,
                textMeasurer = textMeasurer,
                fontSizeSp = state.fontSize.sizeSp,
                iconBitmaps = iconBitmaps,
                showSelection = showSelection
            )
        }

        // Vẽ mã QR thông tin khảo sát trực tiếp ở góc trên bên phải bảng vẽ (cả khi vẽ và xuất file)
        SurveyQrManager.renderQrOnCanvas(
            drawScope = this,
            state = state,
            canvasSize = size
        )

        // Khi lưu / xuất hình: Vẽ đường viền bo cong trang trí mỹ thuật bên ngoài
        if (isExporting) {
            val borderInset = 8f
            val cornerRad = 18f

            // 1. Phủ màu trắng tinh sạch vùng 4 góc nhọn ngoài đường cong bo viền
            val maskPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(
                    RoundRect(
                        left = borderInset,
                        top = borderInset,
                        right = size.width - borderInset,
                        bottom = size.height - borderInset,
                        cornerRadius = CornerRadius(cornerRad, cornerRad)
                    )
                )
            }
            drawPath(maskPath, color = Color.White)

            // 2. Đường viền khung chính bo cong (Slate-800 sang trọng, sắc sảo)
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(borderInset, borderInset),
                size = Size(size.width - borderInset * 2, size.height - borderInset * 2),
                cornerRadius = CornerRadius(cornerRad, cornerRad),
                style = Stroke(width = 2.5f)
            )

            // 3. Đường chỉ viền đôi phụ mảnh bên trong (Slate-400 thanh lịch tạo chiều sâu bản vẽ)
            val innerInset = borderInset + 4f
            val innerRad = (cornerRad - 4f).coerceAtLeast(4f)
            drawRoundRect(
                color = Color(0xFF94A3B8).copy(alpha = 0.85f),
                topLeft = Offset(innerInset, innerInset),
                size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                cornerRadius = CornerRadius(innerRad, innerRad),
                style = Stroke(width = 1f)
            )
        }
    }
}
