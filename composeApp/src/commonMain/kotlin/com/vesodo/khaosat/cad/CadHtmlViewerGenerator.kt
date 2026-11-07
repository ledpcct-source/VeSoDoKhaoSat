package com.vesodo.khaosat.cad

import com.vesodo.khaosat.database.SavedSurveyUnitConfig
import com.vesodo.khaosat.drawing.DrawingRenderer
import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.map.GpsVectorRoadManager
import com.vesodo.khaosat.map.VectorBasemap
import com.vesodo.khaosat.model.MOBILE_KHAOSAT_KHANG
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import com.vesodo.khaosat.qr.QrPosition
import qrcode.QRCode
import kotlin.math.*

/**
 * Trình sinh tệp HTML / SVG Vector CAD khổ A4 chuẩn kỹ thuật EVN & TCVN.
 * Có thể mở trên bất kỳ trình duyệt web nào (Chrome, Edge, Firefox, Android Browser)
 * với chất lượng hiển thị vector sắc nét 100%, tỷ lệ chuẩn và tương tác mượt mà.
 */
object CadHtmlViewerGenerator {

    fun generateHtml(
        items: List<Obj_CANVAS>,
        width: Int,
        height: Int,
        fontSizeSp: Float,
        customer: MOBILE_KHAOSAT_KHANG?,
        vectorBasemap: VectorBasemap?,
        showQrCode: Boolean,
        qrDataString: String?,
        qrScalePercent: Int,
        qrPosition: QrPosition,
        unitConfig: SavedSurveyUnitConfig?
    ): String {
        val designW = if (width > 0) width.toFloat() else 1920f
        val designH = if (height > 0) height.toFloat() else 1080f

        // Kích thước khổ giấy A4 chuẩn: 297mm x 210mm
        val paperW = 297f
        val paperH = 210f

        // Khung bản vẽ trong: lề trái 20mm, phải 10mm, trên 10mm, dưới 10mm
        val borderLeft = 20f
        val borderRight = 287f
        val borderTop = 10f
        val borderBottom = 200f
        val innerW = borderRight - borderLeft
        val innerH = borderBottom - borderTop

        // Tỷ lệ chuyển đổi tọa độ thiết kế sang mm trong khung A4
        val scale = minOf(innerW / designW, innerH / designH) * 0.94f
        val offsetX = borderLeft + (innerW - designW * scale) / 2f
        val offsetY = borderTop + (innerH - designH * scale) / 2f

        fun mapX(x: Float): Float = offsetX + x * scale
        fun mapY(y: Float): Float = offsetY + y * scale

        // Khung tên A4: góc dưới bên phải (140mm x 38mm)
        val titleW = 140f
        val titleH = 38f
        val titleLeft = borderRight - titleW
        val titleTop = borderBottom - titleH

        // Mã QR
        val qrScaleMult = (qrScalePercent.coerceIn(100, 150) / 100f)
        val qrBoxSize = 24f * qrScaleMult
        val qrMargin = 5f
        val (qrLeft, qrTop) = when (qrPosition) {
            QrPosition.TOP_RIGHT -> Pair(borderRight - qrBoxSize - qrMargin, borderTop + qrMargin)
            QrPosition.TOP_LEFT -> Pair(borderLeft + qrMargin, borderTop + qrMargin)
            QrPosition.BOTTOM_LEFT -> Pair(borderLeft + qrMargin, borderBottom - qrBoxSize - qrMargin - 5f)
            QrPosition.BOTTOM_RIGHT -> Pair(borderRight - qrBoxSize - qrMargin, titleTop - qrBoxSize - qrMargin - 5f)
        }

        val sb = StringBuilder(64 * 1024)
        sb.append("""
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bản Vẽ CAD Khảo Sát - Khổ A4</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background-color: #0b0f19;
            color: #f1f5f9;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            height: 100vh;
        }
        .header {
            background: #1e293b;
            padding: 10px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 1px solid #334155;
            z-index: 10;
        }
        .header h1 { font-size: 15px; font-weight: 700; color: #38bdf8; }
        .header p { font-size: 11px; color: #94a3b8; }
        .controls { display: flex; gap: 8px; align-items: center; }
        button {
            background: #334155;
            color: #fff;
            border: none;
            padding: 6px 14px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            transition: 0.15s;
        }
        button:hover { background: #475569; }
        button.primary { background: #0284c7; }
        button.primary:hover { background: #0369a1; }
        .viewer-container {
            flex: 1;
            position: relative;
            background: #090d16;
            cursor: grab;
            overflow: hidden;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .viewer-container:active { cursor: grabbing; }
        svg {
            background: #12141a;
            box-shadow: 0 10px 40px rgba(0,0,0,0.8);
            border-radius: 2px;
        }
        @media print {
            .header { display: none; }
            body { background: #fff; }
            svg { box-shadow: none; }
        }
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1>SƠ ĐỒ KHẢO SÁT CẤP ĐIỆN - BẢN VẼ CAD A4</h1>
            <p>Đơn vị: ${unitConfig?.tenDvi?.ifBlank { customer?.MA_DVIQLY ?: "EVN" } ?: "EVN"} | KTV: ${unitConfig?.nguoiKhaoSat ?: "KTV"} | KH: ${customer?.TEN_KHANG_KS ?: "Khách hàng"}</p>
        </div>
        <div class="controls">
            <button onclick="toggleRoads()">Ẩn/Hiện đường</button>
            <button onclick="zoomIn()">Phóng to (+)</button>
            <button onclick="zoomOut()">Thu nhỏ (-)</button>
            <button onclick="resetZoom()">Vừa A4</button>
            <button class="primary" onclick="window.print()">In bản vẽ (A4)</button>
        </div>
    </div>
    <div class="viewer-container" id="container">
        <svg id="cadSvg" width="1188" height="840" viewBox="0 0 297 210" xmlns="http://www.w3.org/2000/svg">
            <defs>
                <pattern id="cadGrid" width="10" height="10" patternUnits="userSpaceOnUse">
                    <circle cx="5" cy="5" r="0.25" fill="#252c3c" />
                </pattern>
                <clipPath id="innerDrawingClip">
                    <rect x="20" y="10" width="267" height="190" />
                </clipPath>
            </defs>

            <!-- 1. Nền CAD và Lưới điểm -->
            <rect width="297" height="210" fill="#12141a" />
            <rect width="297" height="210" fill="url(#cadGrid)" />

            <!-- 2. Mép ngoài giấy A4 & Vạch định vị tâm -->
            <rect x="0" y="0" width="297" height="210" fill="none" stroke="#475569" stroke-width="0.3" />
            <line x1="0" y1="105" x2="5" y2="105" stroke="#94a3b8" stroke-width="0.3" />
            <line x1="292" y1="105" x2="297" y2="105" stroke="#94a3b8" stroke-width="0.3" />
            <line x1="148.5" y1="0" x2="148.5" y2="5" stroke="#94a3b8" stroke-width="0.3" />
            <line x1="148.5" y1="205" x2="148.5" y2="210" stroke="#94a3b8" stroke-width="0.3" />

            <!-- 3. Khung viền đôi trang trí song song A4 (2 đường kẻ song song tuyệt đẹp) -->
            <!-- Đường 1: Viền ngoài trang trí (cách viền chính 2mm) -->
            <rect x="18" y="8" width="271" height="194" fill="none" stroke="#64748b" stroke-width="0.35" />
            <!-- Đường 2: Viền trong bản vẽ chính (Khung kỹ thuật nét đậm) -->
            <rect x="20" y="10" width="267" height="190" fill="none" stroke="#cbd5e1" stroke-width="0.65" />
        """.trimIndent())

        // 4. Mạng lưới đường giao thông (Khống chế 100% trong khung định sẵn)
        if (vectorBasemap != null && vectorBasemap.roads.isNotEmpty()) {
            sb.append("\n            <!-- 4. Lớp Giao Thông Vector (Cắt gọn trong khung) -->\n            <g id=\"roadLayer\" clip-path=\"url(#innerDrawingClip)\">\n")
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
                    Pair(mapX(scr.x), mapY(scr.y))
                }
                if (pts.size >= 2) {
                    val isMajor = road.roadType in listOf("primary", "secondary", "trunk", "motorway")
                    val polyPoints = pts.joinToString(" ") { "${it.first},${it.second}" }
                    val roadWidth = if (isMajor) "3.2" else "2.2"
                    val roadColor = if (isMajor) "#475569" else "#334155"

                    sb.append("                <polyline points=\"$polyPoints\" fill=\"none\" stroke=\"$roadColor\" stroke-width=\"$roadWidth\" stroke-linecap=\"round\" stroke-linejoin=\"round\" />\n")
                    if (isMajor) {
                        sb.append("                <polyline points=\"$polyPoints\" fill=\"none\" stroke=\"#94a3b8\" stroke-width=\"0.3\" stroke-dasharray=\"2, 1.5\" />\n")
                    }
                }
            }
            sb.append("            </g>\n")
        }

        // 5. Đường dây dẫn điện
        sb.append("\n            <!-- 5. Lớp Đường Dây Điện -->\n            <g id=\"powerLines\">\n")
        for (item in items) {
            if (DrawingRenderer.isLineCommand(item.LENH)) {
                val x1 = mapX(item.X1)
                val y1 = mapY(item.Y1)
                val x2 = mapX(item.X3)
                val y2 = mapY(item.Y3)

                val (strokeColor, strokeWidth, dashArray) = when (item.LENH) {
                    Variables.VE_LINE_DUT_NET -> Triple("#e879f9", "0.45", "stroke-dasharray=\"2, 1.2\"") // Dây nhánh Tím
                    Variables.VE_2LINE -> Triple("#22c55e", "0.45", "") // 2 dây song song
                    else -> Triple("#22c55e", "0.6", "") // Dây chính Xanh lá
                }

                if (item.LENH == Variables.VE_2LINE) {
                    val dx = x2 - x1
                    val dy = y2 - y1
                    val len = hypot(dx, dy)
                    val off = 0.8f
                    val nx = if (len > 0f) -dy / len * off else 0f
                    val ny = if (len > 0f) dx / len * off else 0f
                    sb.append("                <line x1=\"${x1 + nx}\" y1=\"${y1 + ny}\" x2=\"${x2 + nx}\" y2=\"${y2 + ny}\" stroke=\"$strokeColor\" stroke-width=\"$strokeWidth\" />\n")
                    sb.append("                <line x1=\"${x1 - nx}\" y1=\"${y1 - ny}\" x2=\"${x2 - nx}\" y2=\"${y2 - ny}\" stroke=\"$strokeColor\" stroke-width=\"$strokeWidth\" />\n")
                } else {
                    sb.append("                <line x1=\"$x1\" y1=\"$y1\" x2=\"$x2\" y2=\"$y2\" stroke=\"$strokeColor\" stroke-width=\"$strokeWidth\" $dashArray />\n")
                }

                // Nhãn cự ly / tên dây
                if (item.TEN.isNotBlank()) {
                    val midX = (x1 + x2) / 2f
                    val midY = (y1 + y2) / 2f - 1.2f
                    sb.append("                <text x=\"$midX\" y=\"$midY\" fill=\"#ffffff\" font-size=\"2.0\" text-anchor=\"middle\" font-family=\"Arial\">${escapeXml(item.TEN)}</text>\n")
                }
            }
        }
        sb.append("            </g>\n")

        // 6. Đối tượng Icon / Trụ điện / Trạm / Công tơ (Kích thước nhỏ gọn đúng chuẩn CAD)
        sb.append("\n            <!-- 6. Lớp Thiết Bị & Biểu Tượng CAD (Icon kích thước chuẩn) -->\n            <g id=\"cadBlocks\">\n")
        for (item in items) {
            if (!DrawingRenderer.isLineCommand(item.LENH)) {
                val cx = mapX(item.X3)
                val cy = mapY(item.Y3)
                val isTru = IconRegistry.isTru(item.LENH)
                val isTram = IconRegistry.TRAM_TOOLS.any { it.command == item.LENH }
                val isCongTo = item.LENH in listOf(
                    Variables.VE_HINH_CONGTO,
                    Variables.VE_CONGTO_TREO_NHA,
                    Variables.VE_CONGTO_TREO_TRU_BTLT_105_HHUU,
                    Variables.VE_CONGTO_TREO_TRU_BTLT_105_SLAP,
                    Variables.VE_CONGTO_TREO_TRU_BTLT_12_HHUU,
                    Variables.VE_CONGTO_TREO_TRU_BTV_65_HHUU,
                    Variables.VE_CONGTO_TREO_TRU_BTV_65_SLAP
                )

                if (item.LENH == Variables.VE_TEXT) {
                    val txt = item.TEN.ifBlank { "Ghi chú" }
                    sb.append("                <text x=\"$cx\" y=\"$cy\" fill=\"#ffffff\" font-size=\"2.5\" font-weight=\"bold\" font-family=\"Arial\">${escapeXml(txt)}</text>\n")
                } else if (isTram) {
                    // Trạm biến áp: Vuông đỏ 5x5mm
                    sb.append("                <rect x=\"${cx - 2.5f}\" y=\"${cy - 2.5f}\" width=\"5\" height=\"5\" fill=\"#ef4444\" stroke=\"#ffffff\" stroke-width=\"0.3\" />\n")
                    sb.append("                <text x=\"$cx\" y=\"${cy + 0.8f}\" fill=\"#ffffff\" font-size=\"2.0\" font-weight=\"bold\" text-anchor=\"middle\">T</text>\n")
                } else if (isCongTo) {
                    // Công tơ: Tròn vàng r=1.8mm
                    sb.append("                <circle cx=\"$cx\" cy=\"$cy\" r=\"1.8\" fill=\"#facc15\" stroke=\"#000000\" stroke-width=\"0.3\" />\n")
                    sb.append("                <circle cx=\"$cx\" cy=\"$cy\" r=\"0.8\" fill=\"#000000\" />\n")
                } else if (isTru) {
                    // Trụ điện: Vòng tròn đôi Cyan r=1.8mm (chuẩn CAD)
                    sb.append("                <circle cx=\"$cx\" cy=\"$cy\" r=\"1.8\" fill=\"#12141a\" stroke=\"#22d3ee\" stroke-width=\"0.35\" />\n")
                    sb.append("                <circle cx=\"$cx\" cy=\"$cy\" r=\"0.8\" fill=\"#22d3ee\" />\n")
                } else {
                    // Địa vật / Nhà
                    sb.append("                <rect x=\"${cx - 2f}\" y=\"${cy - 2f}\" width=\"4\" height=\"4\" fill=\"#fb923c\" stroke=\"#ffffff\" stroke-width=\"0.2\" />\n")
                }

                // Tên đối tượng (Số trụ, tên KH)
                if (item.TEN.isNotBlank() && item.LENH != Variables.VE_TEXT) {
                    sb.append("                <text x=\"${cx + 2.8f}\" y=\"${cy + 0.7f}\" fill=\"#f8fafc\" font-size=\"2.0\" font-weight=\"bold\" font-family=\"Arial\">${escapeXml(item.TEN)}</text>\n")
                }
            }
        }
        sb.append("            </g>\n")

        // 7. Khung tên tiêu chuẩn A4 EVN (Góc dưới bên phải - Nền xám nhẹ chống xuyên)
        val dvi = unitConfig?.tenDvi?.ifBlank { customer?.MA_DVIQLY ?: "ĐIỆN LỰC" } ?: "ĐIỆN LỰC"
        val ktv = unitConfig?.nguoiKhaoSat?.ifBlank { "KTV Khảo Sát" } ?: "KTV Khảo Sát"
        val kiemTra = unitConfig?.nguoiKiemTra?.ifBlank { "Tổ Trưởng" } ?: "Tổ Trưởng"
        val tenKh = customer?.TEN_KHANG_KS ?: "Khách hàng"
        val dchi = customer?.DIA_CHI ?: ""
        val tram = customer?.MA_TRAM ?: ""

        sb.append("""
            <!-- 7. Khung Tên Tiêu Chuẩn A4 EVN (Nền xám nhẹ chống xuyên) -->
            <g id="titleBlock">
                <rect x="$titleLeft" y="$titleTop" width="$titleW" height="$titleH" fill="#e2e8f0" stroke="#0f172a" stroke-width="0.5" />
                <line x1="$titleLeft" y1="${titleTop + 9}" x2="$borderRight" y2="${titleTop + 9}" stroke="#0f172a" stroke-width="0.3" />
                <line x1="$titleLeft" y1="${titleTop + 18}" x2="$borderRight" y2="${titleTop + 18}" stroke="#0f172a" stroke-width="0.3" />
                <line x1="$titleLeft" y1="${titleTop + 27}" x2="$borderRight" y2="${titleTop + 27}" stroke="#0f172a" stroke-width="0.3" />
                <line x1="${titleLeft + 70}" y1="${titleTop + 18}" x2="${titleLeft + 70}" y2="$borderBottom" stroke="#0f172a" stroke-width="0.3" />

                <!-- Text Khung tên -->
                <text x="${titleLeft + 70}" y="${titleTop + 6}" fill="#0f172a" font-size="3.2" font-weight="bold" text-anchor="middle" font-family="Arial">${escapeXml(dvi.uppercase())}</text>
                <text x="${titleLeft + 70}" y="${titleTop + 14.5}" fill="#0369a1" font-size="3.0" font-weight="bold" text-anchor="middle" font-family="Arial">SƠ ĐỒ KHẢO SÁT CẤP ĐIỆN HẠ THẾ</text>

                <text x="${titleLeft + 3}" y="${titleTop + 22.5}" fill="#334155" font-size="2.2" font-family="Arial">KHÁCH HÀNG: <tspan font-weight="bold" fill="#0f172a">${escapeXml(tenKh)}</tspan></text>
                <text x="${titleLeft + 3}" y="${titleTop + 26}" fill="#475569" font-size="2.0" font-family="Arial">ĐỊA CHỈ: ${escapeXml(dchi)}</text>

                <text x="${titleLeft + 73}" y="${titleTop + 22.5}" fill="#334155" font-size="2.2" font-family="Arial">TRẠM BIẾN ÁP: <tspan font-weight="bold" fill="#0f172a">${escapeXml(tram)}</tspan></text>
                <text x="${titleLeft + 73}" y="${titleTop + 26}" fill="#475569" font-size="2.0" font-family="Arial">TỶ LỆ: 1/500 | KHỔ GIẤY: A4</text>

                <text x="${titleLeft + 3}" y="${titleTop + 33}" fill="#334155" font-size="2.2" font-family="Arial">KTV KHẢO SÁT: <tspan font-weight="bold" fill="#0f172a">${escapeXml(ktv)}</tspan></text>
                <text x="${titleLeft + 73}" y="${titleTop + 33}" fill="#334155" font-size="2.2" font-family="Arial">NGƯỜI KIỂM TRA: <tspan font-weight="bold" fill="#0f172a">${escapeXml(kiemTra)}</tspan></text>
            </g>
        """.trimIndent())

        // 8. Mã QR Khảo sát (Nền trắng che mờ tên đường)
        if (showQrCode) {
            val qrText = qrDataString ?: customer?.let {
                "{\"ma_tram\":\"${it.MA_TRAM}\",\"khach_hang\":\"${it.TEN_KHANG_KS}\",\"ma_yc\":\"${it.MA_YCAU_KNAI}\"}"
            }
            if (!qrText.isNullOrBlank()) {
                try {
                    val qrCode = QRCode.ofSquares().build(qrText)
                    val rawData = qrCode.rawData
                    val rowCount = rawData.size
                    val cellSize = qrBoxSize / rowCount

                    sb.append("""
            <!-- 8. Mã QR Khảo sát (Nền trắng che nhẹ tên đường) -->
            <g id="qrCodeBlock">
                <rect x="$qrLeft" y="$qrTop" width="$qrBoxSize" height="$qrBoxSize" fill="#ffffff" stroke="#94a3b8" stroke-width="0.3" rx="0.5" />
                    """.trimIndent())

                    for (r in 0 until rowCount) {
                        val row = rawData[r]
                        for (c in 0 until row.size) {
                            if (row[c].dark) {
                                val cellX = qrLeft + c * cellSize
                                val cellY = qrTop + r * cellSize
                                sb.append("\n                <rect x=\"$cellX\" y=\"$cellY\" width=\"$cellSize\" height=\"$cellSize\" fill=\"#000000\" />")
                            }
                        }
                    }

                    sb.append("""
                <text x="${qrLeft + qrBoxSize / 2f}" y="${qrTop + qrBoxSize + 3.2f}" fill="#ffffff" font-size="1.8" font-weight="bold" text-anchor="middle" font-family="Arial">MÃ QR KHẢO SÁT</text>
            </g>
                    """.trimIndent())
                } catch (e: Exception) {
                    // bỏ qua nếu lỗi tạo QR
                }
            }
        }

        // Đóng thẻ SVG và nhúng Script tương tác Pan & Zoom
        sb.append("""
        </svg>
    </div>

    <script>
        const container = document.getElementById('container');
        const svg = document.getElementById('cadSvg');
        let scale = 1.0;
        let pointX = 0;
        let pointY = 0;
        let start = { x: 0, y: 0 };
        let isPanning = false;

        function setTransform() {
            svg.style.transform = "translate(" + pointX + "px, " + pointY + "px) scale(" + scale + ")";
        }

        container.onmousedown = function (e) {
            e.preventDefault();
            start = { x: e.clientX - pointX, y: e.clientY - pointY };
            isPanning = true;
        };

        window.onmouseup = function () {
            isPanning = false;
        };

        window.onmousemove = function (e) {
            if (!isPanning) return;
            e.preventDefault();
            pointX = (e.clientX - start.x);
            pointY = (e.clientY - start.y);
            setTransform();
        };

        container.onwheel = function (e) {
            e.preventDefault();
            const xs = (e.clientX - pointX) / scale;
            const ys = (e.clientY - pointY) / scale;
            const delta = -e.deltaY;
            if (delta > 0) {
                scale = Math.min(scale * 1.2, 8.0);
            } else {
                scale = Math.max(scale / 1.2, 0.4);
            }
            pointX = e.clientX - xs * scale;
            pointY = e.clientY - ys * scale;
            setTransform();
        };

        function zoomIn() {
            scale = Math.min(scale * 1.25, 8.0);
            setTransform();
        }

        function zoomOut() {
            scale = Math.max(scale / 1.25, 0.4);
            setTransform();
        }

        function resetZoom() {
            scale = 1.0;
            pointX = 0;
            pointY = 0;
            setTransform();
        }

        let showRoads = true;
        function toggleRoads() {
            const roadLayer = document.getElementById('roadLayer');
            if (roadLayer) {
                showRoads = !showRoads;
                roadLayer.style.display = showRoads ? 'block' : 'none';
            }
        }
    </script>
</body>
</html>
        """.trimIndent())

        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
