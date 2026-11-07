package com.vesodo.khaosat.debug

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.vesodo.khaosat.platform.getFormattedCurrentTime

enum class LogType(val label: String, val badgeColorHex: Long) {
    SELECT("CHỌN", 0xFF6366F1),          // Indigo
    DRAG_START("KÉO ĐẦU", 0xFF0284C7),    // Sky Blue
    DRAG_MOVE("ĐANG KÉO", 0xFF0D9488),    // Teal
    DRAG_END("THẢ XONG", 0xFF059669),    // Emerald
    DRAW_NEW("VẼ MỚI", 0xFFD97706),      // Amber
    SNAP("SNAP", 0xFF9333EA),            // Purple
    SYSTEM("HỆ THỐNG", 0xFF64748B)       // Slate
}

data class LogEntry(
    val id: Long,
    val timeStr: String,
    val type: LogType,
    val title: String,
    val detail: String,
    val coords: String
)

object DebugLogger {
    var isEnabled: Boolean
        get() = com.vesodo.khaosat.config.AppConfig.isTEST
        set(value) { com.vesodo.khaosat.config.AppConfig.isTEST = value }

    private var _isPanelVisible by mutableStateOf(true)
    var isPanelVisible: Boolean
        get() = com.vesodo.khaosat.config.AppConfig.isTEST && _isPanelVisible
        set(value) { _isPanelVisible = value }

    var isAutoScroll by mutableStateOf(true)

    // Toạ độ con trỏ thời gian thực
    var liveCursorX by mutableStateOf(0f)
    var liveCursorY by mutableStateOf(0f)
    var liveMovingItemDesc by mutableStateOf("")

    val logs = mutableStateListOf<LogEntry>()
    private var counter = 0L

    private fun getCurrentTime(): String = getFormattedCurrentTime()

    fun log(type: LogType, title: String, detail: String = "", coords: String = "") {
        if (!com.vesodo.khaosat.config.AppConfig.isTEST || !isEnabled) return
        val entry = LogEntry(
            id = ++counter,
            timeStr = getCurrentTime(),
            type = type,
            title = title,
            detail = detail,
            coords = coords
        )
        logs.add(entry)
        if (logs.size > 200) {
            logs.removeAt(0)
        }
    }

    fun logSelect(itemDesc: String, x: Float, y: Float) {
        log(
            type = LogType.SELECT,
            title = "Chọn đối tượng: $itemDesc",
            detail = "Đã chọn làm đối tượng hoạt động",
            coords = "X=${x.toInt()}, Y=${y.toInt()}"
        )
    }

    fun logDragStart(itemDesc: String, startX: Float, startY: Float, isExistingItem: Boolean) {
        val mode = if (isExistingItem) "Di chuyển phần tử có sẵn" else "Bắt đầu vẽ nét mới"
        log(
            type = LogType.DRAG_START,
            title = "Bắt đầu kéo: $itemDesc ($mode)",
            detail = if (isExistingItem) "Bắt đầu kéo rê icon theo con trỏ" else "Điểm bắt đầu vẽ",
            coords = "Bắt đầu: (${startX.toInt()}, ${startY.toInt()})"
        )
    }

    fun logDrag(itemDesc: String, currentX: Float, currentY: Float, dx: Float, dy: Float) {
        if (!com.vesodo.khaosat.config.AppConfig.isTEST) return
        liveCursorX = currentX
        liveCursorY = currentY
        liveMovingItemDesc = itemDesc

        // Ghi log bước kéo (giới hạn tần suất để không làm chậm UI)
        if (counter % 3L == 0L) {
            val dxSign = if (dx >= 0) "+${dx.toInt()}" else "${dx.toInt()}"
            val dySign = if (dy >= 0) "+${dy.toInt()}" else "${dy.toInt()}"
            log(
                type = LogType.DRAG_MOVE,
                title = "Kéo di chuyển $itemDesc",
                detail = "Delta: (dx=$dxSign, dy=$dySign)",
                coords = "Hiện tại: (${currentX.toInt()}, ${currentY.toInt()})"
            )
        }
    }

    fun logDragEnd(itemDesc: String, endX: Float, endY: Float, totalDx: Float, totalDy: Float) {
        liveMovingItemDesc = ""
        log(
            type = LogType.DRAG_END,
            title = "Thả đối tượng: $itemDesc",
            detail = "Tổng dịch chuyển: dx=${totalDx.toInt()}, dy=${totalDy.toInt()}",
            coords = "Toạ độ đích: (${endX.toInt()}, ${endY.toInt()})"
        )
    }

    fun logDrawNew(cmdName: String, x1: Float, y1: Float, x3: Float, y3: Float) {
        log(
            type = LogType.DRAW_NEW,
            title = "Tạo mới: $cmdName",
            detail = "Thêm phần tử vào danh sách sơ đồ",
            coords = "Từ (${x1.toInt()}, ${y1.toInt()}) -> (${x3.toInt()}, ${y3.toInt()})"
        )
    }

    fun logSnap(target: String, fromX: Float, fromY: Float, toX: Float, toY: Float) {
        log(
            type = LogType.SNAP,
            title = "Bắt dính điểm (Snap)",
            detail = "Dính vào $target",
            coords = "(${fromX.toInt()}, ${fromY.toInt()}) -> (${toX.toInt()}, ${toY.toInt()})"
        )
    }

    fun clear() {
        logs.clear()
        counter = 0L
    }
}
