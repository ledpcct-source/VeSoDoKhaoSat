package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.map.GpsBasemapManager
import com.vesodo.khaosat.map.GpsCoordinate
import com.vesodo.khaosat.map.GpsVectorRoadManager
import com.vesodo.khaosat.map.MapTileProvider
import com.vesodo.khaosat.map.VectorBasemap
import kotlinx.coroutines.launch

/**
 * Giao diện Bản đồ GPS độc lập Toàn màn hình Nằm ngang (Full-screen Landscape):
 * Kỹ sư di chuyển (Pan), lăn chuột phóng to thu nhỏ (Zoom), căn tâm ngắm chính xác,
 * và bấm nút CHỤP để lấy ảnh PNG làm nền tràn full toàn bộ bản vẽ CAD.
 */
@Composable
fun GpsMapExplorerDialog(
    state: DrawingState,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var mapViewportSize by remember { mutableStateOf(IntSize(1280, 720)) }

    // Giao diện Full-screen Modal Overlay phủ kín toàn bộ cửa sổ ứng dụng (Không dùng JDialog để tránh lỗi maxHeight)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* Chặn click xuyên qua nền */ }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

                // --- 1. HEADER NẰM NGANG TINH GỌN (HORIZONTAL TOOLBAR) ---
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Tiêu đề & Icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "BẢN ĐỒ KHẢO SÁT GPS (FULL MÀN HÌNH)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Kéo chuột để di chuyển • Lăn chuột để zoom • Căn tâm ngắm vào vị trí khảo sát",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Các vị trí nhanh (Presets)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Vị trí:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            GpsBasemapManager.ALL_PRESETS.forEach { preset ->
                                val isSelected = state.gpsCenter.name == preset.name
                                Surface(
                                    modifier = Modifier.clickable {
                                        state.gpsCenter = preset
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF334155)
                                ) {
                                    Text(
                                        text = preset.name.split('(').first().trim(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Bộ chọn nguồn bản đồ & Nút đóng
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Nguồn:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            listOf(
                                MapTileProvider.ESRI_STREET_GRAY to "ESRI Street (⭐)",
                                MapTileProvider.OSM_GRAYSCALE to "OSM Xám"
                            ).forEach { (prov, label) ->
                                val isSelected = state.mapTileProvider == prov
                                Surface(
                                    modifier = Modifier.clickable { state.mapTileProvider = prov },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF16A34A) else Color(0xFF334155)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                            }
                        }
                    }
                }

                // --- 2. VÙNG XEM BẢN ĐỒ TƯƠNG TÁC RỘNG LỚN (FULL VIEWPORT) ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .background(Color(0xFFE2E8F0))
                        .onSizeChanged { size ->
                            if (size.width > 0 && size.height > 0) {
                                mapViewportSize = size
                            }
                        }
                        // Lăn chuột để Zoom In / Zoom Out
                        .pointerInput(state.gpsZoomLevel) {
                            awaitPointerEventScope {
                                var scrollAccumulator = 0f
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Scroll) {
                                        val deltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                        if (deltaY != 0f) {
                                            scrollAccumulator += deltaY
                                            if (scrollAccumulator <= -0.5f) {
                                                if (state.gpsZoomLevel < 22) {
                                                    state.gpsZoomLevel += 1
                                                }
                                                scrollAccumulator = 0f
                                            } else if (scrollAccumulator >= 0.5f) {
                                                if (state.gpsZoomLevel > 12) {
                                                    state.gpsZoomLevel -= 1
                                                }
                                                scrollAccumulator = 0f
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            }
                        }
                        // Kéo rê chuột để Pan bản đồ
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var lastPos = down.position

                                do {
                                    val event = awaitPointerEvent()
                                    val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!pointer.pressed) break

                                    val curr = pointer.position
                                    val dx = curr.x - lastPos.x
                                    val dy = curr.y - lastPos.y

                                    if (dx != 0f || dy != 0f) {
                                        pointer.consume()
                                        state.gpsCenter = GpsBasemapManager.panCenter(
                                            currentCenter = state.gpsCenter,
                                            dxPixels = dx,
                                            dyPixels = dy,
                                            zoom = state.gpsZoomLevel
                                        )
                                        lastPos = curr
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    // Vẽ các ô gạch bản đồ (Map Tiles)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerOffset = Offset(size.width / 2f, size.height / 2f)
                        val visibleTiles = GpsBasemapManager.calculateVisibleTiles(
                            centerGps = state.gpsCenter,
                            zoom = state.gpsZoomLevel,
                            canvasCenterScreen = centerOffset,
                            canvasWidth = size.width,
                            canvasHeight = size.height
                        )

                        GpsBasemapManager.preloadTiles(
                            scope = coroutineScope,
                            tiles = visibleTiles,
                            zoom = state.gpsZoomLevel,
                            provider = state.mapTileProvider
                        )

                        val grayscaleFilter = if (state.mapTileProvider.isGrayscale) {
                            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
                            )
                        } else null

                        visibleTiles.forEach { tile ->
                            val cacheKey = "${state.mapTileProvider.id}_${tile.fetchZoom}_${tile.tileX}_${tile.tileY}"
                            val bmp = GpsBasemapManager.tileMemoryCache[cacheKey]
                            if (bmp != null) {
                                drawImage(
                                    image = bmp,
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(bmp.width, bmp.height),
                                    dstOffset = IntOffset(tile.screenOffset.x.toInt(), tile.screenOffset.y.toInt()),
                                    dstSize = IntSize(tile.drawSize.toInt(), tile.drawSize.toInt()),
                                    colorFilter = grayscaleFilter
                                )
                            }
                        }
                    }

                    // TÂM NGẮM ĐỊNH VỊ CHÍNH GIỮA (Center Reticle Target & Blue GPS Dot)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val mid = Offset(size.width / 2f, size.height / 2f)
                            // 1. Quầng sáng xanh lan tỏa quanh toạ độ
                            drawCircle(
                                color = Color(0x330080FF),
                                radius = 28f
                            )
                            // 2. Vòng tròn ngắm định vị
                            drawCircle(
                                color = Color(0xFF007AFF),
                                radius = 20f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                            )
                            // 3. Chấm xanh định vị toạ độ chính xác (Blue GPS Dot)
                            drawCircle(
                                color = Color(0xFF007AFF),
                                radius = 7f
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f
                            )
                            // Trục chữ thập ngang
                            drawLine(
                                color = Color(0xFF007AFF),
                                start = Offset(0f, mid.y),
                                end = Offset(mid.x - 26f, mid.y),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = Color(0xFF007AFF),
                                start = Offset(mid.x + 26f, mid.y),
                                end = Offset(size.width, mid.y),
                                strokeWidth = 2f
                            )
                            // Trục chữ thập dọc
                            drawLine(
                                color = Color(0xFF007AFF),
                                start = Offset(mid.x, 0f),
                                end = Offset(mid.x, mid.y - 26f),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = Color(0xFF007AFF),
                                start = Offset(mid.x, mid.y + 26f),
                                end = Offset(mid.x, size.height),
                                strokeWidth = 2f
                            )
                        }
                    }

                    // Nhãn toạ độ trung tâm bản đồ
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            val zoomText = if (state.gpsZoomLevel > 19) {
                                "${state.gpsZoomLevel}x (Cận cảnh ${1 shl (state.gpsZoomLevel - 19)}x)"
                            } else {
                                "${state.gpsZoomLevel}x"
                            }
                            Text(
                                text = "Tâm khảo sát: (${state.gpsCenter.latitude.toString().take(8)}, ${state.gpsCenter.longitude.toString().take(9)}) • Zoom: $zoomText",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Nhãn bản quyền bản đồ (Attribution Badge tuân thủ bản quyền OpenStreetMap / Esri)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 12.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = state.mapTileProvider.attribution,
                            fontSize = 10.sp,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Cụm nút Phóng to / Thu nhỏ Zoom nổi ở góc phải
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (state.gpsZoomLevel < 22) state.gpsZoomLevel += 1
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Phóng to", tint = Color.White)
                            }
                            Text(
                                text = "${state.gpsZoomLevel}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (state.gpsZoomLevel > 12) state.gpsZoomLevel -= 1
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Thu nhỏ", tint = Color.White)
                            }
                        }
                    }
                }

                // --- 3. FOOTER THANH HÀNH ĐỘNG DƯỚI (ACTION FOOTER) ---
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Thanh chỉnh độ mờ nền bản vẽ
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.width(320.dp)
                        ) {
                            Text("Độ mờ khi lên bản vẽ:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = state.mapOpacity,
                                onValueChange = { state.mapOpacity = it },
                                valueRange = 0.15f..0.85f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${(state.mapOpacity * 100).toInt()}%", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Cụm nút hành động chính
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.capturedMapBackground != null) {
                                OutlinedButton(
                                    onClick = {
                                        state.capturedMapBackground = null
                                        state.statusMessage = "Đã xoá ảnh nền bản đồ"
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                                    border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xoá ảnh nền", fontSize = 11.5.sp)
                                }
                            }

                            if (state.vectorBasemap != null) {
                                OutlinedButton(
                                    onClick = {
                                        state.vectorBasemap = null
                                        state.statusMessage = "Đã xoá sơ đồ đường vector"
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                                    border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xoá vector (${state.vectorBasemap!!.roads.size})", fontSize = 11.5.sp)
                                }
                            }

                            // PHƯƠNG ÁN 1: NÚT CHỤP ẢNH LÀM NỀN (BITMAP)
                            Button(
                                onClick = {
                                    val w = mapViewportSize.width
                                    val h = mapViewportSize.height
                                    val snapshot = GpsBasemapManager.captureMapSnapshot(
                                        width = w,
                                        height = h,
                                        centerGps = state.gpsCenter,
                                        zoom = state.gpsZoomLevel,
                                        provider = state.mapTileProvider
                                    )
                                    state.capturedMapBackground = snapshot
                                    state.showCapturedMap = true
                                    state.capturedMapCenter = state.gpsCenter
                                    state.capturedMapZoom = state.gpsZoomLevel
                                    state.statusMessage = "📸 Đã chụp nền bản đồ thành công (${w}x${h}px)! Đang hiển thị tràn full toàn bộ bản vẽ."
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "📸 Chụp ảnh nền",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // PHƯƠNG ÁN 2: NÚT LẤY DỮ LIỆU SƠ ĐỒ ĐƯỜNG VECTOR TỪ GPS
                            var showVectorFetchDialog by remember { mutableStateOf(false) }

                            Button(
                                onClick = {
                                    val cached = com.vesodo.khaosat.map.GpsVectorRoadManager.findMatchingCachedZone(
                                        state.gpsCenter,
                                        state.gpsZoomLevel
                                    )
                                    if (cached != null && cached.roads.isNotEmpty()) {
                                        state.vectorBasemap = VectorBasemap(
                                            centerGps = state.gpsCenter,
                                            zoom = state.gpsZoomLevel,
                                            roads = cached.roads
                                        )
                                        state.showVectorRoads = true
                                        state.capturedMapCenter = state.gpsCenter
                                        state.statusMessage = "⚡ Vị trí nằm trong phạm vi đã lưu: Đã nạp vẽ lại ngay ${cached.roads.size} tuyến đường từ cache, không tốn truy vấn mạng!"
                                        state.autoSaveLastSession()
                                        onDismiss()
                                    } else {
                                        showVectorFetchDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "⚡ Lấy sơ đồ đường (Vector)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }


                            if (showVectorFetchDialog) {
                                GpsVectorFetchDialog(
                                    centerGps = state.gpsCenter,
                                    zoom = state.gpsZoomLevel,
                                    onDismiss = { showVectorFetchDialog = false },
                                    onApplyRoads = { roads, fetchZoom ->
                                        state.vectorBasemap = VectorBasemap(
                                            centerGps = state.gpsCenter,
                                            zoom = fetchZoom,
                                            roads = roads
                                        )
                                        state.showVectorRoads = true
                                        state.capturedMapCenter = state.gpsCenter
                                        state.statusMessage = "⚡ Đã nạp thành công ${roads.size} tuyến đường vector vào bản vẽ CAD!"
                                        showVectorFetchDialog = false
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
