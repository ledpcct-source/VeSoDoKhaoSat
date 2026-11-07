package com.vesodo.khaosat.cad

import com.vesodo.khaosat.map.GpsCoordinate
import com.vesodo.khaosat.map.GpsVectorRoadManager
import com.vesodo.khaosat.map.VectorBasemap
import kotlin.math.*

/**
 * Bộ xử lý hình học Mạng lưới Đường giao thông CAD (Road Network Geometry Processor).
 *
 * Chức năng cốt lõi:
 * 1. Tính toán mép đường song song 2 bên (Road Casing) bằng thuật toán Miter Bisector (phân giác góc cua)
 *    giúp các khúc cua trên cùng một con đường hoàn toàn mềm mại, liên tục và không bị đè chéo hay hở góc.
 * 2. Tự động phát hiện các nút giao nhau (Ngã 3, Ngã 4, Điểm cắt giữa các tuyến đường).
 * 3. Cắt tỉa (Corridor Clipping / Trimming): Triệt tiêu hoàn toàn hiện tượng mép đường đâm xuyên lòng đường khác,
 *    giúp tất cả các ngã ba, ngã tư thông suốt, sạch sẽ.
 * 4. Bo góc lượn mềm mại (Smooth Corner Fillets): Tại các góc giao lộ ngã ba, ngã tư, tạo cung lượn mềm mại
 *    nối mép đường nhánh với mép đường chính đúng theo chuẩn thiết kế nút giao thông CAD chuyên nghiệp.
 * 5. Tim đường nét đứt (Dashed Centerline) ngắt hợp lý tại điểm vào giao lộ.
 */
object CadRoadNetwork {

    data class Point2D(val x: Float, val y: Float)

    data class Segment2D(val p1: Point2D, val p2: Point2D)

    data class Rect2D(val left: Float, val bottom: Float, val right: Float, val top: Float) {
        val minX: Float get() = minOf(left, right)
        val maxX: Float get() = maxOf(left, right)
        val minY: Float get() = minOf(bottom, top)
        val maxY: Float get() = maxOf(bottom, top)

        fun contains(x: Float, y: Float): Boolean {
            return x in minX..maxX && y in minY..maxY
        }

        fun contains(p: Point2D): Boolean = contains(p.x, p.y)
    }

    data class RoadEntity(
        val id: Long,
        val name: String,
        val isMajor: Boolean,
        val halfWidth: Float,
        val centerline: List<Point2D>
    )

    /**
     * Xuất toàn bộ mạng lưới đường giao thông sang mã DXF với điểm nối mượt mà, không đè lên nhau,
     * tự động cắt tỉa tránh các vùng bảo vệ (Khung tên A4, Mã QR).
     */
    fun renderRoadsDxf(
        sb: StringBuilder,
        vectorBasemap: VectorBasemap,
        canvasW: Float,
        canvasH: Float,
        offsetX: Float,
        offsetY: Float,
        exclusionBoxes: List<Rect2D> = emptyList(),
        boundaryBox: Rect2D? = null
    ) {
        val roads = vectorBasemap.roads
        if (roads.isEmpty()) return

        val zoom = vectorBasemap.zoom
        val centerGps = vectorBasemap.centerGps
        val layer = CadBlockDefinitions.LAYER_GIAO_THONG

        // 1. Chuyển đổi dữ liệu GPS sang hệ toạ độ CAD (có tính offset khung A4)
        val roadEntities = mutableListOf<RoadEntity>()
        for (road in roads) {
            val pts = road.points.map { pt ->
                val scr = GpsVectorRoadManager.gpsToScreen(
                    lat = pt.lat,
                    lon = pt.lon,
                    centerGps = centerGps,
                    zoom = zoom,
                    canvasWidth = canvasW,
                    canvasHeight = canvasH,
                    panOffsetX = 0f,
                    panOffsetY = 0f
                )
                // Chuyển sang toạ độ CAD: X = scr.x + offsetX, Y = (canvasH - scr.y) + offsetY
                Point2D(scr.x + offsetX, (canvasH - scr.y) + offsetY)
            }

            // Loại bỏ các điểm quá gần nhau liên tiếp
            val cleaned = mutableListOf<Point2D>()
            for (p in pts) {
                if (cleaned.isEmpty() || hypot(p.x - cleaned.last().x, p.y - cleaned.last().y) >= 1.0f) {
                    cleaned.add(p)
                }
            }

            if (cleaned.size >= 2) {
                val isMajor = road.roadType in listOf("primary", "secondary", "trunk", "motorway")
                val isService = road.roadType in listOf("service", "footway", "path", "living_street")
                val hw = if (isMajor) 9.0f else if (isService) 5.0f else 7.0f
                roadEntities.add(RoadEntity(road.id, road.name, isMajor, hw, cleaned))
            }
        }

        if (roadEntities.isEmpty()) return

        // 2. Tính toán các dải mép đường trái và phải (Miter Offset Edges)
        val rawEdgesByRoad = mutableListOf<Pair<RoadEntity, List<Segment2D>>>()
        for (road in roadEntities) {
            val (leftPath, rightPath) = computeMiterEdges(road.centerline, road.halfWidth)
            val segments = mutableListOf<Segment2D>()
            for (i in 0 until leftPath.size - 1) {
                segments.add(Segment2D(leftPath[i], leftPath[i + 1]))
            }
            for (i in 0 until rightPath.size - 1) {
                segments.add(Segment2D(rightPath[i], rightPath[i + 1]))
            }
            rawEdgesByRoad.add(road to segments)
        }

        // 3. Cắt tỉa (Corridor Clipping): Loại bỏ các đoạn mép đường nằm bên trong lòng đường khác
        val clippedSegments = mutableListOf<Segment2D>()
        val junctionEndpoints = mutableListOf<Point2D>()

        for (idxA in rawEdgesByRoad.indices) {
            val (roadA, segsA) = rawEdgesByRoad[idxA]
            val otherRoads = roadEntities.filterIndexed { idxB, _ -> idxB != idxA }

            for (seg in segsA) {
                // Kiểm tra xem đoạn seg có bị cắt bởi lòng đường của các tuyến đường khác không
                val validSubsegments = clipSegmentAgainstOtherRoads(seg, otherRoads, junctionEndpoints)
                clippedSegments.addAll(validSubsegments)
            }
        }

        // 4. Cắt tỉa mép đường chống đè lên các vùng bảo vệ (Khung tên A4, Mã QR) và khống chế trong khung bản vẽ
        var finalClippedSegments = clipSegmentsAgainstBoxes(clippedSegments, exclusionBoxes)
        if (boundaryBox != null) {
            finalClippedSegments = finalClippedSegments.mapNotNull { clipSegmentToBoundary(it.p1, it.p2, boundaryBox) }
        }

        // Vẽ các đoạn mép đường đã cắt tỉa sạch sẽ (không còn bị đè lên nhau và không xâm phạm khung tên / QR)
        for (seg in finalClippedSegments) {
            if (hypot(seg.p2.x - seg.p1.x, seg.p2.y - seg.p1.y) >= 0.5f) {
                writeCadLine(sb, seg.p1.x, seg.p1.y, seg.p2.x, seg.p2.y, layer)
            }
        }

        // 5. Nối các điểm góc giao lộ mềm mại (Smooth Corner Fillets tại ngã 3, ngã 4, loại trừ điểm trong vùng cấm)
        val validJunctions = junctionEndpoints.filter { pt ->
            (boundaryBox == null || boundaryBox.contains(pt)) &&
            exclusionBoxes.none { box -> box.contains(pt) }
        }
        renderCornerFillets(sb, validJunctions, layer)

        // 6. Vẽ tim đường nét đứt (DASHED) cho các đường lớn, ngắt trước khi vào ngã ba/ngã tư và tránh vùng bảo vệ
        for (road in roadEntities) {
            if (road.isMajor) {
                val otherRoads = roadEntities.filter { it.id != road.id }
                for (i in 0 until road.centerline.size - 1) {
                    val p1 = road.centerline[i]
                    val p2 = road.centerline[i + 1]
                    val seg = Segment2D(p1, p2)
                    val dummy = mutableListOf<Point2D>()
                    val validCenterlines = clipSegmentAgainstOtherRoads(seg, otherRoads, dummy)
                    var finalCenterlines = clipSegmentsAgainstBoxes(validCenterlines, exclusionBoxes)
                    if (boundaryBox != null) {
                        finalCenterlines = finalCenterlines.mapNotNull { clipSegmentToBoundary(it.p1, it.p2, boundaryBox) }
                    }
                    for (cSeg in finalCenterlines) {
                        if (hypot(cSeg.p2.x - cSeg.p1.x, cSeg.p2.y - cSeg.p1.y) >= 4.0f) {
                            writeCadDashedLine(sb, cSeg.p1.x, cSeg.p1.y, cSeg.p2.x, cSeg.p2.y, layer)
                        }
                    }
                }
            }
        }

        // 7. Ghi tên đường sắc nét, căn giữa và xoay theo trục đoạn đường (loại trừ nếu nằm trong khung tên/QR hoặc ngoài khung)
        val drawnNames = mutableSetOf<String>()
        for (road in roadEntities) {
            if (road.name.isNotBlank() && !drawnNames.contains(road.name)) {
                val pts = road.centerline
                if (pts.size >= 2) {
                    // Tìm đoạn dài nhất để ghi tên
                    var bestP1 = pts[0]
                    var bestP2 = pts[1]
                    var maxLen = 0f
                    for (i in 0 until pts.size - 1) {
                        val len = hypot(pts[i + 1].x - pts[i].x, pts[i + 1].y - pts[i].y)
                        if (len > maxLen) {
                            maxLen = len
                            bestP1 = pts[i]
                            bestP2 = pts[i + 1]
                        }
                    }

                    if (maxLen >= 60f) {
                        val midX = (bestP1.x + bestP2.x) / 2f
                        val midY = (bestP1.y + bestP2.y) / 2f

                        if (boundaryBox != null && !boundaryBox.contains(midX, midY)) continue

                        val isInsideProtectedBox = exclusionBoxes.any { box ->
                            box.contains(midX, midY) || box.contains(bestP1) || box.contains(bestP2)
                        }

                        if (!isInsideProtectedBox) {
                            drawnNames.add(road.name)
                            var angleDeg = (atan2(bestP2.y - bestP1.y, bestP2.x - bestP1.x) * 180.0 / PI).toFloat()
                            if (angleDeg > 90f) angleDeg -= 180f
                            else if (angleDeg < -90f) angleDeg += 180f

                            sb.append("  0\nTEXT\n  8\n$layer\n")
                            sb.append(" 10\n${CadBlockDefinitions.fmt(midX)}\n 20\n${CadBlockDefinitions.fmt(midY)}\n 30\n0.0\n")
                            sb.append(" 40\n11.0\n")
                            sb.append("  1\n${CadBlockDefinitions.encodeDxfText(road.name)}\n")
                            sb.append(" 50\n${CadBlockDefinitions.fmt(angleDeg)}\n")
                            sb.append(" 72\n1\n") // Center horizontal
                            sb.append(" 11\n${CadBlockDefinitions.fmt(midX)}\n 21\n${CadBlockDefinitions.fmt(midY)}\n 31\n0.0\n")
                        }
                    }
                }
            }
        }
    }

    /**
     * Tính toán các đỉnh mép đường trái và phải bằng thuật toán Miter Bisector.
     * Đảm bảo mọi góc cua trên đường liên tục, không bị đè chéo hoặc hở rách.
     */
    private fun computeMiterEdges(pts: List<Point2D>, hw: Float): Pair<List<Point2D>, List<Point2D>> {
        val n = pts.size
        val left = mutableListOf<Point2D>()
        val right = mutableListOf<Point2D>()

        for (i in 0 until n) {
            when (i) {
                0 -> {
                    val dx = pts[1].x - pts[0].x
                    val dy = pts[1].y - pts[0].y
                    val len = hypot(dx, dy)
                    val nx = if (len > 0f) -dy / len else 0f
                    val ny = if (len > 0f) dx / len else 1f
                    left.add(Point2D(pts[0].x + nx * hw, pts[0].y + ny * hw))
                    right.add(Point2D(pts[0].x - nx * hw, pts[0].y - ny * hw))
                }
                n - 1 -> {
                    val dx = pts[n - 1].x - pts[n - 2].x
                    val dy = pts[n - 1].y - pts[n - 2].y
                    val len = hypot(dx, dy)
                    val nx = if (len > 0f) -dy / len else 0f
                    val ny = if (len > 0f) dx / len else 1f
                    left.add(Point2D(pts[n - 1].x + nx * hw, pts[n - 1].y + ny * hw))
                    right.add(Point2D(pts[n - 1].x - nx * hw, pts[n - 1].y - ny * hw))
                }
                else -> {
                    val d1x = pts[i].x - pts[i - 1].x
                    val d1y = pts[i].y - pts[i - 1].y
                    val len1 = hypot(d1x, d1y)
                    val n1x = if (len1 > 0f) -d1y / len1 else 0f
                    val n1y = if (len1 > 0f) d1x / len1 else 1f

                    val d2x = pts[i + 1].x - pts[i].x
                    val d2y = pts[i + 1].y - pts[i].y
                    val len2 = hypot(d2x, d2y)
                    val n2x = if (len2 > 0f) -d2y / len2 else 0f
                    val n2y = if (len2 > 0f) d2x / len2 else 1f

                    val sx = n1x + n2x
                    val sy = n1y + n2y
                    val slen = hypot(sx, sy)

                    if (slen < 1e-4f) {
                        left.add(Point2D(pts[i].x + n1x * hw, pts[i].y + n1y * hw))
                        right.add(Point2D(pts[i].x - n1x * hw, pts[i].y - n1y * hw))
                    } else {
                        val mx = sx / slen
                        val my = sy / slen
                        val dot = mx * n1x + my * n1y
                        val k = (1f / maxOf(dot, 0.45f)).coerceIn(0.6f, 1.75f)
                        left.add(Point2D(pts[i].x + mx * hw * k, pts[i].y + my * hw * k))
                        right.add(Point2D(pts[i].x - mx * hw * k, pts[i].y - my * hw * k))
                    }
                }
            }
        }
        return Pair(left, right)
    }

    /**
     * Cắt tỉa một đoạn mép đường chống đâm vào lòng đường của các tuyến đường khác.
     */
    private fun clipSegmentAgainstOtherRoads(
        seg: Segment2D,
        otherRoads: List<RoadEntity>,
        junctionEndpoints: MutableList<Point2D>
    ): List<Segment2D> {
        var currentSegments = listOf(seg)

        for (road in otherRoads) {
            val nextSegments = mutableListOf<Segment2D>()
            val hw = road.halfWidth - 0.3f // Khoảng đệm an toàn

            for (s in currentSegments) {
                // Kiểm tra 2 điểm đầu mút
                val in1 = isPointInsideRoadCorridor(s.p1, road, hw)
                val in2 = isPointInsideRoadCorridor(s.p2, road, hw)

                if (in1 && in2) {
                    // Cả 2 điểm đều nằm bên trong lòng đường khác -> Bỏ hoàn toàn
                    continue
                }

                if (!in1 && !in2) {
                    // Cả 2 điểm đều ở ngoài: Kiểm tra có đâm xuyên qua lòng đường không
                    val mid = Point2D((s.p1.x + s.p2.x) / 2f, (s.p1.y + s.p2.y) / 2f)
                    if (isPointInsideRoadCorridor(mid, road, hw)) {
                        // Tìm 2 giao điểm cắt ngang
                        val t1 = findBoundaryIntersection(s.p1, mid, road, hw)
                        val t2 = findBoundaryIntersection(mid, s.p2, road, hw)
                        if (t1 != null) {
                            nextSegments.add(Segment2D(s.p1, t1))
                            junctionEndpoints.add(t1)
                        }
                        if (t2 != null) {
                            nextSegments.add(Segment2D(t2, s.p2))
                            junctionEndpoints.add(t2)
                        }
                    } else {
                        nextSegments.add(s)
                    }
                } else if (!in1 && in2) {
                    // p1 ở ngoài, p2 ở trong -> Cắt tại biên giao nhau
                    val inter = findBoundaryIntersection(s.p1, s.p2, road, hw)
                    if (inter != null) {
                        nextSegments.add(Segment2D(s.p1, inter))
                        junctionEndpoints.add(inter)
                    }
                } else {
                    // p1 ở trong, p2 ở ngoài -> Cắt tại biên giao nhau
                    val inter = findBoundaryIntersection(s.p2, s.p1, road, hw)
                    if (inter != null) {
                        nextSegments.add(Segment2D(inter, s.p2))
                        junctionEndpoints.add(inter)
                    }
                }
            }
            currentSegments = nextSegments
        }

        return currentSegments
    }

    /**
     * Kiểm tra một điểm có nằm trong lòng đường (corridor) của con đường không.
     */
    private fun isPointInsideRoadCorridor(p: Point2D, road: RoadEntity, hw: Float): Boolean {
        val pts = road.centerline
        for (i in 0 until pts.size - 1) {
            val d = distPointToSegment(p, pts[i], pts[i + 1])
            if (d < hw) return true
        }
        return false
    }

    /**
     * Tìm toạ độ giao điểm giữa đoạn thẳng và ranh giới lòng đường bằng phương pháp nhị phân.
     */
    private fun findBoundaryIntersection(
        outP: Point2D,
        inP: Point2D,
        road: RoadEntity,
        targetDist: Float
    ): Point2D? {
        var low = 0f
        var high = 1f
        var bestP = outP

        for (iter in 0..8) {
            val midT = (low + high) / 2f
            val cx = outP.x + midT * (inP.x - outP.x)
            val cy = outP.y + midT * (inP.y - outP.y)
            val cur = Point2D(cx, cy)
            val d = minDistanceToCenterline(cur, road)

            if (d < targetDist) {
                high = midT
            } else {
                low = midT
                bestP = cur
            }
        }
        return bestP
    }

    private fun minDistanceToCenterline(p: Point2D, road: RoadEntity): Float {
        var minD = Float.MAX_VALUE
        val pts = road.centerline
        for (i in 0 until pts.size - 1) {
            val d = distPointToSegment(p, pts[i], pts[i + 1])
            if (d < minD) minD = d
        }
        return minD
    }

    private fun distPointToSegment(p: Point2D, a: Point2D, b: Point2D): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-5f) return hypot(p.x - a.x, p.y - a.y)
        val t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq
        val clampedT = t.coerceIn(0f, 1f)
        val px = a.x + clampedT * dx
        val py = a.y + clampedT * dy
        return hypot(p.x - px, p.y - py)
    }

    /**
     * Bo góc lượn mềm mại (Smooth Corner Fillet) kết nối các đầu mép đường tại các góc ngã 3, ngã 4.
     */
    private fun renderCornerFillets(
        sb: StringBuilder,
        endpoints: List<Point2D>,
        layer: String
    ) {
        if (endpoints.size < 2) return

        val used = BooleanArray(endpoints.size)
        val maxCornerDist = 18.0f // Cự ly tối đa giữa 2 mép đường tại góc giao lộ

        for (i in endpoints.indices) {
            if (used[i]) continue
            var closestIdx = -1
            var closestDist = Float.MAX_VALUE

            for (j in endpoints.indices) {
                if (i != j && !used[j]) {
                    val d = hypot(endpoints[i].x - endpoints[j].x, endpoints[i].y - endpoints[j].y)
                    if (d in 2.0f..maxCornerDist && d < closestDist) {
                        closestDist = d
                        closestIdx = j
                    }
                }
            }

            if (closestIdx != -1) {
                used[i] = true
                used[closestIdx] = true
                val p1 = endpoints[i]
                val p2 = endpoints[closestIdx]

                // Tạo cung lượn mềm mại với 3 đoạn nội suy (Bezier/Fillet arc)
                val midX = (p1.x + p2.x) / 2f
                val midY = (p1.y + p2.y) / 2f
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val len = hypot(dx, dy)
                val curveDepth = len * 0.18f

                val nx = -dy / len * curveDepth
                val ny = dx / len * curveDepth
                val ctrl = Point2D(midX + nx, midY + ny)

                // Vẽ cung bo góc bằng các đoạn thẳng chuyển tiếp mượt mà
                writeCadLine(sb, p1.x, p1.y, ctrl.x, ctrl.y, layer)
                writeCadLine(sb, ctrl.x, ctrl.y, p2.x, p2.y, layer)
            }
        }
    }

    private fun writeCadLine(sb: StringBuilder, x1: Float, y1: Float, x2: Float, y2: Float, layer: String) {
        sb.append("  0\nLINE\n  8\n$layer\n")
        sb.append(" 10\n${CadBlockDefinitions.fmt(x1)}\n 20\n${CadBlockDefinitions.fmt(y1)}\n 30\n0.0\n")
        sb.append(" 11\n${CadBlockDefinitions.fmt(x2)}\n 21\n${CadBlockDefinitions.fmt(y2)}\n 31\n0.0\n")
    }

    private fun writeCadDashedLine(sb: StringBuilder, x1: Float, y1: Float, x2: Float, y2: Float, layer: String) {
        sb.append("  0\nLINE\n  8\n$layer\n  6\nDASHED\n")
        sb.append(" 10\n${CadBlockDefinitions.fmt(x1)}\n 20\n${CadBlockDefinitions.fmt(y1)}\n 30\n0.0\n")
        sb.append(" 11\n${CadBlockDefinitions.fmt(x2)}\n 21\n${CadBlockDefinitions.fmt(y2)}\n 31\n0.0\n")
    }

    /**
     * Cắt tỉa danh sách các đoạn thẳng với danh sách các hình chữ nhật bảo vệ (Khung tên A4, Mã QR).
     */
    private fun clipSegmentsAgainstBoxes(segments: List<Segment2D>, boxes: List<Rect2D>): List<Segment2D> {
        if (boxes.isEmpty()) return segments
        var current = segments
        for (box in boxes) {
            val next = mutableListOf<Segment2D>()
            for (seg in current) {
                next.addAll(clipSegmentAgainstRect(seg, box))
            }
            current = next
        }
        return current
    }

    /**
     * Cắt tỉa 1 đoạn thẳng seg(p1, p2) với 1 hình chữ nhật rect:
     * Giữ lại các phần của đoạn thẳng nằm bên ngoài rect; loại bỏ phần nằm bên trong rect.
     */
    private fun clipSegmentAgainstRect(seg: Segment2D, rect: Rect2D): List<Segment2D> {
        val p1 = seg.p1
        val p2 = seg.p2
        val in1 = rect.contains(p1)
        val in2 = rect.contains(p2)

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y

        val tValues = mutableListOf(0f, 1f)

        if (abs(dx) > 1e-4f) {
            val tLeft = (rect.minX - p1.x) / dx
            if (tLeft in 0.0001f..0.9999f) {
                val yAtT = p1.y + tLeft * dy
                if (yAtT in (rect.minY - 0.01f)..(rect.maxY + 0.01f)) tValues.add(tLeft)
            }
            val tRight = (rect.maxX - p1.x) / dx
            if (tRight in 0.0001f..0.9999f) {
                val yAtT = p1.y + tRight * dy
                if (yAtT in (rect.minY - 0.01f)..(rect.maxY + 0.01f)) tValues.add(tRight)
            }
        }

        if (abs(dy) > 1e-4f) {
            val tBottom = (rect.minY - p1.y) / dy
            if (tBottom in 0.0001f..0.9999f) {
                val xAtT = p1.x + tBottom * dx
                if (xAtT in (rect.minX - 0.01f)..(rect.maxX + 0.01f)) tValues.add(tBottom)
            }
            val tTop = (rect.maxY - p1.y) / dy
            if (tTop in 0.0001f..0.9999f) {
                val xAtT = p1.x + tTop * dx
                if (xAtT in (rect.minX - 0.01f)..(rect.maxX + 0.01f)) tValues.add(tTop)
            }
        }

        tValues.sort()

        val result = mutableListOf<Segment2D>()
        for (i in 0 until tValues.size - 1) {
            val tStart = tValues[i]
            val tEnd = tValues[i + 1]
            if (tEnd - tStart < 1e-4f) continue
            val tMid = (tStart + tEnd) / 2f
            val midX = p1.x + tMid * dx
            val midY = p1.y + tMid * dy

            // Nếu trung điểm nằm ngoài rect thì giữ lại
            if (!rect.contains(midX, midY)) {
                val sp1 = Point2D(p1.x + tStart * dx, p1.y + tStart * dy)
                val sp2 = Point2D(p1.x + tEnd * dx, p1.y + tEnd * dy)
                if (hypot(sp2.x - sp1.x, sp2.y - sp1.y) >= 0.5f) {
                    result.add(Segment2D(sp1, sp2))
                }
            }
        }
        return result
    }

    // Mã vùng cho thuật toán Cohen-Sutherland
    private const val CODE_INSIDE = 0
    private const val CODE_LEFT = 1
    private const val CODE_RIGHT = 2
    private const val CODE_BOTTOM = 4
    private const val CODE_TOP = 8

    private fun computeOutCode(x: Float, y: Float, box: Rect2D): Int {
        var code = CODE_INSIDE
        if (x < box.minX) code = code or CODE_LEFT
        else if (x > box.maxX) code = code or CODE_RIGHT
        if (y < box.minY) code = code or CODE_BOTTOM
        else if (y > box.maxY) code = code or CODE_TOP
        return code
    }

    /**
     * Cắt tỉa (clip) một đoạn thẳng chính xác bên trong khung bao hình chữ nhật box
     * theo thuật toán Cohen-Sutherland. Trả về đoạn thẳng nằm trọn bên trong khung,
     * hoặc null nếu đoạn thẳng hoàn toàn nằm ngoài khung.
     */
    fun clipSegmentToBoundary(p1: Point2D, p2: Point2D, box: Rect2D): Segment2D? {
        var x0 = p1.x
        var y0 = p1.y
        var x1 = p2.x
        var y1 = p2.y
        var outcode0 = computeOutCode(x0, y0, box)
        var outcode1 = computeOutCode(x1, y1, box)

        while (true) {
            if ((outcode0 or outcode1) == 0) {
                return Segment2D(Point2D(x0, y0), Point2D(x1, y1))
            } else if ((outcode0 and outcode1) != 0) {
                return null
            } else {
                val outcodeOut = if (outcode0 != 0) outcode0 else outcode1
                var x = 0f
                var y = 0f

                val dx = x1 - x0
                val dy = y1 - y0

                if ((outcodeOut and CODE_TOP) != 0) {
                    x = if (abs(dy) > 1e-4f) x0 + dx * (box.maxY - y0) / dy else x0
                    y = box.maxY
                } else if ((outcodeOut and CODE_BOTTOM) != 0) {
                    x = if (abs(dy) > 1e-4f) x0 + dx * (box.minY - y0) / dy else x0
                    y = box.minY
                } else if ((outcodeOut and CODE_RIGHT) != 0) {
                    y = if (abs(dx) > 1e-4f) y0 + dy * (box.maxX - x0) / dx else y0
                    x = box.maxX
                } else if ((outcodeOut and CODE_LEFT) != 0) {
                    y = if (abs(dx) > 1e-4f) y0 + dy * (box.minX - x0) / dx else y0
                    x = box.minX
                }

                if (outcodeOut == outcode0) {
                    x0 = x
                    y0 = y
                    outcode0 = computeOutCode(x0, y0, box)
                } else {
                    x1 = x
                    y1 = y
                    outcode1 = computeOutCode(x1, y1, box)
                }
            }
        }
    }
}
