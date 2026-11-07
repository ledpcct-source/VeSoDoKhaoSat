package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.ai.DiagramRuleEngine
import com.vesodo.khaosat.drawing.IconRegistry
import com.vesodo.khaosat.model.Length_text
import com.vesodo.khaosat.model.Obj_CANVAS

@Composable
fun NoteEditDialog(
    item: Obj_CANVAS,
    allCanvasItems: List<Obj_CANVAS> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(item.TEN) }

    val isPole = remember(item) {
        IconRegistry.isTru(item.LENH) || DiagramRuleEngine.isPoleItem(item)
    }

    val adjacentSuggestions = remember(item, allCanvasItems) {
        if (isPole) {
            DiagramRuleEngine.suggestAdjacentPoleNames(item, allCanvasItems)
        } else {
            emptyList()
        }
    }

    val quickSuggestions = remember {
        listOf(
            "Trụ 1", "Trụ 2", "Trụ 3", "Trụ 4",
            "Trạm TBA", "Nhà KH", "Công tơ",
            "10m", "15m", "20m", "25m", "30m", "50m",
            "Qua lộ", "Băng rạch", "Đất trống", "Dây đôi"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPole) "Đặt tên Trụ điện" else "Nhập ghi chú / Tên đối tượng",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        if (it.length <= Length_text.ML_TEN) {
                            textValue = it
                        }
                    },
                    label = { Text(if (isPole) "Tên trụ (VD: Trụ 1, Trụ 1/1...)" else "Nội dung ghi chú / khoảng cách / tên") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // 1. GỢI Ý THÔNG MINH DỰA THEO TRỤ LIỀN KỀ
                if (adjacentSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "💡 Đề xuất theo trụ liền kề / tuyến:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(adjacentSuggestions) { sug ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    textValue = sug.name
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = sug.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = sug.explanation,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Gợi ý phổ biến:",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickSuggestions) { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { textValue = suggestion }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(textValue) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
