package com.vesodo.khaosat.ai

import kotlin.math.roundToInt

enum class CustomerSide(val label: String, val directionText: String) {
    LEFT("Bên trái", "trái"),
    RIGHT("Bên phải", "phải")
}

data class CustomerAiInput(
    val parentItemTitle: String = "",
    val side: CustomerSide = CustomerSide.LEFT,
    val customerName: String = "Nguyễn Văn A",
    val customerCode: String = "",
    val distanceMeters: Double = 15.0,
    val sagFactor: Double = 1.2, // Độ võng mặc định là 1.2
    val powerKw: Double = 4.0,
    val crossesRoad: Boolean = false,
    val nearMetalRoof: Boolean = false,
    val isWallMounted: Boolean = false
)

data class CustomerAiResult(
    val geometricDistance: Double,
    val sagFactor: Double,
    val actualWireLength: Double,
    val recommendedCable: String,
    val meterHeightGuideline: String,
    val hookClearanceGuideline: String,
    val operationSafetyNotes: List<String>,
    val safetyRecommendations: List<String>,
    val summaryDisplayLabel: String
)

object LocalCustomerAiAdvisor {

    /**
     * Tính toán khoảng cách dây có độ võng mặc định là 1.2 và đưa ra phương án an toàn
     */
    fun evaluateCustomerAttachment(input: CustomerAiInput): CustomerAiResult {
        val dist = input.distanceMeters.coerceAtLeast(1.0)
        val sag = if (input.sagFactor > 0.5) input.sagFactor else 1.2
        val actualLength = ((dist * sag) * 10.0).roundToInt() / 10.0

        // 1. Đề xuất tiết diện cáp dựa trên công suất đăng ký và khoảng cách sụt áp
        val cable = when {
            input.powerKw <= 4.0 && dist <= 30.0 -> "Dây đồng bọc Cu/PVC 2x4 mm² (hoặc ABC 2x11)"
            input.powerKw <= 7.0 || dist <= 40.0 -> "Dây đồng bọc Cu/PVC 2x6 mm²"
            else -> "Dây đồng bọc Cu/PVC 2x10 mm² (chống sụt áp tải lớn)"
        }

        // 2. Nhắc nhớ khoảng thao tác an toàn tiêu chuẩn ngành điện EVN / QCVN
        val meterHeight = "1.5m - 1.7m tính từ nền/sàn nhà đến tâm mặt kính công tơ (thuận tiện thao tác và ghi chỉ số)"
        val hookClearance = "Điểm neo móc dây vào tường/nhà ≥ 2.5m (tránh cản trở lối đi và phương tiện ra vào)"

        val operationNotes = mutableListOf<String>()
        operationNotes.add("Độ cao treo công tơ: $meterHeight.")
        operationNotes.add("Khoảng cách điểm móc néo: $hookClearance.")
        operationNotes.add("Khoảng cách cách điện an toàn khi đấu nối tại trụ/hộp chia: ≥ 0.7m so với các pha hạ thế khác; tuyệt đối không đứng dưới hành lang lưới trung áp.")

        // 3. Đề xuất phương án an toàn theo hiện trường khảo sát
        val safetyRecommendations = mutableListOf<String>()

        if (input.crossesRoad) {
            safetyRecommendations.add(
                "⚠️ Dây băng qua đường: Khoảng tĩnh không tối thiểu phải đạt ≥ 5.5m (đường xe cơ giới) hoặc ≥ 4.5m (đường thôn ngõ). Đề xuất đặt trụ phụ hoặc lắp cáp vặn xoắn gia cường dây treo chịu lực."
            )
        }

        if (dist > 30.0) {
            safetyRecommendations.add(
                "⚠️ Nhịp dây vượt quá 30m (thực tế kéo ${actualLength}m): Độ võng lớn có nguy cơ chùng chạm cây cối khi giông gió. Đề xuất trồng thêm 01 trụ phụ bê tông hoặc móc néo trung gian."
            )
        }

        if (input.nearMetalRoof) {
            safetyRecommendations.add(
                "💡 Đoạn dây đi gần mái tole/tường: Phải luồn ống nhựa xoắn HDPE hoặc gen cách điện chống cọ xát rò điện; đề xuất lắp Aptomat chống dòng rò (ELCB/RCCB) 30mA tại đầu vào nhà khách hàng."
            )
        }

        safetyRecommendations.add(
            "🔒 Đảm bảo ép đầu Cosse đúng lực, bấm chì niêm phong nắp hộp đấu dây và thực hiện nối đất an toàn vỏ kim loại (nếu có)."
        )

        val summaryLabel = "L = ${actualLength}m (võng ${sag})"

        return CustomerAiResult(
            geometricDistance = dist,
            sagFactor = sag,
            actualWireLength = actualLength,
            recommendedCable = cable,
            meterHeightGuideline = meterHeight,
            hookClearanceGuideline = hookClearance,
            operationSafetyNotes = operationNotes,
            safetyRecommendations = safetyRecommendations,
            summaryDisplayLabel = summaryLabel
        )
    }
}
