package com.mystudytracker.app.ui.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystudytracker.app.ui.theme.AccentBlue
import com.mystudytracker.app.ui.theme.AccentEmerald
import com.mystudytracker.app.ui.theme.AccentRed
import com.mystudytracker.app.ui.theme.ZincBackground
import com.mystudytracker.app.ui.theme.ZincBorder
import com.mystudytracker.app.ui.theme.ZincSurface
import com.mystudytracker.app.ui.theme.ZincSurfaceVariant
import com.mystudytracker.app.ui.theme.ZincTextMuted
import com.mystudytracker.app.ui.theme.ZincTextPrimary
import com.mystudytracker.app.ui.theme.ZincTextSecondary
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

/**
 * Backlog report: every outstanding unit across the whole tracked history, rolled up bottom-up
 * from leaf tasks through group and section subtotals. Tapping a leaf drills into the specific
 * dates it is still outstanding on.
 */
@Composable
fun ReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val tree by viewModel.tree.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val pendingDates by viewModel.pendingDates.collectAsState()
    val totalPending = tree.sumOf { it.pendingUnits }

    Box(modifier = Modifier.fillMaxSize().background(ZincBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ZincTextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Backlog Report", color = ZincTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (totalPending == 0) "Fully caught up" else "$totalPending unit${if (totalPending == 1) "" else "s"} outstanding",
                        color = if (totalPending == 0) AccentEmerald else ZincTextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (totalPending == 0) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(text = "No backlog - everything is caught up.", color = ZincTextSecondary, fontSize = 15.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    tree.filter { it.pendingUnits > 0 }.forEach { section ->
                        SectionBacklogCard(section = section, onSelectLeaf = viewModel::selectTask)
                    }
                }
            }
        }
    }

    if (selectedTask != null) {
        val task = selectedTask!!
        AlertDialog(
            onDismissRequest = viewModel::clearSelection,
            containerColor = ZincSurface,
            title = { Text(text = task.titlePath.joinToString(" › "), color = ZincTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                if (pendingDates.isEmpty()) {
                    Text(text = "No specific outstanding dates found.", color = ZincTextMuted, fontSize = 13.sp)
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${pendingDates.size} date${if (pendingDates.size == 1) "" else "s"} pending:",
                            color = ZincTextMuted,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                        pendingDates.forEach { date ->
                            Text(text = date.format(DATE_LABEL_FORMAT), color = ZincTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::clearSelection) {
                    Text(text = "Close", color = AccentBlue)
                }
            }
        )
    }
}

@Composable
private fun SectionBacklogCard(section: BacklogNode, onSelectLeaf: (LeafBacklog) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(ZincSurface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        BacklogNodeRow(node = section, depth = 0, isTopLevel = true, onSelectLeaf = onSelectLeaf)
    }
}

@Composable
private fun BacklogNodeRow(node: BacklogNode, depth: Int, isTopLevel: Boolean, onSelectLeaf: (LeafBacklog) -> Unit) {
    if (node.leaf != null) {
        LeafBacklogRow(leaf = node.leaf, title = node.title, depth = depth, pendingUnits = node.pendingUnits, onSelectLeaf = onSelectLeaf)
        return
    }
    if (node.pendingUnits == 0) return

    var expanded by remember(node) { mutableStateOf(isTopLevel) }
    val chevronRotation by animateFloatAsState(if (expanded) 90f else 0f, tween(180), label = "reportChevron")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = if (isTopLevel) 0.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isTopLevel) {
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = ZincTextMuted,
                        modifier = Modifier.size(18.dp).rotate(chevronRotation)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = node.title,
                color = ZincTextPrimary,
                fontSize = if (isTopLevel) 15.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            PendingBadge(pendingUnits = node.pendingUnits)
            if (isTopLevel) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = ZincTextMuted,
                    modifier = Modifier.size(18.dp).rotate(chevronRotation)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(150)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            Column(modifier = Modifier.padding(start = if (isTopLevel) 6.dp else 18.dp, top = 4.dp)) {
                node.children.forEach { child ->
                    BacklogNodeRow(node = child, depth = depth + 1, isTopLevel = false, onSelectLeaf = onSelectLeaf)
                }
            }
        }
    }
}

@Composable
private fun LeafBacklogRow(leaf: LeafBacklog, title: String, depth: Int, pendingUnits: Int, onSelectLeaf: (LeafBacklog) -> Unit) {
    if (pendingUnits == 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelectLeaf(leaf) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = ZincTextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        PendingBadge(pendingUnits = pendingUnits)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = "View dates", tint = ZincTextMuted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PendingBadge(pendingUnits: Int) {
    val tint = if (pendingUnits > 5) AccentRed else AccentBlue
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$pendingUnits", color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
