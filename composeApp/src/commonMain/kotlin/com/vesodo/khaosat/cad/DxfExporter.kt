package com.vesodo.khaosat.cad

import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.map.VectorBasemap
import com.vesodo.khaosat.model.MOBILE_KHAOSAT_KHANG
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import qrcode.QRCode
import kotlin.math.*

/**
 * Bộ sinh tệp AutoCAD ASCII DXF (tương thích 100% với AutoCAD 2000-2025+, ZWCAD, BricsCAD, DWG FastView).
 *
 * Các tính năng nổi bật:
 * 1. ĐỊNH DẠNG MẶC ĐỊNH KHỔ GIẤY A4 TIÊU CHUẨN TCVN:
 *    - Khổ A4 Landscape (297mm x 210mm) chuẩn tỷ lệ 1.4142.
 *    - Khung viền ngoài (Mép giấy A4) + dấu định vị tâm 4 cạnh (Centering Marks).
 *    - Khung bản vẽ trong (Drawing Border): Lề trái 20mm đóng gáy hồ sơ, các lề còn lại 10mm.
 *    - Khung tên A4 kỹ thuật tiêu chuẩn Điện lực EVN / TCVN góc dưới bên phải.
 *    - Khai báo Drawing Limits ($LIMMIN, $LIMMAX) để mở lên hoặc bấm Ctrl+P / Zoom All là vừa khít 100% A4.
 *
 * 2. TÍNH TOÁN NỐI ĐƯỜNG MỀM MẠI, KHÔNG ĐÈ LÊN NHAU:
 *    - Mép đường phân giác góc cua (Miter Bisector): Giúp khúc cua liên tục, không bị đứt rời hay đè chéo.
 *    - Cắt tỉa lòng đường giao lộ (Corridor Clipping): Triệt tiêu hoàn toàn đường kẻ đâm xuyên lòng đường khác.
 *    - Bo góc lượn mềm mại (Smooth Corner Fillets): Nối cong mượt mà giữa đường nhánh và đường chính tại ngã 3, ngã 4.
 *
 * 3. MÃ QR KHẢO SÁT VECTOR:
 *    - Chèn trực tiếp lên bản vẽ bằng các khối ô vuông SOLID chuẩn, quét được ngay trên màn hình CAD.
 *
 * 4. THƯ VIỆN CAD BLOCK CHUYÊN NGÀNH:
 *    - 21 CAD Block vector kỹ thuật chuẩn hóa thay thế hoàn toàn các điểm POINT đơn điệu.
 *
 * 5. FONT TIẾNG VIỆT UNICODE CHUẨN XÁC:
 *    - AC1015 + ANSI_1258 + Arial.ttf + Escape Unicode \U+XXXX.
 */
object DxfExporter {

    fun generateDxf(
        items: List<Obj_CANVAS>,
        width: Int,
        height: Int,
        fontSizeSp: Float,
        customer: MOBILE_KHAOSAT_KHANG? = null,
        vectorBasemap: VectorBasemap? = null,
        showQrCode: Boolean = true,
        qrDataString: String? = null,
        qrScalePercent: Int = 100,
        qrPosition: com.vesodo.khaosat.qr.QrPosition = com.vesodo.khaosat.qr.QrPosition.TOP_RIGHT,
        unitConfig: com.vesodo.khaosat.database.SavedSurveyUnitConfig? = null
    ): String {
        val canvasH = if (height > 0) height.toFloat() else 1080f
        val canvasW = if (width > 0) width.toFloat() else 1920f
        val sb = StringBuilder(256 * 1024)

        // ==============================================================
        // 1. TÍNH TOÁN BỐ CỤC KHỔ GIẤY A4 TIÊU CHUẨN (297mm x 210mm)
        // ==============================================================
        val usableA4WidthMm = 267f  // 297mm - 20mm (lề trái đóng gáy) - 10mm (lề phải)
        val usableA4HeightMm = 190f // 210mm - 10mm (lề trên) - 10mm (lề dưới)
        val a4Scale = maxOf(canvasW / usableA4WidthMm, canvasH / usableA4HeightMm) * 1.05f

        val a4PaperW = 297f * a4Scale
        val a4PaperH = 210f * a4Scale

        val borderLeft = 20f * a4Scale
        val borderRight = a4PaperW - 10f * a4Scale
        val borderBottom = 10f * a4Scale
        val borderTop = a4PaperH - 10f * a4Scale

        val innerW = borderRight - borderLeft
        val innerH = borderTop - borderBottom

        // Căn giữa nội dung Canvas vào bên trong khung bản vẽ A4
        val offsetX = borderLeft + (innerW - canvasW) / 2f
        val offsetY = borderBottom + (innerH - canvasH) / 2f

        // Tính toán trước vùng bảo vệ Khung tên A4 & Mã QR để cắt tỉa đường giao thông phía dưới
        val titleW = 140f * a4Scale
        val titleH = 38f * a4Scale
        val titleX = borderRight - titleW
        val titleY = borderBottom

        val titleExclusionBox = CadRoadNetwork.Rect2D(
            left = titleX - 2.5f * a4Scale,
            bottom = titleY - 2.5f * a4Scale,
            right = borderRight + 2.5f * a4Scale,
            top = titleY + titleH + 2.5f * a4Scale
        )

        var qrExclusionBox: CadRoadNetwork.Rect2D? = null
        if (showQrCode) {
            val scaleMult = (qrScalePercent.coerceIn(100, 150) / 100f)
            val qrBoxSize = 26f * a4Scale * scaleMult
            val margin = 6f * a4Scale
            val labelHeight = 4.5f * a4Scale

            val (boxX, boxY) = when (qrPosition) {
                com.vesodo.khaosat.qr.QrPosition.TOP_RIGHT -> Pair(borderRight - qrBoxSize - margin, borderTop - margin)
                com.vesodo.khaosat.qr.QrPosition.TOP_LEFT -> Pair(borderLeft + margin, borderTop - margin)
                com.vesodo.khaosat.qr.QrPosition.BOTTOM_LEFT -> Pair(borderLeft + margin, borderBottom + margin + qrBoxSize + labelHeight)
                com.vesodo.khaosat.qr.QrPosition.BOTTOM_RIGHT -> Pair(borderRight - qrBoxSize - margin, borderBottom + 38f * a4Scale + margin + qrBoxSize + labelHeight)
            }

            qrExclusionBox = CadRoadNetwork.Rect2D(
                left = boxX - 3.5f * a4Scale,
                bottom = boxY - qrBoxSize - 7.5f * a4Scale,
                right = boxX + qrBoxSize + 3.5f * a4Scale,
                top = boxY + 3.5f * a4Scale
            )
        }
        val roadExclusionBoxes = listOfNotNull(titleExclusionBox, qrExclusionBox)

        // ==============================================================
        // 2. SECTION HEADER
        // ==============================================================
        sb.append("  0\nSECTION\n  2\nHEADER\n")
        sb.append("  9\n\$ACADVER\n  1\nAC1015\n")       // AutoCAD 2000 DXF Unicode
        sb.append("  9\n\$DWGCODEPAGE\n  3\nANSI_1258\n") // Bảng mã tiếng Việt chuẩn Windows (CP1258)
        sb.append("  9\n\$INSUNITS\n 70\n6\n")           // Đơn vị hệ mét (Meters)
        sb.append("  9\n\$MEASUREMENT\n 70\n1\n")         // Hệ Mét (Metric)
        sb.append("  9\n\$HANDSEED\n  5\n5000\n")         // Handle kế tiếp cho AutoCAD

        // Thiết lập Drawing Extents & Drawing Limits theo đúng khổ A4
        sb.append("  9\n\$EXTMIN\n 10\n0.0\n 20\n0.0\n 30\n0.0\n")
        sb.append("  9\n\$EXTMAX\n 10\n${CadBlockDefinitions.fmt(a4PaperW)}\n 20\n${CadBlockDefinitions.fmt(a4PaperH)}\n 30\n0.0\n")
        sb.append("  9\n\$LIMMIN\n 10\n0.0\n 20\n0.0\n")
        sb.append("  9\n\$LIMMAX\n 10\n${CadBlockDefinitions.fmt(a4PaperW)}\n 20\n${CadBlockDefinitions.fmt(a4PaperH)}\n")
        sb.append("  0\nENDSEC\n")

        // ==============================================================
        // 2.5 SECTION CLASSES (Khai báo các lớp đối tượng raster AcDbRasterImage cho AutoCAD)
        // ==============================================================
        sb.append("  0\nSECTION\n  2\nCLASSES\n")
        sb.append("  0\nCLASS\n  1\nIMAGE\n  2\nAcDbRasterImage\n  3\nISM\n 90\n0\n280\n0\n281\n1\n")
        sb.append("  0\nCLASS\n  1\nIMAGEDEF\n  2\nAcDbRasterImageDef\n  3\nISM\n 90\n0\n280\n0\n281\n0\n")
        sb.append("  0\nCLASS\n  1\nIMAGEDEF_REACTOR\n  2\nAcDbRasterImageDefReactor\n  3\nISM\n 90\n0\n280\n0\n281\n0\n")
        sb.append("  0\nENDSEC\n")

        // ==============================================================
        // 3. SECTION TABLES (Linetypes, Layers, Styles)
        // ==============================================================
        sb.append("  0\nSECTION\n  2\nTABLES\n")

        // 3.1 Bảng kiểu đường (LTYPE)
        sb.append("  0\nTABLE\n  2\nLTYPE\n 70\n2\n")
        sb.append("  0\nLTYPE\n  2\nCONTINUOUS\n 70\n0\n  3\nSolid line\n 72\n65\n 73\n0\n 40\n0.0\n")
        sb.append("  0\nLTYPE\n  2\nDASHED\n 70\n0\n  3\nDashed __ __ __\n 72\n65\n 73\n2\n 40\n16.0\n 49\n10.0\n 49\n-6.0\n")
        sb.append("  0\nENDTAB\n")

        // 3.2 Bảng Layer (Lớp bản vẽ kỹ thuật)
        sb.append("  0\nTABLE\n  2\nLAYER\n 70\n13\n")
        writeLayer(sb, "0", 7, "CONTINUOUS")
        writeLayer(sb, CadBlockDefinitions.LAYER_KHUNG_TEN, 7, "CONTINUOUS")        // Khung tên tiêu chuẩn A4 (Trắng/Đen)
        writeLayer(sb, CadBlockDefinitions.LAYER_NEN_KHUNG_TEN, 254, "CONTINUOUS")  // Màu 254: Nền xám nhẹ che đường phía dưới
        writeLayer(sb, CadBlockDefinitions.LAYER_NEN_QR, 255, "CONTINUOUS")         // Màu 255: Nền trắng che nhẹ tên đường
        writeLayer(sb, CadBlockDefinitions.LAYER_TRAM, 1, "CONTINUOUS")              // Trạm biến áp (Đỏ)
        writeLayer(sb, CadBlockDefinitions.LAYER_TRU, 4, "CONTINUOUS")               // Trụ điện hạ thế (Cyan)
        writeLayer(sb, CadBlockDefinitions.LAYER_DAY_CHINH, 3, "CONTINUOUS")         // Dây chính (Xanh lá)
        writeLayer(sb, CadBlockDefinitions.LAYER_DAY_NHANH, 6, "DASHED")             // Dây nhánh (Tím Magenta)
        writeLayer(sb, CadBlockDefinitions.LAYER_THIET_BI, 2, "CONTINUOUS")           // Công tơ / Thiết bị (Vàng)
        writeLayer(sb, CadBlockDefinitions.LAYER_GHI_CHU, 7, "CONTINUOUS")           // Ghi chú / Cự ly (Trắng/Đen)
        writeLayer(sb, CadBlockDefinitions.LAYER_DIA_VAT, 30, "CONTINUOUS")           // Nhà / Địa vật (Cam)
        writeLayer(sb, CadBlockDefinitions.LAYER_GIAO_THONG, 8, "CONTINUOUS")        // Đường giao thông (Xám Gray)
        writeLayer(sb, CadBlockDefinitions.LAYER_HINH_ANH_ICON, 7, "CONTINUOUS")     // Biểu tượng icon raster
        sb.append("  0\nENDTAB\n")

        // 3.3 Bảng kiểu chữ (STYLE) - Đăng ký font TrueType Arial.ttf Unicode tiếng Việt
        sb.append("  0\nTABLE\n  2\nSTYLE\n 70\n1\n")
        sb.append("  0\nSTYLE\n  2\nSTANDARD\n 70\n0\n 40\n0.0\n 41\n1.0\n 50\n0.0\n 71\n0\n 42\n2.5\n  3\nArial.ttf\n  4\n\n")
        sb.append("  0\nENDTAB\n")

        sb.append("  0\nENDSEC\n")

        // ==============================================================
        // 4. SECTION BLOCKS (Thư viện Icon Chuyên ngành Điện lực)
        // ==============================================================
        sb.append("  0\nSECTION\n  2\nBLOCKS\n")
        sb.append(CadBlockDefinitions.buildAllBlocksDxf())
        sb.append("  0\nENDSEC\n")

        // ==============================================================
        // 5. SECTION ENTITIES (Dữ liệu hình học thực tế)
        // ==============================================================
        sb.append("  0\nSECTION\n  2\nENTITIES\n")

        // 5.1 MẠNG LƯỚI ĐƯỜNG GIAO THÔNG: Nối mềm mại, tự động cắt tỉa tránh vùng Khung tên & Mã QR và khống chế trong khung bản vẽ
        val drawingBoundaryBox = CadRoadNetwork.Rect2D(
            left = borderLeft,
            bottom = borderBottom,
            right = borderRight,
            top = borderTop
        )

        if (vectorBasemap != null && vectorBasemap.roads.isNotEmpty()) {
            CadRoadNetwork.renderRoadsDxf(
                sb = sb,
                vectorBasemap = vectorBasemap,
                canvasW = canvasW,
                canvasH = canvasH,
                offsetX = offsetX,
                offsetY = offsetY,
                exclusionBoxes = roadExclusionBoxes,
                boundaryBox = drawingBoundaryBox
            )
        }

        // 5.2 Khung viền bản vẽ A4 & Khung tên tiêu chuẩn TCVN / EVN (Nền xám nhẹ đè lên đường)
        writeA4StandardFrameAndTitleBlock(
            sb = sb,
            a4PaperW = a4PaperW,
            a4PaperH = a4PaperH,
            borderLeft = borderLeft,
            borderRight = borderRight,
            borderBottom = borderBottom,
            borderTop = borderTop,
            a4Scale = a4Scale,
            customer = customer,
            unitConfig = unitConfig
        )

        // 5.3 Chèn MÃ QR KHẢO SÁT thành hình vẽ vector trực tiếp lên bản vẽ A4 (Nền trắng che nhẹ tên đường)
        if (showQrCode) {
            val qrText = qrDataString ?: customer?.let {
                "{\"ma_tram\":\"${it.MA_TRAM}\",\"ten_khach_hang\":\"${it.TEN_KHANG_KS}\",\"ma_yc\":\"${it.MA_YCAU_KNAI}\"}"
            }
            if (!qrText.isNullOrBlank()) {
                writeQrCodeEntity(
                    sb = sb,
                    qrDataString = qrText,
                    borderLeft = borderLeft,
                    borderRight = borderRight,
                    borderBottom = borderBottom,
                    borderTop = borderTop,
                    a4Scale = a4Scale,
                    qrScalePercent = qrScalePercent,
                    qrPosition = qrPosition
                )
            }
        }

        // Thu thập các đối tượng có biểu tượng ảnh raster PNG
        val imageDefs = mutableMapOf<String, CadImageDef>()
        val imageInstances = mutableListOf<CadImageInst>()

        var nextDefId = 1
        var nextInstId = 1

        for (cv in items) {
            if (!CadBlockDefinitions.isLineCommand(cv.LENH) && cv.LENH != Variables.VE_TEXT) {
                val fileName = IconRegistry.getIconFileName(cv.LENH)
                val dims = IconRegistry.getIconDimensions(cv.LENH)
                if (fileName != null && dims != null) {
                    val imgDef = imageDefs.getOrPut(fileName) {
                        val handle = "100" + nextDefId.toString(16).uppercase()
                        nextDefId++
                        val keyName = fileName.substringBeforeLast(".")
                        CadImageDef(
                            fileName = fileName,
                            keyName = keyName,
                            pixelWidth = dims.first,
                            pixelHeight = dims.second,
                            imageDefHandle = handle
                        )
                    }

                    val imgHandle = "200" + nextInstId.toString(16).uppercase()
                    val reactorHandle = "300" + nextInstId.toString(16).uppercase()
                    nextInstId++

                    imgDef.reactorHandles.add(reactorHandle)

                    imageInstances.add(
                        CadImageInst(
                            cv = cv,
                            fileName = fileName,
                            pixelWidth = dims.first,
                            pixelHeight = dims.second,
                            imageHandle = imgHandle,
                            reactorHandle = reactorHandle,
                            imageDefHandle = imgDef.imageDefHandle
                        )
                    )
                }
            }
        }
        val instanceByCanvasItem = imageInstances.associateBy { it.cv }

        // 5.4 Vẽ các đường dây dẫn điện (Line, 2Line, Dashed, Arrow)
        for (cv in items) {
            if (CadBlockDefinitions.isLineCommand(cv.LENH)) {
                writeLineEntity(sb, cv, canvasH, offsetX, offsetY, fontSizeSp)
            }
        }

        // 5.5 Vẽ các đối tượng biểu tượng icon (Trụ, Trạm, Công tơ, Nhà KH, Địa vật)
        for (cv in items) {
            if (!CadBlockDefinitions.isLineCommand(cv.LENH)) {
                writeIconEntity(sb, cv, canvasH, offsetX, offsetY, fontSizeSp, instanceByCanvasItem[cv])
            }
        }

        sb.append("  0\nENDSEC\n")

        // ==============================================================
        // 6. SECTION OBJECTS (Từ điển ACAD_IMAGE_DICT & Định nghĩa IMAGEDEF cho ảnh raster)
        // ==============================================================
        if (imageDefs.isNotEmpty()) {
            sb.append("  0\nSECTION\n  2\nOBJECTS\n")

            // 6.1 Root Named Object Dictionary (Handle C cố định theo chuẩn AutoCAD)
            sb.append("  0\nDICTIONARY\n  5\nC\n330\n0\n100\nAcDbDictionary\n281\n1\n")
            sb.append("  3\nACAD_IMAGE_DICT\n350\n10D\n")

            // 6.2 Từ điển ACAD_IMAGE_DICT ánh xạ các khóa tên ảnh tới handle của IMAGEDEF
            sb.append("  0\nDICTIONARY\n  5\n10D\n330\nC\n100\nAcDbDictionary\n281\n1\n")
            for (def in imageDefs.values) {
                sb.append("  3\n${def.keyName}\n350\n${def.imageDefHandle}\n")
            }

            // 6.3 Các đối tượng IMAGEDEF chỉ định đường dẫn tương đối icons\<tên_ảnh>.png và kích thước
            for (def in imageDefs.values) {
                sb.append("  0\nIMAGEDEF\n")
                sb.append("  5\n${def.imageDefHandle}\n")
                sb.append("330\n10D\n")
                sb.append("102\n{ACAD_REACTORS\n")
                for (rHandle in def.reactorHandles) {
                    sb.append("330\n$rHandle\n")
                }
                sb.append("102\n}\n")
                sb.append("100\nAcDbRasterImageDef\n")
                sb.append(" 90\n0\n")
                sb.append("  1\nicons\\${def.fileName}\n")
                sb.append(" 10\n${CadBlockDefinitions.fmt(def.pixelWidth.toFloat())}\n 20\n${CadBlockDefinitions.fmt(def.pixelHeight.toFloat())}\n")
                sb.append(" 11\n1.0\n 21\n1.0\n")
                sb.append("280\n1\n")
                sb.append("281\n0\n")
            }

            // 6.4 Các đối tượng IMAGEDEF_REACTOR liên kết ngược giữa IMAGEDEF và thực thể IMAGE
            for (inst in imageInstances) {
                sb.append("  0\nIMAGEDEF_REACTOR\n")
                sb.append("  5\n${inst.reactorHandle}\n")
                sb.append("330\n${inst.imageDefHandle}\n")
                sb.append("100\nAcDbRasterImageDefReactor\n")
                sb.append(" 90\n2\n")
                sb.append("330\n${inst.imageHandle}\n")
            }

            sb.append("  0\nENDSEC\n")
        }

        // ==============================================================
        // 7. KẾT THÚC TỆP DXF
        // ==============================================================
        sb.append("  0\nEOF\n")

        return sb.toString()
    }

    private fun writeLayer(sb: StringBuilder, name: String, color: Int, linetype: String) {
        sb.append("  0\nLAYER\n")
        sb.append("  2\n$name\n")
        sb.append(" 70\n0\n")
        sb.append(" 62\n$color\n")
        sb.append("  6\n$linetype\n")
    }

    /**
     * Xuất khung bản vẽ tiêu chuẩn A4 (297mm x 210mm) theo quy phạm TCVN 3821:2008 & EVN.
     */
    private fun writeA4StandardFrameAndTitleBlock(
        sb: StringBuilder,
        a4PaperW: Float,
        a4PaperH: Float,
        borderLeft: Float,
        borderRight: Float,
        borderBottom: Float,
        borderTop: Float,
        a4Scale: Float,
        customer: MOBILE_KHAOSAT_KHANG?,
        unitConfig: com.vesodo.khaosat.database.SavedSurveyUnitConfig? = null
    ) {
        val layer = CadBlockDefinitions.LAYER_KHUNG_TEN

        // 1. Mép giấy ngoài A4 (Paper outline)
        writeCadRect(sb, 0f, 0f, a4PaperW, a4PaperH, layer)

        // 2. Dấu định vị tâm 4 cạnh giấy (Centering Marks - 5mm)
        val tick = 5f * a4Scale
        val midX = a4PaperW / 2f
        val midY = a4PaperH / 2f
        writeCadLine(sb, 0f, midY, tick, midY, layer)
        writeCadLine(sb, a4PaperW, midY, a4PaperW - tick, midY, layer)
        writeCadLine(sb, midX, 0f, midX, tick, layer)
        writeCadLine(sb, midX, a4PaperH, midX, a4PaperH - tick, layer)

        // 3. Khung viền đôi trang trí song song A4 (2 đường kẻ song song tuyệt đẹp)
        val decorGap = 2.0f * a4Scale // Khoảng cách 2mm song song giữa 2 viền
        val outerLeft = borderLeft - decorGap
        val outerBottom = borderBottom - decorGap
        val outerRight = borderRight + decorGap
        val outerTop = borderTop + decorGap

        // Đường 1: Viền ngoài trang trí (Outer Decorative Line)
        writeCadRect(sb, outerLeft, outerBottom, outerRight, outerTop, layer)

        // Đường 2: Viền trong bản vẽ chính (Inner Drawing Border - Nét đậm kỹ thuật)
        writeCadRect(sb, borderLeft, borderBottom, borderRight, borderTop, layer)

        // 4. Khung tên tiêu chuẩn A4 EVN (Title Block - góc dưới bên phải)
        val titleW = 140f * a4Scale // Rộng 140mm chuẩn TCVN
        val titleH = 38f * a4Scale  // Cao 38mm chuẩn TCVN
        val titleX = borderRight - titleW
        val titleY = borderBottom

        // MÀU NỀN XÁM NHẸ (AutoCAD Color Index 254: Light Gray) đè lên trên đường, chống trong suốt nhìn qua khó chịu
        writeCadSolidRect(
            sb = sb,
            left = titleX,
            bottom = titleY,
            right = borderRight,
            top = titleY + titleH,
            layer = CadBlockDefinitions.LAYER_NEN_KHUNG_TEN,
            color = 254
        )

        // Khung chữ nhật bao quanh khung tên
        writeCadRect(sb, titleX, titleY, borderRight, titleY + titleH, layer)

        // Các đường kẻ phân ô ngang trong khung tên
        val row1Y = titleY + titleH - 9f * a4Scale
        val row2Y = titleY + titleH - 17f * a4Scale
        val row3Y = titleY + titleH - 24f * a4Scale
        val row4Y = titleY + titleH - 31f * a4Scale
        writeCadLine(sb, titleX, row1Y, borderRight, row1Y, layer)
        writeCadLine(sb, titleX, row2Y, borderRight, row2Y, layer)
        writeCadLine(sb, titleX, row3Y, borderRight, row3Y, layer)
        writeCadLine(sb, titleX, row4Y, borderRight, row4Y, layer)

        // Đường kẻ phân cột dọc
        val colDivX = titleX + 85f * a4Scale
        writeCadLine(sb, colDivX, titleY, colDivX, row2Y, layer)

        // Nội dung thông tin trong khung tên từ cấu hình đơn vị & khách hàng
        val dviText = unitConfig?.tenDvi?.ifBlank { customer?.MA_DVIQLY }
            ?: customer?.MA_DVIQLY?.ifBlank { "TỔNG CÔNG TY ĐIỆN LỰC MIỀN NAM" }
            ?: "TỔNG CÔNG TY ĐIỆN LỰC MIỀN NAM"
        val tramText = customer?.MA_TRAM?.ifBlank { "---" } ?: "---"
        val khText = customer?.TEN_KHANG_KS?.ifBlank { "Khách hàng" } ?: "Khách hàng"
        val maYcText = customer?.MA_YCAU_KNAI?.ifBlank { "---" } ?: "---"
        val diaChiText = customer?.DIA_CHI?.ifBlank { "---" } ?: "---"
        val ktvText = unitConfig?.nguoiKhaoSat?.ifBlank { "KTV" } ?: "KTV"
        val ktraText = unitConfig?.nguoiKiemTra?.ifBlank { "Tổ trưởng" } ?: "Tổ trưởng"

        // Hàng 1: Đơn vị quản lý
        writeCadText(sb, dviText, titleX + 4f * a4Scale, titleY + titleH - 6.5f * a4Scale, 3.2f * a4Scale, layer)

        // Hàng 2: Tiêu đề bản vẽ (Chữ hoa đậm, nổi bật)
        writeCadText(sb, "SƠ ĐỒ KHẢO SÁT CẤP ĐIỆN HẠ THẾ", titleX + 4f * a4Scale, titleY + titleH - 14.5f * a4Scale, 4.2f * a4Scale, layer)

        // Hàng 3: Khách hàng & Mã yêu cầu
        writeCadText(sb, "Khách hàng: $khText", titleX + 4f * a4Scale, titleY + titleH - 21.5f * a4Scale, 2.7f * a4Scale, layer)
        writeCadText(sb, "Mã YC: $maYcText", colDivX + 3f * a4Scale, titleY + titleH - 21.5f * a4Scale, 2.7f * a4Scale, layer)

        // Hàng 4: Địa chỉ khảo sát
        writeCadText(sb, "Địa chỉ: $diaChiText", titleX + 4f * a4Scale, titleY + titleH - 28.5f * a4Scale, 2.7f * a4Scale, layer)
        writeCadText(sb, "Trạm: $tramText", colDivX + 3f * a4Scale, titleY + titleH - 28.5f * a4Scale, 2.7f * a4Scale, layer)

        // Hàng 5: Chữ ký kỹ thuật & Khổ giấy A4
        writeCadText(sb, "Khảo sát: $ktvText  |  Kiểm tra: $ktraText", titleX + 4f * a4Scale, titleY + 3.0f * a4Scale, 2.4f * a4Scale, layer)
        writeCadText(sb, "Khổ: A4 (297x210)  |  Sơ đồ nguyên lý", colDivX + 3f * a4Scale, titleY + 3.0f * a4Scale, 2.4f * a4Scale, layer)
    }

    /**
     * Chèn MÃ QR KHẢO SÁT thành hình vẽ vector trực tiếp lên bản vẽ CAD.
     * Nền trắng nhẹ che mờ nét đường giao thông phía dưới để mã QR quét tức thì.
     */
    private fun writeQrCodeEntity(
        sb: StringBuilder,
        qrDataString: String,
        borderLeft: Float,
        borderRight: Float,
        borderBottom: Float,
        borderTop: Float,
        a4Scale: Float,
        qrScalePercent: Int = 100,
        qrPosition: com.vesodo.khaosat.qr.QrPosition = com.vesodo.khaosat.qr.QrPosition.TOP_RIGHT
    ) {
        try {
            val qr = QRCode.ofSquares().build(qrDataString)
            val rawData = qr.rawData
            val rowCount = rawData.size
            if (rowCount == 0) return

            val scaleMult = (qrScalePercent.coerceIn(100, 150) / 100f)
            val qrBoxSize = 26f * a4Scale * scaleMult
            val margin = 6f * a4Scale
            val labelHeight = 4.5f * a4Scale

            // Định vị 1 trong 4 góc trên khung bản vẽ A4
            val (boxX, boxY) = when (qrPosition) {
                com.vesodo.khaosat.qr.QrPosition.TOP_RIGHT -> Pair(borderRight - qrBoxSize - margin, borderTop - margin)
                com.vesodo.khaosat.qr.QrPosition.TOP_LEFT -> Pair(borderLeft + margin, borderTop - margin)
                com.vesodo.khaosat.qr.QrPosition.BOTTOM_LEFT -> Pair(borderLeft + margin, borderBottom + margin + qrBoxSize + labelHeight)
                com.vesodo.khaosat.qr.QrPosition.BOTTOM_RIGHT -> {
                    // Góc dưới bên phải đã có khung tên chuẩn (cao 38mm), đặt mã QR ngay phía trên khung tên
                    Pair(borderRight - qrBoxSize - margin, borderBottom + 38f * a4Scale + margin + qrBoxSize + labelHeight)
                }
            }
            val layer = CadBlockDefinitions.LAYER_GHI_CHU

            // MÀU NỀN TRẮNG (TrueColor White #FFFFFF / ACI 255) che nhẹ tên đường hoặc nét vẽ phía dưới để QR nổi bật
            val bgPad = 2.0f * a4Scale
            writeCadSolidRect(
                sb = sb,
                left = boxX - bgPad,
                bottom = boxY - qrBoxSize - 6.2f * a4Scale,
                right = boxX + qrBoxSize + bgPad,
                top = boxY + bgPad,
                layer = CadBlockDefinitions.LAYER_NEN_QR,
                color = 255,
                trueColor = 16777215 // RGB: 255, 255, 255 (Trắng tuyệt đối trong AutoCAD)
            )

            // Khung bảo vệ viền ngoài mã QR
            writeCadRect(sb, boxX, boxY - qrBoxSize, boxX + qrBoxSize, boxY, layer)

            // Vẽ các ô vuông module SOLID (Màu Đen Tuyệt Đối TrueColor 0 / ACI 250)
            val padding = 1.6f * a4Scale * scaleMult
            val matrixSize = qrBoxSize - padding * 2
            val moduleSize = matrixSize / rowCount.toFloat()
            val startX = boxX + padding
            val startY = boxY - padding

            for (r in 0 until rowCount) {
                val row = rawData[r]
                for (c in 0 until row.size) {
                    val square = row[c]
                    if (square.dark) {
                        val x1 = startX + c * moduleSize
                        val x2 = x1 + moduleSize
                        val yTop = startY - r * moduleSize
                        val yBottom = yTop - moduleSize

                        sb.append("  0\nSOLID\n  8\n$layer\n")
                        sb.append(" 62\n250\n") // ACI 250: Đen
                        sb.append(" 420\n0\n")   // TrueColor: Đen tuyệt đối (0, 0, 0)
                        sb.append(" 10\n${CadBlockDefinitions.fmt(x1)}\n 20\n${CadBlockDefinitions.fmt(yBottom)}\n 30\n0.0\n")
                        sb.append(" 11\n${CadBlockDefinitions.fmt(x2)}\n 21\n${CadBlockDefinitions.fmt(yBottom)}\n 31\n0.0\n")
                        sb.append(" 12\n${CadBlockDefinitions.fmt(x1)}\n 22\n${CadBlockDefinitions.fmt(yTop)}\n 32\n0.0\n")
                        sb.append(" 13\n${CadBlockDefinitions.fmt(x2)}\n 23\n${CadBlockDefinitions.fmt(yTop)}\n 33\n0.0\n")
                    }
                }
            }

            // Nhãn chữ chú thích bên dưới mã QR (Chữ đen rõ nét trên nền trắng)
            val labelY = boxY - qrBoxSize - 3.2f * a4Scale
            sb.append("  0\nTEXT\n  8\n$layer\n")
            sb.append(" 62\n250\n")
            sb.append(" 420\n0\n")
            sb.append(" 10\n${CadBlockDefinitions.fmt(boxX + qrBoxSize / 2f)}\n 20\n${CadBlockDefinitions.fmt(labelY)}\n 30\n0.0\n")
            sb.append(" 40\n${CadBlockDefinitions.fmt(2.5f * a4Scale * scaleMult)}\n")
            sb.append("  1\n${CadBlockDefinitions.encodeDxfText("MÃ QR KHẢO SÁT")}\n")
            sb.append(" 72\n1\n")
            sb.append(" 11\n${CadBlockDefinitions.fmt(boxX + qrBoxSize / 2f)}\n 21\n${CadBlockDefinitions.fmt(labelY)}\n 31\n0.0\n")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun writeLineEntity(
        sb: StringBuilder,
        cv: Obj_CANVAS,
        canvasH: Float,
        offsetX: Float,
        offsetY: Float,
        fontSizeSp: Float
    ) {
        val x1 = cv.X1 + offsetX
        val y1 = (canvasH - cv.Y1) + offsetY
        val x3 = cv.X3 + offsetX
        val y3 = (canvasH - cv.Y3) + offsetY
        val layer = CadBlockDefinitions.getLayerForCommand(cv.LENH)

        when (cv.LENH) {
            Variables.VE_DUONG_THANG -> {
                writeCadLine(sb, x1, y1, x3, y3, layer)
                writeLineLabel(sb, cv.TEN, x1, y1, x3, y3, fontSizeSp)
            }

            Variables.VE_LINE_DUT_NET -> {
                sb.append("  0\nLINE\n  8\n$layer\n  6\nDASHED\n")
                sb.append(" 10\n${CadBlockDefinitions.fmt(x1)}\n 20\n${CadBlockDefinitions.fmt(y1)}\n 30\n0.0\n")
                sb.append(" 11\n${CadBlockDefinitions.fmt(x3)}\n 21\n${CadBlockDefinitions.fmt(y3)}\n 31\n0.0\n")
                writeLineLabel(sb, cv.TEN, x1, y1, x3, y3, fontSizeSp)
            }

            Variables.VE_2LINE -> {
                // Đường đôi (cách nhau 10 đơn vị)
                val dx = x3 - x1
                val dy = y3 - y1
                val len = sqrt(dx * dx + dy * dy)
                val offset = 8.5f
                val ox = if (len > 0f) -dy / len * offset else 0f
                val oy = if (len > 0f) dx / len * offset else 0f

                writeCadLine(sb, x1 + ox, y1 + oy, x3 + ox, y3 + oy, layer)
                writeCadLine(sb, x1 - ox, y1 - oy, x3 - ox, y3 - oy, layer)
                writeLineLabel(sb, cv.TEN, x1, y1, x3, y3, fontSizeSp)
            }

            Variables.VE_MUI_TEN, Variables.VE_ARROW -> {
                val arrowLayer = if (cv.THUOC_TINH == 1) CadBlockDefinitions.LAYER_DAY_NHANH else layer
                val linetype = if (cv.THUOC_TINH == 1) "DASHED" else null
                writeCadLine(sb, x1, y1, x3, y3, arrowLayer, linetype)
                val dx = x3 - x1
                val dy = y3 - y1
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0.1f) {
                    val arrowLen = 22f
                    val arrowWidth = 9f
                    val ux = dx / len
                    val uy = dy / len
                    val vx = -uy
                    val vy = ux

                    val baseX = x3 - ux * arrowLen
                    val baseY = y3 - uy * arrowLen
                    val w1x = baseX + vx * arrowWidth
                    val w1y = baseY + vy * arrowWidth
                    val w2x = baseX - vx * arrowWidth
                    val w2y = baseY - vy * arrowWidth

                    sb.append("  0\nSOLID\n  8\n$arrowLayer\n")
                    sb.append(" 10\n${CadBlockDefinitions.fmt(x3)}\n 20\n${CadBlockDefinitions.fmt(y3)}\n 30\n0.0\n")
                    sb.append(" 11\n${CadBlockDefinitions.fmt(w1x)}\n 21\n${CadBlockDefinitions.fmt(w1y)}\n 31\n0.0\n")
                    sb.append(" 12\n${CadBlockDefinitions.fmt(w2x)}\n 22\n${CadBlockDefinitions.fmt(w2y)}\n 32\n0.0\n")
                    sb.append(" 13\n${CadBlockDefinitions.fmt(w2x)}\n 23\n${CadBlockDefinitions.fmt(w2y)}\n 33\n0.0\n")
                }
                writeLineLabel(sb, cv.TEN, x1, y1, x3, y3, fontSizeSp)
            }
        }
    }

    private fun writeLineLabel(
        sb: StringBuilder,
        text: String,
        x1: Float,
        y1: Float,
        x3: Float,
        y3: Float,
        fontSizeSp: Float
    ) {
        if (text.isBlank()) return
        val midX = (x1 + x3) / 2f
        val midY = (y1 + y3) / 2f
        val dx = x3 - x1
        val dy = y3 - y1
        val len = sqrt(dx * dx + dy * dy)
        val nx = if (len > 0f) -dy / len * 14f else 0f
        val ny = if (len > 0f) dx / len * 14f else 14f

        val textHeight = maxOf(12f, fontSizeSp * 1.1f)
        sb.append("  0\nTEXT\n  8\n${CadBlockDefinitions.LAYER_GHI_CHU}\n")
        sb.append(" 10\n${CadBlockDefinitions.fmt(midX + nx)}\n 20\n${CadBlockDefinitions.fmt(midY + ny)}\n 30\n0.0\n")
        sb.append(" 40\n${CadBlockDefinitions.fmt(textHeight)}\n")
        sb.append("  1\n${CadBlockDefinitions.encodeDxfText(text)}\n")
        sb.append(" 72\n1\n") // Center horizontal
        sb.append(" 11\n${CadBlockDefinitions.fmt(midX + nx)}\n 21\n${CadBlockDefinitions.fmt(midY + ny)}\n 31\n0.0\n")
    }

    private fun writeIconEntity(
        sb: StringBuilder,
        cv: Obj_CANVAS,
        canvasH: Float,
        offsetX: Float,
        offsetY: Float,
        fontSizeSp: Float,
        imageInst: CadImageInst? = null
    ) {
        val insertX = cv.X3 + offsetX
        val insertY = (canvasH - cv.Y3) + offsetY
        val scale = if (cv.SCALE > 0f) cv.SCALE else 1f
        val rotCad = ((360f - cv.ROTATION) % 360f + 360f) % 360f

        if (cv.LENH == Variables.VE_TEXT) {
            val text = cv.TEN.ifBlank { "Ghi chú" }
            val textHeight = maxOf(13f, fontSizeSp * 1.3f) * scale
            sb.append("  0\nTEXT\n  8\n${CadBlockDefinitions.LAYER_GHI_CHU}\n")
            sb.append(" 10\n${CadBlockDefinitions.fmt(insertX)}\n 20\n${CadBlockDefinitions.fmt(insertY)}\n 30\n0.0\n")
            sb.append(" 40\n${CadBlockDefinitions.fmt(textHeight)}\n")
            sb.append("  1\n${CadBlockDefinitions.encodeDxfText(text)}\n")
            if (rotCad != 0f) {
                sb.append(" 50\n${CadBlockDefinitions.fmt(rotCad)}\n")
            }
            return
        }

        // 1. Chèn ảnh raster IMAGE biểu tượng đầy đủ màu sắc (Layer HINH_ANH_ICON)
        if (imageInst != null) {
            val pixelW = imageInst.pixelWidth.toFloat()
            val pixelH = imageInst.pixelHeight.toFloat()
            val rad = rotCad * (PI.toFloat() / 180f)
            val cosA = cos(rad)
            val sinA = sin(rad)

            // Vector U (chiều ngang ảnh) & Vector V (chiều dọc ảnh) theo tỉ lệ vẽ thực tế
            val ux = scale * cosA
            val uy = scale * sinA
            val vx = -scale * sinA
            val vy = scale * cosA

            // Tọa độ góc dưới bên trái của ảnh trong hệ tọa độ CAD
            val cornerX = insertX - 0.5f * (pixelW * ux + pixelH * vx)
            val cornerY = insertY - 0.5f * (pixelW * uy + pixelH * vy)

            sb.append("  0\nIMAGE\n")
            sb.append("  5\n${imageInst.imageHandle}\n")
            sb.append("100\nAcDbEntity\n")
            sb.append("  8\n${CadBlockDefinitions.LAYER_HINH_ANH_ICON}\n")
            sb.append("100\nAcDbRasterImage\n")
            sb.append(" 90\n0\n")
            sb.append(" 10\n${CadBlockDefinitions.fmt(cornerX)}\n 20\n${CadBlockDefinitions.fmt(cornerY)}\n 30\n0.0\n")
            sb.append(" 11\n${CadBlockDefinitions.fmt(ux)}\n 21\n${CadBlockDefinitions.fmt(uy)}\n 31\n0.0\n")
            sb.append(" 12\n${CadBlockDefinitions.fmt(vx)}\n 22\n${CadBlockDefinitions.fmt(vy)}\n 32\n0.0\n")
            sb.append(" 13\n${CadBlockDefinitions.fmt(pixelW)}\n 23\n${CadBlockDefinitions.fmt(pixelH)}\n")
            sb.append("340\n${imageInst.imageDefHandle}\n")
            sb.append(" 70\n7\n")  // 1: Hiển thị ảnh + 2: Hiển thị khi xoay + 4: Khung biên
            sb.append("280\n0\n")  // Không bật khung clipping che khuất
            sb.append("281\n50\n") // Độ sáng chuẩn (Brightness 50)
            sb.append("282\n50\n") // Độ tương phản chuẩn (Contrast 50)
            sb.append("283\n0\n")  // Độ mờ (Fade 0)
            sb.append("360\n${imageInst.reactorHandle}\n")
        }

        // 2. Chèn BLOCK vector chuẩn kỹ thuật (đảm bảo tương thích 100% với LibreOffice Draw, CAD cổ điển và in ấn kỹ thuật)
        val blockName = CadBlockDefinitions.getBlockNameForCommand(cv.LENH)
        val layer = CadBlockDefinitions.getLayerForCommand(cv.LENH)

        if (blockName != null) {
            sb.append("  0\nINSERT\n  8\n$layer\n")
            sb.append("  2\n$blockName\n")
            sb.append(" 10\n${CadBlockDefinitions.fmt(insertX)}\n 20\n${CadBlockDefinitions.fmt(insertY)}\n 30\n0.0\n")
            sb.append(" 41\n${CadBlockDefinitions.fmt(scale)}\n")
            sb.append(" 42\n${CadBlockDefinitions.fmt(scale)}\n")
            sb.append(" 43\n${CadBlockDefinitions.fmt(scale)}\n")
            if (rotCad != 0f) {
                sb.append(" 50\n${CadBlockDefinitions.fmt(rotCad)}\n")
            }
        } else {
            // Biểu tượng dự phòng: Vòng tròn
            sb.append("  0\nCIRCLE\n  8\n$layer\n")
            sb.append(" 10\n${CadBlockDefinitions.fmt(insertX)}\n 20\n${CadBlockDefinitions.fmt(insertY)}\n 30\n0.0\n")
            sb.append(" 40\n${CadBlockDefinitions.fmt(12f * scale)}\n")
        }

        // 3. Nhãn tên đối tượng (Số trụ, tên trạm, mã công tơ)
        if (cv.TEN.isNotBlank()) {
            val labelOffset = if (imageInst != null) {
                (maxOf(imageInst.pixelWidth, imageInst.pixelHeight) / 2f + 5f) * scale
            } else {
                16f * scale
            }
            val labelX = insertX + labelOffset
            val labelY = insertY + 10f * scale
            val textHeight = maxOf(12f, fontSizeSp * 1.05f) * scale
            sb.append("  0\nTEXT\n  8\n${CadBlockDefinitions.LAYER_GHI_CHU}\n")
            sb.append(" 10\n${CadBlockDefinitions.fmt(labelX)}\n 20\n${CadBlockDefinitions.fmt(labelY)}\n 30\n0.0\n")
            sb.append(" 40\n${CadBlockDefinitions.fmt(textHeight)}\n")
            sb.append("  1\n${CadBlockDefinitions.encodeDxfText(cv.TEN)}\n")
        }
    }

    private fun writeCadLine(sb: StringBuilder, x1: Float, y1: Float, x2: Float, y2: Float, layer: String, linetype: String? = null) {
        sb.append("  0\nLINE\n  8\n$layer\n")
        if (linetype != null) {
            sb.append("  6\n$linetype\n")
        }
        sb.append(" 10\n${CadBlockDefinitions.fmt(x1)}\n 20\n${CadBlockDefinitions.fmt(y1)}\n 30\n0.0\n")
        sb.append(" 11\n${CadBlockDefinitions.fmt(x2)}\n 21\n${CadBlockDefinitions.fmt(y2)}\n 31\n0.0\n")
    }

    private fun writeCadRect(sb: StringBuilder, left: Float, bottom: Float, right: Float, top: Float, layer: String) {
        writeCadLine(sb, left, bottom, right, bottom, layer)
        writeCadLine(sb, right, bottom, right, top, layer)
        writeCadLine(sb, right, top, left, top, layer)
        writeCadLine(sb, left, top, left, bottom, layer)
    }

    private fun writeCadText(sb: StringBuilder, text: String, x: Float, y: Float, height: Float, layer: String) {
        sb.append("  0\nTEXT\n  8\n$layer\n")
        sb.append(" 10\n${CadBlockDefinitions.fmt(x)}\n 20\n${CadBlockDefinitions.fmt(y)}\n 30\n0.0\n")
        sb.append(" 40\n${CadBlockDefinitions.fmt(height)}\n")
        sb.append("  1\n${CadBlockDefinitions.encodeDxfText(text)}\n")
    }

    private fun writeCadSolidRect(
        sb: StringBuilder,
        left: Float,
        bottom: Float,
        right: Float,
        top: Float,
        layer: String,
        color: Int = 0,
        trueColor: Int? = null
    ) {
        sb.append("  0\nSOLID\n  8\n$layer\n")
        if (color > 0) {
            sb.append(" 62\n$color\n")
        }
        if (trueColor != null) {
            sb.append(" 420\n$trueColor\n")
        }
        // Thứ tự đỉnh của SOLID tứ giác trong AutoCAD DXF: 1, 2, 4, 3
        sb.append(" 10\n${CadBlockDefinitions.fmt(left)}\n 20\n${CadBlockDefinitions.fmt(bottom)}\n 30\n0.0\n")
        sb.append(" 11\n${CadBlockDefinitions.fmt(right)}\n 21\n${CadBlockDefinitions.fmt(bottom)}\n 31\n0.0\n")
        sb.append(" 12\n${CadBlockDefinitions.fmt(left)}\n 22\n${CadBlockDefinitions.fmt(top)}\n 32\n0.0\n")
        sb.append(" 13\n${CadBlockDefinitions.fmt(right)}\n 23\n${CadBlockDefinitions.fmt(top)}\n 33\n0.0\n")
    }
}

private data class CadImageDef(
    val fileName: String,
    val keyName: String,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val imageDefHandle: String,
    val reactorHandles: MutableList<String> = mutableListOf()
)

private data class CadImageInst(
    val cv: Obj_CANVAS,
    val fileName: String,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val imageHandle: String,
    val reactorHandle: String,
    val imageDefHandle: String
)
