package com.vesodo.khaosat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.vesodo.khaosat.ai.DiagramIssue
import com.vesodo.khaosat.ai.DiagramRuleEngine
import com.vesodo.khaosat.ai.IssueLevel
import com.vesodo.khaosat.ai.QualityScore
import com.vesodo.khaosat.model.Obj_CANVAS

@Composable
fun SmartCheckDialog(
    items: List<Obj_CANVAS>,
    onDismiss: () -> Unit,
    onHighlightItem: (Obj_CANVAS) -> Unit,
    onApplyQuickFix: (DiagramIssue) -> Unit
) {
    var issues by remember { mutableStateOf(DiagramRuleEngine.analyzeDiagram(items)) }
    var score by remember { mutableStateOf(DiagramRuleEngine.calculateScore(issues, items.size, items)) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Tất cả, 1: Lỗi & Cảnh báo, 2: Gợi ý

    val filteredIssues = remember(issues, selectedTab) {
        when (selectedTab) {
            1 -> issues.filter { it.level == IssueLevel.ERROR || it.level == IssueLevel.WARNING }
            2 -> issues.filter { it.level == IssueLevel.INFO }
            else -> issues
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "AI Kiểm Tra & Chấm Điểm Sơ Đồ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Tiêu chí điểm cao: Đủ Trạm • Trụ • Dây • Công tơ/Khách hàng & Chuẩn EVN",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                // Card điểm số chất lượng + Checklist 4 thành phần bắt buộc
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            score.score >= 90 -> Color(0xFFECFDF5)
                            score.score >= 70 -> Color(0xFFFFFBEB)
                            else -> Color(0xFFFEF2F2)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ĐIỂM ĐÁNH GIÁ SƠ ĐỒ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${score.score} / 100",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        score.score >= 90 -> Color(0xFF059669)
                                        score.score >= 70 -> Color(0xFFD97706)
                                        else -> Color(0xFFDC2626)
                                    }
                                )
                                Text(
                                    text = score.ratingText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            // Thống kê số lượng
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BadgeStat(
                                    label = "Thiếu/Lỗi",
                                    count = score.errorCount,
                                    color = Color(0xFFDC2626),
                                    bgColor = Color(0xFFFEE2E2)
                                )
                                BadgeStat(
                                    label = "Cảnh báo",
                                    count = score.warningCount,
                                    color = Color(0xFFD97706),
                                    bgColor = Color(0xFFFEF3C7)
                                )
                                BadgeStat(
                                    label = "Gợi ý",
                                    count = score.infoCount,
                                    color = Color(0xFF2563EB),
                                    bgColor = Color(0xFFDBEAFE)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.6f))

                        // Checklist 4 thành phần bắt buộc để đạt điểm cao
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CoreComponentChip(
                                label = "Trạm",
                                isPresent = score.hasSubstation,
                                modifier = Modifier.weight(1f)
                            )
                            CoreComponentChip(
                                label = "Trụ",
                                isPresent = score.hasPole,
                                modifier = Modifier.weight(1f)
                            )
                            CoreComponentChip(
                                label = "Dây",
                                isPresent = score.hasWire,
                                modifier = Modifier.weight(1f)
                            )
                            CoreComponentChip(
                                label = "Công tơ/KH",
                                isPresent = score.hasMeterOrCustomer,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab phân loại
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Tất cả (${issues.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Cần xử lý (${score.errorCount + score.warningCount})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Khuyến nghị (${score.infoCount})", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Danh sách vấn đề
                if (filteredIssues.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tuyệt vời! Sơ đồ đạt chuẩn 100 điểm.",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Đầy đủ Trạm, Trụ, Dây, Công tơ/Khách hàng và tuân thủ tiêu chuẩn EVN.",
                                color = Color(0xFF475569),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredIssues, key = { it.id }) { issue ->
                            IssueItemRow(
                                issue = issue,
                                onHighlight = {
                                    issue.targetItem?.let { item ->
                                        onHighlightItem(item)
                                        onDismiss()
                                    }
                                },
                                onQuickFix = {
                                    onApplyQuickFix(issue)
                                    // Refresh issues
                                    issues = DiagramRuleEngine.analyzeDiagram(items)
                                    score = DiagramRuleEngine.calculateScore(issues, items.size, items)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Đóng")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun CoreComponentChip(
    label: String,
    isPresent: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPresent) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val contentColor = if (isPresent) Color(0xFF047857) else Color(0xFFB91C1C)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun BadgeStat(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "$count", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = color)
        Text(text = label, fontSize = 10.sp, color = color.copy(alpha = 0.85f))
    }
}

@Composable
private fun IssueItemRow(
    issue: DiagramIssue,
    onHighlight: () -> Unit,
    onQuickFix: () -> Unit
) {
    val borderColor = when (issue.level) {
        IssueLevel.ERROR -> Color(0xFFEF4444)
        IssueLevel.WARNING -> Color(0xFFF59E0B)
        IssueLevel.INFO -> Color(0xFF3B82F6)
    }

    val iconVector = when (issue.level) {
        IssueLevel.ERROR -> Icons.Default.Cancel
        IssueLevel.WARNING -> Icons.Default.Warning
        IssueLevel.INFO -> Icons.Default.Info
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHighlight() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                iconVector,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = issue.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = issue.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                if (issue.quickFixAvailable && issue.quickFixTitle != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FilledTonalButton(
                        onClick = onQuickFix,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = issue.quickFixTitle, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (issue.targetItem != null) {
                IconButton(
                    onClick = onHighlight,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Xem vị trí",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
