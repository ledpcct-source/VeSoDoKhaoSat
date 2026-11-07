package com.vesodo.khaosat.ai

import com.vesodo.khaosat.math.Function_tinhtoan_ve
import com.vesodo.khaosat.math.Point2D
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import kotlin.math.max
import kotlin.math.roundToInt

data class AutoRouteResult(
    val newPoles: List<Obj_CANVAS>,
    val newWires: List<Obj_CANVAS>,
    val totalDistanceMeters: Double,
    val spanCount: Int,
    val spanLengthMeters: Double
)

object SmartRoutingEngine {

    /**
     * Tự động rải cột theo cự ly nhịp chuẩn EVN
     * Cho điểm bắt đầu và điểm kết thúc:
     * - Tự động tính khoảng cách và số nhịp tối ưu theo cự ly chỉ định (ví dụ 35m / nhịp)
     * - Tự động nội suy các vị trí cột trung gian đều nhau
     * - Tự động tạo các đoạn dây dẫn nối liên tiếp kèm nhãn cự ly (ví dụ "35m")
     */
    fun generateAutoPolesRoute(
        startPt: Point2D,
        endPt: Point2D,
        poleCommand: Int = Variables.VE_TRU_BTLT_105_HHUU,
        wireCommand: Int = Variables.VE_DUONG_THANG,
        targetSpanMeters: Double = 35.0,
        pixelsPerMeter: Double = DiagramRuleEngine.DEFAULT_PIXELS_PER_METER,
        startStt: Int = 1,
        poleNamePrefix: String = "Trụ",
        poleScale: Float = 1.0f,
        isEndHouseOrMeter: Boolean = false,
        isStartHouseOrMeter: Boolean = false
    ): AutoRouteResult {
        val totalPx = Function_tinhtoan_ve.TINH_KC(startPt.x, startPt.y, endPt.x, endPt.y)
        val totalMeters = totalPx / pixelsPerMeter

        // Tính số nhịp (ít nhất là 1 nhịp)
        val spanCount = max(1, (totalMeters / targetSpanMeters).roundToInt())
        val actualSpanMeters = totalMeters / spanCount

        val newPoles = mutableListOf<Obj_CANVAS>()
        val newWires = mutableListOf<Obj_CANVAS>()

        val routePoints = mutableListOf<Point2D>()
        routePoints.add(startPt)

        var curStt = startStt

        // Sinh các cột trung gian
        for (i in 1 until spanCount) {
            val ratio = i.toDouble() / spanCount
            val x = (startPt.x + ratio * (endPt.x - startPt.x)).toFloat()
            val y = (startPt.y + ratio * (endPt.y - startPt.y)).toFloat()
            val polePt = Point2D(x, y)
            routePoints.add(polePt)

            newPoles.add(
                Obj_CANVAS(
                    STT = (curStt++).toLong(),
                    LENH = poleCommand,
                    X1 = 0f,
                    Y1 = 0f,
                    X3 = x,
                    Y3 = y,
                    TEN = "$poleNamePrefix $i",
                    SCALE = poleScale,
                    CHON = 0
                )
            )
        }
        routePoints.add(endPt)

        // Sinh các đoạn dây dẫn nối giữa các điểm:
        // Quy chuẩn kỹ thuật:
        // - Dây nối giữa các trụ/trạm: Dây liền không đứt nét (Variables.VE_DUONG_THANG).
        // - Chỉ có đoạn dây nối từ trụ cuối đến nhà khách hàng mới là dây nét đứt (Variables.VE_LINE_DUT_NET).
        // - Tuyệt đối không dùng mũi tên (VE_MUI_TEN/VE_ARROW) làm dây dẫn nối.
        for (i in 0 until routePoints.size - 1) {
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]

            // Tính khoảng cách dây thực tế có tính thêm hệ số độ võng 1.2
            val wireLengthMeters = (actualSpanMeters * 1.2).roundToInt()

            val isLastSpanToHouse = isEndHouseOrMeter && (i == routePoints.size - 2)
            val isFirstSpanFromHouse = isStartHouseOrMeter && (i == 0)

            val effectiveWireCmd = when {
                isLastSpanToHouse || isFirstSpanFromHouse -> Variables.VE_LINE_DUT_NET
                wireCommand == Variables.VE_MUI_TEN || wireCommand == Variables.VE_ARROW -> Variables.VE_DUONG_THANG
                else -> wireCommand
            }

            newWires.add(
                Obj_CANVAS(
                    STT = (curStt++).toLong(),
                    LENH = effectiveWireCmd,
                    X1 = p1.x,
                    Y1 = p1.y,
                    X3 = p2.x,
                    Y3 = p2.y,
                    TEN = "${wireLengthMeters}m",
                    CHON = 0
                )
            )
        }

        return AutoRouteResult(
            newPoles = newPoles,
            newWires = newWires,
            totalDistanceMeters = totalMeters,
            spanCount = spanCount,
            spanLengthMeters = actualSpanMeters
        )
    }

    /**
     * Tự động tách 1 đoạn dây thành 2 đoạn khi chèn thêm 1 trụ ở giữa
     */
    fun splitWireWithPole(
        wire: Obj_CANVAS,
        polePt: Point2D,
        pixelsPerMeter: Double = DiagramRuleEngine.DEFAULT_PIXELS_PER_METER,
        startStt: Int
    ): Pair<Obj_CANVAS, Obj_CANVAS> {
        val d1 = Function_tinhtoan_ve.TINH_KC(wire.X1, wire.Y1, polePt.x, polePt.y) / pixelsPerMeter
        val d2 = Function_tinhtoan_ve.TINH_KC(polePt.x, polePt.y, wire.X3, wire.Y3) / pixelsPerMeter

        val wireLength1 = (d1 * 1.2).roundToInt()
        val wireLength2 = (d2 * 1.2).roundToInt()

        // Chuẩn hóa loại dây: Tuyệt đối không dùng mũi tên làm dây nối
        val effectiveWireCmd = if (wire.LENH == Variables.VE_MUI_TEN || wire.LENH == Variables.VE_ARROW) {
            Variables.VE_DUONG_THANG
        } else {
            wire.LENH
        }

        val wire1 = Obj_CANVAS(
            STT = startStt.toLong(),
            LENH = effectiveWireCmd,
            X1 = wire.X1,
            Y1 = wire.Y1,
            X3 = polePt.x,
            Y3 = polePt.y,
            TEN = "${wireLength1}m",
            CHON = 0
        )

        val wire2 = Obj_CANVAS(
            STT = (startStt + 1).toLong(),
            LENH = effectiveWireCmd,
            X1 = polePt.x,
            Y1 = polePt.y,
            X3 = wire.X3,
            Y3 = wire.Y3,
            TEN = "${wireLength2}m",
            CHON = 0
        )

        return Pair(wire1, wire2)
    }

    /**
     * Tự động sinh tuyến dây nắn góc vuông 90 độ (Orthogonal / Manhattan Routing)
     */
    fun generateOrthogonalWires(
        startPt: Point2D,
        endPt: Point2D,
        wireCommand: Int = Variables.VE_DUONG_THANG,
        pixelsPerMeter: Double = DiagramRuleEngine.DEFAULT_PIXELS_PER_METER,
        horizontalFirst: Boolean = true,
        startStt: Int
    ): List<Obj_CANVAS> {
        val corner = if (horizontalFirst) {
            Point2D(endPt.x, startPt.y)
        } else {
            Point2D(startPt.x, endPt.y)
        }

        val d1 = (Function_tinhtoan_ve.TINH_KC(startPt.x, startPt.y, corner.x, corner.y) / pixelsPerMeter).roundToInt()
        val d2 = (Function_tinhtoan_ve.TINH_KC(corner.x, corner.y, endPt.x, endPt.y) / pixelsPerMeter).roundToInt()

        val wire1 = Obj_CANVAS(
            STT = startStt.toLong(),
            LENH = wireCommand,
            X1 = startPt.x,
            Y1 = startPt.y,
            X3 = corner.x,
            Y3 = corner.y,
            TEN = "${d1}m",
            CHON = 0
        )

        val wire2 = Obj_CANVAS(
            STT = (startStt + 1).toLong(),
            LENH = wireCommand,
            X1 = corner.x,
            Y1 = corner.y,
            X3 = endPt.x,
            Y3 = endPt.y,
            TEN = "${d2}m",
            CHON = 0
        )

        return listOf(wire1, wire2)
    }
}
