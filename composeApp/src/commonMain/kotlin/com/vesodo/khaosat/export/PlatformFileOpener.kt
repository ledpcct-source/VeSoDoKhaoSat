package com.vesodo.khaosat.export

/**
 * Trình hỗ trợ tạo file tạm và gọi thư viện / ứng dụng hệ thống để mở xem tệp CAD/Web.
 */
expect object PlatformFileOpener {

    /**
     * Tạo một tệp DXF tạm thời từ nội dung chuỗi DXF
     * @return Đường dẫn tuyệt đối đến tệp tạm, hoặc null nếu lỗi
     */
    fun createTempDxfFile(dxfContent: String, prefix: String = "sodo_preview_"): String?

    /**
     * Tạo một tệp HTML xem trước Vector CAD tạm thời
     * @return Đường dẫn tuyệt đối đến tệp tạm, hoặc null nếu lỗi
     */
    fun createTempHtmlFile(htmlContent: String, prefix: String = "sodo_webcad_"): String?

    /**
     * Mở tệp bằng ứng dụng chuyên dụng của hệ điều hành (AutoCAD, DWG FastView, trình duyệt, v.v.)
     */
    fun openFile(filePath: String, mimeType: String = "*/*"): Boolean

    /**
     * Mở URL hoặc đường dẫn file bằng trình duyệt web mặc định
     */
    fun openUrl(url: String): Boolean

    /**
     * Mở thư mục chứa file vừa lưu trên Desktop (tự động bôi sáng/chọn tệp vừa tạo trong File Explorer)
     */
    fun openFolder(folderOrFilePath: String): Boolean
}
