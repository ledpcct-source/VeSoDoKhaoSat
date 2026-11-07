package com.vesodo.khaosat.drawing

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import com.vesodo.khaosat.ai.DiagramIssue
import com.vesodo.khaosat.ai.DiagramRuleEngine
import com.vesodo.khaosat.ai.IssueCategory
import com.vesodo.khaosat.ai.SmartRoutingEngine
import com.vesodo.khaosat.math.Function_tinhtoan_ve
import com.vesodo.khaosat.math.Point2D
import com.vesodo.khaosat.database.LastCanvasSession
import com.vesodo.khaosat.database.PlatformTemplateStorage
import com.vesodo.khaosat.model.MOBILE_KHAOSAT_KHANG
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class DragHandleType {
    WHOLE_ITEM,        // Di chuyển toàn bộ đối tượng (cột, trạm, hoặc cả đoạn dây khi chạm trung điểm)
    LINE_START,        // Chỉ di chuyển đầu mút X1, Y1 của đường thẳng/mũi tên
    LINE_END,          // Chỉ di chuyển đầu mút X3, Y3 của đường thẳng/mũi tên
    NONE               // Không di chuyển khi chạm thân ngoài trung điểm
}

data class AutoSpacingConfig(
    val spanMeters: Double = 35.0,
    val poleCmd: Int = Variables.VE_TRU_BTLT_105_HHUU,
    val wireCmd: Int = Variables.VE_DUONG_THANG,
    val polePrefix: String = "Trụ"
)

enum class TextFontSize(val label: String, val sizeSp: Float, val code: String) {
    CUC_SMALL("Cực nhỏ", 10f, "cucsmall"),
    VERY_SMALL("Rất nhỏ", 12f, "verysmall"),
    SMALL("Nhỏ", 14f, "small"),
    MEDIUM("Vừa", 16f, "medium"),
    LARGE("Lớn", 20f, "large")
}

@Stable
class DrawingState(
    initialCustomer: MOBILE_KHAOSAT_KHANG = MOBILE_KHAOSAT_KHANG(
        MA_YCAU_KNAI = "YC20261014",
        MA_DVIQLY = "DVI_01",
        TEN_KHANG_KS = "Khách Hàng Khảo Sát",
        DIA_CHI = "Vị trí khảo sát",
        MA_TRAM = "TRAM_01"
    )
) {
    var customer by mutableStateOf(initialCustomer)

    var coroutineScope: CoroutineScope? = null

    // Quản lý số lần vẽ từng công cụ (chạm vẽ / add item)
    val toolUsageCounts = mutableStateMapOf<Int, Int>()

    fun getToolUsageCount(command: Int): Int {
        return toolUsageCounts[command] ?: 0
    }

    fun incrementToolUsage(command: Int) {
        if (command == Variables.MOVE) return
        val current = toolUsageCounts[command] ?: 0
        toolUsageCounts[command] = current + 1
        coroutineScope?.launch {
            try {
                PlatformTemplateStorage.saveToolUsageCounts(toolUsageCounts.toMap())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadToolUsageCounts(saved: Map<Int, Int>) {
        toolUsageCounts.clear()
        toolUsageCounts.putAll(saved)
    }

    var isRestoringSession by mutableStateOf(false)

    fun createSessionSnapshot(): LastCanvasSession {
        return LastCanvasSession(
            items = listCanvas.map { it.copy() },
            panOffsetX = panOffsetX,
            panOffsetY = panOffsetY,
            zoomScale = zoomScale,
            timestamp = com.vesodo.khaosat.platform.currentEpochMillis(),
            vectorBasemap = vectorBasemap
        )
    }

    fun autoSaveLastSession() {
        if (isRestoringSession) return
        coroutineScope?.launch {
            try {
                val snapshot = createSessionSnapshot()
                PlatformTemplateStorage.saveLastCanvasSession(snapshot)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreCanvasSession(session: LastCanvasSession) {
        isRestoringSession = true
        try {
            listCanvas.clear()
            listCanvas.addAll(session.items)
            panOffsetX = session.panOffsetX
            panOffsetY = session.panOffsetY
            zoomScale = if (session.zoomScale > 0.05f) session.zoomScale else 1f
            selectedItem = null
            selectedItemRotation = 0f
            selectedItemScale = 1f

            // Khôi phục tỷ lệ scale trụ/đối tượng từ các đối tượng hiện hữu gần nhất
            val lastScaledItem = session.items.findLast { IconRegistry.isTru(it.LENH) && it.SCALE >= 0.4f }
            if (lastScaledItem != null && lastScaledItem.SCALE > 0f) {
                defaultObjectScale = lastScaledItem.SCALE
            }

            if (session.vectorBasemap != null) {
                vectorBasemap = session.vectorBasemap
                showVectorRoads = true
                capturedMapCenter = session.vectorBasemap.centerGps
            }

            val roadInfo = if (session.vectorBasemap != null) " kèm ${session.vectorBasemap.roads.size} tuyến đường vector" else ""
            statusMessage = "Đã khôi phục bảng vẽ gần nhất (${session.items.size} đối tượng$roadInfo)"
            triggerRedraw()
        } finally {
            isRestoringSession = false
        }
    }


    var currentCommand by mutableStateOf(Variables.MOVE)
    var selectedCategory by mutableStateOf(ToolCategory.POPULAR)

    val listCanvas = mutableStateListOf<Obj_CANVAS>()
    val undoStack = mutableStateListOf<List<Obj_CANVAS>>()
    val redoStack = mutableStateListOf<List<Obj_CANVAS>>()

    var selectedItem by mutableStateOf<Obj_CANVAS?>(null)
    var selectedItemRotation by mutableFloatStateOf(0f)
    var selectedItemScale by mutableFloatStateOf(1f)

    var editingItemForNote by mutableStateOf<Obj_CANVAS?>(null)

    var activeDragStart by mutableStateOf<Point2D?>(null)
    var activeDragCurrent by mutableStateOf<Point2D?>(null)

    var fontSize by mutableStateOf(TextFontSize.MEDIUM)
    var snapDistance by mutableStateOf(25.0)
    var arrowSize by mutableStateOf(20)

    // Zoom & Pan state
    var zoomScale by mutableStateOf(1f)
    var panOffsetX by mutableStateOf(0f)
    var panOffsetY by mutableStateOf(0f)

    // Tỷ lệ scale mặc định của đối tượng vẽ (lưu DB nhớ cho các lần vẽ sau)
    var defaultObjectScale by mutableFloatStateOf(1f)

    // Cấu hình thông tin Đơn vị khảo sát & KTV (lưu DB dùng khi xuất bản vẽ CAD DXF)
    var unitConfig by mutableStateOf(com.vesodo.khaosat.database.SavedSurveyUnitConfig())
    var showUnitConfigDialog by mutableStateOf(false)

    // Dialog states
    var showNoteDialog by mutableStateOf(false)
    var showTemplateDialog by mutableStateOf(false)
    var showSaveTemplateDialog by mutableStateOf(false)
    var showExportDialog by mutableStateOf(false)
    var showCadPreviewDialog by mutableStateOf(false)
    var showConfirmClearDialog by mutableStateOf(false)
    var showAboutDialog by mutableStateOf(false)
    var showFontSizeMenu by mutableStateOf(false)
    var showHistoryDrawer by mutableStateOf(false)
    var historyFabPosition by mutableStateOf<Pair<Float, Float>?>(null)

    // AI States
    var showSmartCheckDialog by mutableStateOf(false)
    var showAutoSpacingDialog by mutableStateOf(false)
    var isAutoSpacingActive by mutableStateOf(false)
    var autoSpacingConfig by mutableStateOf(AutoSpacingConfig())
    var autoSpacingStartPoint by mutableStateOf<Point2D?>(null)

    // AI Đề xuất Khách hàng bên trái / bên phải (Local AI Advisor)
    var showCustomerAttachmentDialog by mutableStateOf(false)
    var pendingCustomerParentItem by mutableStateOf<Obj_CANVAS?>(null)
    var pendingCustomerSide by mutableStateOf(com.vesodo.khaosat.ai.CustomerSide.LEFT)
    var dismissedCustomerSuggestionItemId by mutableStateOf<Long?>(null)

    // Palette Drag & Drop Tool state
    var canvasBoundsInRoot by mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
    var draggingPaletteTool by mutableStateOf<DrawToolItem?>(null)
    var draggingPaletteOffset by mutableStateOf<Offset?>(null)

    // Kích thước thực tế của vùng vẽ hiện tại
    var canvasSize by mutableStateOf(Size(1920f, 1080f))

    // Hàm callback kết xuất toàn bộ bản vẽ thành ImageBitmap (WYSIWYG: Bao gồm nền bản đồ, vector, GPS, icon...)
    var captureCanvasBitmap by mutableStateOf<(() -> ImageBitmap)?>(null)

    // Nền bản đồ đã chụp (Tách độc lập bảng vẽ và bản đồ)
    var capturedMapBackground by mutableStateOf<ImageBitmap?>(null)
    var showCapturedMap by mutableStateOf(true)
    var capturedMapCenter by mutableStateOf(com.vesodo.khaosat.map.GpsBasemapManager.PRESET_CAN_THO_NINH_KIEU)
    var capturedMapZoom by mutableStateOf(17)

    // Sơ đồ đường dạng Vector siêu nét từ GPS (Phương án 2)
    var vectorBasemap by mutableStateOf<com.vesodo.khaosat.map.VectorBasemap?>(null)
    var showVectorRoads by mutableStateOf(true)
    var isFetchingVectorRoads by mutableStateOf(false)

    // Giao diện Bản đồ GPS độc lập để ngắm vị trí, zoom và chụp ảnh nền
    var showGpsExplorerDialog by mutableStateOf(false)
    var gpsCenter by mutableStateOf(com.vesodo.khaosat.map.GpsBasemapManager.PRESET_CAN_THO_NINH_KIEU)
    var gpsZoomLevel by mutableStateOf(19)
    var mapOpacity by mutableStateOf(0.60f)
    var mapTileProvider by mutableStateOf(com.vesodo.khaosat.map.MapTileProvider.ESRI_STREET_GRAY)
    var showGpsBasemap by mutableStateOf(false)
    var showGpsConfigDialog by mutableStateOf(false)

    // Mã QR thông tin khảo sát gắn liền góc bản vẽ
    var showSurveyQrCode by mutableStateOf(true)
    var isQrCardExpanded by mutableStateOf(true)
    var showQrDetailDialog by mutableStateOf(false)
    var customSoTru by mutableStateOf("")
    var qrScalePercent by mutableIntStateOf(100) // 100% đến 150%
    var qrPosition by mutableStateOf(com.vesodo.khaosat.qr.QrPosition.TOP_RIGHT)

    fun getPolesCount(): Int {
        return listCanvas.count { item ->
            IconRegistry.isTru(item.LENH)
        }
    }

    fun getEffectiveSoTru(): String {
        if (customSoTru.isNotBlank()) return customSoTru
        val count = getPolesCount()
        return if (count > 0) count.toString() else "0"
    }

    var statusMessage by mutableStateOf("")

    // Biến phiên bản để ép Canvas vẽ lại tức thì khi kéo thả liên tục trên Android
    var renderVersion by mutableLongStateOf(0L)

    fun triggerRedraw() {
        renderVersion++
    }

    private fun pushHistory() {
        val snapshot = listCanvas.map { it.copy() }
        undoStack.add(snapshot)
        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        autoSaveLastSession()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentSnapshot = listCanvas.map { it.copy() }
            redoStack.add(currentSnapshot)
            val previous = undoStack.removeAt(undoStack.lastIndex)
            listCanvas.clear()
            listCanvas.addAll(previous)
            selectedItem = null
            statusMessage = "Đã hoàn tác thao tác"
            triggerRedraw()
            autoSaveLastSession()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentSnapshot = listCanvas.map { it.copy() }
            undoStack.add(currentSnapshot)
            val next = redoStack.removeAt(redoStack.lastIndex)
            listCanvas.clear()
            listCanvas.addAll(next)
            selectedItem = null
            statusMessage = "Đã làm lại thao tác"
            triggerRedraw()
            autoSaveLastSession()
        }
    }


    fun duplicateSelectedItem() {
        val item = selectedItem ?: return
        pushHistory()
        val stt = com.vesodo.khaosat.platform.currentEpochMillis()
        val isLine = DrawingRenderer.isLineCommand(item.LENH)
        val offset = 45f
        val duplicated = if (isLine) {
            item.copy(
                STT = stt,
                X1 = item.X1 + offset,
                Y1 = item.Y1 + offset,
                X3 = item.X3 + offset,
                Y3 = item.Y3 + offset,
                CHON = 1
            )
        } else {
            val safePt = Function_tinhtoan_ve.PHAN_GIAI_VA_CHAM_NODE(
                listCanvas = listCanvas,
                targetX = item.X3 + offset,
                targetY = item.Y3 + offset,
                minDistance = 42f
            )
            item.copy(
                STT = stt,
                X3 = safePt.x,
                Y3 = safePt.y,
                CHON = 1
            )
        }
        listCanvas.add(duplicated)
        selectItem(duplicated)
        centerViewportOn(duplicated.X3, duplicated.Y3)
        triggerRedraw()
    }

    /**
     * Di chuyển góc nhìn bản đồ (Pan) lấy vị trí (canvasX, canvasY) làm tâm màn hình
     * Đảm bảo đối tượng vừa vẽ hoặc vừa chọn luôn hiển thị ở trung tâm không gian làm việc,
     * tuyệt đối không bị thanh điều khiển xoay & dịch chuyển ở đáy màn hình che khuất.
     */
    fun centerViewportOn(canvasX: Float, canvasY: Float) {
        val screenW = if (canvasSize.width > 0f) canvasSize.width else 1080f
        val screenH = if (canvasSize.height > 0f) canvasSize.height else 1920f
        val z = if (zoomScale > 0.001f) zoomScale else 1f

        // Căn giữa theo trục X
        panOffsetX = (screenW / 2f) - (canvasX * z)

        // Căn theo trục Y: Tính toán trừ hao thanh điều khiển xoay (bottom) & toolbar (top)
        // Tâm hiển thị an toàn rơi vào khoảng 42% chiều cao màn hình từ trên xuống
        val targetScreenY = screenH * 0.42f
        panOffsetY = targetScreenY - (canvasY * z)

        renderVersion++
    }

    fun addItem(
        x1: Float,
        y1: Float,
        x3: Float,
        y3: Float,
        command: Int,
        title: String = "",
        imageRes: Int = 0,
        autoCenter: Boolean = true
    ) {
        pushHistory()
        val stt = com.vesodo.khaosat.platform.currentEpochMillis()
        val item = Obj_CANVAS(
            MA_DVIQLY = customer.MA_DVIQLY,
            MA_YCAU_KNAI = customer.MA_YCAU_KNAI,
            STT = stt,
            TEN = title,
            LENH = command,
            X1 = x1,
            Y1 = y1,
            X2 = 0f,
            Y2 = 0f,
            X3 = x3,
            Y3 = y3,
            HINH = imageRes,
            THUOC_TINH = if (imageRes > 0) 1 else 0,
            SCALE = defaultObjectScale,
            CHON = 0
        )
        listCanvas.add(item)
        selectItem(item)
        incrementToolUsage(command)
        if (autoCenter) {
            val targetX = if (DrawingRenderer.isLineCommand(command)) (x1 + x3) / 2f else x3
            val targetY = if (DrawingRenderer.isLineCommand(command)) (y1 + y3) / 2f else y3
            centerViewportOn(targetX, targetY)
        }
        autoSaveLastSession()
    }

    fun removeItem(item: Obj_CANVAS) {
        pushHistory()
        listCanvas.remove(item)
        if (selectedItem == item) {
            selectedItem = null
            selectedItemRotation = 0f
            selectedItemScale = 1f
        }
        statusMessage = "Đã xóa phần tử"
        triggerRedraw()
        autoSaveLastSession()
    }

    fun clearAll() {
        if (listCanvas.isNotEmpty()) {
            pushHistory()
            listCanvas.clear()
            selectedItem = null
            selectedItemRotation = 0f
            selectedItemScale = 1f
            statusMessage = "Đã xóa toàn bộ sơ đồ"
            triggerRedraw()
            autoSaveLastSession()
        }
    }


    fun updateItemNote(item: Obj_CANVAS, newNote: String) {
        pushHistory()
        item.TEN = newNote
        val idx = listCanvas.indexOfFirst { it === item || (it.STT != 0L && it.STT == item.STT) }
        if (idx >= 0) {
            val updated = item.copy(TEN = newNote)
            listCanvas[idx] = updated
            selectedItem = updated
        }
        statusMessage = "Đã cập nhật ghi chú"
        triggerRedraw()
    }

    fun selectItem(item: Obj_CANVAS) {
        listCanvas.forEach { it.CHON = 0 }
        val idx = listCanvas.indexOfFirst { it === item || (it.STT != 0L && it.STT == item.STT) }
        val target = if (idx >= 0) listCanvas[idx] else item
        target.CHON = 1
        selectedItem = target
        selectedItemRotation = target.ROTATION
        val itemScale = if (target.SCALE > 0f) target.SCALE else 1f
        selectedItemScale = itemScale

        // Nếu đối tượng được chọn là Trụ điện hoặc Thiết bị đồ hoạ (không phải Text hay Dây):
        // Đồng bộ ngay tỷ lệ scale này làm mặc định cho các đối tượng vẽ tiếp theo
        if (target.LENH != Variables.VE_TEXT && !DrawingRenderer.isLineCommand(target.LENH) && itemScale >= 0.4f) {
            defaultObjectScale = itemScale
            coroutineScope?.launch {
                com.vesodo.khaosat.database.PlatformTemplateStorage.saveDefaultObjectScale(itemScale)
            }
        }

        currentCommand = Variables.MOVE
        triggerRedraw()
    }

    fun deselectAll() {
        listCanvas.forEach { it.CHON = 0 }
        selectedItem = null
        selectedItemRotation = 0f
        selectedItemScale = 1f
        triggerRedraw()
    }

    fun nudgeSelected(direction: Int, amount: Float = 5f) {
        val item = selectedItem ?: return
        var dx = 0f
        var dy = 0f
        when (direction) {
            Variables.LEN -> dy = -amount
            Variables.XUONG -> dy = amount
            Variables.TRAI -> dx = -amount
            Variables.PHAI -> dx = amount
        }
        translateItem(item, dx, dy)
        triggerRedraw()
    }

    fun rotateSelectedItem(degrees: Float, saveHistory: Boolean = false) {
        val item = selectedItem ?: return
        if (saveHistory) {
            pushHistory()
        }
        val normalized = ((degrees % 360f) + 360f) % 360f
        val updated = item.copy(ROTATION = normalized)
        val idx = listCanvas.indexOfFirst { it.STT == item.STT || it === item }
        if (idx >= 0) {
            listCanvas[idx] = updated
        }
        item.ROTATION = normalized
        selectedItem = updated
        selectedItemRotation = normalized
        triggerRedraw()
    }

    fun scaleSelectedItem(scale: Float, saveHistory: Boolean = false) {
        val item = selectedItem ?: return
        if (saveHistory) {
            pushHistory()
        }
        val clamped = (scale.coerceIn(0.4f, 3.5f) * 100).toInt() / 100f
        val updated = item.copy(SCALE = clamped)
        val idx = listCanvas.indexOfFirst { it.STT == item.STT || it === item }
        if (idx >= 0) {
            listCanvas[idx] = updated
        }
        item.SCALE = clamped
        selectedItem = updated
        selectedItemScale = clamped
        defaultObjectScale = clamped
        coroutineScope?.launch {
            com.vesodo.khaosat.database.PlatformTemplateStorage.saveDefaultObjectScale(clamped)
        }
        triggerRedraw()
    }

    fun updateDefaultObjectScale(scale: Float) {
        val clamped = (scale.coerceIn(0.4f, 3.5f) * 100).toInt() / 100f
        defaultObjectScale = clamped
        if (selectedItem != null) {
            scaleSelectedItem(clamped, saveHistory = true)
        }
        coroutineScope?.launch {
            com.vesodo.khaosat.database.PlatformTemplateStorage.saveDefaultObjectScale(clamped)
        }
        triggerRedraw()
    }

    fun updateUnitConfig(newConfig: com.vesodo.khaosat.database.SavedSurveyUnitConfig) {
        unitConfig = newConfig
        customer = customer.copy(MA_DVIQLY = newConfig.maDvi)
        coroutineScope?.launch {
            com.vesodo.khaosat.database.PlatformTemplateStorage.saveUnitConfig(newConfig)
        }
        statusMessage = "Đã lưu cấu hình đơn vị: ${newConfig.tenDvi}"
    }

    fun zoomInSelectedItem(step: Float = 0.1f) {
        scaleSelectedItem(selectedItemScale + step, saveHistory = true)
    }

    fun zoomOutSelectedItem(step: Float = 0.1f) {
        scaleSelectedItem(selectedItemScale - step, saveHistory = true)
    }

    fun resetSelectedItemScale() {
        scaleSelectedItem(1f, saveHistory = true)
    }

    fun saveHistorySnapshot() {
        pushHistory()
    }

    data class ConnectedEndpoint(
        var item: Obj_CANVAS,
        val isStartPoint: Boolean = false,
        val isEndPoint: Boolean = false,
        val isSingleNode: Boolean = false
    )

    val activeConnectedEndpoints = mutableListOf<ConnectedEndpoint>()

    /**
     * Tìm và đính kèm tất cả các đối tượng (trụ điện, nhà...) và đầu mút đường dây có toạ độ
     * trùng hoặc nằm trong bán kính kết nối với toạ độ anchor (anchorX, anchorY)
     */
    private fun findAndAttachEndpointsAt(anchorX: Float, anchorY: Float, excludeItem: Obj_CANVAS) {
        val threshold = maxOf(snapDistance.toFloat(), 28f)
        for (other in listCanvas) {
            if (other === excludeItem) continue

            if (DrawingRenderer.isLineCommand(other.LENH)) {
                // Kiểm tra đầu mút P1 (X1, Y1) của đường dây
                val d1 = Function_tinhtoan_ve.TINH_KC(anchorX, anchorY, other.X1, other.Y1)
                if (d1 <= threshold) {
                    other.X1 = anchorX
                    other.Y1 = anchorY
                    if (activeConnectedEndpoints.none { it.item === other && it.isStartPoint }) {
                        activeConnectedEndpoints.add(ConnectedEndpoint(other, isStartPoint = true))
                    }
                }

                // Kiểm tra đầu mút P2 (X3, Y3) của đường dây
                val d3 = Function_tinhtoan_ve.TINH_KC(anchorX, anchorY, other.X3, other.Y3)
                if (d3 <= threshold) {
                    other.X3 = anchorX
                    other.Y3 = anchorY
                    if (activeConnectedEndpoints.none { it.item === other && it.isEndPoint }) {
                        activeConnectedEndpoints.add(ConnectedEndpoint(other, isEndPoint = true))
                    }
                }
            } else {
                // Đối tượng 1 toạ độ (Nhà, Trụ điện, Trạm, Công tơ...)
                val dist = Function_tinhtoan_ve.TINH_KC(anchorX, anchorY, other.X3, other.Y3)
                if (dist <= threshold) {
                    other.X3 = anchorX
                    other.Y3 = anchorY
                    if (activeConnectedEndpoints.none { it.item === other && it.isSingleNode }) {
                        activeConnectedEndpoints.add(ConnectedEndpoint(other, isSingleNode = true))
                    }
                }
            }
        }
    }

    /**
     * Bắt đầu kéo thả đối tượng: Tự động phát hiện tất cả các đầu mút đường dây hoặc đối tượng
     * có cùng toạ độ với điểm đang chọn để di chuyển đồng bộ theo (Rubber-banding / Topological dragging)
     * - Khi kéo đối tượng 1 toạ độ (trụ, nhà...): Kéo theo các đầu dây nối vào nó.
     * - Khi kéo dây (đầu mút hoặc cả thân dây): Kéo theo các đối tượng (trụ, nhà...) trùng điểm đầu/cuối của dây.
     */
    fun startDragTranslate(item: Obj_CANVAS, handleType: DragHandleType = DragHandleType.WHOLE_ITEM) {
        pushHistory()
        activeConnectedEndpoints.clear()

        val isLine = DrawingRenderer.isLineCommand(item.LENH)

        if (isLine) {
            when (handleType) {
                DragHandleType.LINE_START -> {
                    // Kéo đầu mút P1 của dây -> Kéo theo các đối tượng/dây trùng toạ độ P1
                    findAndAttachEndpointsAt(item.X1, item.Y1, item)
                }
                DragHandleType.LINE_END -> {
                    // Kéo đầu mút P2 của dây -> Kéo theo các đối tượng/dây trùng toạ độ P2
                    findAndAttachEndpointsAt(item.X3, item.Y3, item)
                }
                DragHandleType.WHOLE_ITEM -> {
                    // Kéo cả thân dây -> Kéo theo các đối tượng/dây trùng toạ độ ở cả đầu P1 và đầu P2
                    findAndAttachEndpointsAt(item.X1, item.Y1, item)
                    findAndAttachEndpointsAt(item.X3, item.Y3, item)
                }
                DragHandleType.NONE -> {}
            }
        } else {
            // Đối tượng 1 toạ độ (Trụ điện, Nhà, Trạm...) -> Kéo theo các dây và đối tượng nối vào (X3, Y3)
            findAndAttachEndpointsAt(item.X3, item.Y3, item)
        }
    }

    fun endDragTranslate() {
        val movedItem = selectedItem
        if (movedItem != null) {
            updateConnectedWireDistances(movedItem)
        }
        activeConnectedEndpoints.clear()
    }

    /**
     * Tính toán khoảng cách thực tế của dây dẫn nối giữa 2 điểm (mét),
     * tự động nhân với hệ số độ võng 1.2 theo quy chuẩn thiết kế điện lực EVN:
     * - Nếu có nền bản đồ GPS (ảnh chụp hoặc sơ đồ vector): Chuyển đổi toạ độ canvas sang toạ độ GPS (lat/lon)
     *   và tính khoảng cách Haversine mặt đất chính xác, sau đó nhân với độ võng 1.2
     * - Nếu không có nền bản đồ GPS: Tính theo tỷ lệ pixel chuẩn (DEFAULT_PIXELS_PER_METER) nhân với 1.2
     */
    fun calculateWireDistanceWithSag(x1: Float, y1: Float, x2: Float, y2: Float): Int {
        val pxDist = Function_tinhtoan_ve.TINH_KC(x1, y1, x2, y2)
        if (pxDist <= 0.5f) return 0

        val hasMapActive = (showCapturedMap && capturedMapBackground != null) ||
                           (showVectorRoads && vectorBasemap != null)

        val straightMeters = if (hasMapActive) {
            val centerGps = vectorBasemap?.centerGps ?: capturedMapCenter
            val zoom = (vectorBasemap?.zoom ?: capturedMapZoom).coerceIn(10, 22)
            val canvasW = if (canvasSize.width > 0f) canvasSize.width else 1920f
            val canvasH = if (canvasSize.height > 0f) canvasSize.height else 1080f

            val latRad = centerGps.latitude * kotlin.math.PI / 180.0
            val worldSize = 256.0 * (1 shl zoom)

            val centerWorldX = (centerGps.longitude + 180.0) / 360.0 * worldSize
            val centerWorldY = (1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / kotlin.math.PI) / 2.0 * worldSize

            val p1WorldX = centerWorldX + (x1 - canvasW / 2f)
            val p1WorldY = centerWorldY + (y1 - canvasH / 2f)
            val lon1 = (p1WorldX / worldSize) * 360.0 - 180.0
            val lat1Rad = kotlin.math.atan(kotlin.math.sinh(kotlin.math.PI * (1.0 - 2.0 * (p1WorldY / worldSize))))
            val lat1 = lat1Rad * 180.0 / kotlin.math.PI

            val p2WorldX = centerWorldX + (x2 - canvasW / 2f)
            val p2WorldY = centerWorldY + (y2 - canvasH / 2f)
            val lon2 = (p2WorldX / worldSize) * 360.0 - 180.0
            val lat2Rad = kotlin.math.atan(kotlin.math.sinh(kotlin.math.PI * (1.0 - 2.0 * (p2WorldY / worldSize))))
            val lat2 = lat2Rad * 180.0 / kotlin.math.PI

            // Haversine formula
            val earthRadius = 6371000.0 // Bán kính Trái Đất (m)
            val dLat = (lat2 - lat1) * kotlin.math.PI / 180.0
            val dLon = (lon2 - lon1) * kotlin.math.PI / 180.0
            val a = kotlin.math.sin(dLat / 2.0).let { it * it } +
                    kotlin.math.cos(lat1 * kotlin.math.PI / 180.0) *
                    kotlin.math.cos(lat2 * kotlin.math.PI / 180.0) *
                    kotlin.math.sin(dLon / 2.0).let { it * it }
            val c = 2.0 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1.0 - a))
            earthRadius * c
        } else {
            pxDist / com.vesodo.khaosat.ai.DiagramRuleEngine.DEFAULT_PIXELS_PER_METER
        }

        val sagFactor = 1.2
        val actualLengthMeters = straightMeters * sagFactor
        return kotlin.math.round(actualLengthMeters).toInt()
    }

    /**
     * Tự động tính toán lại khoảng cách (kèm độ võng 1.2) cho tất cả các đường dây
     * nối giữa 2 trụ hoặc liên kết với đối tượng (trụ điện) vừa di chuyển.
     */
    fun updateConnectedWireDistances(targetItem: Obj_CANVAS) {
        val isTargetLine = DrawingRenderer.isLineCommand(targetItem.LENH)
        val threshold = 45f

        fun isPoleOrSub(cv: Obj_CANVAS): Boolean {
            return IconRegistry.isTru(cv.LENH) ||
                   com.vesodo.khaosat.ai.DiagramRuleEngine.isPoleCommand(cv.LENH) ||
                   com.vesodo.khaosat.ai.DiagramRuleEngine.isSubstationCommand(cv.LENH)
        }

        val candidateWires = if (isTargetLine) {
            listOf(targetItem)
        } else {
            // Tìm tất cả các đường dây nối vào đối tượng (trụ điện / trạm) này
            listCanvas.filter { wire ->
                DrawingRenderer.isLineCommand(wire.LENH) &&
                (Function_tinhtoan_ve.TINH_KC(wire.X1, wire.Y1, targetItem.X3, targetItem.Y3) <= threshold ||
                 Function_tinhtoan_ve.TINH_KC(wire.X3, wire.Y3, targetItem.X3, targetItem.Y3) <= threshold)
            }
        }

        var hasChanges = false
        for (wire in candidateWires) {
            val p1Pole = listCanvas.firstOrNull { it !== wire && isPoleOrSub(it) && Function_tinhtoan_ve.TINH_KC(wire.X1, wire.Y1, it.X3, it.Y3) <= threshold }
            val p2Pole = listCanvas.firstOrNull { it !== wire && isPoleOrSub(it) && Function_tinhtoan_ve.TINH_KC(wire.X3, wire.Y3, it.X3, it.Y3) <= threshold }

            if (p1Pole != null && p2Pole != null && p1Pole !== p2Pole) {
                val isDistanceLabel = wire.TEN.isBlank() || wire.TEN.matches(Regex("""^\d+(\.\d+)?\s*m$"""))
                if (isDistanceLabel) {
                    val meters = calculateWireDistanceWithSag(wire.X1, wire.Y1, wire.X3, wire.Y3)
                    if (meters > 0) {
                        val newLabel = "${meters}m"
                        if (wire.TEN != newLabel) {
                            wire.TEN = newLabel
                            hasChanges = true
                        }
                    }
                }
            }
        }

        if (hasChanges) {
            triggerRedraw()
            autoSaveLastSession()
        }
    }

    /**
     * Di chuyển đầu mút (LINE_START / LINE_END) hoặc cả khối đối tượng (WHOLE_ITEM)
     * đồng thời kéo theo tất cả các đầu mút đường dây và đối tượng được kết nối với nó
     */
    fun translateItemHandle(item: Obj_CANVAS, handleType: DragHandleType, dx: Float, dy: Float): Obj_CANVAS {
        if (dx == 0f && dy == 0f) return item

        when (handleType) {
            DragHandleType.LINE_START -> {
                item.X1 += dx
                item.Y1 += dy
            }
            DragHandleType.LINE_END -> {
                item.X3 += dx
                item.Y3 += dy
            }
            DragHandleType.WHOLE_ITEM -> {
                item.X3 += dx
                item.Y3 += dy
                if (DrawingRenderer.isLineCommand(item.LENH)) {
                    item.X1 += dx
                    item.Y1 += dy
                }
            }
            DragHandleType.NONE -> return item
        }

        // Di chuyển đồng thời tất cả các điểm cùng toạ độ đã được liên kết
        for (conn in activeConnectedEndpoints) {
            if (conn.isSingleNode) {
                conn.item.X3 += dx
                conn.item.Y3 += dy
            } else {
                if (conn.isStartPoint) {
                    conn.item.X1 += dx
                    conn.item.Y1 += dy
                }
                if (conn.isEndPoint) {
                    conn.item.X3 += dx
                    conn.item.Y3 += dy
                }
            }
        }

        var resultItem = item
        val movingIdx = listCanvas.indexOfFirst { it === item || (it.STT != 0L && it.STT == item.STT) }
        if (movingIdx >= 0) {
            val copy = item.copy()
            listCanvas[movingIdx] = copy
            selectedItem = copy
            resultItem = copy
        }
        for (conn in activeConnectedEndpoints) {
            val cIdx = listCanvas.indexOfFirst { it === conn.item || (conn.item.STT != 0L && it.STT == conn.item.STT) }
            if (cIdx >= 0) {
                val copyConn = conn.item.copy()
                listCanvas[cIdx] = copyConn
                conn.item = copyConn
            }
        }
        renderVersion++
        return resultItem
    }

    fun translateItem(item: Obj_CANVAS, dx: Float, dy: Float) {
        startDragTranslate(item, DragHandleType.WHOLE_ITEM)
        translateItemHandle(item, DragHandleType.WHOLE_ITEM, dx, dy)
        endDragTranslate()
    }

    /**
     * Dịch chuyển toàn bộ các đối tượng trên bản vẽ theo bản đồ khi người dùng kéo rê bản đồ nền
     */
    fun panAllItems(dx: Float, dy: Float) {
        if (dx == 0f && dy == 0f) return
        for (i in listCanvas.indices) {
            val itm = listCanvas[i]
            listCanvas[i] = itm.copy(
                X1 = itm.X1 + dx,
                Y1 = itm.Y1 + dy,
                X3 = itm.X3 + dx,
                Y3 = itm.Y3 + dy
            )
        }
        renderVersion++
    }

    /**
     * Thu phóng toàn bộ bảng vẽ (CAD Viewport Zoom) quanh tâm (focusX, focusY)
     * Kích hoạt khi người dùng dùng 2 ngón tay thu phóng (Pinch-to-zoom) lúc không chọn đối tượng nào.
     */
    fun zoomAllCanvas(factor: Float, focusX: Float, focusY: Float) {
        if (factor <= 0f || kotlin.math.abs(factor - 1f) < 0.0005f) return

        val newZoom = (zoomScale * factor).coerceIn(0.20f, 6.0f)
        val actualFactor = newZoom / zoomScale
        if (kotlin.math.abs(actualFactor - 1f) < 0.0002f) return

        // Dịch chuyển toạ độ pan để điểm tại (focusX, focusY) không bị trôi
        panOffsetX = focusX - (focusX - panOffsetX) * actualFactor
        panOffsetY = focusY - (focusY - panOffsetY) * actualFactor
        zoomScale = newZoom

        renderVersion++
    }

    /**
     * Đặt lại mức thu phóng và vị trí bảng vẽ về gốc 100%
     */
    fun resetCanvasZoom() {
        if (zoomScale == 1f && panOffsetX == 0f && panOffsetY == 0f) return
        zoomScale = 1f
        panOffsetX = 0f
        panOffsetY = 0f
        renderVersion++
        statusMessage = "Đã đặt lại kích thước bảng vẽ 100%"
    }

    /**
     * Thả công cụ từ thanh Palette trực tiếp lên vùng vẽ Canvas
     */
    fun dropPaletteToolAt(rootPos: Offset): Boolean {
        val tool = draggingPaletteTool ?: return false
        val bounds = canvasBoundsInRoot ?: return false
        if (bounds.contains(rootPos)) {
            val rawX = rootPos.x - bounds.left
            val rawY = rootPos.y - bounds.top
            val z = if (zoomScale > 0.001f) zoomScale else 1f
            val canvasX = (rawX - panOffsetX) / z
            val canvasY = (rawY - panOffsetY) / z
            val canvasSnapDist = snapDistance / z

            val isLine = DrawingRenderer.isLineCommand(tool.command)
            if (isLine) {
                val snapped = Function_tinhtoan_ve.KT_DIEM_GAN_NHAT(listCanvas, canvasX, canvasY, canvasSnapDist)
                val finalX = snapped.x
                val finalY = snapped.y
                addItem(
                    x1 = finalX - 40f,
                    y1 = finalY,
                    x3 = finalX + 40f,
                    y3 = finalY,
                    command = tool.command,
                    title = ""
                )
            } else {
                // Áp dụng thuật toán phân giải va chạm cho các đối tượng 1 điểm (cột, trạm, thiết bị...)
                // Ngăn ngừa việc đặt trùng hoặc quá gần (< 42px) một đối tượng nút khác
                val safePoint = Function_tinhtoan_ve.PHAN_GIAI_VA_CHAM_NODE(
                    listCanvas = listCanvas,
                    targetX = canvasX,
                    targetY = canvasY,
                    minDistance = 42f / z
                )
                val isPoleOrSub = IconRegistry.isTru(tool.command) || DiagramRuleEngine.isSubstationCommand(tool.command) || DiagramRuleEngine.isPoleCommand(tool.command)
                val defaultTitle = if (isPoleOrSub) "" else tool.name
                addItem(
                    x1 = 0f,
                    y1 = 0f,
                    x3 = safePoint.x,
                    y3 = safePoint.y,
                    command = tool.command,
                    title = defaultTitle,
                    imageRes = tool.command
                )
                if (safePoint.x != canvasX || safePoint.y != canvasY) {
                    statusMessage = "Đã tự động căn chỉnh khoảng cách để không bị đè icon"
                }
            }
            val newlyAdded = listCanvas.lastOrNull()
            if (newlyAdded != null) {
                selectItem(newlyAdded)
            }
            currentCommand = Variables.MOVE
            com.vesodo.khaosat.debug.DebugLogger.log(
                com.vesodo.khaosat.debug.LogType.DRAW_NEW,
                "Thả công cụ: ${tool.name}",
                "Đã vẽ tại (${canvasX.toInt()}, ${canvasY.toInt()})"
            )
            draggingPaletteTool = null
            draggingPaletteOffset = null
            return true
        }
        draggingPaletteTool = null
        draggingPaletteOffset = null
        return false
    }

    /**
     * Xử lý di chuyển theo thuật toán move_to_point_KT
     */
    fun movePoint(xFrom: Float, yFrom: Float, xTo: Float, yTo: Float) {
        pushHistory()
        val mutableCopy = listCanvas.toMutableList()
        Function_tinhtoan_ve.MOVE_TO_POINT(mutableCopy, xFrom, yFrom, xTo, yTo, snapDistance)
        listCanvas.clear()
        listCanvas.addAll(mutableCopy)
        renderVersion++
    }

    fun loadTemplate(templateItems: List<Obj_CANVAS>) {
        pushHistory()
        listCanvas.clear()
        templateItems.forEach {
            var lenh = it.LENH
            // Chuẩn hóa loại dây trong sơ đồ mẫu:
            // Tuyệt đối không dùng mũi tên làm dây nối
            if (lenh == Variables.VE_MUI_TEN || lenh == Variables.VE_ARROW) {
                lenh = Variables.VE_DUONG_THANG
            }
            val copy = it.copy(
                MA_DVIQLY = customer.MA_DVIQLY,
                MA_YCAU_KNAI = customer.MA_YCAU_KNAI,
                STT = com.vesodo.khaosat.platform.currentEpochMillis() + (0..1000).random(),
                LENH = lenh
            )
            listCanvas.add(copy)
        }
        statusMessage = "Đã nạp sơ đồ mẫu (${templateItems.size} đối tượng)"
        autoSaveLastSession()
    }

    /**
     * Tự động sửa lỗi kỹ thuật theo đề xuất của AI Rule Engine
     */
    fun applyQuickFix(issue: DiagramIssue) {
        val target = issue.targetItem ?: return
        pushHistory()
        when (issue.category) {
            IssueCategory.SPAN_LENGTH -> {
                // Tách đôi nhịp dây và cắm trụ ở giữa
                val midX = (target.X1 + target.X3) / 2f
                val midY = (target.Y1 + target.Y3) / 2f
                val midPt = Point2D(midX, midY)

                val (wire1, wire2) = SmartRoutingEngine.splitWireWithPole(
                    wire = target,
                    polePt = midPt,
                    startStt = listCanvas.size + 1
                )

                // Nếu có nền bản đồ hoặc dây target ban đầu đã có nhãn mét thì tính lại khoảng cách chính xác kèm độ võng 1.2
                val hasBasemap = (showCapturedMap && capturedMapBackground != null) ||
                                 (showVectorRoads && vectorBasemap != null)
                if (hasBasemap || target.TEN.isNotBlank()) {
                    val m1 = calculateWireDistanceWithSag(wire1.X1, wire1.Y1, wire1.X3, wire1.Y3)
                    val m2 = calculateWireDistanceWithSag(wire2.X1, wire2.Y1, wire2.X3, wire2.Y3)
                    if (m1 > 0) wire1.TEN = "${m1}m"
                    if (m2 > 0) wire2.TEN = "${m2}m"
                } else {
                    wire1.TEN = ""
                    wire2.TEN = ""
                }

                // Tìm trụ vừa vẽ gần nhất để sao chép thuộc tính và tỷ lệ scale:
                // Ưu tiên 1: Các trụ kết nối ở hai đầu dây target (lấy trụ có STT lớn hơn - vừa vẽ gần đây)
                val p1 = listCanvas.firstOrNull { IconRegistry.isTru(it.LENH) && Function_tinhtoan_ve.TINH_KC(target.X1, target.Y1, it.X3, it.Y3) <= 45f }
                val p2 = listCanvas.firstOrNull { IconRegistry.isTru(it.LENH) && Function_tinhtoan_ve.TINH_KC(target.X3, target.Y3, it.X3, it.Y3) <= 45f }
                val connectedPoles = listOfNotNull(p1, p2)
                val referencePole = connectedPoles.maxByOrNull { it.STT }
                    ?: listCanvas.findLast { IconRegistry.isTru(it.LENH) }

                val poleCmd = referencePole?.LENH ?: autoSpacingConfig.poleCmd
                val poleThuocTinh = referencePole?.THUOC_TINH ?: 0
                val poleHinh = referencePole?.HINH ?: poleCmd
                val poleScale = if (referencePole != null && referencePole.SCALE > 0.1f) referencePole.SCALE else defaultObjectScale

                val newPole = Obj_CANVAS(
                    MA_DVIQLY = referencePole?.MA_DVIQLY ?: "",
                    MA_YCAU_KNAI = referencePole?.MA_YCAU_KNAI ?: "",
                    STT = (listCanvas.size + 3).toLong(),
                    LENH = poleCmd,
                    X1 = 0f,
                    Y1 = 0f,
                    X3 = midX,
                    Y3 = midY,
                    TEN = "", // Không cần nhãn theo yêu cầu
                    HINH = poleHinh,
                    THUOC_TINH = poleThuocTinh,
                    SCALE = poleScale, // Tỉ lệ scale tương tự trụ gần nhất
                    CHON = 0
                )
                listCanvas.remove(target)
                listCanvas.add(newPole)
                listCanvas.add(wire1)
                listCanvas.add(wire2)
                selectItem(newPole)
                centerViewportOn(midX, midY)
                com.vesodo.khaosat.debug.DebugLogger.log(
                    com.vesodo.khaosat.debug.LogType.DRAW_NEW,
                    "Chèn trụ đệm",
                    "Đã chèn trụ đệm loại #$poleCmd tại ($midX, $midY) với tỷ lệ ${(poleScale * 100).toInt()}% (không nhãn)"
                )
                autoSaveLastSession()
            }
            IssueCategory.MISSING_NOTE -> {
                val meters = calculateWireDistanceWithSag(target.X1, target.Y1, target.X3, target.Y3)
                updateItemNote(target, "${meters}m")
                statusMessage = "Đã tự động cập nhật nhãn cự ly ${meters}m (kèm độ võng 1.2)"
            }
            IssueCategory.INVALID_WIRE_TYPE -> {
                val isConnectedToHouse = isHouseOrMeterAtPoint(target.X1, target.Y1) || isHouseOrMeterAtPoint(target.X3, target.Y3)
                target.LENH = if (isConnectedToHouse) Variables.VE_LINE_DUT_NET else Variables.VE_DUONG_THANG
                renderVersion++
                statusMessage = "Đã chuẩn hóa loại dây thành ${if (isConnectedToHouse) "dây nét đứt vào nhà KH" else "dây nối liền không đứt nét"}"
                autoSaveLastSession()
            }
            else -> {}
        }
    }

    /**
     * Kiểm tra tại toạ độ chỉ định có Nhà khách hàng hoặc Công tơ không
     */
    fun isHouseOrMeterAtPoint(x: Float, y: Float, thresholdPx: Float = 50f): Boolean {
        return listCanvas.any { item ->
            val isHouseOrMeter = (item.LENH == Variables.VE_NHA ||
                                  item.LENH == Variables.VE_CONGTO_TREO_NHA ||
                                  item.LENH == Variables.VE_HINH_CONGTO ||
                                  item.LENH in 701..706)
            isHouseOrMeter && Function_tinhtoan_ve.TINH_KC(x, y, item.X3, item.Y3) <= thresholdPx
        }
    }

    /**
     * AI tự động phân chia nhịp và cắm trụ chuẩn EVN nối giữa hai điểm
     */
    fun applyAutoRoute(startPt: Point2D, endPt: Point2D) {
        pushHistory()
        val isStartHouse = isHouseOrMeterAtPoint(startPt.x, startPt.y)
        val isEndHouse = isHouseOrMeterAtPoint(endPt.x, endPt.y)

        val result = SmartRoutingEngine.generateAutoPolesRoute(
            startPt = startPt,
            endPt = endPt,
            poleCommand = autoSpacingConfig.poleCmd,
            wireCommand = autoSpacingConfig.wireCmd,
            targetSpanMeters = autoSpacingConfig.spanMeters,
            startStt = listCanvas.size + 1,
            poleNamePrefix = autoSpacingConfig.polePrefix,
            poleScale = defaultObjectScale,
            isEndHouseOrMeter = isEndHouse,
            isStartHouseOrMeter = isStartHouse
        )
        // Nếu có nền bản đồ, tính lại cự ly từng dây theo toạ độ GPS * 1.2 độ võng
        val hasBasemap = (showCapturedMap && capturedMapBackground != null) ||
                         (showVectorRoads && vectorBasemap != null)
        if (hasBasemap) {
            for (wire in result.newWires) {
                val meters = calculateWireDistanceWithSag(wire.X1, wire.Y1, wire.X3, wire.Y3)
                if (meters > 0) {
                    wire.TEN = "${meters}m"
                }
            }
        }
        listCanvas.addAll(result.newPoles)
        listCanvas.addAll(result.newWires)
        isAutoSpacingActive = false
        autoSpacingStartPoint = null
        statusMessage = "AI đã tự động rải ${result.newPoles.size} cột trên ${result.spanCount} nhịp (${result.totalDistanceMeters.toInt()}m)"
        autoSaveLastSession()
    }

    /**
     * Mở hộp thoại đề xuất thêm khách hàng bên trái / bên phải so với điểm gắn đồng hồ
     */
    fun openCustomerAttachmentDialog(parentItem: Obj_CANVAS, side: com.vesodo.khaosat.ai.CustomerSide) {
        pendingCustomerParentItem = parentItem
        pendingCustomerSide = side
        showCustomerAttachmentDialog = true
    }

    /**
     * Thêm thông tin khách hàng vào sơ đồ:
     * - Ô chữ nhật bao bọc thông tin khách hàng bằng nét đứt (VE_TEXT)
     * - Chỉ gồm thông tin Mã KH (nếu có) và Tên KH kèm ghi chú vị trí trái/phải
     * - Nếu không có mã: không hiển thị chữ không có mã, chỉ hiển thị dòng Tên kèm ghi chú vị trí (ví dụ: Tên: Nguyễn Văn An (KH Trái))
     * - Tỷ lệ scale theo đối tượng text (SCALE = 1.0f), không theo defaultObjectScale của bảng vẽ
     * - Không vẽ mũi tên, không cần khoảng cách hay độ võng
     */
    fun addCustomerAttachment(
        parentItem: Obj_CANVAS,
        side: com.vesodo.khaosat.ai.CustomerSide,
        customerName: String,
        customerCode: String
    ) {
        pushHistory()
        val sttBase = com.vesodo.khaosat.platform.currentEpochMillis()
        val isLeft = (side == com.vesodo.khaosat.ai.CustomerSide.LEFT)

        // 1. Tọa độ của ô chữ nhật thông tin khách hàng (đặt lệch trái hoặc phải so với điểm đồng hồ)
        val boxOffsetX = if (isLeft) -130f else 130f
        val boxX = parentItem.X3 + boxOffsetX
        val boxY = parentItem.Y3 - 18f

        val sideNote = if (isLeft) "KH Trái" else "KH Phải"
        val rawName = customerName.trim()
        val nameClean = if (rawName.isBlank()) "Khách hàng" else rawName
        val nameWithSide = if (nameClean.contains("trái", ignoreCase = true) || nameClean.contains("phải", ignoreCase = true)) {
            nameClean
        } else {
            "$nameClean ($sideNote)"
        }
        val codeClean = customerCode.trim()

        val customerTextContent = if (codeClean.isNotBlank()) {
            "Mã: $codeClean\nTên: $nameWithSide"
        } else {
            "Tên: $nameWithSide"
        }

        val textBoxItem = Obj_CANVAS(
            MA_DVIQLY = customer.MA_DVIQLY,
            MA_YCAU_KNAI = customer.MA_YCAU_KNAI,
            STT = sttBase + 1L,
            TEN = customerTextContent,
            LENH = Variables.VE_TEXT,
            X1 = 0f,
            Y1 = 0f,
            X3 = boxX,
            Y3 = boxY,
            SCALE = 1.0f, // Tỷ lệ scale của text theo đối tượng text, KHÔNG theo defaultObjectScale của bảng vẽ
            CHON = 0
        )

        listCanvas.add(textBoxItem)
        selectItem(textBoxItem)
        statusMessage = "Đã thêm thông tin khách hàng ($sideNote)"
        showCustomerAttachmentDialog = false
        pendingCustomerParentItem = null
        triggerRedraw()
        autoSaveLastSession()
    }
}


