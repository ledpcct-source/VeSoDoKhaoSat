package com.vesodo.khaosat.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.database.PlatformTemplateStorage
import com.vesodo.khaosat.drawing.DrawingState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Nút Floating Action Button (FAB) Lịch sử vẽ:
 * - Có thể kéo thả (Drag & Drop) tự do trên màn hình để không che khuất mã QR khảo sát hay bản vẽ.
 * - Tự động ghi nhớ và lưu lại vị trí sau mỗi lần kéo vào bộ nhớ thiết bị (SQLite) để lần sau mở lại đúng vị trí đó.
 * - Chạm nhẹ (Tap/Click) để mở / đóng Drawer danh sách lịch sử các thao tác vẽ.
 */
@Composable
fun DraggableHistoryFab(
    state: DrawingState,
    containerWidthDp: Dp,
    containerHeightDp: Dp,
    onToggleHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val touchSlop = viewConfiguration.touchSlop
    val coroutineScope = rememberCoroutineScope()

    val fabSizeDp = 46.dp
    val fabSizePx = with(density) { fabSizeDp.toPx() }
    val containerWidthPx = with(density) { containerWidthDp.toPx() }
    val containerHeightPx = with(density) { containerHeightDp.toPx() }

    // Vị trí mặc định an toàn: Bên phải, cách cạnh 14dp, cách đỉnh 135dp (nằm phía dưới mã QR tránh đè lên)
    val defaultMarginEndPx = with(density) { 14.dp.toPx() }
    val defaultTopPx = with(density) { 135.dp.toPx() }
    val defaultX = maxOf(0f, containerWidthPx - fabSizePx - defaultMarginEndPx)
    val defaultY = minOf(maxOf(0f, containerHeightPx - fabSizePx), defaultTopPx)

    var offsetX by remember { mutableFloatStateOf(defaultX) }
    var offsetY by remember { mutableFloatStateOf(defaultY) }
    var isDragging by remember { mutableStateOf(false) }

    // Đồng bộ toạ độ từ state.historyFabPosition (được nạp từ DB) khi kích thước màn hình thay đổi
    LaunchedEffect(state.historyFabPosition, containerWidthPx, containerHeightPx) {
        if (!isDragging && containerWidthPx > fabSizePx && containerHeightPx > fabSizePx) {
            val savedPos = state.historyFabPosition
            val maxX = maxOf(0f, containerWidthPx - fabSizePx)
            val maxY = maxOf(0f, containerHeightPx - fabSizePx)
            if (savedPos != null) {
                offsetX = (savedPos.first * maxX).coerceIn(0f, maxX)
                offsetY = (savedPos.second * maxY).coerceIn(0f, maxY)
            } else {
                offsetX = defaultX.coerceIn(0f, maxX)
                offsetY = defaultY.coerceIn(0f, maxY)
            }
        }
    }

    val elevation by animateDpAsState(targetValue = if (isDragging) 14.dp else 6.dp)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(fabSizeDp)
            .pointerInput(containerWidthPx, containerHeightPx, fabSizePx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var hasDragged = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            // Nhấc ngón tay lên
                            if (!hasDragged) {
                                // Người dùng chỉ Click/Tap -> Chuyển trạng thái bật/tắt Drawer Lịch sử
                                change.consume()
                                onToggleHistory()
                            } else {
                                // Người dùng vừa kéo xong -> Lưu vị trí nút theo tỉ lệ tương đối vào CSDL
                                change.consume()
                                isDragging = false
                                val maxX = maxOf(1f, containerWidthPx - fabSizePx)
                                val maxY = maxOf(1f, containerHeightPx - fabSizePx)
                                val xRatio = (offsetX / maxX).coerceIn(0f, 1f)
                                val yRatio = (offsetY / maxY).coerceIn(0f, 1f)
                                state.historyFabPosition = Pair(xRatio, yRatio)
                                coroutineScope.launch {
                                    PlatformTemplateStorage.saveHistoryFabPosition(xRatio, yRatio)
                                }
                            }
                            break
                        }

                        val dragVector = change.position - down.position
                        val dragDistance = sqrt(dragVector.x * dragVector.x + dragVector.y * dragVector.y)
                        if (!hasDragged && dragDistance > touchSlop) {
                            hasDragged = true
                            isDragging = true
                        }

                        if (hasDragged) {
                            change.consume()
                            val prev = change.previousPosition
                            val curr = change.position
                            val dx = curr.x - prev.x
                            val dy = curr.y - prev.y
                            val maxX = maxOf(0f, containerWidthPx - fabSizePx)
                            val maxY = maxOf(0f, containerHeightPx - fabSizePx)
                            offsetX = (offsetX + dx).coerceIn(0f, maxX)
                            offsetY = (offsetY + dy).coerceIn(0f, maxY)
                        }
                    }
                    isDragging = false
                }
            }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = if (isDragging) {
                Color(0xFF0369A1)
            } else if (state.showHistoryDrawer) {
                Color(0xFF0284C7)
            } else {
                Color(0xFF0F172A).copy(alpha = 0.92f)
            },
            border = BorderStroke(
                width = if (isDragging) 1.5.dp else 1.dp,
                color = if (isDragging) Color(0xFF38BDF8) else Color(0xFF38BDF8).copy(alpha = 0.5f)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                BadgedBox(
                    badge = {
                        if (state.listCanvas.isNotEmpty()) {
                            Badge(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White,
                                modifier = Modifier.offset(x = 6.dp, y = (-6).dp)
                            ) {
                                Text(
                                    text = state.listCanvas.size.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Lịch sử vẽ (Kéo để di chuyển)",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
