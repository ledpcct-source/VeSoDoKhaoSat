package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesodo.khaosat.database.PlatformTemplateStorage
import com.vesodo.khaosat.model.Obj_CANVAS
import kotlinx.coroutines.launch

@Composable
fun SaveTemplateDialog(
    currentItems: List<Obj_CANVAS>,
    defaultName: String,
    donVi: String,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    var templateName by remember { mutableStateOf(defaultName) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Lưu Sơ Đồ Thành Mẫu",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Lưu ${currentItems.size} đối tượng hiện có thành mẫu vẽ vào cơ sở dữ liệu Room/SQLite nội bộ để tái sử dụng nhanh:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Tên mẫu sơ đồ (TEN_MAU)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && templateName.isNotBlank() && currentItems.isNotEmpty(),
                onClick = {
                    isSaving = true
                    scope.launch {
                        val ok = PlatformTemplateStorage.saveTemplate(
                            tenMau = templateName.trim(),
                            donVi = donVi,
                            items = currentItems
                        )
                        isSaving = false
                        if (ok) {
                            onSaved("Đã lưu mẫu '$templateName' vào cơ sở dữ liệu thành công")
                        } else {
                            onSaved("Lưu mẫu thất bại")
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Lưu mẫu")
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
