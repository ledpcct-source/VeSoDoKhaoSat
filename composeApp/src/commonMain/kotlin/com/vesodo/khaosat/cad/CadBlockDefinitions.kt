package com.vesodo.khaosat.cad

import com.vesodo.khaosat.model.Variables
import kotlin.math.roundToInt

/**
 * Định nghĩa cấu trúc hình học của các CAD Block đại diện cho 68+ biểu tượng
 * chuyên ngành Điện lực (Trụ BTLT, Trụ BTV, Trụ Gỗ, Trụ Nhôm, Tháp sắt, Trạm 1P/3P,
 * Công tơ, Nhà KH, Cầu, KDT...).
 *
 * Khi xuất tệp DXF, mỗi điểm đặt icon trên sơ đồ sẽ được chuyển đổi thành một
 * thực thể INSERT gọi Block tương ứng, thay vì chỉ là một điểm (POINT) đơn điệu.
 */
object CadBlockDefinitions {

    /**
     * Tên các Layer kỹ thuật chuẩn hóa
     */
    const val LAYER_TRAM = "DIEN_TRAM_BIEN_AP"      // Màu 1: Đỏ (Red)
    const val LAYER_TRU = "DIEN_TRU_HA_THE"         // Màu 4: Xanh Cyan
    const val LAYER_DAY_CHINH = "DIEN_DAY_DAN"      // Màu 3: Xanh Green
    const val LAYER_DAY_NHANH = "DIEN_DAY_NHANH"    // Màu 6: Tím Magenta (DASHED)
    const val LAYER_THIET_BI = "DIEN_THIET_BI"      // Màu 2: Vàng Yellow
    const val LAYER_GHI_CHU = "GHI_CHU_KICH_THUOC"  // Màu 7: Trắng/Đen (White)
    const val LAYER_DIA_VAT = "DIA_VAT_CONG_TRINH"  // Màu 30: Cam (Orange)
    const val LAYER_GIAO_THONG = "DIA_VAT_GIAO_THONG" // Màu 8: Xám (Gray)
    const val LAYER_KHUNG_TEN = "KHUNG_TEN_BAN_VE"   // Màu 7: Trắng/Đen (White) tiêu chuẩn TCVN A4
    const val LAYER_NEN_KHUNG_TEN = "NEN_KHUNG_TEN_XAM" // Màu 254: Nền xám nhẹ che đường phía dưới
    const val LAYER_NEN_QR = "NEN_MA_QR_TRANG"           // Màu 7 / 255: Nền trắng che nhẹ tên đường phía dưới
    const val LAYER_HINH_ANH_ICON = "HINH_ANH_ICON"     // Màu 7: Lớp hiển thị ảnh raster biểu tượng

    /**
     * Xác định tên CAD Block tương ứng với mã lệnh vẽ icon
     */
    fun getBlockNameForCommand(command: Int): String? {
        return when (command) {
            // Trụ BTLT Hiện hữu
            Variables.VE_TRU_BTLT_12_HHUU,
            Variables.VE_TRU_BTLT_14_HHUU,
            Variables.VE_TRU_BTLT_20_HHUU,
            Variables.VE_TRU_BTLT_75_HHUU,
            Variables.VE_TRU_BTLT_85_HHUU,
            Variables.VE_TRU_BTLT_105_HHUU -> "TRU_BTLT_HHUU"

            // Trụ BTLT Xây lắp mới
            Variables.VE_TRU_BTLT_12_SLAP,
            Variables.VE_TRU_BTLT_14_SLAP,
            Variables.VE_TRU_BTLT_20_SLAP,
            Variables.VE_TRU_BTLT_75_SLAP,
            Variables.VE_TRU_BTLT_85_SLAP,
            Variables.VE_TRU_BTLT_105_SLAP -> "TRU_BTLT_SLAP"

            // Trụ BTV Hiện hữu
            Variables.VE_TRU_BTV_11_HHUU,
            Variables.VE_TRU_BTV_65_HHUU,
            Variables.VE_TRU_BTV_75_HHUU,
            Variables.VE_TRU_BTV_85_HHUU -> "TRU_BTV_HHUU"

            // Trụ BTV Xây lắp mới
            Variables.VE_TRU_BTV_11_SLAP,
            Variables.VE_TRU_BTV_65_SLAP,
            Variables.VE_TRU_BTV_75_SLAP,
            Variables.VE_TRU_BTV_85_SLAP -> "TRU_BTV_SLAP"

            // Trụ Gỗ, Nhôm
            Variables.VE_TRU_GO_HHUU, Variables.VE_TRU_GO_SLAP -> "TRU_GO"
            Variables.VE_TRU_NHOM_HHUU, Variables.VE_TRU_NHOM_SLAP -> "TRU_NHOM"

            // Tháp sắt
            Variables.VE_THAPSAT_10_HHUU,
            Variables.VE_THAPSAT_10_SLAP,
            Variables.VE_THAPSAT_12_HHUU,
            Variables.VE_THAPSAT_12_SLAP -> "THAP_SAT"

            // Trạm biến áp
            Variables.VE_TRAM_1PHA_HHUU -> "TRAM_1PHA_HHUU"
            Variables.VE_TRAM_1PHA_SLAP -> "TRAM_1PHA_SLAP"
            Variables.VE_TRAM_3PHA_HHUU -> "TRAM_3PHA_HHUU"
            Variables.VE_TRAM_3PHA_SLAP -> "TRAM_3PHA_SLAP"
            Variables.VE_TRAM_3PHA_NEN_HHUU, Variables.VE_TRAM_3PHA_NEN_SLAP -> "TRAM_3PHA_NEN"
            Variables.VE_TRAM_3PHA_NHA_HHUU, Variables.VE_TRAM_3PHA_NHA_SLAP -> "TRAM_3PHA_NHA"

            // Công tơ
            Variables.VE_CONGTO_TREO_NHA -> "CONGTO_TREO_NHA"
            Variables.VE_CONGTO_TREO_TRU_BTLT_105_HHUU,
            Variables.VE_CONGTO_TREO_TRU_BTLT_105_SLAP,
            Variables.VE_CONGTO_TREO_TRU_BTLT_12_HHUU,
            Variables.VE_CONGTO_TREO_TRU_BTV_65_HHUU,
            Variables.VE_CONGTO_TREO_TRU_BTV_65_SLAP,
            Variables.VE_HINH_CONGTO -> "CONGTO_TREO_TRU"

            // Hộp / Cột
            Variables.VE_HOP, Variables.VE_HOP_NGANG -> "HOP_PHAN_PHOI"
            Variables.VE_COT, Variables.VE_COT_NGANG -> "COT_PHAN_PHOI"

            // Địa vật
            Variables.VE_NHA -> "NHA_KH"
            Variables.VE_CAU -> "CAU"
            Variables.VE_KDT -> "KDT"
            Variables.VE_SONG -> "SONG"

            else -> null
        }
    }

    /**
     * Kiểm tra xem mã lệnh có phải là dạng đường thẳng/mũi tên hay không
     */
    fun isLineCommand(command: Int): Boolean {
        return command == Variables.VE_DUONG_THANG ||
                command == Variables.VE_LINE_DUT_NET ||
                command == Variables.VE_2LINE ||
                command == Variables.VE_MUI_TEN ||
                command == Variables.VE_ARROW
    }

    /**
     * Xác định tên Layer cho từng đối tượng
     */
    fun getLayerForCommand(command: Int): String {
        return when {
            isTram(command) -> LAYER_TRAM
            isTru(command) -> LAYER_TRU
            isCongTo(command) || isHop(command) -> LAYER_THIET_BI
            isDiaVat(command) -> LAYER_DIA_VAT
            command == Variables.VE_LINE_DUT_NET -> LAYER_DAY_NHANH
            command == Variables.VE_TEXT -> LAYER_GHI_CHU
            else -> LAYER_DAY_CHINH
        }
    }

    fun isTram(command: Int): Boolean {
        return command == Variables.VE_TRAM_1PHA_HHUU ||
                command == Variables.VE_TRAM_1PHA_SLAP ||
                command == Variables.VE_TRAM_3PHA_HHUU ||
                command == Variables.VE_TRAM_3PHA_SLAP ||
                command == Variables.VE_TRAM_3PHA_NEN_HHUU ||
                command == Variables.VE_TRAM_3PHA_NEN_SLAP ||
                command == Variables.VE_TRAM_3PHA_NHA_HHUU ||
                command == Variables.VE_TRAM_3PHA_NHA_SLAP
    }

    fun isTru(command: Int): Boolean {
        return command in 401..421 || command in listOf(4, 7, 9, 19, 27) ||
                command in 901..904
    }

    fun isCongTo(command: Int): Boolean {
        return command in 701..706 || command == Variables.VE_HINH_CONGTO
    }

    fun isHop(command: Int): Boolean {
        return command in 103..106
    }

    fun isDiaVat(command: Int): Boolean {
        return command in listOf(
            Variables.VE_NHA,
            Variables.VE_CAU,
            Variables.VE_KDT,
            Variables.VE_SONG,
            Variables.VE_HINH_CHU_NHAT,
            Variables.VE_HINH_Z_NGANG,
            Variables.VE_HINH_Z_NGANG_DUT
        )
    }

    /**
     * Sinh toàn bộ mã DXF của phần SECTION BLOCKS chứa hình học của từng biểu tượng.
     * Toàn bộ thực thể con trong Block được đặt trên Layer 0 để thừa hưởng màu sắc
     * và thuộc tính của Layer INSERT từ bên ngoài.
     */
    fun buildAllBlocksDxf(): String {
        val sb = StringBuilder()

        // 1. TRU_BTLT_HHUU: Trụ tròn BTLT Hiện hữu (Vòng tròn đôi + tâm)
        writeBlock(sb, "TRU_BTLT_HHUU") {
            writeCircle(sb, 0f, 0f, 14f)
            writeCircle(sb, 0f, 0f, 6f)
            writeCircle(sb, 0f, 0f, 1.8f)
        }

        // 2. TRU_BTLT_SLAP: Trụ tròn BTLT Xây lắp mới (Vòng tròn đôi + 2 gạch chéo X)
        writeBlock(sb, "TRU_BTLT_SLAP") {
            writeCircle(sb, 0f, 0f, 14f)
            writeCircle(sb, 0f, 0f, 6f)
            writeLine(sb, -10f, -10f, 10f, 10f)
            writeLine(sb, -10f, 10f, 10f, -10f)
        }

        // 3. TRU_BTV_HHUU: Trụ vuông BTV Hiện hữu (Hình vuông đôi + tâm)
        writeBlock(sb, "TRU_BTV_HHUU") {
            writeRect(sb, -12f, -12f, 12f, 12f)
            writeRect(sb, -6f, -6f, 6f, 6f)
            writeCircle(sb, 0f, 0f, 1.8f)
        }

        // 4. TRU_BTV_SLAP: Trụ vuông BTV Xây lắp mới (Hình vuông + 2 gạch chéo X)
        writeBlock(sb, "TRU_BTV_SLAP") {
            writeRect(sb, -12f, -12f, 12f, 12f)
            writeLine(sb, -12f, -12f, 12f, 12f)
            writeLine(sb, -12f, 12f, 12f, -12f)
        }

        // 5. TRU_GO: Trụ gỗ (Hình tròn + chữ G)
        writeBlock(sb, "TRU_GO") {
            writeCircle(sb, 0f, 0f, 13f)
            writeText(sb, "G", 0f, -3.5f, 8f)
        }

        // 6. TRU_NHOM: Trụ nhôm (Hình tam giác đều)
        writeBlock(sb, "TRU_NHOM") {
            writeLine(sb, -12f, -9f, 12f, -9f)
            writeLine(sb, 12f, -9f, 0f, 13f)
            writeLine(sb, 0f, 13f, -12f, -9f)
            writeText(sb, "Al", 0f, -4f, 6.5f)
        }

        // 7. THAP_SAT: Tháp sắt (Khung giàn thép giằng chéo)
        writeBlock(sb, "THAP_SAT") {
            writeRect(sb, -14f, -14f, 14f, 14f)
            writeRect(sb, -7f, -7f, 7f, 7f)
            writeLine(sb, -14f, -14f, 14f, 14f)
            writeLine(sb, -14f, 14f, 14f, -14f)
        }

        // 8. TRAM_1PHA_HHUU: Trạm 1 pha hiện hữu (2 vòng tròn giao nhau IEC)
        writeBlock(sb, "TRAM_1PHA_HHUU") {
            writeCircle(sb, 0f, 6f, 10f)
            writeCircle(sb, 0f, -6f, 10f)
            writeText(sb, "1P", 0f, -3f, 7f)
        }

        // 9. TRAM_1PHA_SLAP: Trạm 1 pha xây lắp (2 vòng tròn + gạch chéo)
        writeBlock(sb, "TRAM_1PHA_SLAP") {
            writeCircle(sb, 0f, 6f, 10f)
            writeCircle(sb, 0f, -6f, 10f)
            writeLine(sb, -10f, -14f, 10f, 14f)
            writeText(sb, "1P", 0f, -3f, 7f)
        }

        // 10. TRAM_3PHA_HHUU: Trạm 3 pha hiện hữu (2 vòng tròn giao nhau lớn)
        writeBlock(sb, "TRAM_3PHA_HHUU") {
            writeCircle(sb, 0f, 7.5f, 12f)
            writeCircle(sb, 0f, -7.5f, 12f)
            writeText(sb, "3P", 0f, -3.5f, 8f)
        }

        // 11. TRAM_3PHA_SLAP: Trạm 3 pha xây lắp (2 vòng tròn + gạch chéo)
        writeBlock(sb, "TRAM_3PHA_SLAP") {
            writeCircle(sb, 0f, 7.5f, 12f)
            writeCircle(sb, 0f, -7.5f, 12f)
            writeLine(sb, -12f, -16f, 12f, 16f)
            writeText(sb, "3P", 0f, -3.5f, 8f)
        }

        // 12. TRAM_3PHA_NEN: Trạm biến áp đặt trên nền/bệ móng
        writeBlock(sb, "TRAM_3PHA_NEN") {
            writeRect(sb, -18f, -18f, 18f, -6f)
            writeCircle(sb, -7f, 6f, 9f)
            writeCircle(sb, 7f, 6f, 9f)
            writeText(sb, "NỀN", 0f, -14f, 6.5f)
        }

        // 13. TRAM_3PHA_NHA: Trạm phòng / trạm trong nhà
        writeBlock(sb, "TRAM_3PHA_NHA") {
            writeRect(sb, -18f, -16f, 18f, 16f)
            writeCircle(sb, 0f, 5f, 8f)
            writeCircle(sb, 0f, -5f, 8f)
            writeText(sb, "TRẠM", 0f, 10f, 5.5f)
        }

        // 14. CONGTO_TREO_TRU: Công tơ treo trụ
        writeBlock(sb, "CONGTO_TREO_TRU") {
            writeCircle(sb, 0f, 0f, 11f)
            writeRect(sb, -7f, -4f, 7f, 4f)
            writeText(sb, "kWh", 0f, -2.5f, 4.5f)
        }

        // 15. CONGTO_TREO_NHA: Công tơ gắn tại tường nhà
        writeBlock(sb, "CONGTO_TREO_NHA") {
            writeRect(sb, -12f, -8f, 12f, 8f)
            writeCircle(sb, 0f, 0f, 5f)
            writeText(sb, "CT", 0f, -2.5f, 5f)
        }

        // 16. HOP_PHAN_PHOI: Hộp chia dây / phân phối
        writeBlock(sb, "HOP_PHAN_PHOI") {
            writeRect(sb, -14f, -8f, 14f, 8f)
            writeLine(sb, 0f, -8f, 0f, 8f)
        }

        // 17. COT_PHAN_PHOI: Cột phân phối điện
        writeBlock(sb, "COT_PHAN_PHOI") {
            writeRect(sb, -7f, -14f, 7f, 14f)
            writeLine(sb, -7f, 0f, 7f, 0f)
        }

        // 18. NHA_KH: Nhà khách hàng cấp điện (Tường + Mái dốc)
        writeBlock(sb, "NHA_KH") {
            writeRect(sb, -14f, -14f, 14f, 2f)
            // Mái dốc tam giác
            writeLine(sb, -16f, 2f, 0f, 16f)
            writeLine(sb, 0f, 16f, 16f, 2f)
            writeLine(sb, 16f, 2f, -16f, 2f)
            // Cửa ra vào
            writeRect(sb, -4f, -14f, 4f, -5f)
        }

        // 19. CAU: Cầu qua sông/kênh
        writeBlock(sb, "CAU") {
            writeLine(sb, -16f, 7f, 16f, 7f)
            writeLine(sb, -16f, -7f, 16f, -7f)
            writeLine(sb, -10f, -7f, -10f, 7f)
            writeLine(sb, 0f, -7f, 0f, 7f)
            writeLine(sb, 10f, -7f, 10f, 7f)
        }

        // 20. KDT: Khu đất trống
        writeBlock(sb, "KDT") {
            writeRect(sb, -18f, -10f, 18f, 10f)
            writeText(sb, "KDT", 0f, -3.5f, 7.5f)
        }

        // 21. SONG: Sông ngòi
        writeBlock(sb, "SONG") {
            writeLine(sb, -16f, 4f, 16f, 4f)
            writeLine(sb, -16f, -4f, 16f, -4f)
            writeText(sb, "SÔNG", 0f, -2.5f, 6f)
        }

        return sb.toString()
    }

    private inline fun writeBlock(sb: StringBuilder, name: String, blockEntities: () -> Unit) {
        sb.append("  0\nBLOCK\n")
        sb.append("  8\n0\n")
        sb.append("  2\n$name\n")
        sb.append(" 70\n0\n")
        sb.append(" 10\n0.0\n 20\n0.0\n 30\n0.0\n")
        sb.append("  3\n$name\n")
        sb.append("  1\n\n")

        blockEntities()

        sb.append("  0\nENDBLK\n")
        sb.append("  8\n0\n")
    }

    fun writeCircle(sb: StringBuilder, cx: Float, cy: Float, radius: Float) {
        sb.append("  0\nCIRCLE\n")
        sb.append("  8\n0\n")
        sb.append(" 10\n${fmt(cx)}\n")
        sb.append(" 20\n${fmt(cy)}\n")
        sb.append(" 30\n0.0\n")
        sb.append(" 40\n${fmt(radius)}\n")
    }

    fun writeLine(sb: StringBuilder, x1: Float, y1: Float, x2: Float, y2: Float) {
        sb.append("  0\nLINE\n")
        sb.append("  8\n0\n")
        sb.append(" 10\n${fmt(x1)}\n")
        sb.append(" 20\n${fmt(y1)}\n")
        sb.append(" 30\n0.0\n")
        sb.append(" 11\n${fmt(x2)}\n")
        sb.append(" 21\n${fmt(y2)}\n")
        sb.append(" 31\n0.0\n")
    }

    fun writeRect(sb: StringBuilder, left: Float, bottom: Float, right: Float, top: Float) {
        writeLine(sb, left, bottom, right, bottom)
        writeLine(sb, right, bottom, right, top)
        writeLine(sb, right, top, left, top)
        writeLine(sb, left, top, left, bottom)
    }

    fun writeText(sb: StringBuilder, text: String, x: Float, y: Float, height: Float) {
        sb.append("  0\nTEXT\n")
        sb.append("  8\n0\n")
        sb.append(" 10\n${fmt(x)}\n")
        sb.append(" 20\n${fmt(y)}\n")
        sb.append(" 30\n0.0\n")
        sb.append(" 40\n${fmt(height)}\n")
        sb.append("  1\n${encodeDxfText(text)}\n")
        sb.append(" 72\n1\n") // Center horizontal
        sb.append(" 11\n${fmt(x)}\n")
        sb.append(" 21\n${fmt(y)}\n")
        sb.append(" 31\n0.0\n")
    }

    /**
     * Chuyển đổi chuỗi tiếng Việt Unicode có dấu thành định dạng chuẩn \U+XXXX của AutoCAD.
     * Cơ chế này đảm bảo 100% hiển thị tiếng Việt chính xác trên mọi phiên bản AutoCAD,
     * mọi hệ điều hành (Windows, Mac) mà không bao giờ bị lỗi font hay biến thành dấu hỏi (?) hay ký tự lạ.
     */
    fun encodeDxfText(text: String): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder(text.length * 2)
        for (ch in text) {
            val code = ch.code
            if (code in 32..126) {
                // Ký tự ASCII thông thường hiển thị trực tiếp
                sb.append(ch)
            } else if (ch == '\n' || ch == '\r' || ch == '\t') {
                sb.append(' ')
            } else {
                // Ký tự tiếng Việt Unicode (á, à, ả, ã, ạ, ư, ơ, đ...) chuyển thành \U+XXXX
                val hex = code.toString(16).uppercase().padStart(4, '0')
                sb.append("\\U+").append(hex)
            }
        }
        return sb.toString()
    }

    fun fmt(v: Float): String {
        val rounded = (v * 100f).roundToInt() / 100f
        return if (rounded == rounded.toInt().toFloat()) {
            "${rounded.toInt()}.0"
        } else {
            "$rounded"
        }
    }
}
