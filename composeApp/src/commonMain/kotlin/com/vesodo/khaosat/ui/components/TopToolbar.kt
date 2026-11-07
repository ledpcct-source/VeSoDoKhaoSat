package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.ai.DiagramRuleEngine
import com.vesodo.khaosat.config.AppConfig
import com.vesodo.khaosat.debug.DebugLogger
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.drawing.TextFontSize
import com.vesodo.khaosat.drawing.ToolCategory
import com.vesodo.khaosat.model.Variables
import com.vesodo.khaosat.platform.isAndroidPlatform
import com.vesodo.khaosat.platform.isDesktopPlatform
import org.jetbrains.compose.resources.painterResource

@Composable
fun TopToolbar(
    state: DrawingState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        color = Color(0xFF1E293B), // Slate 800
        contentColor = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var isScrolling = false
                        val downPosition = down.position
                        var previousPos = down.position

                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    if (isScrolling && change.type == PointerType.Mouse) {
                                        change.consume()
                                    }
                                    break
                                }

                                val currentPos = change.position
                                val deltaX = currentPos.x - previousPos.x
                                val totalDx = currentPos.x - downPosition.x
                                val totalDy = currentPos.y - downPosition.y
                                val dragDistSquared = totalDx * totalDx + totalDy * totalDy

                                if (!isScrolling) {
                                    if (dragDistSquared > 64f) { // Di chuyển > 8px
                                        val absDx = kotlin.math.abs(totalDx)
                                        val absDy = kotlin.math.abs(totalDy)
                                        if (absDx > absDy) {
                                            isScrolling = true
                                        }
                                    }
                                }

                                if (isScrolling) {
                                    if (change.type == PointerType.Mouse) {
                                        scrollState.dispatchRawDelta(-deltaX)
                                        change.consume()
                                    }
                                }
                                previousPos = currentPos
                            }
                        } finally {
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // Nút Lưu ảnh (Mobile: Android & iOS) hoặc Xuất file CAD/Ảnh/PDF (Desktop)
            if (isDesktopPlatform) {
                Button(
                    onClick = {
                        state.selectedItem = null
                        state.listCanvas.forEach { it.CHON = 0 }
                        state.showExportDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)), // Green
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Xuất file\n(CAD/Ảnh)", fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                }

                // Nút Xem trước CAD (A4) - Chỉ dành riêng cho Desktop
                Button(
                    onClick = {
                        state.selectedItem = null
                        state.listCanvas.forEach { it.CHON = 0 }
                        state.showCadPreviewDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)), // Ocean Blue
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Xem trước\nCAD (A4)", fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                }
            } else {
                Button(
                    onClick = {
                        state.selectedItem = null
                        state.listCanvas.forEach { it.CHON = 0 }
                        state.showExportDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)), // Green
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lưu ảnh\n(Thư viện)", fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                }
            }

            // Nút Lưu mẫu
            Button(
                onClick = { state.showSaveTemplateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)), // Blue
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lưu mẫu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Nút Chọn mẫu
            Button(
                onClick = { state.showTemplateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)), // Amber
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chọn mẫu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Nút AI Kiểm tra
            val issues = remember(state.listCanvas.size, state.renderVersion) {
                DiagramRuleEngine.analyzeDiagram(state.listCanvas)
            }
            val issueCount = issues.size

            Button(
                onClick = { state.showSmartCheckDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)), // Indigo
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (issueCount > 0) {
                            Badge(containerColor = Color(0xFFEF4444)) {
                                Text("$issueCount", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Kiểm tra", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Nút Rải cột AI
            Button(
                onClick = {
                    if (state.isAutoSpacingActive) {
                        state.isAutoSpacingActive = false
                    } else {
                        state.deselectAll()
                        state.showAutoSpacingDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isAutoSpacingActive) Color(0xFF0284C7) else Color(0xFF0F766E)
                ), // Cyan / Teal
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Polyline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (state.isAutoSpacingActive) "Đang rải cột..." else "Rải cột AI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Nút Bản đồ nền GPS & Chụp ảnh nền
            val hasCapturedBg = state.capturedMapBackground != null
            Button(
                onClick = { state.showGpsExplorerDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasCapturedBg) Color(0xFF0284C7) else Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    if (hasCapturedBg) Icons.Default.CameraAlt else Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (hasCapturedBg) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (hasCapturedBg) "Nền: ${state.capturedMapCenter.name.split('(').first().trim().ifBlank { "Đã chụp" }}" else "Bản đồ GPS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Nút Cài đặt Mã QR (Mã trạm, kích thước 100-150%, vị trí 4 góc)
            Button(
                onClick = { state.showQrDetailDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Mã QR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Nút Cấu hình Đơn vị khảo sát & KTV (chỉ dành cho xuất CAD DXF trên Desktop)
            if (isDesktopPlatform) {
                Button(
                    onClick = { state.showUnitConfigDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Đơn vị & KTV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Chọn Cỡ chữ
            Box {
                OutlinedButton(
                    onClick = { state.showFontSizeMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.4f))),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cỡ: ${state.fontSize.label}", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = state.showFontSizeMenu,
                    onDismissRequest = { state.showFontSizeMenu = false }
                ) {
                    TextFontSize.values().forEach { sizeOption ->
                        DropdownMenuItem(
                            text = { Text(sizeOption.label) },
                            onClick = {
                                state.fontSize = sizeOption
                                state.showFontSizeMenu = false
                            }
                        )
                    }
                }
            }

            VerticalDivider(modifier = Modifier.height(36.dp).padding(horizontal = 4.dp), color = Color.White.copy(alpha = 0.2f))

            // Tabs danh mục: Hay dùng / Trụ / Trạm / Khác / Đường kẻ
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CategoryTabButton(
                    title = "Hay dùng",
                    isSelected = state.selectedCategory == ToolCategory.POPULAR,
                    onClick = { state.selectedCategory = ToolCategory.POPULAR }
                )
                CategoryTabButton(
                    title = "Trụ",
                    isSelected = state.selectedCategory == ToolCategory.TRU,
                    onClick = { state.selectedCategory = ToolCategory.TRU }
                )
                CategoryTabButton(
                    title = "Trạm",
                    isSelected = state.selectedCategory == ToolCategory.TRAM,
                    onClick = { state.selectedCategory = ToolCategory.TRAM }
                )
                CategoryTabButton(
                    title = "Khác",
                    isSelected = state.selectedCategory == ToolCategory.KHAC,
                    onClick = { state.selectedCategory = ToolCategory.KHAC }
                )
                CategoryTabButton(
                    title = "Đường kẻ",
                    isSelected = state.selectedCategory == ToolCategory.LINE,
                    onClick = { state.selectedCategory = ToolCategory.LINE }
                )
            }


            VerticalDivider(modifier = Modifier.height(36.dp).padding(horizontal = 4.dp), color = Color.White.copy(alpha = 0.2f))

            // Nút hoàn tác (Undo)
            IconButton(
                onClick = { state.undo() },
                enabled = state.undoStack.isNotEmpty(),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.undoStack.isNotEmpty()) Color(0xFF334155) else Color(0xFF1E293B))
            ) {
                Icon(
                    Icons.Default.Undo,
                    contentDescription = "Hoàn tác (Undo)",
                    tint = if (state.undoStack.isNotEmpty()) Color.White else Color.Gray
                )
            }

            // Nút làm lại (Redo)
            IconButton(
                onClick = { state.redo() },
                enabled = state.redoStack.isNotEmpty(),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.redoStack.isNotEmpty()) Color(0xFF334155) else Color(0xFF1E293B))
            ) {
                Icon(
                    Icons.Default.Redo,
                    contentDescription = "Làm lại (Redo)",
                    tint = if (state.redoStack.isNotEmpty()) Color.White else Color.Gray
                )
            }

            // Nút Thông tin phần mềm & Bản quyền (About & Licenses)
            IconButton(
                onClick = { state.showAboutDialog = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF334155))
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Thông tin & Bản quyền (About)",
                    tint = Color(0xFF38BDF8)
                )
            }

            // Giao diện xem Log Debug (Chỉ hiển thị khi isTEST = true, tự ẩn khi xuất production)
            if (AppConfig.isTEST) {
                Button(
                    onClick = { DebugLogger.isPanelVisible = !DebugLogger.isPanelVisible },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (DebugLogger.isPanelVisible) Color(0xFF0284C7) else Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = "Bật/Tắt Log Debug",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (DebugLogger.isPanelVisible) "Log: Bật" else "Log: Tắt",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
}

@Composable
private fun CategoryTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF0284C7) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}
