package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.drawing.DrawingState
import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.model.Obj_CANVAS
import org.jetbrains.compose.resources.painterResource

@Composable
fun HistorySidebar(
    state: DrawingState,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    Surface(
        color = Color(0xFFF8FAFC), // Slate 50
        contentColor = Color(0xFF0F172A),
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = 280.dp, max = 320.dp)
            .border(width = 1.dp, color = Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Tiêu đề + Nút Xóa hết + Nút Đóng (khi ở chế độ Drawer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lịch sử (${state.listCanvas.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { state.showConfirmClearDialog = true },
                        enabled = state.listCanvas.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), // Red
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Xoá hết", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng lịch sử",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Danh sách các đối tượng đã vẽ
            if (state.listCanvas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sơ đồ rỗng\nChạm vẽ hoặc nạp mẫu",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(state.listCanvas) { index, item ->
                        HistoryRowItem(
                            index = index + 1,
                            item = item,
                            isSelected = (state.selectedItem == item),
                            onSelect = { state.selectItem(item) },
                            onEdit = {
                                state.editingItemForNote = item
                                state.showNoteDialog = true
                            },
                            onDelete = { state.removeItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRowItem(
    index: Int,
    item: Obj_CANVAS,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tool = IconRegistry.findTool(item.LENH)
    val displayName = if (item.TEN.isNotBlank()) item.TEN else (tool?.name ?: "Nét vẽ #${index}")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect),
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon đối tượng
            if (tool != null) {
                Image(
                    painter = painterResource(tool.iconResource),
                    contentDescription = tool.name,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.Gesture,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Tên và thông số
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A),
                    maxLines = 1
                )
                Text(
                    text = "X:${item.X3.toInt()} Y:${item.Y3.toInt()}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // Nút sửa ghi chú
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Sửa",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Nút xóa
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Xóa",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

