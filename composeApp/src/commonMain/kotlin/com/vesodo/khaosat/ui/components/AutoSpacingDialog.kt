package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.math.Point2D
import com.vesodo.khaosat.model.Variables

data class PoleOption(val name: String, val command: Int)

val DEFAULT_POLE_OPTIONS = listOf(
    PoleOption("Trụ BTLT 10.5m", Variables.VE_TRU_BTLT_105_HHUU),
    PoleOption("Trụ BTLT 12m", Variables.VE_TRU_BTLT_12_HHUU),
    PoleOption("Trụ BTV 6.5m", Variables.VE_TRU_BTV_65_HHUU),
    PoleOption("Trụ BTV 7.5m", Variables.VE_TRU_BTV_75_HHUU),
    PoleOption("Trụ BTV 8.5m", Variables.VE_TRU_BTV_85_HHUU),
    PoleOption("Trụ BTLT 8.5m", Variables.VE_TRU_BTLT_85_HHUU)
)

data class WireOption(val name: String, val command: Int)

val DEFAULT_WIRE_OPTIONS = listOf(
    WireOption("Đường dây liền nét (Mặc định)", Variables.VE_DUONG_THANG),
    WireOption("Đường dây nét đứt (Dây nhánh vào nhà)", Variables.VE_LINE_DUT_NET),
    WireOption("Dây đôi (2Line)", Variables.VE_2LINE)
)

@Composable
fun AutoSpacingDialog(
    onDismiss: () -> Unit,
    onConfirmConfig: (targetSpanMeters: Double, poleCmd: Int, wireCmd: Int, polePrefix: String) -> Unit
) {
    var selectedSpanMeters by remember { mutableStateOf(35.0) }
    var selectedPoleCmd by remember { mutableStateOf(Variables.VE_TRU_BTLT_105_HHUU) }
    var selectedWireCmd by remember { mutableStateOf(Variables.VE_DUONG_THANG) }
    var polePrefix by remember { mutableStateOf("Trụ") }

    val presetSpans = listOf(30.0, 35.0, 40.0, 50.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Polyline,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = "Rải Cột Tự Động Theo Nhịp (Auto-Span)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Tự động phân chia nhịp dây và cắm trụ chuẩn EVN",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Chọn cự ly nhịp
                Column {
                    Text(
                        text = "Cự ly nhịp tiêu chuẩn (Mét):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetSpans.forEach { span ->
                            val isSelected = selectedSpanMeters == span
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSpanMeters = span },
                                label = { Text("${span.toInt()}m", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        text = "Quy chuẩn EVN: Khuyến nghị 35m đối với khu dân cư, 45-50m đối với đồng ruộng.",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Chọn loại trụ
                Column {
                    Text(
                        text = "Loại trụ điện chèn tự động:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DEFAULT_POLE_OPTIONS.take(4).forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPoleCmd = option.command }
                                .padding(vertical = 3.dp)
                        ) {
                            RadioButton(
                                selected = selectedPoleCmd == option.command,
                                onClick = { selectedPoleCmd = option.command }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(option.name, fontSize = 13.sp)
                        }
                    }
                }

                // Tiền tố tên trụ
                OutlinedTextField(
                    value = polePrefix,
                    onValueChange = { polePrefix = it },
                    label = { Text("Tên định danh trụ (Ví dụ: Trụ, Cột, P)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Hướng dẫn thao tác
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LinearScale,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sau khi bấm kích hoạt, hãy bấm kéo đường dây từ điểm đầu tới điểm cuối trên màn hình. AI sẽ tự động tính toán số cột và sinh toàn bộ tuyến dây đều nhau.",
                            fontSize = 11.sp,
                            color = Color(0xFF0369A1),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmConfig(selectedSpanMeters, selectedPoleCmd, selectedWireCmd, polePrefix.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Kích hoạt chế độ rải cột")
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
