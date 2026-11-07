package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.debug.DebugLogger
import com.vesodo.khaosat.debug.LogType
import com.vesodo.khaosat.drawing.DrawToolItem
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.drawing.ToolCategory
import com.vesodo.khaosat.model.Variables
import org.jetbrains.compose.resources.painterResource

@Composable
fun BottomPaletteBar(
    state: DrawingState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val currentTools = remember(state.selectedCategory, state.toolUsageCounts.toMap()) {
        when (state.selectedCategory) {
            ToolCategory.POPULAR -> {
                val used = IconRegistry.ALL_TOOLS
                    .filter { state.getToolUsageCount(it.command) > 0 }
                    .distinctBy { it.command }
                    .sortedByDescending { state.getToolUsageCount(it.command) }
                if (used.isNotEmpty()) used else IconRegistry.POPULAR_TOOLS
            }
            ToolCategory.TRAM -> IconRegistry.TRAM_TOOLS.sortedWith(
                compareByDescending<DrawToolItem> { state.getToolUsageCount(it.command) }
                    .thenBy { it.command }
            )
            ToolCategory.TRU -> IconRegistry.TRU_TOOLS.sortedWith(
                compareByDescending<DrawToolItem> { state.getToolUsageCount(it.command) }
                    .thenBy { it.command }
            )
            ToolCategory.KHAC -> IconRegistry.KHAC_TOOLS.sortedWith(
                compareByDescending<DrawToolItem> { state.getToolUsageCount(it.command) }
                    .thenBy { it.command }
            )
            ToolCategory.LINE -> IconRegistry.LINE_TOOLS.sortedWith(
                compareByDescending<DrawToolItem> { state.getToolUsageCount(it.command) }
                    .thenBy { it.command }
            )
        }
    }

    Surface(
        color = Color(0xFF0F172A), // Slate 900
        contentColor = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hiển thị tên danh mục đang mở
            val catColor = if (state.selectedCategory == ToolCategory.POPULAR) Color(0xFFF59E0B) else Color(0xFF38BDF8)
            Text(
                text = if (state.selectedCategory == ToolCategory.POPULAR) "★ ${state.selectedCategory.title}:" else "${state.selectedCategory.title}:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = catColor,
                modifier = Modifier.padding(end = 4.dp)
            )

            currentTools.forEach { tool ->
                val isSelected = (state.currentCommand == tool.command)
                PaletteItemButton(
                    tool = tool,
                    isSelected = isSelected,
                    state = state,
                    scrollState = scrollState,
                    onClick = {
                        state.currentCommand = tool.command
                        state.selectedItem = null
                        if (tool.command == Variables.VE_TEXT) {
                            state.showNoteDialog = true
                        }
                        DebugLogger.log(
                            LogType.SELECT,
                            "Chọn công cụ: ${tool.name}",
                            "Chạm vào bản vẽ để vẽ ngay hoặc kéo thả ra ngoài"
                        )
                    }
                )
            }
        }
    }
}


@Composable
private fun PaletteItemButton(
    tool: DrawToolItem,
    isSelected: Boolean,
    state: DrawingState,
    scrollState: ScrollState,
    onClick: () -> Unit
) {
    var buttonRootPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color.White else Color(0xFF334155),
                shape = RoundedCornerShape(8.dp)
            )
            .onGloballyPositioned { coords ->
                buttonRootPosition = coords.positionInRoot()
            }
            .pointerInput(tool) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isDragging = false
                    var isScrolling = false
                    val downPosition = down.position
                    var previousPos = down.position

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                val upRootPos = buttonRootPosition + change.position
                                if (isDragging) {
                                    state.dropPaletteToolAt(upRootPos)
                                } else if (!isScrolling) {
                                    onClick()
                                }
                                break
                            }

                            val currentPos = change.position
                            val currentRootPos = buttonRootPosition + currentPos
                            val deltaX = currentPos.x - previousPos.x
                            val totalDx = currentPos.x - downPosition.x
                            val totalDy = currentPos.y - downPosition.y
                            val dragDistSquared = totalDx * totalDx + totalDy * totalDy

                            if (!isDragging && !isScrolling) {
                                if (dragDistSquared > 64f) { // Di chuyển > 8px
                                    val absDx = kotlin.math.abs(totalDx)
                                    val absDy = kotlin.math.abs(totalDy)

                                    if (absDx > absDy * 1.2f) {
                                        // Vuốt ngang chiếm ưu thế -> Chế độ cuộn danh sách (không kéo thả tool)
                                        isScrolling = true
                                    } else if (totalDy < -6f) {
                                        // Kéo hướng lên trên về phía bảng vẽ -> Kéo thả công cụ ra bản vẽ
                                        isDragging = true
                                        state.draggingPaletteTool = tool
                                        state.draggingPaletteOffset = currentRootPos
                                        DebugLogger.log(
                                            LogType.DRAG_START,
                                            "Kéo công cụ: ${tool.name}",
                                            "Bắt đầu kéo thả ra ngoài bản vẽ..."
                                        )
                                    }
                                }
                            }

                            if (isDragging) {
                                change.consume()
                                state.draggingPaletteOffset = currentRootPos
                                DebugLogger.liveCursorX = currentRootPos.x
                                DebugLogger.liveCursorY = currentRootPos.y
                            } else if (isScrolling) {
                                // Nếu là thao tác chuột trên Desktop thì cuộn trực tiếp theo khoảng kéo chuột
                                if (change.type == PointerType.Mouse) {
                                    scrollState.dispatchRawDelta(-deltaX)
                                    change.consume()
                                }
                                // Nếu là Touch thì không consume để Row.horizontalScroll xử lý cuộn tự nhiên và hỗ trợ fling
                            }

                            previousPos = currentPos
                        }
                    } finally {
                        if (isDragging && state.draggingPaletteTool != null) {
                            state.draggingPaletteTool = null
                            state.draggingPaletteOffset = null
                        }
                    }
                }
            }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(2.dp)
        ) {
            val usageCount = state.getToolUsageCount(tool.command)
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isSelected) Color.White else Color(0xFFF1F5F9))
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(tool.iconResource),
                        contentDescription = tool.name,
                        modifier = Modifier.size(26.dp)
                    )
                }

                if (usageCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEA580C))
                            .padding(horizontal = 4.dp, vertical = 0.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (usageCount > 99) "99+" else "$usageCount",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = tool.shortName,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFFF8FAFC),
                maxLines = 1
            )
        }
    }
}
