package com.vesodo.khaosat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.config.AppConfig
import com.vesodo.khaosat.debug.DebugLogger
import com.vesodo.khaosat.debug.LogEntry
import kotlinx.coroutines.launch

@Composable
fun DebugLogPanel(
    modifier: Modifier = Modifier
) {
    // Nếu chế độ TEST = false (Production), tuyệt đối không render bất kỳ thành phần nào
    if (!AppConfig.isTEST) return

    var isExpanded by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Tự động cuộn xuống dưới cùng khi có log mới nếu auto-scroll bật
    LaunchedEffect(DebugLogger.logs.size) {
        if (DebugLogger.isAutoScroll && DebugLogger.logs.isNotEmpty() && isExpanded) {
            scope.launch {
                listState.animateScrollToItem(DebugLogger.logs.size - 1)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header thanh tiêu đề Debug Log
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DEBUG LOG THAO TÁC KÉO THẢ & CHỌN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    // Toạ độ con trỏ thời gian thực
                    Surface(
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Con trỏ: (${DebugLogger.liveCursorX.toInt()}, ${DebugLogger.liveCursorY.toInt()})",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (DebugLogger.liveMovingItemDesc.isNotBlank()) {
                        Surface(
                            color = Color(0xFF065F46),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Đang kéo: ${DebugLogger.liveMovingItemDesc}",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Nút bật/tắt Tự cuộn
                    TextButton(
                        onClick = { DebugLogger.isAutoScroll = !DebugLogger.isAutoScroll },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = if (DebugLogger.isAutoScroll) "Tự cuộn: BẬT" else "Tự cuộn: TẮT",
                            color = if (DebugLogger.isAutoScroll) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    // Nút Xóa log
                    IconButton(
                        onClick = { DebugLogger.clear() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Xóa log",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Nút Thu gọn / Mở rộng
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Nội dung danh sách Logs (khi mở rộng)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (DebugLogger.logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có thao tác nào. Hãy click vào đối tượng hoặc kéo thả trên màn hình vẽ để theo dõi log.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(DebugLogger.logs, key = { it.id }) { entry ->
                            LogItemRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(entry: LogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Thời gian
        Text(
            text = entry.timeStr,
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )

        // Badge loại thao tác
        Surface(
            color = Color(entry.type.badgeColorHex),
            shape = RoundedCornerShape(3.dp)
        ) {
            Text(
                text = entry.type.label,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        // Tiêu đề thao tác
        Text(
            text = entry.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Toạ độ
        if (entry.coords.isNotBlank()) {
            Text(
                text = entry.coords,
                color = Color(0xFF38BDF8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
