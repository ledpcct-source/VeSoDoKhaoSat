package com.vesodo.khaosat.math

import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Point2D(val x: Float, val y: Float)

data class ArrowPoints(
    val tip: Point2D,
    val wing1: Point2D,
    val wing2: Point2D
)

object Function_tinhtoan_ve {

    /**
     * Thuật toán kiểm tra điểm gần nhất (snapping to nearest node)
     * Giữ nguyên 100% logic từ Java:
     * - Quét toàn bộ điểm X3, Y3 của các nét vẽ
     * - Quét toàn bộ điểm X1, Y1 của các nét vẽ
     * - Nếu khoảng cách nhỏ nhất <= DO_LECH_CHUAN thì snap vào điểm đó
     * - Ngược lại giữ nguyên toạ độ (x, y)
     */
    fun KT_DIEM_GAN_NHAT(
        list_canvas: List<Obj_CANVAS>?,
        x: Float,
        y: Float,
        DO_LECH_CHUAN: Double
    ): Point2D {
        var nearPoint = Point2D(x, y)
        var DO_LECH_MIN = 1000f

        if (list_canvas != null) {
            // Xét điểm cuối x3, y3 của các nét vẽ
            for (CV_SS in list_canvas) {
                // Bỏ qua các nét vẽ không có toạ độ hợp lệ hoặc nền
                val kc3 = TINH_KC(CV_SS.X3, CV_SS.Y3, x, y).toFloat()
                if (DO_LECH_MIN > kc3) {
                    DO_LECH_MIN = kc3
                    nearPoint = Point2D(CV_SS.X3, CV_SS.Y3)
                }
            }

            // Xét điểm đầu x1, y1 của các nét vẽ đường thẳng
            for (CV_SS in list_canvas) {
                if (CV_SS.X1 != 0f || CV_SS.Y1 != 0f) {
                    val kc1 = TINH_KC(CV_SS.X1, CV_SS.Y1, x, y).toFloat()
                    if (DO_LECH_MIN > kc1) {
                        DO_LECH_MIN = kc1
                        nearPoint = Point2D(CV_SS.X1, CV_SS.Y1)
                    }
                }
            }
        }

        return if (DO_LECH_MIN > DO_LECH_CHUAN) {
            Point2D(x, y)
        } else {
            nearPoint
        }
    }

    /**
     * Thuật toán phân giải va chạm cho các đối tượng 1 điểm (Cột, Trụ BTLT, Trạm biến áp, Công tơ...)
     * Ngăn ngừa việc đặt 2 đối tượng đơn điểm trùng toạ độ hoặc quá gần nhau (< minDistance)
     * - Nếu quá gần một node hiện có: tự động tính vector đẩy lùi ra ngoài bán kính an toàn minDistance
     * - Nếu trùng khít hoàn toàn toạ độ: tự động dịch chuyển theo góc 45 độ
     * - Lặp tối đa 5 bước nếu sau khi dịch vẫn chạm vào node khác
     */
    fun PHAN_GIAI_VA_CHAM_NODE(
        listCanvas: List<Obj_CANVAS>?,
        targetX: Float,
        targetY: Float,
        minDistance: Float = 42f,
        excludeItem: Obj_CANVAS? = null
    ): Point2D {
        if (listCanvas.isNullOrEmpty()) return Point2D(targetX, targetY)

        var curX = targetX
        var curY = targetY
        var iterations = 0
        val maxIterations = 5

        while (iterations < maxIterations) {
            var collided = false
            for (item in listCanvas) {
                if (item === excludeItem) continue
                // Bỏ qua các đối tượng đường thẳng (chỉ xét các đối tượng node 1 điểm)
                val isLine = item.LENH == Variables.VE_DUONG_THANG ||
                             item.LENH == Variables.VE_LINE_DUT_NET ||
                             item.LENH == Variables.VE_2LINE ||
                             item.LENH == Variables.VE_MUI_TEN ||
                             item.LENH == Variables.VE_ARROW ||
                             item.LENH == Variables.VE_TU_DO
                if (isLine) continue

                val dist = TINH_KC(item.X3, item.Y3, curX, curY).toFloat()
                if (dist < minDistance) {
                    collided = true
                    if (dist < 2f) {
                        // Trùng toạ độ hoàn toàn: đẩy lệch 1 góc 45 độ theo bán kính an toàn
                        curX = item.X3 + minDistance * 0.7071f
                        curY = item.Y3 + minDistance * 0.7071f
                    } else {
                        // Quá gần: đẩy dọc theo vector nối từ tâm node hiện tại ra xa
                        val ux = (curX - item.X3) / dist
                        val uy = (curY - item.Y3) / dist
                        curX = item.X3 + ux * minDistance
                        curY = item.Y3 + uy * minDistance
                    }
                    break
                }
            }
            if (!collided) break
            iterations++
        }

        return Point2D(curX, curY)
    }

    /**
     * Thuật toán làm thẳng (horizontal / vertical line snapping)
     * Nếu |y3 - y1| < DO_LECH_CHUAN -> y = y1 (đường ngang hoàn toàn)
     * Nếu |x3 - x1| < DO_LECH_CHUAN -> x = x1 (đường dọc hoàn toàn)
     */
    fun LAM_THANG(
        x1: Float,
        y1: Float,
        x3: Float,
        y3: Float,
        DO_LECH_CHUAN: Double
    ): Point2D {
        var x = x3
        var y = y3
        if (abs(y3 - y1) < DO_LECH_CHUAN) {
            y = y1
        }
        if (abs(x3 - x1) < DO_LECH_CHUAN) {
            x = x1
        }
        return Point2D(x, y)
    }

    /**
     * Thuật toán tính khoảng cách Euclid giữa hai điểm
     */
    fun TINH_KC(x1: Float, y1: Float, x3: Float, y3: Float): Double {
        val dx = (x3 - x1).toDouble()
        val dy = (y3 - y1).toDouble()
        return abs(sqrt(dx * dx + dy * dy))
    }

    /**
     * Tính khoảng cách ngắn nhất từ một điểm (px, py) tới đoạn thẳng (x1, y1) -> (x2, y2)
     */
    fun DIST_POINT_TO_SEGMENT(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) {
            return TINH_KC(px, py, x1, y1)
        }
        val t = ((px - x1) * dx + (py - y1) * dy) / lenSq
        val clampedT = t.coerceIn(0f, 1f)
        val projX = x1 + clampedT * dx
        val projY = y1 + clampedT * dy
        return TINH_KC(px, py, projX, projY)
    }

    /**
     * Thuật toán vẽ mũi tên chuẩn từ ThtCanvas.ve_mui_ten trong thtsoftlib.jar
     * x1, y1 -> x2, y2 với do_lon_ten (mặc định 20)
     */
    fun TINH_MUI_TEN(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        do_lon: Int = 20
    ): ArrowPoints {
        val sipush = 320.0
        val angle = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        val d10 = abs(do_lon.toDouble() / cos(sipush))

        val d12 = angle + PI + sipush
        val xArrow1 = (x2.toDouble() + d10 * cos(d12)).toFloat()
        val yArrow1 = (y2.toDouble() + d10 * sin(d12)).toFloat()

        val d16 = angle + PI - sipush
        val xArrow2 = (x2.toDouble() + d10 * cos(d16)).toFloat()
        val yArrow2 = (y2.toDouble() + d10 * sin(d16)).toFloat()

        return ArrowPoints(
            tip = Point2D(x2, y2),
            wing1 = Point2D(xArrow1, yArrow1),
            wing2 = Point2D(xArrow2, yArrow2)
        )
    }

    /**
     * Di chuyển điểm/đối tượng theo thuật toán move_to_point_KT trong DrawingView.java
     */
    fun MOVE_TO_POINT(
        listCanvas: MutableList<Obj_CANVAS>,
        xFrom: Float,
        yFrom: Float,
        xTo: Float,
        yTo: Float,
        doLechChuan: Double
    ) {
        val choosePoint = KT_DIEM_GAN_NHAT(listCanvas, xFrom, yFrom, doLechChuan)
        val toPoint = KT_DIEM_GAN_NHAT(listCanvas, xTo, yTo, doLechChuan)

        for (cv in listCanvas) {
            // Nếu chọn điểm đầu
            if (cv.X1 == choosePoint.x && cv.Y1 == choosePoint.y) {
                if (cv.LENH == Variables.VE_TEXT || cv.LENH == Variables.VE_MUI_TEN ||
                    cv.LENH == Variables.VE_2LINE || cv.LENH == Variables.VE_CAU ||
                    cv.LENH == Variables.VE_HINH_CONGTO
                ) {
                    if (cv.LENH == Variables.VE_2LINE || cv.LENH == Variables.VE_MUI_TEN) {
                        val pThang = LAM_THANG(cv.X3, cv.Y3, xTo, yTo, doLechChuan)
                        cv.X1 = pThang.x
                        cv.Y1 = pThang.y
                    } else {
                        cv.X1 = xTo
                        cv.Y1 = yTo
                    }
                } else {
                    if (cv.X1 != toPoint.x && cv.Y1 != toPoint.y) {
                        cv.X1 = toPoint.x
                        cv.Y1 = toPoint.y
                    } else {
                        cv.X1 = xTo
                        cv.Y1 = yTo
                    }
                }
            }

            // Nếu chọn điểm cuối
            if (cv.X3 == choosePoint.x && cv.Y3 == choosePoint.y) {
                if (cv.LENH == Variables.VE_TEXT || cv.LENH == Variables.VE_MUI_TEN ||
                    cv.LENH == Variables.VE_2LINE || cv.LENH == Variables.VE_CAU ||
                    cv.LENH == Variables.VE_HINH_CONGTO
                ) {
                    if (cv.LENH == Variables.VE_2LINE || cv.LENH == Variables.VE_MUI_TEN) {
                        val pThang = LAM_THANG(cv.X1, cv.Y1, xTo, yTo, doLechChuan)
                        cv.X3 = pThang.x
                        cv.Y3 = pThang.y
                    } else {
                        cv.X3 = xTo
                        cv.Y3 = yTo
                    }
                } else {
                    if (cv.X3 != toPoint.x && cv.Y3 != toPoint.y) {
                        var cvTo: Obj_CANVAS? = null
                        for (obj in listCanvas) {
                            if (obj.X3 == toPoint.x && obj.Y3 == toPoint.y) {
                                cvTo = obj
                                break
                            }
                        }
                        if (cvTo != null && (cvTo.LENH == Variables.VE_TEXT || cvTo.LENH == Variables.VE_MUI_TEN ||
                                    cvTo.LENH == Variables.VE_2LINE || cvTo.LENH == Variables.VE_CAU ||
                                    cv.LENH == Variables.VE_HINH_CONGTO)
                        ) {
                            cv.X3 = xTo
                            cv.Y3 = yTo
                        } else {
                            cv.X3 = toPoint.x
                            cv.Y3 = toPoint.y
                        }
                    } else {
                        cv.X3 = xTo
                        cv.Y3 = yTo
                    }
                }
            }
        }
    }
}
