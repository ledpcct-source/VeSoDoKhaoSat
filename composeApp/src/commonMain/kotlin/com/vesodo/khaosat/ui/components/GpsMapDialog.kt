package com.vesodo.khaosat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.map.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GpsMapDialog(
    state: DrawingState,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Trạng thái cục bộ trong Dialog (để người dùng có thể HỦY/ĐÓNG mà không ảnh hưởng state gốc)
    var currentCenter by remember { mutableStateOf(state.gpsCenter) }
    var currentZoom by remember { mutableStateOf(if (state.gpsZoomLevel <= 17) 19 else state.gpsZoomLevel) }
    var selectedProvider by remember { mutableStateOf(if (state.mapTileProvider == MapTileProvider.CARTO_POSITRON_NO_LABELS) MapTileProvider.ESRI_STREET_GRAY else state.mapTileProvider) }
    var isBasemapEnabled by remember { mutableStateOf(state.showGpsBasemap) }
    var currentOpacity by remember { mutableStateOf(state.mapOpacity) }

    var latText by remember { mutableStateOf(state.gpsCenter.latitude.toString()) }
    var lonText by remember { mutableStateOf(state.gpsCenter.longitude.toString()) }

    var viewportSize by remember { mutableStateOf(IntSize(600, 400)) }
    var zoomAccumulator by remember { mutableStateOf(0f) }

    // Quản lý Định vị GPS / Vị trí hiện tại
    val locationHandler = rememberPlatformLocationHandler()
    var isLocating by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var locationNoticeDialogMessage by remember { mutableStateOf<String?>(null) }
    var locationNoticeDialogTitle by remember { mutableStateOf<String?>(null) }

    // Quản lý Tỉnh/Thành ước tính (kết hợp offline 0ms và tra cứu trực tuyến OSM)
    var estimatedProvince by remember {
        mutableStateOf(
            GpsProvinceEstimator.estimateProvinceOffline(currentCenter.latitude, currentCenter.longitude)
        )
    }

    LaunchedEffect(currentCenter.latitude, currentCenter.longitude) {
        val offlineName = GpsProvinceEstimator.estimateProvinceOffline(currentCenter.latitude, currentCenter.longitude)
        estimatedProvince = offlineName
        delay(350)
        val onlineName = GpsProvinceEstimator.fetchOnlineAddress(currentCenter.latitude, currentCenter.longitude)
        if (!onlineName.isNullOrBlank()) {
            estimatedProvince = onlineName
        }
    }

    fun persistRememberedGps(coord: GpsCoordinate, zoom: Int) {
        state.gpsCenter = coord
        state.gpsZoomLevel = zoom
        coroutineScope.launch {
            com.vesodo.khaosat.database.PlatformTemplateStorage.saveLastGpsState(
                com.vesodo.khaosat.database.SavedGpsState(
                    latitude = coord.latitude,
                    longitude = coord.longitude,
                    name = coord.name,
                    zoom = zoom
                )
            )
        }
    }

    fun executeLocationFetch() {
        isLocating = true
        coroutineScope.launch {
            try {
                val result = locationHandler.getCurrentLocation()
                if (result.errorMessage != null && (result.latitude == 0.0 && result.longitude == 0.0)) {
                    locationNoticeDialogTitle = "Không lấy được vị trí"
                    locationNoticeDialogMessage = result.errorMessage
                } else {
                    val offlineName = GpsProvinceEstimator.estimateProvinceOffline(result.latitude, result.longitude)
                    estimatedProvince = offlineName

                    val newCoord = GpsCoordinate(
                        latitude = result.latitude,
                        longitude = result.longitude,
                        name = offlineName
                    )
                    currentCenter = newCoord
                    latText = result.latitude.toString()
                    lonText = result.longitude.toString()
                    if (currentZoom < 18) {
                        currentZoom = 19
                    }
                    // Nhớ ngay vị trí hiện tại vừa định vị được để lần sau mở lên hiện đúng vị trí này
                    persistRememberedGps(newCoord, currentZoom)

                    // Tải ngay tên trực tuyến chi tiết
                    coroutineScope.launch {
                        val onlineName = GpsProvinceEstimator.fetchOnlineAddress(result.latitude, result.longitude)
                        if (!onlineName.isNullOrBlank()) {
                            estimatedProvince = onlineName
                            val updatedCoord = currentCenter.copy(name = onlineName)
                            currentCenter = updatedCoord
                            persistRememberedGps(updatedCoord, currentZoom)
                        }
                    }

                    val providerDesc = if (result.providerName.isNotBlank()) " [${result.providerName}]" else ""
                    state.statusMessage = "📍 Đã nhớ vị trí hiện tại: $offlineName$providerDesc (${result.latitude.toString().take(8)}, ${result.longitude.toString().take(8)})"
                }
            } catch (e: Throwable) {
                locationNoticeDialogTitle = "Lỗi định vị vị trí"
                locationNoticeDialogMessage = "Đã xảy ra lỗi khi lấy vị trí: ${e.message ?: "Không rõ nguyên nhân"}"
            } finally {
                isLocating = false
            }
        }
    }

    fun requestCurrentLocation() {
        if (isLocating) return
        if (locationHandler.requiresPermissionRationale && !locationHandler.isPermissionGranted()) {
            showPermissionRationaleDialog = true
            return
        }
        executeLocationFetch()
    }

    // Quản lý Drawer Log Tải dữ liệu đường Vector OpenStreetMap trôi nổi
    var isFetchDrawerOpen by remember { mutableStateOf(false) }
    var showRoadListTab by remember { mutableStateOf(true) }
    var fetchReport by remember {
        mutableStateOf(
            VectorFetchReport(
                status = FetchStatus.CONNECTING,
                message = "Sẵn sàng tải dữ liệu..."
            )
        )
    }
    val logMessages = remember { mutableStateListOf<String>() }

    fun startFetchVectorRoads(forceRefresh: Boolean = true, targetZoom: Int = currentZoom) {
        isFetchDrawerOpen = true
        showRoadListTab = true
        logMessages.clear()
        val radiusMeters = when {
            targetZoom >= 20 -> 280
            targetZoom == 19 -> 390
            targetZoom == 18 -> 550
            else -> 780
        }
        logMessages.add("🚀 Khởi tạo truy vấn OpenStreetMap...")
        logMessages.add("📍 Tọa độ tâm bản đồ: (${currentCenter.latitude.toString().take(8)}, ${currentCenter.longitude.toString().take(8)})")
        logMessages.add("🔍 Mức Zoom: ${targetZoom}x (Bán kính ~$radiusMeters m)")

        coroutineScope.launch {
            fetchReport = VectorFetchReport(
                status = FetchStatus.CONNECTING,
                message = "Đang kết nối tới máy chủ OpenStreetMap API..."
            )
            fetchReport = GpsVectorRoadManager.fetchVectorRoadsWithReport(
                centerGps = currentCenter,
                zoom = targetZoom,
                forceRefresh = forceRefresh,
                onProgress = { update ->
                    fetchReport = update
                    if (update.endpoint.isNotBlank()) {
                        logMessages.add("🌐 Máy chủ: ${update.endpoint.substringAfter("https://").substringBefore("/")}")
                    }
                    if (update.message.isNotBlank()) {
                        logMessages.add("ℹ️ ${update.message}")
                    }
                }
            )

            if (fetchReport.status == FetchStatus.SUCCESS) {
                logMessages.add("✅ Tải thành công ${fetchReport.roads.size} tuyến đường!")
            } else if (fetchReport.status == FetchStatus.EMPTY) {
                logMessages.add("⚠️ Không tìm thấy tuyến đường nào trong phạm vi này.")
            } else if (fetchReport.status == FetchStatus.ERROR) {
                logMessages.add("❌ Lỗi: ${fetchReport.message}")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.95f)
                .widthIn(max = 700.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ==================== 1. HEADER ====================
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Map,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Bản Đồ Định Vị GPS",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tâm: (${currentCenter.latitude.toString().take(7)}, ${currentCenter.longitude.toString().take(7)}) • Zoom ${currentZoom}x",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Nút Đóng 'X' góc phải
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }



                // ==================== 3. VÙNG XEM BẢN ĐỒ TƯƠNG TÁC ====================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE2E8F0))
                        .onSizeChanged { size ->
                            if (size.width > 0 && size.height > 0) {
                                viewportSize = size
                            }
                        }
                ) {
                    // --- LỚP 1 (BÊN DƯỚI): BẢN ĐỒ CANVAS & CỬ CHỈ CẢM ỨNG (PAN + PINCH TO ZOOM) ---
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // 1. Kéo 1 ngón để Pan & Chụm 2 ngón tay để Zoom (Pinch to Zoom)
                            .pointerInput(currentZoom) {
                                detectTransformGestures(panZoomLock = false) { _, pan, zoomChange, _ ->
                                    // Xử lý kéo di chuyển (Pan)
                                    if (pan.x != 0f || pan.y != 0f) {
                                        currentCenter = GpsBasemapManager.panCenter(
                                            currentCenter = currentCenter,
                                            dxPixels = pan.x,
                                            dyPixels = pan.y,
                                            zoom = currentZoom
                                        )
                                        latText = currentCenter.latitude.toString()
                                        lonText = currentCenter.longitude.toString()
                                    }

                                    // Xử lý zoom bằng 2 ngón tay (Pinch to Zoom)
                                    if (zoomChange != 1f) {
                                        zoomAccumulator += (zoomChange - 1f)
                                        if (zoomAccumulator > 0.16f) {
                                            if (currentZoom < 22) currentZoom += 1
                                            zoomAccumulator = 0f
                                        } else if (zoomAccumulator < -0.16f) {
                                            if (currentZoom > 12) currentZoom -= 1
                                            zoomAccumulator = 0f
                                        }
                                    }
                                }
                            }
                            // 2. Lăn chuột / Touchpad (Desktop)
                            .pointerInput(currentZoom) {
                                awaitPointerEventScope {
                                    var scrollAccumulator = 0f
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Scroll) {
                                            val deltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                            if (deltaY != 0f) {
                                                scrollAccumulator += deltaY
                                                if (scrollAccumulator <= -0.5f) {
                                                    if (currentZoom < 22) currentZoom += 1
                                                    scrollAccumulator = 0f
                                                } else if (scrollAccumulator >= 0.5f) {
                                                    if (currentZoom > 12) currentZoom -= 1
                                                    scrollAccumulator = 0f
                                                }
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        // Vẽ các tile bản đồ qua Canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerOffset = Offset(size.width / 2f, size.height / 2f)
                            val visibleTiles = GpsBasemapManager.calculateVisibleTiles(
                                centerGps = currentCenter,
                                zoom = currentZoom,
                                canvasCenterScreen = centerOffset,
                                canvasWidth = size.width,
                                canvasHeight = size.height
                            )

                            GpsBasemapManager.preloadTiles(
                                scope = coroutineScope,
                                tiles = visibleTiles,
                                zoom = currentZoom,
                                provider = selectedProvider
                            )

                            val grayscaleFilter = if (selectedProvider.isGrayscale) {
                                androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                    ColorMatrix().apply { setToSaturation(0f) }
                                )
                            } else null

                            visibleTiles.forEach { tile ->
                                val cacheKey = "${selectedProvider.id}_${tile.fetchZoom}_${tile.tileX}_${tile.tileY}"
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
                                .size(76.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val mid = Offset(size.width / 2f, size.height / 2f)
                                drawCircle(color = Color(0x330284C7), radius = 26f)
                                drawCircle(
                                    color = Color(0xFF0284C7),
                                    radius = 18f,
                                    style = Stroke(width = 2f)
                                )
                                drawCircle(color = Color(0xFF0284C7), radius = 6f)
                                drawCircle(color = Color.White, radius = 2.5f)
                                drawLine(
                                    color = Color(0xFF0284C7),
                                    start = Offset(0f, mid.y),
                                    end = Offset(mid.x - 22f, mid.y),
                                    strokeWidth = 1.8f
                                )
                                drawLine(
                                    color = Color(0xFF0284C7),
                                    start = Offset(mid.x + 22f, mid.y),
                                    end = Offset(size.width, mid.y),
                                    strokeWidth = 1.8f
                                )
                                drawLine(
                                    color = Color(0xFF0284C7),
                                    start = Offset(mid.x, 0f),
                                    end = Offset(mid.x, mid.y - 22f),
                                    strokeWidth = 1.8f
                                )
                                drawLine(
                                    color = Color(0xFF0284C7),
                                    start = Offset(mid.x, mid.y + 22f),
                                    end = Offset(mid.x, size.height),
                                    strokeWidth = 1.8f
                                )
                            }
                        }
                    }

                    // --- LỚP 2 (BÊN TRÊN): CÁC NÚT ĐIỀU KHIỂN NỔI (ĐẶT TÁCH BIỆT ĐỂ CHẠM MƯỢT 100%) ---

                    // 2. Bộ chọn Nguồn bản đồ (Được đưa lên góc trên bên phải của bản đồ)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                MapTileProvider.ESRI_STREET_GRAY to "ESRI Xám",
                                MapTileProvider.OSM_STANDARD to "OSM Màu"
                            ).forEach { (prov, label) ->
                                val isChosen = (selectedProvider == prov)
                                Surface(
                                    onClick = {
                                        selectedProvider = prov
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChosen) Color(0xFF0284C7) else Color.Transparent,
                                    border = if (isChosen) BorderStroke(1.dp, Color(0xFF38BDF8)) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isChosen) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isChosen) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Banner hiển thị khi đang tìm kiếm vị trí GPS
                    if (isLocating) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "Đang xác định tọa độ vị trí hiện tại...",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Nhãn bản quyền bản đồ (Attribution Badge tuân thủ bản quyền OpenStreetMap / Esri)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 10.dp, bottom = 10.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = selectedProvider.attribution,
                            fontSize = 9.5.sp,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // 3. Cụm điều khiển nổi góc dưới phải (Nút vị trí GPS cân đối & Nút Zoom)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Nút Định vị GPS Hiện Tại (Cân đối 44dp, cách cụm nút zoom 10dp)
                        Surface(
                            onClick = { requestCurrentLocation() },
                            enabled = !isLocating,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isLocating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF38BDF8)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = "Vị trí hiện tại",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Cụm nút Phóng to / Thu nhỏ Zoom (+ / -) ở góc dưới phải (Touch target lớn 44dp)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentZoom < 22) currentZoom += 1
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Phóng to", tint = Color.White, modifier = Modifier.size(24.dp))
                                }

                                Text(
                                    text = "${currentZoom}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )

                                IconButton(
                                    onClick = {
                                        if (currentZoom > 12) currentZoom -= 1
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Thu nhỏ", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }

                    // 4. Hướng dẫn thao tác chạm ở góc dưới trái
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                            Text(
                                text = "Kéo để di chuyển • Chụm 2 ngón hoặc bấm +/- để zoom",
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // --- LỚP 3 (DRAWER / OVERLAY NỔI): BẢNG LOG & DANH SÁCH ĐƯỜNG VECTOR ---
                    if (isFetchDrawerOpen) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1329)),
                            border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(8.dp)
                                .heightIn(min = 220.dp, max = 340.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                // Tiêu đề Drawer & Các nút chuyển tab & Nút đóng
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (fetchReport.status == FetchStatus.CONNECTING) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        } else if (fetchReport.status == FetchStatus.SUCCESS) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                                        } else if (fetchReport.status == FetchStatus.EMPTY) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                        } else {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }

                                        Text(
                                            text = if (fetchReport.status == FetchStatus.SUCCESS)
                                                "Đã tìm thấy ${fetchReport.roads.size} tuyến đường"
                                            else
                                                "Tiến Trình Tải Sơ Đồ Đường Vector",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color.White
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (fetchReport.status == FetchStatus.SUCCESS && fetchReport.roads.isNotEmpty()) {
                                            Surface(
                                                onClick = { showRoadListTab = !showRoadListTab },
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF1E293B),
                                                border = BorderStroke(1.dp, Color(0xFF334155))
                                            ) {
                                                Text(
                                                    text = if (showRoadListTab) "Xem Log" else "Danh sách đường (${fetchReport.roads.size})",
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF38BDF8),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { isFetchDrawerOpen = false },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFF1E293B))

                                // Nội dung: Hiển thị danh sách đường hoặc Log máy chủ
                                if (fetchReport.status == FetchStatus.SUCCESS && showRoadListTab && fetchReport.roads.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Danh sách các tuyến đường tại vị trí chọn (${currentCenter.latitude.toString().take(7)}, ${currentCenter.longitude.toString().take(7)}):",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.5.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Surface(
                                            color = Color(0xFF030712),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier.padding(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                items(fetchReport.roads) { road ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        val displayName = road.name.ifBlank { "Đường nội bộ / chưa đặt tên" }
                                                        Text(
                                                            text = "• $displayName",
                                                            color = if (road.name.isNotBlank()) Color.White else Color(0xFF94A3B8),
                                                            fontSize = 11.sp,
                                                            fontWeight = if (road.name.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                                            maxLines = 1,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Surface(
                                                            color = Color(0xFF1E293B),
                                                            shape = RoundedCornerShape(3.dp)
                                                        ) {
                                                            Text(
                                                                text = "${road.roadType} (${road.points.size} pts)",
                                                                color = Color(0xFF38BDF8),
                                                                fontSize = 9.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (fetchReport.status == FetchStatus.EMPTY) {
                                    Surface(
                                        color = Color(0xFF78350F).copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Không tìm thấy tuyến đường nào trong phạm vi này",
                                                color = Color(0xFFFBBF24),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Ở mức Zoom ${currentZoom}x, bán kính quét hẹp (~${when { currentZoom >= 20 -> 280; currentZoom == 19 -> 390; currentZoom == 18 -> 550; else -> 780 }}m). Bạn có thể thử quét mở rộng ở Zoom 18x.",
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                } else {
                                    // Khung cuộn hiển thị Log theo thời gian thực
                                    val listState = rememberLazyListState()
                                    LaunchedEffect(logMessages.size) {
                                        if (logMessages.isNotEmpty()) {
                                            listState.animateScrollToItem(logMessages.size - 1)
                                        }
                                    }

                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .background(Color(0xFF030712), RoundedCornerShape(6.dp))
                                            .padding(6.dp)
                                    ) {
                                        items(logMessages) { msg ->
                                            Text(
                                                text = msg,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = when {
                                                    msg.startsWith("✅") -> Color(0xFF4ADE80)
                                                    msg.startsWith("❌") -> Color(0xFFF87171)
                                                    msg.startsWith("⚠️") -> Color(0xFFFBBF24)
                                                    msg.startsWith("🌐") -> Color(0xFF38BDF8)
                                                    else -> Color(0xFFE2E8F0)
                                                },
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Thanh nút hành động trong Drawer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (fetchReport.status == FetchStatus.EMPTY) {
                                        OutlinedButton(
                                            onClick = { startFetchVectorRoads(forceRefresh = true, targetZoom = 18) },
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                                            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.ZoomOut, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Thử quét Zoom 18x", fontSize = 11.sp)
                                        }
                                    } else if (fetchReport.status == FetchStatus.ERROR) {
                                        OutlinedButton(
                                            onClick = { startFetchVectorRoads(forceRefresh = true) },
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Thử lại", fontSize = 11.5.sp)
                                        }
                                    }

                                    if (fetchReport.status == FetchStatus.SUCCESS && fetchReport.roads.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                state.vectorBasemap = VectorBasemap(
                                                    centerGps = currentCenter,
                                                    zoom = currentZoom,
                                                    roads = fetchReport.roads
                                                )
                                                state.showVectorRoads = true
                                                state.capturedMapCenter = currentCenter
                                                state.capturedMapZoom = currentZoom
                                                persistRememberedGps(currentCenter, currentZoom)
                                                state.statusMessage = "⚡ Đã nạp thành công ${fetchReport.roads.size} tuyến đường vector vào bản vẽ CAD!"
                                                state.autoSaveLastSession()
                                                isFetchDrawerOpen = false
                                                onDismiss()
                                            },
                                            modifier = Modifier.weight(1.3f).height(34.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Nạp vào bản vẽ (${fetchReport.roads.size} đường)",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { startFetchVectorRoads(forceRefresh = true) },
                                            modifier = Modifier.height(34.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Tải lại", fontSize = 11.sp)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { isFetchDrawerOpen = false },
                                        modifier = Modifier.height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF475569)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("Đóng log", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== 4. KHU VỰC CÀI ĐẶT & TOẠ ĐỘ ====================
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Dòng 1: Nhãn Tọa độ hiển thị có Icon Location & Màu sắc đẹp mắt + Công tắc bật/tắt bản đồ nền cân đối
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Label hiển thị tọa độ có màu sắc và icon location
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0B1329),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Tâm bản đồ",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF94A3B8)
                                            )
                                            if (estimatedProvince.isNotBlank()) {
                                                Text(
                                                    text = "• $estimatedProvince",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF38BDF8),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${currentCenter.latitude.toString().take(10)}, ${currentCenter.longitude.toString().take(10)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
                            }

                            // Công tắc Bật/Tắt Bản đồ nền (Cân đối, gọn gàng)
                            Surface(
                                onClick = { isBasemapEnabled = !isBasemapEnabled },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isBasemapEnabled) Color(0xFF0284C7).copy(alpha = 0.12f) else Color(0xFF0B1329),
                                border = BorderStroke(
                                    1.dp,
                                    if (isBasemapEnabled) Color(0xFF0284C7) else Color(0xFF334155)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Bản đồ nền",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (isBasemapEnabled) "Đang bật" else "Tắt",
                                            fontSize = 10.sp,
                                            color = if (isBasemapEnabled) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                                        )
                                    }
                                    Switch(
                                        checked = isBasemapEnabled,
                                        onCheckedChange = { isBasemapEnabled = it },
                                        modifier = Modifier.size(36.dp),
                                        thumbContent = null
                                    )
                                }
                            }
                        }

                        // Dòng 2: Thanh trượt chỉnh Độ mờ (Opacity)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Độ mờ:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            )
                            Slider(
                                value = currentOpacity,
                                onValueChange = { currentOpacity = it },
                                valueRange = 0.15f..0.85f,
                                modifier = Modifier.weight(1f).height(24.dp)
                            )
                            Text(
                                text = "${(currentOpacity * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )

                            // Nút làm mới cache
                            IconButton(
                                onClick = {
                                    GpsBasemapManager.clearCache()
                                    state.statusMessage = "Đã làm mới dữ liệu bản đồ"
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Tải lại", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // ==================== 5. FOOTER THANH HÀNH ĐỘNG CHÍNH ====================
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Nút 1: ĐÓNG / BỎ QUA (Không chọn gì cả)
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF64748B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đóng", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Nút 2: TẢI DỮ LIỆU ĐƯỜNG (VECTOR) TÍNH TỪ TÂM BẢN ĐỒ (VỊ TRÍ ĐANG CHỌN)
                        val cached = remember(currentCenter.latitude, currentCenter.longitude, currentZoom) {
                            GpsVectorRoadManager.findMatchingCachedZone(currentCenter, currentZoom)
                        }

                        if (cached != null && cached.roads.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    state.vectorBasemap = VectorBasemap(
                                        centerGps = currentCenter,
                                        zoom = currentZoom,
                                        roads = cached.roads
                                    )
                                    state.showVectorRoads = true
                                    state.capturedMapCenter = currentCenter
                                    state.capturedMapZoom = currentZoom
                                    persistRememberedGps(currentCenter, currentZoom)
                                    state.statusMessage = "⚡ Đã nạp ${cached.roads.size} tuyến đường từ cache cho tâm bản đồ!"
                                    state.autoSaveLastSession()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Dùng cache (${cached.roads.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                startFetchVectorRoads(forceRefresh = true)
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (cached != null && cached.roads.isNotEmpty()) "Tải mới dữ liệu" else "Tải dữ liệu",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }


                        // Nút 3: CHỤP ẢNH NỀN BẢN ĐỒ
                        Button(
                            onClick = {
                                val w = viewportSize.width.coerceAtLeast(300)
                                val h = viewportSize.height.coerceAtLeast(200)
                                val snapshot = GpsBasemapManager.captureMapSnapshot(
                                    width = w,
                                    height = h,
                                    centerGps = currentCenter,
                                    zoom = currentZoom,
                                    provider = selectedProvider
                                )
                                persistRememberedGps(currentCenter, currentZoom)
                                state.mapTileProvider = selectedProvider
                                state.mapOpacity = currentOpacity
                                state.capturedMapBackground = snapshot
                                state.showCapturedMap = true
                                state.capturedMapCenter = currentCenter
                                state.capturedMapZoom = currentZoom
                                state.statusMessage = "📸 Đã chụp nền bản đồ & ghi nhớ vị trí thành công (${w}x${h}px)!"
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chụp nền", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Nút 4: ÁP DỤNG TOẠ ĐỘ GPS
                        Button(
                            onClick = {
                                persistRememberedGps(currentCenter, currentZoom)
                                state.mapTileProvider = selectedProvider
                                state.showGpsBasemap = isBasemapEnabled
                                state.mapOpacity = currentOpacity
                                state.statusMessage = "📍 Đã áp dụng & ghi nhớ toạ độ GPS: (${currentCenter.latitude.toString().take(7)}, ${currentCenter.longitude.toString().take(7)})"
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Áp dụng", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // ==================== DIALOG GIẢI THÍCH QUYỀN TRUY CẬP VỊ TRÍ (RATIONALE) ====================
    if (showPermissionRationaleDialog) {
        Dialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Biểu tượng GPS
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Yêu Cầu Quyền Truy Cập Vị Trí",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ứng dụng cần quyền vị trí (GPS) nhằm:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8)
                            )
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📍", fontSize = 12.sp)
                                Text(
                                    "Xác định tọa độ hiện trường thực tế của bạn.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🗺️", fontSize = 12.sp)
                                Text(
                                    "Tự động quay tâm bản đồ về vị trí bạn đang đứng để khảo sát và đo vẽ.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🔒", fontSize = 12.sp)
                                Text(
                                    "Tọa độ được xử lý trực tiếp trên thiết bị của bạn, bảo mật tuyệt đối.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPermissionRationaleDialog = false },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text("Để sau", fontSize = 12.5.sp, color = Color(0xFF94A3B8))
                        }

                        Button(
                            onClick = {
                                showPermissionRationaleDialog = false
                                locationHandler.requestPermission { granted ->
                                    if (granted) {
                                        executeLocationFetch()
                                    } else {
                                        locationNoticeDialogTitle = "Chưa cấp quyền vị trí"
                                        locationNoticeDialogMessage = "Bạn đã từ chối cấp quyền vị trí. Để sử dụng tính năng định vị này, bạn có thể cấp quyền trong Cài đặt ứng dụng bất cứ lúc nào."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.3f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cấp quyền", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // ==================== DIALOG THÔNG BÁO LỖI / NHẮC NHỞ ĐỊNH VỊ ====================
    if (locationNoticeDialogMessage != null) {
        Dialog(
            onDismissRequest = {
                locationNoticeDialogMessage = null
                locationNoticeDialogTitle = null
            }
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 400.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = locationNoticeDialogTitle ?: "Thông báo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = locationNoticeDialogMessage.orEmpty(),
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            locationNoticeDialogMessage = null
                            locationNoticeDialogTitle = null
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Text("Đã hiểu", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
