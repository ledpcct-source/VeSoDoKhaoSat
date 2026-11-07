package com.vesodo.khaosat.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vesodo.khaosat.debug.DebugLogger
import com.vesodo.khaosat.debug.LogType

/**
 * ==============================================================================================
 * CẤU HÌNH TOÀN CỤC ỨNG DỤNG (GLOBAL APPLICATION CONFIGURATION)
 * ==============================================================================================
 */
object AppConfig {

    // ==========================================================================================
    // ⚙️ CỜ BẬT / TẮT GHI LOG VÀ GIAO DIỆN DEBUG DÙNG CHUNG TOÀN ỨNG DỤNG
    // ------------------------------------------------------------------------------------------
    // • ĐẶT = true : Khi đang lập trình / kiểm thử (Hiện nút Log trên TopToolbar, hiện bảng log nổi,
    //                theo dõi tọa độ con trỏ thời gian thực).
    // • ĐẶT = false: KHI XUẤT BẢN PRODUCTION THƯƠNG MẠI.
    //                -> Vô hiệu hóa 100% việc ghi log nền (tiết kiệm tối đa CPU, RAM và pin).
    //                -> Tự động ẩn hoàn toàn nút "Log: Bật/Tắt" trên thanh công cụ TopToolbar.
    //                -> Tự động ẩn bảng log nổi và mọi toạ độ debug để màn hình sạch sẽ 100%.
    // ==========================================================================================
    var isTEST by mutableStateOf(false) // <--- CHỈ CẦN ĐỔI THÀNH false Ở ĐÂY KHI XUẤT PRODUCTION

    /** Bí danh đọc nhanh trạng thái cờ log */
    val ENABLE_DEBUG_LOG: Boolean
        get() = isTEST

    // Quản lý Dialog hiển thị lỗi toàn cục (Global Error Boundary)
    var currentError by mutableStateOf<Throwable?>(null)
    var errorMessage by mutableStateOf("")
    var errorStackTrace by mutableStateOf("")
    var showErrorDialog by mutableStateOf(false)

    /**
     * Bắt lỗi và hiển thị dialog cảnh báo thay vì để ứng dụng bị crash/đóng đột ngột
     */
    fun reportError(throwable: Throwable, contextInfo: String = "") {
        currentError = throwable
        val msg = throwable.message ?: throwable.toString()
        errorMessage = if (contextInfo.isNotBlank()) "$contextInfo: $msg" else msg
        errorStackTrace = throwable.stackTraceToString()
        showErrorDialog = true

        DebugLogger.log(
            type = LogType.SYSTEM,
            title = "PHÁT HIỆN LỖI: ${throwable::class.simpleName ?: "Exception"}",
            detail = errorMessage,
            coords = ""
        )
    }

    fun dismissError() {
        showErrorDialog = false
        currentError = null
        errorMessage = ""
        errorStackTrace = ""
    }
}
