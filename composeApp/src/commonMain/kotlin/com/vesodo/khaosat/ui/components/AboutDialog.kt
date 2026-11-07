package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.painterResource
import vesodokhaosat.composeapp.generated.resources.Res
import vesodokhaosat.composeapp.generated.resources.app_icon

/**
 * ==============================================================================================
 * HỘP THOẠI THÔNG TIN ỨNG DỤNG, BẢN QUYỀN PHẦN MỀM, QUYỀN TRUY CẬP VÀ BẢN QUYỀN BẢN ĐỒ (ABOUT)
 * ==============================================================================================
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A), // Slate 900
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- 1. HEADER (ICON, TÊN ỨNG DỤNG, PHIÊN BẢN) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B)) // Slate 800
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.app_icon),
                                contentDescription = "Logo VeSoDoKhaoSat",
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "VẼ SƠ ĐỒ KHẢO SÁT",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    color = Color(0xFF0284C7),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "v1.0.0",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Phân hệ khảo sát lưới điện phục vụ cấp điện khách hàng EVN",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                // --- 2. TAB NAVIGATION (3 MỤC: THÔNG TIN, BẢN QUYỀN BẢN ĐỒ, QUYỀN HẠN) ---
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    divider = { HorizontalDivider(color = Color(0xFF334155)) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Phần mềm", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Bản đồ & Dữ liệu", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Quyền ứng dụng", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                // --- 3. NỘI DUNG CUỘN CHI TIẾT ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedTab) {
                            0 -> TabSoftwareInfo()
                            1 -> TabMapLicenses()
                            2 -> TabPermissionsExplanation()
                        }
                    }
                }

                // --- 4. FOOTER ---
                HorizontalDivider(color = Color(0xFF334155))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "© 2026 VeSoDo • Hệ sinh thái GPSGO. Mọi quyền được bảo lưu.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Đóng", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * TAB 1: THÔNG TIN PHẦN MỀM & BẢN QUYỀN TÁC QUYỀN
 */
@Composable
private fun TabSoftwareInfo() {
    InfoCard(
        icon = Icons.Default.ElectricBolt,
        iconTint = Color(0xFFF59E0B),
        title = "Mục Đích Ứng Dụng",
        content = "Phần mềm chuyên dụng cho cán bộ, kỹ sư khảo sát cấp điện thuộc các đơn vị Điện lực (EVN):\n" +
                "• Vẽ nhanh sơ đồ cấp điện hiện trường với hơn 68+ biểu tượng chuẩn ngành Điện.\n" +
                "• Rải cột tự động, chèn trụ đệm thông minh và tính toán chiều dài dây nối thực địa kèm hệ số võng 1.2.\n" +
                "• Kiểm duyệt bản vẽ tự động theo quy chuẩn EVN (AI Diagram Rule Engine).\n" +
                "• Đóng gói hồ sơ kỹ thuật, xuất bản vẽ AutoCAD DXF (R12/2000), hình ảnh JPG độ phân giải cao và tài liệu PDF khổ A4."
    )

    InfoCard(
        icon = Icons.Default.Devices,
        iconTint = Color(0xFF38BDF8),
        title = "Nền Tảng Công Nghệ",
        content = "• Công nghệ: Jetpack Compose Multiplatform (Kotlin).\n" +
                "• Hỗ trợ đa nền tảng: Máy tính bảng Android (Tablet), Điện thoại Android và Máy tính để bàn (Windows, macOS, Linux).\n" +
                "• Tương thích 100% với hệ sinh thái ứng dụng bản đồ hiện trường GPSGO."
    )

    InfoCard(
        icon = Icons.Default.Copyright,
        iconTint = Color(0xFF10B981),
        title = "Bản Quyền & Tác Quyền Phần Mềm",
        content = "• Tên sản phẩm: VeSoDoKhaoSat (Vẽ Sơ Đồ Khảo Sát Lưới Điện)\n" +
                "• Bản quyền tác giả: © 2026 Thiện Trần • Phòng KHCNSCĐS PC Cần Thơ.\n" +
                "• Mọi quyền được bảo lưu (All Rights Reserved). Nghiêm cấm mọi hành vi sao chép, dịch ngược mã nguồn trái phép khi chưa có sự đồng ý bằng văn bản của đơn vị phát triển."
    )
}

/**
 * TAB 2: DIỄN GIẢI BẢN QUYỀN BẢN ĐỒ & DỮ LIỆU ĐỊA LÝ
 */
@Composable
private fun TabMapLicenses() {
    InfoCard(
        icon = Icons.Default.Map,
        iconTint = Color(0xFF0284C7),
        title = "OpenStreetMap (OSM) — Bản Quyền & Giấy Phép",
        content = "• Dữ liệu bản đồ nền và hình ảnh ô gạch (Tiles) được cung cấp bởi Tổ chức OpenStreetMap (OSMF) và cộng đồng đóng góp toàn cầu.\n" +
                "• Dòng ghi nhận bản quyền (Credit): © OpenStreetMap contributors\n" +
                "• Giấy phép phân phối: Open Data Commons Open Database License (ODbL 1.0).\n" +
                "• Hình ảnh gạch bản đồ tuân thủ giấy phép Creative Commons Attribution-ShareAlike 2.0 (CC BY-SA 2.0).\n" +
                "• Truy vấn vector đường sá: Sử dụng OpenStreetMap Overpass API tuân thủ nghiêm ngặt chính sách Tile & API Usage Policy."
    )

    InfoCard(
        icon = Icons.Default.Public,
        iconTint = Color(0xFF8B5CF6),
        title = "ESRI ArcGIS — Nguồn Bản Đồ Số GIS",
        content = "• Máy chủ bản đồ: World Street Map & World Light Gray Base của Esri (Environmental Systems Research Institute, Inc.).\n" +
                "• Dòng ghi nhận bản quyền (Attribution): Tiles © Esri — Source: Esri, DeLorme, USGS, NPS, NRCAN, GeoBase, IGN, Kadaster NL, Ordnance Survey, Esri Japan, METI, Esri China và Cộng đồng Người dùng GIS toàn cầu.\n" +
                "• Được sử dụng theo điều khoản dịch vụ Esri Master License Agreement phục vụ hiển thị bản đồ nền vị trí kỹ thuật."
    )

    InfoCard(
        icon = Icons.Default.Layers,
        iconTint = Color(0xFFEC4899),
        title = "CARTO Positron",
        content = "• Bản đồ phong cách tối giản CARTO Positron dựa trên dữ liệu OpenStreetMap.\n" +
                "• Dòng ghi nhận: © OpenStreetMap contributors, © CARTO."
    )
}

/**
 * TAB 3: GIẢI THÍCH CÁC QUYỀN ỨNG DỤNG (PERMISSIONS EXPLANATION)
 */
@Composable
private fun TabPermissionsExplanation() {
    InfoCard(
        icon = Icons.Default.MyLocation,
        iconTint = Color(0xFF0284C7),
        title = "Quyền Định Vị GPS (ACCESS_FINE_LOCATION & ACCESS_COARSE_LOCATION)",
        content = "• Mục đích: Giúp kỹ thuật viên tại thực địa xác định tức thời vị trí trạm biến áp, móng trụ điện, điểm đo đếm công tơ và tâm khảo sát trên bản đồ số.\n" +
                "• Cam kết minh bạch & quyền riêng tư: Ứng dụng CHỈ truy cập vị trí khi người dùng chủ động bấm nút 'Định vị GPS' hoặc 'Tải toạ độ'. Tuyệt đối KHÔNG thu thập vị trí ngầm, KHÔNG theo dõi hành trình và KHÔNG gửi dữ liệu toạ độ về máy chủ quảng cáo của bên thứ ba."
    )

    InfoCard(
        icon = Icons.Default.Wifi,
        iconTint = Color(0xFF10B981),
        title = "Quyền Kết Nối Mạng (INTERNET & ACCESS_NETWORK_STATE)",
        content = "• Mục đích: Tải các ô gạch bản đồ vệ tinh / đường phố từ máy chủ Esri và OpenStreetMap; tải dữ liệu vector đường sá xung quanh toạ độ khảo sát.\n" +
                "• Hoạt động ngoại tuyến: Khi không có mạng Internet, toàn bộ tính năng vẽ sơ đồ, rải trụ, tính toán khoảng cách và xuất file CAD / PDF / ảnh vẫn hoạt động bình thường 100% (Offline-first)."
    )

    InfoCard(
        icon = Icons.Default.FolderSpecial,
        iconTint = Color(0xFFF59E0B),
        title = "Chính Sách Lưu Trữ Tệp (Storage & Privacy Compliance)",
        content = "• Tuân thủ nghiêm ngặt chính sách bảo mật Google Play Store: Ứng dụng KHÔNG yêu cầu các quyền truy cập tệp nhạy cảm (READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE).\n" +
                "• Xuất hình ảnh & tài liệu trên Android: Lưu trực tiếp vào Thư viện Ảnh (Media Gallery) hoặc chia sẻ an toàn qua Android FileProvider.\n" +
                "• Xuất bản vẽ trên Desktop: Lưu vào thư mục tệp của ứng dụng và tự động mở Windows Explorer để người dùng dễ dàng đính kèm vào AutoCAD."
    )
}

/**
 * Khung hiển thị thông tin dạng Card đẹp mắt
 */
@Composable
private fun InfoCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    content: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B), // Slate 800
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = content,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFFCBD5E1) // Slate 300
            )
        }
    }
}

