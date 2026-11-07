package com.vesodo.khaosat.ai

import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.math.Function_tinhtoan_ve
import com.vesodo.khaosat.math.Point2D
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PoleNameSuggestion(
    val name: String,
    val explanation: String
)

enum class IssueLevel {
    ERROR,
    WARNING,
    INFO
}

enum class IssueCategory {
    MISSING_SUBSTATION, // Thiếu trạm biến áp / nguồn cấp
    MISSING_POLE,       // Thiếu trụ điện
    MISSING_WIRE,       // Thiếu đường dây dẫn
    MISSING_METER,      // Thiếu công tơ hoặc hộ khách hàng
    SPAN_LENGTH,        // Cự ly nhịp dây vượt quá quy chuẩn
    DISCONNECTED_WIRE,  // Đầu dây lơ lửng không đấu nối
    ISOLATED_DEVICE,    // Trụ hoặc trạm không có dây nối
    CROSSING_WIRES,     // Giao cắt đường dây không nút nối
    MISSING_NOTE,       // Chưa gắn nhãn cự ly hoặc số hiệu
    INVALID_WIRE_TYPE   // Sai quy chuẩn loại dây (dùng mũi tên, hoặc chưa dùng nét đứt vào nhà KH)
}

data class DiagramIssue(
    val id: String,
    val level: IssueLevel,
    val category: IssueCategory,
    val title: String,
    val description: String,
    val targetItem: Obj_CANVAS? = null,
    val secondaryItem: Obj_CANVAS? = null,
    val locationX: Float = 0f,
    val locationY: Float = 0f,
    val quickFixAvailable: Boolean = false,
    val quickFixTitle: String? = null
)

data class QualityScore(
    val score: Int,           // 0 - 100
    val totalIssues: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val ratingText: String,
    val hasSubstation: Boolean = false,
    val hasPole: Boolean = false,
    val hasWire: Boolean = false,
    val hasMeterOrCustomer: Boolean = false
)

object DiagramRuleEngine {

    // Quy chuẩn EVN: Nhịp dây hạ thế thông thường 35m - 50m.
    // Tỉ lệ mặc định: 5 pixels ~ 1 mét (hoặc người dùng có thể tùy chỉnh)
    const val DEFAULT_PIXELS_PER_METER = 5.0
    const val MAX_SPAN_METERS = 50.0 // Ngưỡng cảnh báo nhịp dài
    const val SNAP_THRESHOLD_PX = 32.0 // Ngưỡng xét đấu nối tiếp xúc

    /**
     * Phân tích toàn bộ sơ đồ hiện tại và phát hiện các vi phạm quy chuẩn
     */
    fun analyzeDiagram(
        items: List<Obj_CANVAS>,
        pixelsPerMeter: Double = DEFAULT_PIXELS_PER_METER
    ): List<DiagramIssue> {
        val issues = mutableListOf<DiagramIssue>()
        if (items.isEmpty()) return issues

        val lineItems = items.filter { isLineCommand(it.LENH) }
        val deviceItems = items.filter { isDeviceCommand(it.LENH) }

        val hasSubstation = items.any { isSubstationItem(it) }
        val hasPole = items.any { isPoleItem(it) }
        val hasWire = lineItems.isNotEmpty()
        val hasMeterOrCustomer = items.any { isMeterOrCustomerItem(it) }

        // 0. Kiểm tra 4 thành phần bắt buộc của 1 bản vẽ khảo sát đạt điểm cao: Trạm, Trụ, Dây, Công tơ/Khách hàng
        if (!hasSubstation) {
            issues.add(
                DiagramIssue(
                    id = "MISSING_TRAM",
                    level = IssueLevel.ERROR,
                    category = IssueCategory.MISSING_SUBSTATION,
                    title = "Thiếu Trạm biến áp / Nguồn cấp điện",
                    description = "Bản vẽ đạt điểm cao bắt buộc phải có Trạm biến áp (1 pha, 3 pha hoặc Tháp sắt) làm điểm nguồn cấp điện."
                )
            )
        }
        if (!hasPole) {
            issues.add(
                DiagramIssue(
                    id = "MISSING_TRU",
                    level = IssueLevel.ERROR,
                    category = IssueCategory.MISSING_POLE,
                    title = "Thiếu Trụ điện / Cột điện",
                    description = "Bản vẽ khảo sát bắt buộc phải thể hiện hệ thống Trụ điện (BTLT, BTV, Trụ gỗ/nhôm...) hiện hữu hoặc xây lắp mới."
                )
            )
        }
        if (!hasWire) {
            issues.add(
                DiagramIssue(
                    id = "MISSING_DAY",
                    level = IssueLevel.ERROR,
                    category = IssueCategory.MISSING_WIRE,
                    title = "Thiếu Đường dây dẫn điện",
                    description = "Bản vẽ chưa có đường dây dẫn (Đường thẳng, Mũi tên, Đường đôi hoặc Nét đứt) kết nối giữa Trạm, Trụ và Khách hàng."
                )
            )
        }
        if (!hasMeterOrCustomer) {
            issues.add(
                DiagramIssue(
                    id = "MISSING_CONGTO_KH",
                    level = IssueLevel.ERROR,
                    category = IssueCategory.MISSING_METER,
                    title = "Thiếu Công tơ đo đếm hoặc Nhà khách hàng",
                    description = "Bản vẽ khảo sát cấp điện cần phải có Công tơ (CT treo trụ/nhà, Hộp công tơ) hoặc biểu tượng Nhà khách hàng để xác định điểm đấu nối."
                )
            )
        }

        // 1. Kiểm tra nhịp dây vượt quá quy chuẩn EVN
        for (line in lineItems) {
            val pxDist = Function_tinhtoan_ve.TINH_KC(line.X1, line.Y1, line.X3, line.Y3)
            val meters = pxDist / pixelsPerMeter

            // Đọc mét từ ghi chú nếu có (ví dụ "60m" hoặc "55 m")
            val noteMeters = extractMetersFromNote(line.TEN)
            val effectiveMeters = noteMeters ?: meters

            if (effectiveMeters > MAX_SPAN_METERS) {
                issues.add(
                    DiagramIssue(
                        id = "SPAN_${line.STT}_${line.X1.toInt()}",
                        level = IssueLevel.WARNING,
                        category = IssueCategory.SPAN_LENGTH,
                        title = "Nhịp dây dài quá quy chuẩn EVN (${effectiveMeters.roundToInt()}m > 50m)",
                        description = "Đoạn dây từ (${line.X1.toInt()}, ${line.Y1.toInt()}) đến (${line.X3.toInt()}, ${line.Y3.toInt()}) dài khoảng ${effectiveMeters.roundToInt()} mét. Theo quy chuẩn dây hạ thế không nên vượt quá 50m để tránh võng dây, gãy đổ.",
                        targetItem = line,
                        locationX = (line.X1 + line.X3) / 2f,
                        locationY = (line.Y1 + line.Y3) / 2f,
                        quickFixAvailable = true,
                        quickFixTitle = "Tự động chèn trụ trung gian chia đôi nhịp"
                    )
                )
            } else if (line.TEN.isBlank() && pxDist > 50.0) {
                // Nhịp có độ dài đáng kể nhưng chưa có ghi chú cự ly
                issues.add(
                    DiagramIssue(
                        id = "NOTE_${line.STT}",
                        level = IssueLevel.INFO,
                        category = IssueCategory.MISSING_NOTE,
                        title = "Chưa gắn ghi chú cự ly cho đoạn dây",
                        description = "Đoạn dây dài ~${meters.roundToInt()}m chưa có nhãn cự ly. Nên bổ sung cự ly nhịp để hồ sơ khảo sát đầy đủ.",
                        targetItem = line,
                        locationX = (line.X1 + line.X3) / 2f,
                        locationY = (line.Y1 + line.Y3) / 2f,
                        quickFixAvailable = true,
                        quickFixTitle = "Tự động điền '${meters.roundToInt()}m'"
                    )
                )
            }
        }

        // 2. Kiểm tra đầu dây lơ lửng (Floating wire endpoints)
        for (line in lineItems) {
            val startConnected = isPointConnected(line.X1, line.Y1, items, exclude = line)
            val endConnected = isPointConnected(line.X3, line.Y3, items, exclude = line)

            if (!startConnected) {
                issues.add(
                    DiagramIssue(
                        id = "FLOAT_START_${line.STT}",
                        level = IssueLevel.WARNING,
                        category = IssueCategory.DISCONNECTED_WIRE,
                        title = "Đầu dây lơ lửng chưa đấu nối",
                        description = "Điểm đầu của đường dây tại (${line.X1.toInt()}, ${line.Y1.toInt()}) chưa gắn vào Trụ, Trạm hoặc điểm nối nào.",
                        targetItem = line,
                        locationX = line.X1,
                        locationY = line.Y1
                    )
                )
            }
            if (!endConnected) {
                issues.add(
                    DiagramIssue(
                        id = "FLOAT_END_${line.STT}",
                        level = IssueLevel.WARNING,
                        category = IssueCategory.DISCONNECTED_WIRE,
                        title = "Điểm cuối dây chưa đấu nối",
                        description = "Điểm cuối của đường dây tại (${line.X3.toInt()}, ${line.Y3.toInt()}) chưa gắn vào Trụ, Công tơ hoặc Nhà khách hàng.",
                        targetItem = line,
                        locationX = line.X3,
                        locationY = line.Y3
                    )
                )
            }
        }

        // 3. Kiểm tra Trụ, Trạm, Công tơ hoặc Nhà KH bị cô lập (không có dây nối đến)
        for (dev in deviceItems) {
            val hasConnection = lineItems.any { line ->
                val distStart = Function_tinhtoan_ve.TINH_KC(dev.X3, dev.Y3, line.X1, line.Y1)
                val distEnd = Function_tinhtoan_ve.TINH_KC(dev.X3, dev.Y3, line.X3, line.Y3)
                distStart <= SNAP_THRESHOLD_PX || distEnd <= SNAP_THRESHOLD_PX
            } || (isMeterCommand(dev.LENH) && items.any { other ->
                // Cho phép công tơ gắn sát ngay trên trụ hoặc sát nhà (< 45px) mà trụ/nhà đó đã có dây nối
                other !== dev && (isPoleCommand(other.LENH) || isCustomerCommand(other.LENH)) &&
                        Function_tinhtoan_ve.TINH_KC(dev.X3, dev.Y3, other.X3, other.Y3) <= 45.0
            })

            if (!hasConnection) {
                val devType = when {
                    isSubstationCommand(dev.LENH) -> "Trạm biến áp"
                    isPoleCommand(dev.LENH) -> "Trụ điện"
                    isCustomerCommand(dev.LENH) -> "Nhà khách hàng"
                    else -> "Công tơ / Thiết bị"
                }
                issues.add(
                    DiagramIssue(
                        id = "ISOLATED_${dev.STT}",
                        level = IssueLevel.WARNING,
                        category = IssueCategory.ISOLATED_DEVICE,
                        title = "$devType chưa kết nối vào lưới điện",
                        description = "$devType tại (${dev.X3.toInt()}, ${dev.Y3.toInt()}) hiện đang đứng độc lập, chưa có đường dây nào đấu nối tới.",
                        targetItem = dev,
                        locationX = dev.X3,
                        locationY = dev.Y3
                    )
                )
            }
        }

        // 4. Kiểm tra giao cắt đường dây không nút nối (Crossing wires)
        for (i in 0 until lineItems.size) {
            for (j in i + 1 until lineItems.size) {
                val l1 = lineItems[i]
                val l2 = lineItems[j]
                val intersect = findIntersection(l1.X1, l1.Y1, l1.X3, l1.Y3, l2.X1, l2.Y1, l2.X3, l2.Y3)
                if (intersect != null) {
                    val isEndNode1 = isClose(intersect.x, intersect.y, l1.X1, l1.Y1) || isClose(intersect.x, intersect.y, l1.X3, l1.Y3)
                    val isEndNode2 = isClose(intersect.x, intersect.y, l2.X1, l2.Y1) || isClose(intersect.x, intersect.y, l2.X3, l2.Y3)
                    if (!isEndNode1 || !isEndNode2) {
                        issues.add(
                            DiagramIssue(
                                id = "CROSS_${l1.STT}_${l2.STT}",
                                level = IssueLevel.INFO,
                                category = IssueCategory.CROSSING_WIRES,
                                title = "Hai tuyến dây giao cắt nhau",
                                description = "Tuyến dây #${l1.STT} và #${l2.STT} cắt chéo nhau tại (${intersect.x.toInt()}, ${intersect.y.toInt()}). Cần kiểm tra tĩnh không an toàn.",
                                targetItem = l1,
                                secondaryItem = l2,
                                locationX = intersect.x,
                                locationY = intersect.y
                            )
                        )
                    }
                }
            }
        }

        // 5. AI Kiểm duyệt nhắc nhớ khách hàng trái/phải (Cảnh báo không ràng buộc)
        for (dev in deviceItems) {
            val isHouseOrMeter = (dev.LENH == Variables.VE_NHA || dev.LENH == Variables.VE_HINH_CONGTO || dev.LENH in 701..706)
            if (isHouseOrMeter) {
                // Tìm xem quanh điểm này (bán kính 250px) đã có đối tượng text khách hàng chưa
                val attachedCustomerTexts = items.filter { other ->
                    other.LENH == Variables.VE_TEXT &&
                    Function_tinhtoan_ve.TINH_KC(dev.X3, dev.Y3, other.X3, other.Y3) <= 250.0 &&
                    (other.TEN.contains("KH", ignoreCase = true) || other.TEN.contains("Mã:", ignoreCase = true) || other.TEN.contains("Tên:", ignoreCase = true))
                }

                if (attachedCustomerTexts.isEmpty()) {
                    val name = if (dev.LENH == Variables.VE_NHA) "Điểm nhà" else "Điểm công tơ"
                    issues.add(
                        DiagramIssue(
                            id = "SUGGEST_CUSTOMER_${dev.STT}",
                            level = IssueLevel.INFO, // Mức INFO nhắc nhở (Không phải ràng buộc)
                            category = IssueCategory.MISSING_NOTE,
                            title = "Chưa có thông tin khách hàng trái/phải (Khuyến nghị)",
                            description = "$name #${dev.STT} tại (${dev.X3.toInt()}, ${dev.Y3.toInt()}) chưa được thêm thông tin khách hàng bên trái hoặc bên phải. Bạn có thể bấm vào icon và dùng nút trôi nổi để bổ sung nếu cần (không bắt buộc ràng buộc).",
                            targetItem = dev,
                            locationX = dev.X3,
                            locationY = dev.Y3
                        )
                    )
                } else {
                    // Đã có text khách hàng nhưng kiểm tra xem có mã khách hàng chưa
                    val hasCustomerCode = attachedCustomerTexts.any { txt ->
                        val lines = txt.TEN.lines()
                        val codeLine = lines.firstOrNull { it.startsWith("Mã:", ignoreCase = true) }
                        codeLine != null && !codeLine.contains("(Chưa có)", ignoreCase = true) && codeLine.replace("Mã:", "").trim().isNotBlank()
                    }
                    if (!hasCustomerCode) {
                        issues.add(
                            DiagramIssue(
                                id = "MISSING_CODE_CUSTOMER_${dev.STT}",
                                level = IssueLevel.INFO, // Nhắc nhớ, KHÔNG RÀNG BUỘC
                                category = IssueCategory.MISSING_NOTE,
                                title = "Chưa có mã khách hàng trái/phải (Không bắt buộc)",
                                description = "Khách hàng gắn tại điểm #${dev.STT} hiện chưa có mã khách hàng. Đây là thông tin không bắt buộc ràng buộc, có thể bổ sung sau khi chốt hợp đồng mua bán điện.",
                                targetItem = dev,
                                locationX = dev.X3,
                                locationY = dev.Y3
                            )
                        )
                    }
                }
            }
        }

        // 6. Kiểm tra quy chuẩn loại đường dây điện lực:
        // - Dây nối giữa các trụ/trạm: Dùng dây nối liền không đứt nét (Variables.VE_DUONG_THANG)
        // - Dây nối từ trụ cuối đến nhà khách hàng: Dùng dây nét đứt (Variables.VE_LINE_DUT_NET)
        // - Tuyệt đối không dùng mũi tên làm dây nối
        for (line in lineItems) {
            val d1Dev = deviceItems.firstOrNull { Function_tinhtoan_ve.TINH_KC(line.X1, line.Y1, it.X3, it.Y3) <= SNAP_THRESHOLD_PX }
            val d3Dev = deviceItems.firstOrNull { Function_tinhtoan_ve.TINH_KC(line.X3, line.Y3, it.X3, it.Y3) <= SNAP_THRESHOLD_PX }

            val connectsToHouseOrMeter = (d1Dev != null && (isCustomerCommand(d1Dev.LENH) || d1Dev.LENH == Variables.VE_CONGTO_TREO_NHA)) ||
                                         (d3Dev != null && (isCustomerCommand(d3Dev.LENH) || d3Dev.LENH == Variables.VE_CONGTO_TREO_NHA))
            val connectsBetweenPoles = (d1Dev != null && (isPoleCommand(d1Dev.LENH) || isSubstationCommand(d1Dev.LENH))) &&
                                       (d3Dev != null && (isPoleCommand(d3Dev.LENH) || isSubstationCommand(d3Dev.LENH)))

            // a. Tuyệt đối không dùng mũi tên làm dây nối
            if (line.LENH == Variables.VE_MUI_TEN || line.LENH == Variables.VE_ARROW) {
                val targetName = if (connectsToHouseOrMeter) "dây nét đứt dẫn vào nhà KH" else "dây nối liền không đứt nét"
                issues.add(
                    DiagramIssue(
                        id = "WIRE_ARROW_${line.STT}",
                        level = IssueLevel.WARNING,
                        category = IssueCategory.INVALID_WIRE_TYPE,
                        title = "Không dùng mũi tên làm dây nối",
                        description = "Đoạn dây tại (${line.X1.toInt()}, ${line.Y1.toInt()}) đang dùng mũi tên làm dây dẫn. Quy chuẩn yêu cầu dùng $targetName.",
                        targetItem = line,
                        locationX = (line.X1 + line.X3) / 2f,
                        locationY = (line.Y1 + line.Y3) / 2f,
                        quickFixAvailable = true,
                        quickFixTitle = "Chuyển thành $targetName"
                    )
                )
            } else if (connectsToHouseOrMeter && line.LENH != Variables.VE_LINE_DUT_NET) {
                // b. Dây nối vào nhà KH phải là dây nét đứt
                issues.add(
                    DiagramIssue(
                        id = "WIRE_NOT_DASHED_${line.STT}",
                        level = IssueLevel.WARNING,
                        category = IssueCategory.INVALID_WIRE_TYPE,
                        title = "Dây cấp điện vào nhà KH phải dùng nét đứt",
                        description = "Đoạn dây nối từ trụ cuối đến nhà khách hàng/công tơ tại (${line.X3.toInt()}, ${line.Y3.toInt()}) phải dùng đường nét đứt theo quy chuẩn.",
                        targetItem = line,
                        locationX = (line.X1 + line.X3) / 2f,
                        locationY = (line.Y1 + line.Y3) / 2f,
                        quickFixAvailable = true,
                        quickFixTitle = "Chuyển thành dây nét đứt"
                    )
                )
            } else if (connectsBetweenPoles && line.LENH == Variables.VE_LINE_DUT_NET) {
                // c. Dây giữa các trụ phải là dây liền nét
                issues.add(
                    DiagramIssue(
                        id = "WIRE_DASHED_BETWEEN_POLES_${line.STT}",
                        level = IssueLevel.WARNING,
                        category = IssueCategory.INVALID_WIRE_TYPE,
                        title = "Dây nối giữa các trụ phải là dây liền nét",
                        description = "Đoạn dây nối giữa các trụ điện tại (${line.X1.toInt()}, ${line.Y1.toInt()}) phải dùng dây nối liền không đứt nét.",
                        targetItem = line,
                        locationX = (line.X1 + line.X3) / 2f,
                        locationY = (line.Y1 + line.Y3) / 2f,
                        quickFixAvailable = true,
                        quickFixTitle = "Chuyển thành dây liền nét"
                    )
                )
            }
        }

        return issues
    }

    /**
     * Tính điểm chất lượng sơ đồ (0 - 100):
     * - Một bảng vẽ chấm điểm cao bắt buộc phải có đủ 4 nhóm: Trạm (15đ), Trụ (15đ), Dây (15đ), Công tơ/Khách hàng (15đ) -> 60 điểm cấu trúc.
     * - 40 điểm còn lại đánh giá tính đúng đắn kỹ thuật (không đứt dây, không cô lập thiết bị, nhịp dây <= 50m, có nhãn cự ly).
     */
    fun calculateScore(issues: List<DiagramIssue>, totalItems: Int, items: List<Obj_CANVAS> = emptyList()): QualityScore {
        if (totalItems == 0) {
            return QualityScore(
                score = 0,
                totalIssues = 0,
                errorCount = 0,
                warningCount = 0,
                infoCount = 0,
                ratingText = "Bản vẽ trống - Cần có Trạm, Trụ, Dây và Công tơ/Khách hàng",
                hasSubstation = false,
                hasPole = false,
                hasWire = false,
                hasMeterOrCustomer = false
            )
        }

        val hasSubstation = if (items.isNotEmpty()) items.any { isSubstationItem(it) } else issues.none { it.category == IssueCategory.MISSING_SUBSTATION }
        val hasPole = if (items.isNotEmpty()) items.any { isPoleItem(it) } else issues.none { it.category == IssueCategory.MISSING_POLE }
        val hasWire = if (items.isNotEmpty()) items.any { isLineCommand(it.LENH) } else issues.none { it.category == IssueCategory.MISSING_WIRE }
        val hasMeterOrCustomer = if (items.isNotEmpty()) items.any { isMeterOrCustomerItem(it) } else issues.none { it.category == IssueCategory.MISSING_METER }

        // 1. Điểm thành phần bắt buộc (Tối đa 60 điểm: mỗi thành phần 15 điểm)
        var completenessScore = 0
        if (hasSubstation) completenessScore += 15
        if (hasPole) completenessScore += 15
        if (hasWire) completenessScore += 15
        if (hasMeterOrCustomer) completenessScore += 15

        // 2. Điểm kỹ thuật & đấu nối (Tối đa 40 điểm)
        val errors = issues.count { it.level == IssueLevel.ERROR }
        val warnings = issues.count { it.level == IssueLevel.WARNING }
        val infos = issues.count { it.level == IssueLevel.INFO }

        // Chỉ tính điểm thưởng kỹ thuật tối đa khi đã có dây và thiết bị
        val baseTechScore = if (hasWire && (hasPole || hasSubstation || hasMeterOrCustomer)) 40 else 15
        val techPenalty = (warnings * 8) + (infos * 2)
        val techScore = (baseTechScore - techPenalty).coerceAtLeast(0)

        var score = (completenessScore + techScore).coerceIn(0, 100)

        // Nếu thiếu bất kỳ thành phần cốt lõi nào trong 4 nhóm (Trạm, Trụ, Dây, Công tơ/KH), khống chế trần điểm không vượt quá 75
        val coreCount = listOf(hasSubstation, hasPole, hasWire, hasMeterOrCustomer).count { it }
        if (coreCount < 4 && score > 75) {
            score = 75
        }

        val rating = when {
            coreCount == 4 && score >= 90 -> "Xuất sắc - Đầy đủ Trạm, Trụ, Dây, Công tơ/KH & Chuẩn EVN"
            coreCount == 4 && score >= 75 -> "Tốt - Đủ thành phần, có vài cảnh báo nhỏ cần lưu ý"
            coreCount < 4 && score >= 55 -> "Khá - Cần bổ sung đủ 4 phần (Trạm, Trụ, Dây, Công tơ/KH) để đạt điểm tối đa"
            score >= 40 -> "Trung bình - Thiếu thành phần chính hoặc nhiều đầu nối chưa chuẩn"
            else -> "Chưa đạt - Vui lòng vẽ đủ Trạm, Trụ, Dây, Công tơ/Khách hàng"
        }

        return QualityScore(
            score = score,
            totalIssues = issues.size,
            errorCount = errors,
            warningCount = warnings,
            infoCount = infos,
            ratingText = rating,
            hasSubstation = hasSubstation,
            hasPole = hasPole,
            hasWire = hasWire,
            hasMeterOrCustomer = hasMeterOrCustomer
        )
    }

    private fun isPointConnected(x: Float, y: Float, items: List<Obj_CANVAS>, exclude: Obj_CANVAS): Boolean {
        for (item in items) {
            if (item == exclude) continue
            // Bỏ qua đối tượng văn bản ghi chú (VE_TEXT), không tính là điểm đấu nối vật lý
            if (item.LENH == Variables.VE_TEXT) continue

            // Xét khoảng cách đến điểm X3, Y3 (vị trí thiết bị hoặc điểm cuối dây)
            val d3 = Function_tinhtoan_ve.TINH_KC(x, y, item.X3, item.Y3)
            if (d3 <= SNAP_THRESHOLD_PX) return true

            // Nếu item là đường thẳng, xét thêm X1, Y1
            if (isLineCommand(item.LENH)) {
                val d1 = Function_tinhtoan_ve.TINH_KC(x, y, item.X1, item.Y1)
                if (d1 <= SNAP_THRESHOLD_PX) return true
            }
        }
        return false
    }

    fun isLineCommand(cmd: Int): Boolean {
        return cmd == Variables.VE_DUONG_THANG ||
                cmd == Variables.VE_ARROW ||
                cmd == Variables.VE_MUI_TEN ||
                cmd == Variables.VE_LINE_DUT_NET ||
                cmd == Variables.VE_2LINE ||
                cmd == Variables.VE_DUONG
    }

    private fun isDeviceCommand(cmd: Int): Boolean {
        return isSubstationCommand(cmd) || isPoleCommand(cmd) || isMeterCommand(cmd) || isCustomerCommand(cmd)
    }

    fun isSubstationCommand(cmd: Int): Boolean {
        return cmd in 801..807 || cmd in 901..904 || cmd == Variables.VE_TRAM_3PHA_HHUU
    }

    fun isPoleCommand(cmd: Int): Boolean {
        return (cmd in 401..421) || cmd == 4 || cmd == 7 || cmd == 9 || cmd == 19 || cmd == 27 ||
                cmd == Variables.VE_COT || cmd == Variables.VE_COT_NGANG
    }

    fun isMeterCommand(cmd: Int): Boolean {
        return (cmd in 701..706) || cmd == Variables.VE_HINH_CONGTO || cmd == Variables.VE_HOP || cmd == Variables.VE_HOP_NGANG
    }

    fun isCustomerCommand(cmd: Int): Boolean {
        return cmd == Variables.VE_NHA
    }

    fun isSubstationItem(item: Obj_CANVAS): Boolean {
        val text = item.TEN.trim()
        return isSubstationCommand(item.LENH) ||
                text.contains("trạm", ignoreCase = true) ||
                text.contains("tba", ignoreCase = true)
    }

    fun isPoleItem(item: Obj_CANVAS): Boolean {
        return isPoleCommand(item.LENH) || IconRegistry.isTru(item.LENH) || (item.LENH in 702..706) // Công tơ treo trụ cũng bao gồm cả trụ
    }

    private fun isMeterOrCustomerItem(item: Obj_CANVAS): Boolean {
        val text = item.TEN.trim()
        return isMeterCommand(item.LENH) ||
                isCustomerCommand(item.LENH) ||
                text.contains("công tơ", ignoreCase = true) ||
                text.contains("khách hàng", ignoreCase = true) ||
                text.contains("nhà kh", ignoreCase = true)
    }

    private fun extractMetersFromNote(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        val regex = Regex("""(\d+(\.\d+)?)\s*(m|mét|met)?""", RegexOption.IGNORE_CASE)
        val match = regex.find(text.trim()) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }

    private fun isClose(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        return abs(x1 - x2) < 5f && abs(y1 - y2) < 5f
    }

    private fun findIntersection(
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float
    ): Point2D? {
        val denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
        if (denom == 0f) return null

        val ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom
        val ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom

        if (ua in 0.05f..0.95f && ub in 0.05f..0.95f) {
            val ix = x1 + ua * (x2 - x1)
            val iy = y1 + ua * (y2 - y1)
            return Point2D(ix, iy)
        }
        return null
    }

    /**
     * Đề xuất tên trụ thông minh dựa trên các trụ liền kề kết nối qua đường dây hoặc topo lân cận
     */
    fun suggestAdjacentPoleNames(
        targetItem: Obj_CANVAS,
        allItems: List<Obj_CANVAS>
    ): List<PoleNameSuggestion> {
        val suggestions = mutableListOf<PoleNameSuggestion>()
        if (allItems.isEmpty()) {
            return listOf(
                PoleNameSuggestion("Trụ 1", "Khởi đầu tuyến"),
                PoleNameSuggestion("Trụ 1/1", "Nhánh rẽ khởi đầu")
            )
        }

        val lines = allItems.filter { isLineCommand(it.LENH) }
        val otherPoles = allItems.filter { it !== targetItem && isPoleItem(it) }

        fun dist(x1: Float, y1: Float, x2: Float, y2: Float) = Function_tinhtoan_ve.TINH_KC(x1, y1, x2, y2)
        val snapRadius = 45f

        // 1. Tìm các trụ liền kề kết nối trực tiếp với targetItem qua đường dây
        val connectedPoles = mutableListOf<Obj_CANVAS>()
        for (line in lines) {
            val d1ToTarget = dist(line.X1, line.Y1, targetItem.X3, targetItem.Y3)
            val d2ToTarget = dist(line.X3, line.Y3, targetItem.X3, targetItem.Y3)

            if (d1ToTarget <= snapRadius) {
                val neighbor = otherPoles.firstOrNull { dist(line.X3, line.Y3, it.X3, it.Y3) <= snapRadius }
                if (neighbor != null && neighbor !in connectedPoles) {
                    connectedPoles.add(neighbor)
                }
            } else if (d2ToTarget <= snapRadius) {
                val neighbor = otherPoles.firstOrNull { dist(line.X1, line.Y1, it.X3, it.Y3) <= snapRadius }
                if (neighbor != null && neighbor !in connectedPoles) {
                    connectedPoles.add(neighbor)
                }
            }
        }

        // 2. Nếu chưa có dây nối, tìm trụ gần nhất theo khoảng cách hình học (< 350px)
        if (connectedPoles.isEmpty()) {
            val nearest = otherPoles
                .map { it to dist(it.X3, it.Y3, targetItem.X3, targetItem.Y3) }
                .filter { it.second <= 350f }
                .minByOrNull { it.second }
                ?.first
            if (nearest != null) {
                connectedPoles.add(nearest)
            }
        }

        // 3. Phân tích tên của các trụ kết nối để sinh đề xuất
        for (pole in connectedPoles) {
            val rawName = pole.TEN.trim()
            if (rawName.isBlank()) continue

            // Trường hợp A: Đã là nhánh rẽ dạng "Trụ 1/1" hoặc "1/2" hoặc "T.1/3" hoặc "Cột 2-1"
            val branchMatch = Regex("""^(.*?)(\d+)[/.-](\d+)$""").find(rawName)
            if (branchMatch != null) {
                val prefix = branchMatch.groupValues[1]
                val mainNum = branchMatch.groupValues[2].toIntOrNull() ?: 1
                val branchNum = branchMatch.groupValues[3].toIntOrNull() ?: 1

                suggestions.add(
                    PoleNameSuggestion(
                        "${prefix}${mainNum}/${branchNum + 1}",
                        "Kế tiếp trên nhánh từ $rawName"
                    )
                )
                suggestions.add(
                    PoleNameSuggestion(
                        "${prefix}${mainNum}/${branchNum}/1",
                        "Rẽ nhánh con từ $rawName"
                    )
                )
                suggestions.add(
                    PoleNameSuggestion(
                        "${prefix}${mainNum + 1}",
                        "Tuyến chính tiếp theo"
                    )
                )
                continue
            }

            // Trường hợp B: Tuyến chính dạng "Trụ 1" hoặc "T1" hoặc "Cột 12" hoặc "1"
            val mainMatch = Regex("""^(.*?)(\d+)$""").find(rawName)
            if (mainMatch != null) {
                val prefix = mainMatch.groupValues[1]
                val num = mainMatch.groupValues[2].toIntOrNull() ?: 1

                suggestions.add(
                    PoleNameSuggestion(
                        "${prefix}${num + 1}",
                        "Kế tiếp từ $rawName"
                    )
                )
                suggestions.add(
                    PoleNameSuggestion(
                        "${prefix}${num}/1",
                        "Rẽ nhánh từ $rawName"
                    )
                )
                if (num > 1) {
                    suggestions.add(
                        PoleNameSuggestion(
                            "${prefix}${num - 1}",
                            "Phía trước $rawName"
                        )
                    )
                }
                continue
            }

            // Trường hợp C: Tên chữ thông thường (ví dụ: "Trụ ĐĐ", "Trụ Xuất Tuyến")
            suggestions.add(
                PoleNameSuggestion(
                    "$rawName-1",
                    "Phát triển từ $rawName"
                )
            )
            suggestions.add(
                PoleNameSuggestion(
                    "$rawName/1",
                    "Nhánh từ $rawName"
                )
            )
        }

        // 4. Nếu chưa có gợi ý nào từ trụ kết nối: kiểm tra xem có trạm biến áp kết nối không
        if (suggestions.isEmpty()) {
            val substations = allItems.filter { isSubstationItem(it) }
            val connectedSub = substations.firstOrNull { sub ->
                lines.any { line ->
                    (dist(line.X1, line.Y1, targetItem.X3, targetItem.Y3) <= snapRadius && dist(line.X3, line.Y3, sub.X3, sub.Y3) <= snapRadius) ||
                    (dist(line.X3, line.Y3, targetItem.X3, targetItem.Y3) <= snapRadius && dist(line.X1, line.Y1, sub.X3, sub.Y3) <= snapRadius)
                }
            }
            if (connectedSub != null) {
                suggestions.add(PoleNameSuggestion("Trụ 1", "Xuất tuyến từ trạm"))
                suggestions.add(PoleNameSuggestion("Trụ ĐĐ", "Trụ đầu đường / xuất tuyến"))
            }
        }

        // 5. Nếu vẫn chưa có: tìm số trụ lớn nhất trên toàn sơ đồ
        if (suggestions.isEmpty()) {
            val allNumbers = otherPoles.mapNotNull { p ->
                val match = Regex("""^(?:.*?)(\d+)(?:[/.-]\d+)?$""").find(p.TEN.trim())
                match?.groupValues?.get(1)?.toIntOrNull()
            }
            if (allNumbers.isNotEmpty()) {
                val maxNum = allNumbers.maxOrNull() ?: 0
                suggestions.add(PoleNameSuggestion("Trụ ${maxNum + 1}", "Thứ tự tiếp theo trong sơ đồ"))
                suggestions.add(PoleNameSuggestion("Trụ ${maxNum}/1", "Rẽ nhánh từ Trụ $maxNum"))
            } else {
                suggestions.add(PoleNameSuggestion("Trụ 1", "Khởi đầu tuyến"))
                suggestions.add(PoleNameSuggestion("Trụ 2", "Trụ kế tiếp"))
                suggestions.add(PoleNameSuggestion("Trụ 1/1", "Rẽ nhánh 1"))
            }
        }

        return suggestions.distinctBy { it.name }
    }
}
