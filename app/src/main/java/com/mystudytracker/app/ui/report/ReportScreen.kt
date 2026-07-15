package com.mystudytracker.app.ui.report

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
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

private val DATE_LABEL_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

/**
 * Backlog report: every outstanding unit across the whole tracked history, rolled up bottom-up
 * from leaf tasks through group and section subtotals. Tapping a leaf drills into the specific
 * dates it is still outstanding on.
 *
 * When no date has been synced yet, shows a prompt to sync first — backlog is meaningless
 * without a confirmed "today" anchor. The date is read from [ReportViewModel.today] which is
 * sourced from DateIntegrityManager and never falls back to the phone's wall clock.
 */
@Composable
fun ReportScreen(viewModel: ReportViewModel, onBack: () -> Unit) {
    val today by viewModel.today.collectAsState()
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
            // ── Top bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ZincTextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Backlog Report",
                        color = ZincTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (today != null && !loading) {
                        Text(
                            text = if (totalPending == 0) "Fully caught up"
                                   else "$totalPending unit${if (totalPending == 1) "" else "s"} outstanding",
                            color = if (totalPending == 0) AccentEmerald else ZincTextMuted,
                            fontSize = 12.sp
                        )
                    } else if (today == null) {
                        Text(text = "No date synced", color = ZincTextMuted, fontSize = 12.sp)
                    }
                }
                if (today != null) {
                    // Date chip showing the anchor date used for the backlog calculation
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZincSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = today!!.format(DATE_LABEL_FORMAT),
                            color = ZincTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = ZincBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
            Spacer(Modifier.height(4.dp))

            // ── Body ──────────────────────────────────────────────────────
            when {
                // Not synced yet — show a clean prompt
                today == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AccentBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Sync date to show backlog report",
                            color = ZincTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Go back to the calendar and tap Sync to confirm today's date. The backlog report needs a confirmed date to count outstanding days accurately.",
                            color = ZincTextMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }

                // Loading
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
                    }
                }

                // All caught up
                totalPending == 0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AccentEmerald.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "All caught up",
                            color = ZincTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "No outstanding units through ${today!!.format(DATE_LABEL_FORMAT)}.",
                            color = ZincTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                // Backlog tree
                else -> {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tree.filter { it.pendingUnits > 0 }.forEach { section ->
                            SectionBacklogCard(section = section, onSelectLeaf = viewModel::selectTask)
                        }
                    }
                }
            }
        }
    }

    // ── Drill-down dialog ──────────────────────────────────────────────
    if (selectedTask != null) {
        val task = selectedTask!!
        AlertDialog(
            onDismissRequest = viewModel::clearSelection,
            containerColor = ZincSurface,
            title = {
                Text(
                    text = task.titlePath.joinToString(" › "),
                    color = ZincTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                if (pendingDates.isEmpty()) {
                    Text(
                        text = "No specific outstanding dates found.",
                        color = ZincTextMuted,
                        fontSize = 13.sp
                    )
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${pendingDates.size} date${if (pendingDates.size == 1) "" else "s"} pending",
                            color = ZincTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        pendingDates.forEach { date ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ZincSurfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(AccentRed)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = date.format(DATE_LABEL_FORMAT),
                                    color = ZincTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
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

// ── Section card ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionBacklogCard(section: BacklogNode, onSelectLeaf: (LeafBacklog) -> Unit) {
    var expanded by remember(section) { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(180),
        label = "sectionChevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(ZincSurface)
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                color = ZincTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            PendingBadge(pendingUnits = section.pendingUnits)
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = ZincTextMuted,
                modifier = Modifier.size(16.dp).rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
            exit = shrinkVertically(tween(180, easing = LinearOutSlowInEasing)) + fadeOut(tween(130))
        ) {
            Column {
                HorizontalDivider(color = ZincBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    section.children.filter { it.pendingUnits > 0 }.forEachIndexed { i, child ->
                        BacklogNodeRow(node = child, depth = 0, onSelectLeaf = onSelectLeaf)
                        if (i < section.children.filter { it.pendingUnits > 0 }.lastIndex) {
                            HorizontalDivider(
                                color = ZincBorder.copy(alpha = 0.2f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Recursive backlog node row ────────────────────────────────────────────────────────────────

@Composable
private fun BacklogNodeRow(
    node: BacklogNode,
    depth: Int,
    onSelectLeaf: (LeafBacklog) -> Unit
) {
    if (node.pendingUnits == 0) return

    if (node.leaf != null) {
        LeafBacklogRow(
            leaf = node.leaf,
            title = node.title,
            pendingUnits = node.pendingUnits,
            onSelectLeaf = onSelectLeaf
        )
        return
    }

    var expanded by remember(node) { mutableStateOf(depth == 0) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(180),
        label = "nodeChevron_${node.title}"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 9.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = node.title,
                color = ZincTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            PendingBadge(pendingUnits = node.pendingUnits)
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = ZincTextMuted,
                modifier = Modifier.size(14.dp).rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(150)),
            exit = shrinkVertically(tween(180, easing = LinearOutSlowInEasing)) + fadeOut(tween(120))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (depth == 0) Color(0xFF27272A) else Color(0xFF1F1F22)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val visibleChildren = node.children.filter { it.pendingUnits > 0 }
                visibleChildren.forEachIndexed { i, child ->
                    BacklogNodeRow(node = child, depth = depth + 1, onSelectLeaf = onSelectLeaf)
                    if (i < visibleChildren.lastIndex) {
                        HorizontalDivider(
                            color = ZincBorder.copy(alpha = 0.25f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Leaf backlog row ──────────────────────────────────────────────────────────────────────────

@Composable
private fun LeafBacklogRow(
    leaf: LeafBacklog,
    title: String,
    pendingUnits: Int,
    onSelectLeaf: (LeafBacklog) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelectLeaf(leaf) }
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ZincTextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        PendingBadge(pendingUnits = pendingUnits)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "View dates",
            tint = ZincTextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

// ── Pending badge ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PendingBadge(pendingUnits: Int) {
    val tint = when {
        pendingUnits > 10 -> AccentRed
        pendingUnits > 5  -> Color(0xFFE89D3A)
        else              -> AccentBlue
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.15f))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$pendingUnits",
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
