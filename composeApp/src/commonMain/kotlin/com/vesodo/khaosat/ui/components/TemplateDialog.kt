package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.database.PlatformTemplateStorage
import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import kotlinx.coroutines.launch

data class DiagramTemplate(
    val id: String,
    val name: String,
    val description: String,
    val items: List<Obj_CANVAS>,
    val isCustom: Boolean = false
)

object DefaultTemplates {
    val list: List<DiagramTemplate> = listOf(
        DiagramTemplate(
            id = "T1",
            name = "Sơ đồ nhánh 1 pha chuẩn",
            description = "Trạm 1 pha -> 2 trụ BTLT 12m -> Công tơ treo trụ -> Nhà KH",
            items = listOf(
                Obj_CANVAS(LENH = Variables.VE_TRAM_1PHA_HHUU, X3 = 150f, Y3 = 250f, TEN = "TBA 1P-50kVA"),
                Obj_CANVAS(LENH = Variables.VE_TRU_BTLT_12_HHUU, X3 = 350f, Y3 = 250f, TEN = "Trụ 1"),
                Obj_CANVAS(LENH = Variables.VE_TRU_BTLT_12_SLAP, X3 = 550f, Y3 = 250f, TEN = "Trụ 2 (SL)"),
                Obj_CANVAS(LENH = Variables.VE_CONGTO_TREO_TRU_BTLT_12_HHUU, X3 = 550f, Y3 = 220f, TEN = "CT 1P"),
                Obj_CANVAS(LENH = Variables.VE_NHA, X3 = 750f, Y3 = 250f, TEN = "Nhà KH"),
                Obj_CANVAS(LENH = Variables.VE_DUONG_THANG, X1 = 150f, Y1 = 250f, X3 = 350f, Y3 = 250f, TEN = "35m"),
                Obj_CANVAS(LENH = Variables.VE_DUONG_THANG, X1 = 350f, Y1 = 250f, X3 = 550f, Y3 = 250f, TEN = "40m"),
                Obj_CANVAS(LENH = Variables.VE_LINE_DUT_NET, X1 = 550f, Y1 = 250f, X3 = 750f, Y3 = 250f, TEN = "25m")
            )
        ),
        DiagramTemplate(
            id = "T2",
            name = "Sơ đồ cấp điện qua lộ",
            description = "Trụ BTLT 14m bên lộ -> Dây vượt lộ liền nét -> Trụ BTLT 12m -> Dây nhánh đứt nét -> Nhà KH",
            items = listOf(
                Obj_CANVAS(LENH = Variables.VE_TRU_BTLT_14_HHUU, X3 = 200f, Y3 = 200f, TEN = "Trụ 14m HH"),
                Obj_CANVAS(LENH = Variables.VE_DUONG_THANG, X1 = 200f, Y1 = 200f, X3 = 500f, Y3 = 350f, TEN = "Qua lộ 45m"),
                Obj_CANVAS(LENH = Variables.VE_TRU_BTLT_12_SLAP, X3 = 500f, Y3 = 350f, TEN = "Trụ 12m SL"),
                Obj_CANVAS(LENH = Variables.VE_CONGTO_TREO_NHA, X3 = 700f, Y3 = 350f, TEN = "CT treo nhà"),
                Obj_CANVAS(LENH = Variables.VE_NHA, X3 = 750f, Y3 = 350f, TEN = "Nhà KH"),
                Obj_CANVAS(LENH = Variables.VE_LINE_DUT_NET, X1 = 500f, Y1 = 350f, X3 = 750f, Y3 = 350f, TEN = "15m")
            )
        ),
        DiagramTemplate(
            id = "T3",
            name = "Sơ đồ trạm 3 pha chuyên dùng",
            description = "Trạm 3 pha nền -> Tháp sắt 12m -> Tuyến hạ thế",
            items = listOf(
                Obj_CANVAS(LENH = Variables.VE_TRAM_3PHA_NEN_HHUU, X3 = 180f, Y3 = 280f, TEN = "TBA 3P-250kVA"),
                Obj_CANVAS(LENH = Variables.VE_THAPSAT_12_HHUU, X3 = 380f, Y3 = 280f, TEN = "Tháp sắt 12m"),
                Obj_CANVAS(LENH = Variables.VE_DUONG_THANG, X1 = 180f, Y1 = 280f, X3 = 380f, Y3 = 280f, TEN = "Tuyến chính"),
                Obj_CANVAS(LENH = Variables.VE_TRU_BTLT_14_SLAP, X3 = 600f, Y3 = 280f, TEN = "Trụ SL"),
                Obj_CANVAS(LENH = Variables.VE_DUONG_THANG, X1 = 380f, Y1 = 280f, X3 = 600f, Y3 = 280f, TEN = "50m")
            )
        )
    )
}

@Composable
fun TemplateDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (List<Obj_CANVAS>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var dbTemplates by remember { mutableStateOf<List<DiagramTemplate>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf<DiagramTemplate?>(null) }
    val scope = rememberCoroutineScope()

    // Tải danh sách mẫu từ Database Room/SQLite nội bộ
    fun reloadDbTemplates() {
        scope.launch {
            val saved = PlatformTemplateStorage.getAllTemplates()
            dbTemplates = saved.map { st ->
                DiagramTemplate(
                    id = "DB_${st.tenMau}",
                    name = st.tenMau,
                    description = "Mẫu lưu nội bộ (${st.items.size} đối tượng${if (st.donVi.isNotBlank()) " - " + st.donVi else ""})",
                    items = st.items,
                    isCustom = true
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadDbTemplates()
    }

    val allTemplates = remember(dbTemplates, DefaultTemplates.list) {
        dbTemplates + DefaultTemplates.list
    }

    val filteredTemplates = remember(allTemplates, searchQuery) {
        if (searchQuery.isBlank()) allTemplates
        else allTemplates.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    if (selectedTemplate == null && filteredTemplates.isNotEmpty()) {
        selectedTemplate = filteredTemplates.first()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chọn sơ đồ mẫu (Room Database)", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                // Ô tìm kiếm mẫu
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm kiếm mẫu sơ đồ...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Có ${filteredTemplates.size} mẫu (${dbTemplates.size} mẫu trong CSDL):",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                ) {
                    items(filteredTemplates) { template ->
                        val isChosen = (template.id == selectedTemplate?.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color(0xFFF8FAFC))
                                .clickable { selectedTemplate = template }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = template.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isChosen) MaterialTheme.colorScheme.primary else Color(0xFF1E293B)
                                    )
                                    if (template.isCustom) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF059669).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "Room DB",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = template.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            if (template.isCustom) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            PlatformTemplateStorage.deleteTemplate(template.name)
                                            reloadDbTemplates()
                                            if (selectedTemplate?.id == template.id) {
                                                selectedTemplate = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Xóa mẫu",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (isChosen) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedTemplate != null,
                onClick = {
                    selectedTemplate?.let { onSelectTemplate(it.items) }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Nạp mẫu này")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Đóng")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
