package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.background
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
import com.vesodo.khaosat.database.SavedSurveyUnitConfig
import com.vesodo.khaosat.drawing.DrawingState
import kotlin.math.roundToInt

@Composable
fun SurveyUnitConfigDialog(
    state: DrawingState,
    onDismiss: () -> Unit
) {
    var maDvi by remember { mutableStateOf(state.unitConfig.maDvi) }
    var tenDvi by remember { mutableStateOf(state.unitConfig.tenDvi) }
    var nguoiKhaoSat by remember { mutableStateOf(state.unitConfig.nguoiKhaoSat) }
    var nguoiKiemTra by remember { mutableStateOf(state.unitConfig.nguoiKiemTra) }
    var scaleVal by remember { mutableFloatStateOf(state.defaultObjectScale) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Badge,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cấu Hình Đơn Vị & Khảo Sát",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Thông tin này được lưu vào cơ sở dữ liệu để tự động điền vào Khung tên bản vẽ CAD A4 và các báo cáo khảo sát.",
                    fontSize = 12.5.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Mã đơn vị quản lý
                OutlinedTextField(
                    value = maDvi,
                    onValueChange = { maDvi = it },
                    label = { Text("Mã đơn vị quản lý (ví dụ: PK, PB01, DVI_01)") },
                    leadingIcon = {
                        Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0284C7))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Tên đơn vị quản lý
                OutlinedTextField(
                    value = tenDvi,
                    onValueChange = { tenDvi = it },
                    label = { Text("Tên đơn vị quản lý (Title Block CAD)") },
                    leadingIcon = {
                        Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0284C7))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Người / KTV Khảo sát
                OutlinedTextField(
                    value = nguoiKhaoSat,
                    onValueChange = { nguoiKhaoSat = it },
                    label = { Text("Kỹ thuật viên khảo sát") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0284C7))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Người kiểm tra
                OutlinedTextField(
                    value = nguoiKiemTra,
                    onValueChange = { nguoiKiemTra = it },
                    label = { Text("Người kiểm tra / Tổ trưởng") },
                    leadingIcon = {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0284C7))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Tỷ lệ Scale mặc định của đối tượng vẽ
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0284C7))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tỷ lệ scale mặc định đối tượng:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "${(scaleVal * 100).roundToInt()}% (${((scaleVal * 100).roundToInt() / 100f)}x)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = scaleVal,
                            onValueChange = { scaleVal = (it * 100).roundToInt() / 100f },
                            valueRange = 0.5f..2.5f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("50%", fontSize = 10.5.sp, color = Color.Gray)
                            TextButton(
                                onClick = { scaleVal = 1.0f },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Đặt về 100% (Chuẩn)", fontSize = 11.5.sp, color = Color(0xFF0284C7))
                            }
                            Text("250%", fontSize = 10.5.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newConfig = SavedSurveyUnitConfig(
                        maDvi = maDvi.trim(),
                        tenDvi = tenDvi.trim().ifBlank { "ĐIỆN LỰC..." },
                        nguoiKhaoSat = nguoiKhaoSat.trim().ifBlank { "KTV Khảo sát" },
                        nguoiKiemTra = nguoiKiemTra.trim().ifBlank { "Tổ trưởng" }
                    )
                    state.updateUnitConfig(newConfig)
                    state.updateDefaultObjectScale(scaleVal)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Lưu & Áp dụng")
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
