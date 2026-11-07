package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vesodo.khaosat.ai.CustomerSide
import com.vesodo.khaosat.model.Obj_CANVAS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAttachmentDialog(
    parentItem: Obj_CANVAS,
    initialSide: CustomerSide,
    onDismiss: () -> Unit,
    onConfirm: (side: CustomerSide, customerName: String, customerCode: String) -> Unit
) {
    var side by remember { mutableStateOf(initialSide) }
    var customerName by remember { mutableStateOf("Nguyễn Văn An") }
    var customerCode by remember { mutableStateOf("") }

    val hasCode = customerCode.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 520.dp)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.2.dp, Color(0xFF818CF8).copy(alpha = 0.8f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF4338CA).copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFF818CF8))
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Thêm Khách hàng (${side.label})",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Điểm gắn đồng hồ: ${parentItem.TEN.ifBlank { "Thiết bị #${parentItem.STT}" }}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lựa chọn vị trí: Bên trái hoặc Bên phải
                Text(
                    text = "Vị trí khách hàng so với điểm đồng hồ:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = (side == CustomerSide.LEFT),
                        onClick = { side = CustomerSide.LEFT },
                        label = { Text("👈 Khách hàng Bên Trái", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = (side == CustomerSide.RIGHT),
                        onClick = { side = CustomerSide.RIGHT },
                        label = { Text("👉 Khách hàng Bên Phải", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nhập Mã khách hàng (Không bắt buộc ràng buộc)
                OutlinedTextField(
                    value = customerCode,
                    onValueChange = { customerCode = it },
                    label = { Text("Mã khách hàng (Không bắt buộc)", fontSize = 11.5.sp) },
                    placeholder = { Text("Ví dụ: PD01000123456 hoặc để trống", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE2E8F0),
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Nhập Tên khách hàng
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Tên khách hàng", fontSize = 11.5.sp) },
                    placeholder = { Text("Ví dụ: Nguyễn Văn An", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE2E8F0),
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Panel AI Kiểm duyệt cảnh báo (Nhắc nhở, không ràng buộc)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (hasCode) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFF78350F).copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (hasCode) Color(0xFF059669) else Color(0xFFD97706))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (hasCode) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (hasCode) Color(0xFF34D399) else Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (hasCode) "AI Kiểm duyệt: Đã có mã khách hàng hợp lệ" else "AI Kiểm duyệt: Chưa có mã khách hàng (Không ràng buộc)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasCode) Color(0xFF6EE7B7) else Color(0xFFFDE68A)
                            )
                            Text(
                                text = if (hasCode)
                                    "Mã '${customerCode.trim()}' sẽ được hiển thị cùng tên và ghi chú vị trí (${side.directionText})."
                                else
                                    "Không có mã: Nhãn sẽ chỉ hiển thị dòng Tên kèm ghi chú vị trí (${side.directionText}), không hiển thị chữ chưa có mã để bản vẽ luôn tinh gọn.",
                                fontSize = 10.5.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Xem trước nhãn hiển thị trên bản vẽ
                val previewSideNote = if (side == CustomerSide.LEFT) "KH Trái" else "KH Phải"
                val previewRawName = customerName.trim()
                val previewName = if (previewRawName.isBlank()) "Khách hàng" else previewRawName
                val previewNameWithSide = if (previewName.contains("trái", ignoreCase = true) || previewName.contains("phải", ignoreCase = true)) {
                    previewName
                } else {
                    "$previewName ($previewSideNote)"
                }
                val previewContent = if (hasCode) {
                    "Mã: ${customerCode.trim()}\nTên: $previewNameWithSide"
                } else {
                    "Tên: $previewNameWithSide"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Xem trước nhãn text trên bản vẽ:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.95f),
                            border = BorderStroke(1.2.dp, Color(0xFF2563EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = previewContent,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF94A3B8)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF475569))
                    ) {
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            onConfirm(
                                side,
                                customerName.trim().ifBlank { "Khách hàng" },
                                customerCode.trim()
                            )
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Thêm Khách Hàng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}
