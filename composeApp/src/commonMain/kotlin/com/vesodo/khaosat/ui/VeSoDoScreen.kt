package com.vesodo.khaosat.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.config.AppConfig
import com.vesodo.khaosat.debug.DebugLogger
import com.vesodo.khaosat.debug.LogType
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.ui.components.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource


@Composable
fun VeSoDoScreen(
    state: DrawingState = remember { DrawingState() }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(scope) {
        state.coroutineScope = scope
        onDispose { }
    }

    // Tự động nạp lại: GPS, vị trí nút FAB, thống kê số lần vẽ công cụ và bản vẽ cuối cùng
    LaunchedEffect(Unit) {
        // 1. Tải GPS
        val savedGps = com.vesodo.khaosat.database.PlatformTemplateStorage.loadLastGpsState()
        if (savedGps != null) {
            val rememberedCoord = com.vesodo.khaosat.map.GpsCoordinate(
                latitude = savedGps.latitude,
                longitude = savedGps.longitude,
                name = savedGps.name
            )
            state.gpsCenter = rememberedCoord
            state.capturedMapCenter = rememberedCoord
            state.gpsZoomLevel = savedGps.zoom
        }

        // 2. Tải vị trí Floating FAB
        val savedFabPos = com.vesodo.khaosat.database.PlatformTemplateStorage.loadHistoryFabPosition()
        if (savedFabPos != null) {
            state.historyFabPosition = savedFabPos
        }

        // 3. Tải số lần chạm vẽ của các công cụ
        val savedToolCounts = com.vesodo.khaosat.database.PlatformTemplateStorage.loadToolUsageCounts()
        if (savedToolCounts.isNotEmpty()) {
            state.loadToolUsageCounts(savedToolCounts)
        }

        // 4. Tải danh sách bộ nhớ đệm các vùng đường vector đã từng load
        val savedZones = com.vesodo.khaosat.database.PlatformTemplateStorage.loadCachedVectorRoadZones()
        if (savedZones.isNotEmpty()) {
            com.vesodo.khaosat.map.GpsVectorRoadManager.initCache(savedZones)
        }

        // Lắng nghe khi có vùng đường vector mới được lưu thì ghi ngay vào SQLite
        com.vesodo.khaosat.map.GpsVectorRoadManager.onZoneCachedListener = { _ ->
            scope.launch {
                val all = com.vesodo.khaosat.map.GpsVectorRoadManager.getAllCachedZones()
                com.vesodo.khaosat.database.PlatformTemplateStorage.saveCachedVectorRoadZones(all)
            }
        }

        // 5. Khôi phục bảng vẽ cuối cùng đã vẽ gần nhất (bao gồm cả scale, rotation, vị trí các đối tượng, đường vector)
        val lastSession = com.vesodo.khaosat.database.PlatformTemplateStorage.loadLastCanvasSession()
        if (lastSession != null && lastSession.items.isNotEmpty()) {
            state.restoreCanvasSession(lastSession)
        }

        // 6. Nếu chưa có đường vector hiển thị, kiểm tra xem toạ độ GPS hiện tại có nằm trong phạm vi các tuyến đường đã load trước đó không
        if (state.vectorBasemap == null) {
            val matchingZone = com.vesodo.khaosat.map.GpsVectorRoadManager.findMatchingCachedZone(
                state.gpsCenter,
                state.gpsZoomLevel
            )
            if (matchingZone != null && matchingZone.roads.isNotEmpty()) {
                state.vectorBasemap = com.vesodo.khaosat.map.VectorBasemap(
                    centerGps = state.gpsCenter,
                    zoom = matchingZone.zoom,
                    roads = matchingZone.roads
                )
                state.showVectorRoads = true
                state.capturedMapCenter = state.gpsCenter
                state.statusMessage = "⚡ Vị trí nằm trong phạm vi đã tải: Tự động vẽ ${matchingZone.roads.size} tuyến đường từ bộ nhớ đệm!"
            }
        }

        // 7. Khôi phục cấu hình mã QR (Vị trí 4 góc, kích thước 100%-150%, mã trạm đã lưu)
        val savedQr = com.vesodo.khaosat.database.PlatformTemplateStorage.loadQrConfig()
        if (savedQr != null) {
            state.qrScalePercent = savedQr.scalePercent.coerceIn(100, 150)
            state.qrPosition = com.vesodo.khaosat.qr.QrPosition.fromName(savedQr.positionName)
            if (state.customer.MA_TRAM.isBlank() && savedQr.lastMaTram.isNotBlank()) {
                state.customer = state.customer.copy(MA_TRAM = savedQr.lastMaTram)
            }
        }

        // 8. Khôi phục tỷ lệ scale mặc định đối tượng vẽ đã lưu trong DB
        val savedScale = com.vesodo.khaosat.database.PlatformTemplateStorage.loadDefaultObjectScale()
        if (savedScale != null && savedScale in 0.4f..3.5f) {
            state.defaultObjectScale = savedScale
        }

        // 9. Khôi phục cấu hình Đơn vị quản lý & KTV khảo sát đã lưu trong DB
        val savedUnit = com.vesodo.khaosat.database.PlatformTemplateStorage.loadUnitConfig()
        if (savedUnit != null) {
            state.unitConfig = savedUnit
            if (savedUnit.maDvi.isNotBlank()) {
                state.customer = state.customer.copy(MA_DVIQLY = savedUnit.maDvi)
            }
        }
    }



    // Bỏ hiển thị thông báo Toast / Snackbar nổi trên màn hình để tránh che mất bản vẽ
    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage.isNotBlank()) {
            DebugLogger.log(
                LogType.SYSTEM,
                "Trạng thái",
                state.statusMessage
            )
            state.statusMessage = ""
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopToolbar(state = state)
            },
            bottomBar = {
                BottomPaletteBar(state = state)
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF1F5F9))
            ) {
                // Nhận diện màn hình lớn: >= 10 inch (chiều ngang >= 840dp và cạnh nhỏ >= 550dp)
                // Các điện thoại (kể cả khi xoay ngang) có chiều cao chỉ ~360-430dp, nên minOf(maxWidth, maxHeight) < 550dp
                val isLargeScreen = maxWidth >= 840.dp && minOf(maxWidth, maxHeight) >= 550.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    // Khu vực vẽ chính (Canvas)
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onGloballyPositioned { coords ->
                                state.canvasBoundsInRoot = coords.boundsInRoot()
                            }
                    ) {
                        CanvasArea(
                            state = state,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Nút Icon Lịch sử dạng Floating FAB: Kéo thả tự do không che mã QR, tự nhớ vị trí cho lần sau
                        if (!isLargeScreen) {
                            DraggableHistoryFab(
                                state = state,
                                containerWidthDp = maxWidth,
                                containerHeightDp = maxHeight,
                                onToggleHistory = { state.showHistoryDrawer = !state.showHistoryDrawer }
                            )
                        }

                        // Bảng Debug Log: Chỉ hiển thị khi đang ở chế độ TEST (isTEST = true)
                        if (AppConfig.isTEST && DebugLogger.isPanelVisible) {
                            DebugLogPanel(
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    // Màn hình lớn (>= 10 inch): Cố định danh sách lịch sử ở bên phải
                    if (isLargeScreen) {
                        HistorySidebar(
                            state = state,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }

                // Màn hình nhỏ (< 10 inch): Hiển thị danh sách lịch sử dạng Drawer nổi trượt từ bên phải ra
                if (!isLargeScreen) {
                    AnimatedVisibility(
                        visible = state.showHistoryDrawer,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        // Nền mờ (Scrim) phía sau Drawer, click vào để đóng
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.40f))
                                .clickable { state.showHistoryDrawer = false }
                        )
                    }

                    AnimatedVisibility(
                        visible = state.showHistoryDrawer,
                        enter = slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(250)
                        ) + fadeIn(),
                        exit = slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(250)
                        ) + fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        HistorySidebar(
                            state = state,
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(min = 280.dp, max = 340.dp)
                                .shadow(elevation = 16.dp),
                            onClose = { state.showHistoryDrawer = false }
                        )
                    }
                }
            }

            // Hộp thoại chỉnh sửa ghi chú
            if (state.showNoteDialog) {
                val target = state.editingItemForNote ?: state.selectedItem
                if (target != null) {
                    NoteEditDialog(
                        item = target,
                        allCanvasItems = state.listCanvas,
                        onDismiss = {
                            state.showNoteDialog = false
                            state.editingItemForNote = null
                        },
                        onConfirm = { newNote ->
                            state.updateItemNote(target, newNote)
                            state.showNoteDialog = false
                            state.editingItemForNote = null
                        }
                    )
                } else {
                    state.showNoteDialog = false
                }
            }

            // Hộp thoại chọn sơ đồ mẫu
            if (state.showTemplateDialog) {
                TemplateDialog(
                    onDismiss = { state.showTemplateDialog = false },
                    onSelectTemplate = { items ->
                        state.loadTemplate(items)
                        state.showTemplateDialog = false
                    }
                )
            }

            // Hộp thoại lưu sơ đồ hiện tại thành mẫu
            if (state.showSaveTemplateDialog) {
                SaveTemplateDialog(
                    currentItems = state.listCanvas,
                    defaultName = "Mẫu_${state.customer.MA_YCAU_KNAI}",
                    donVi = state.customer.MA_DVIQLY,
                    onDismiss = { state.showSaveTemplateDialog = false },
                    onSaved = { msg ->
                        state.showSaveTemplateDialog = false
                        state.statusMessage = msg
                    }
                )
            }

            // Hộp thoại AI kiểm tra quy chuẩn sơ đồ
            if (state.showSmartCheckDialog) {
                SmartCheckDialog(
                    items = state.listCanvas,
                    onDismiss = { state.showSmartCheckDialog = false },
                    onHighlightItem = { item ->
                        state.selectItem(item)
                        state.statusMessage = "Đã chọn phần tử #${item.STT} cần hiệu chỉnh"
                    },
                    onApplyQuickFix = { issue ->
                        state.applyQuickFix(issue)
                    }
                )
            }

            // Hộp thoại AI cấu hình rải cột tự động
            if (state.showAutoSpacingDialog) {
                AutoSpacingDialog(
                    onDismiss = { state.showAutoSpacingDialog = false },
                    onConfirmConfig = { spanMeters, poleCmd, wireCmd, prefix ->
                        state.autoSpacingConfig = com.vesodo.khaosat.drawing.AutoSpacingConfig(
                            spanMeters = spanMeters,
                            poleCmd = poleCmd,
                            wireCmd = wireCmd,
                            polePrefix = prefix
                        )
                        state.deselectAll()
                        state.isAutoSpacingActive = true
                        state.statusMessage = "Chế độ rải cột đã kích hoạt: Kéo từ điểm đầu đến điểm cuối (nền bản đồ đã được khóa cố định)"
                    }
                )
            }

            // Hộp thoại xuất lưu sơ đồ (JPG / PDF)
            if (state.showExportDialog) {
                ExportDialog(
                    state = state,
                    defaultFileName = "${state.customer.MA_YCAU_KNAI}_sodo",
                    fontSizeSp = state.fontSize.sizeSp,
                    onDismiss = { state.showExportDialog = false },
                    onExportComplete = { result ->
                        state.showExportDialog = false
                        state.statusMessage = result.message
                    }
                )
            }

            // Hộp thoại cấu hình Đơn vị quản lý & KTV khảo sát (cho xuất DXF và mẫu)
            if (state.showUnitConfigDialog) {
                SurveyUnitConfigDialog(
                    state = state,
                    onDismiss = { state.showUnitConfigDialog = false }
                )
            }

            // Hộp thoại Đề xuất Thêm Khách hàng Bên trái / Bên phải (AI Local)
            if (state.showCustomerAttachmentDialog) {
                val parent = state.pendingCustomerParentItem
                if (parent != null) {
                    com.vesodo.khaosat.ui.components.CustomerAttachmentDialog(
                        parentItem = parent,
                        initialSide = state.pendingCustomerSide,
                        onDismiss = {
                            state.showCustomerAttachmentDialog = false
                            state.pendingCustomerParentItem = null
                        },
                        onConfirm = { selectedSide, name, code ->
                            state.addCustomerAttachment(
                                parentItem = parent,
                                side = selectedSide,
                                customerName = name,
                                customerCode = code
                            )
                        }
                    )
                } else {
                    state.showCustomerAttachmentDialog = false
                }
            }

            // Hộp thoại Xem trước Bản vẽ CAD khổ A4 chuẩn kỹ thuật (AutoCAD DXF Preview)
            if (state.showCadPreviewDialog) {
                CadPreviewDialog(
                    state = state,
                    onDismiss = { state.showCadPreviewDialog = false },
                    onExportCad = {
                        state.showCadPreviewDialog = false
                        state.showExportDialog = true
                    }
                )
            }

            // Hộp thoại xác nhận xoá toàn bộ sơ đồ
            if (state.showConfirmClearDialog) {
                AlertDialog(
                    onDismissRequest = { state.showConfirmClearDialog = false },
                    title = { Text("Xoá toàn bộ sơ đồ?") },
                    text = { Text("Thao tác này sẽ xoá tất cả các đối tượng và đường vẽ trên sơ đồ hiện tại.") },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            onClick = {
                                state.clearAll()
                                state.showConfirmClearDialog = false
                            }
                        ) {
                            Text("Xoá tất cả")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { state.showConfirmClearDialog = false }) {
                            Text("Không")
                        }
                    }
                )
            }

            // Hộp thoại Giới thiệu & Bản quyền ứng dụng (About & Licenses Dialog)
            if (state.showAboutDialog) {
                com.vesodo.khaosat.ui.components.AboutDialog(
                    onDismiss = { state.showAboutDialog = false }
                )
            }
        }

        // Hiệu ứng kéo thả công cụ từ Palette nổi trên toàn màn hình (Ghost Drag Preview)
        val draggingTool = state.draggingPaletteTool
        val dragOffset = state.draggingPaletteOffset
        if (draggingTool != null && dragOffset != null) {
            val density = LocalDensity.current
            val xDp = with(density) { dragOffset.x.toDp() }
            val yDp = with(density) { dragOffset.y.toDp() }

            Surface(
                modifier = Modifier
                    .offset(xDp - 28.dp, yDp - 28.dp)
                    .size(56.dp)
                    .shadow(12.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                border = BorderStroke(2.dp, Color(0xFF38BDF8))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(draggingTool.iconResource),
                            contentDescription = draggingTool.name,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = draggingTool.shortName,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Giao diện Bản đồ định vị GPS (Hộp thoại hiện đại, tương thích hoàn hảo Android & Desktop)
        if (state.showGpsConfigDialog || state.showGpsExplorerDialog) {
            GpsMapDialog(
                state = state,
                onDismiss = {
                    state.showGpsConfigDialog = false
                    state.showGpsExplorerDialog = false
                }
            )
        }

        // Hộp thoại bắt & báo lỗi toàn cục (Global Error Dialog)
        GlobalErrorDialog()
    }
}
