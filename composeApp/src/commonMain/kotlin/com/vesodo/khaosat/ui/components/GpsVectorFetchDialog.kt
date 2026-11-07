package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.map.FetchStatus
import com.vesodo.khaosat.map.GpsCoordinate
import com.vesodo.khaosat.map.GpsVectorRoadManager
import com.vesodo.khaosat.map.VectorFetchReport
import com.vesodo.khaosat.map.VectorRoad
import kotlinx.coroutines.launch

/**
 * Hộp thoại tiến trình kết nối & tải dữ liệu vector từ OpenStreetMap Overpass API
 * Cho phép kỹ sư theo dõi trực quan trạng thái kết nối, số lượng đường và tên đường tìm thấy.
 */
@Composable
fun GpsVectorFetchDialog(
    centerGps: GpsCoordinate,
    zoom: Int,
    onDismiss: () -> Unit,
    onApplyRoads: (List<VectorRoad>, Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentZoom by remember { mutableStateOf(zoom) }
    var report by remember {
        mutableStateOf(
            VectorFetchReport(
                status = FetchStatus.CONNECTING,
                message = "Đang chuẩn bị gửi truy vấn OpenStreetMap..."
            )
        )
    }

    fun startFetch(targetZoom: Int) {
        currentZoom = targetZoom
        coroutineScope.launch {
            report = VectorFetchReport(
                status = FetchStatus.CONNECTING,
                message = "Đang kết nối máy chủ OpenStreetMap..."
            )
            report = GpsVectorRoadManager.fetchVectorRoadsWithReport(
                centerGps = centerGps,
                zoom = targetZoom,
                forceRefresh = true,
                onProgress = { update ->
                    report = update
                }
            )
        }
    }

    LaunchedEffect(centerGps, zoom) {
        startFetch(zoom)
    }

    // Modal Card giữa màn hình
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 450.dp, max = 560.dp)
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 1. HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AltRoute,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "KẾT NỐI TẢI VECTOR ĐƯỜNG",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "OpenStreetMap Overpass API",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. THÔNG TIN TOẠ ĐỘ & MỨC ZOOM
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tâm khảo sát:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "(${centerGps.latitude.toString().take(8)}, ${centerGps.longitude.toString().take(9)})",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Mức Zoom khảo sát:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                            val radiusDesc = when {
                                currentZoom >= 20 -> "20x (Bán kính ~280m)"
                                currentZoom == 19 -> "19x (Bán kính ~390m)"
                                currentZoom == 18 -> "18x (Bán kính ~550m)"
                                else -> "${currentZoom}x (Bán kính ~780m)"
                            }
                            Text(
                                text = radiusDesc,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            )
                        }

                        if (report.endpoint.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Máy chủ API:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp
                                )
                                Text(
                                    text = report.endpoint.replace("https://", "").replace("/api/interpreter", ""),
                                    color = Color(0xFFE2E8F0),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. KHỐI TRẠNG THÁI KẾT NỐI
                when (report.status) {
                    FetchStatus.CONNECTING -> {
                        Surface(
                            color = Color(0xFF0369A1).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 2.5.dp
                                )
                                Column {
                                    Text(
                                        text = "Đang kết nối & truy vấn dữ liệu...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                    Text(
                                        text = report.message,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    FetchStatus.SUCCESS -> {
                        Surface(
                            color = Color(0xFF065F46).copy(alpha = 0.20f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "Đã tìm thấy ${report.roads.size} tuyến đường!",
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Text(
                                    text = "Danh sách các tuyến đường tìm thấy quanh toạ độ:",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )

                                // Danh sách cuộn xem trước các đường
                                Surface(
                                    color = Color(0xFF020617),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 140.dp)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(report.roads) { road ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val displayName = road.name.ifBlank { "Đường nội bộ / chưa đặt tên" }
                                                Text(
                                                    text = "• $displayName",
                                                    color = if (road.name.isNotBlank()) Color.White else Color(0xFF94A3B8),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (road.name.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                                    maxLines = 1
                                                )
                                                Surface(
                                                    color = Color(0xFF1E293B),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = "${road.roadType} (${road.points.size} pts)",
                                                        color = Color(0xFF38BDF8),
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FetchStatus.EMPTY -> {
                        Surface(
                            color = Color(0xFF78350F).copy(alpha = 0.20f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Không có tuyến đường trong phạm vi này",
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                                Text(
                                    text = "Ở mức zoom ${currentZoom}x, bán kính quét rất hẹp. Bạn nên mở rộng bán kính bằng cách thử lại ở Zoom 18x hoặc 19x.",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    FetchStatus.ERROR -> {
                        Surface(
                            color = Color(0xFF7F1D1D).copy(alpha = 0.20f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Không thể kết nối máy chủ Overpass API",
                                        color = Color(0xFFF87171),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                                Text(
                                    text = report.message,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. HÀNG NÚT BẤM HÀNH ĐỘNG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (report.status == FetchStatus.EMPTY) {
                        OutlinedButton(
                            onClick = { startFetch(18) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thử quét ở Zoom 18x", fontSize = 11.5.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (report.status == FetchStatus.ERROR) {
                        OutlinedButton(
                            onClick = { startFetch(currentZoom) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thử lại", fontSize = 11.5.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (report.status == FetchStatus.SUCCESS) {
                        Button(
                            onClick = {
                                onApplyRoads(report.roads, currentZoom)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Áp dụng vào Bản vẽ CAD (${report.roads.size} đường)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF475569))
                    ) {
                        Text(if (report.status == FetchStatus.SUCCESS) "Để sau" else "Đóng", fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}
