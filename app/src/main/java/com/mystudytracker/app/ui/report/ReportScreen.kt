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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarToday
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
                            text = "Through ${today!!.minusDays(1).format(DATE_LABEL_FORMAT)}",
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
                            text = "Sync Required!",
                            color = ZincTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Go back to the calendar and tap Sync to confirm today's date. A verified date is required to accurately calculate outstanding backlog.",
                            color = ZincTextMuted,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
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
                            text = "No outstanding units through ${today!!.minusDays(1).format(DATE_LABEL_FORMAT)}.",
                            color = ZincTextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
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
                            SectionBacklogCard(section = section)
                        }
                    }
                }
            }
        }
    }

}

// ── Section card ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionBacklogCard(section: BacklogNode) {
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
            // Fixed trailing zone — chevron X is anchored to the viewbox, never
            // to the pill content, so it stays at the same position on every row.
            Box(
                modifier = Modifier.width(80.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = ZincTextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.CenterStart)
                        .rotate(chevronRotation)
                )
                PendingBadge(pendingUnits = section.pendingUnits)
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
            exit = shrinkVertically(tween(180, easing = LinearOutSlowInEasing)) + fadeOut(tween(130))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = ZincBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                val visibleChildren = section.children.filter { it.pendingUnits > 0 }
                visibleChildren.forEachIndexed { i, child ->
                    BacklogNodeRow(
                        node = child,
                        depth = 0,
                        showTrailingDivider = i < visibleChildren.lastIndex
                    )
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
    showTrailingDivider: Boolean = false
) {
    if (node.pendingUnits == 0) return

    if (node.leaf != null) {
        LeafBacklogRow(
            title = node.title,
            pendingUnits = node.pendingUnits,
            showTrailingDivider = showTrailingDivider
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
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = node.title,
                color = ZincTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // Fixed trailing zone — chevron X is anchored to the viewbox, never
            // to the pill content, so it stays at the same position on every row.
            Box(
                modifier = Modifier.width(80.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = ZincTextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.CenterStart)
                        .rotate(chevronRotation)
                )
                PendingBadge(pendingUnits = node.pendingUnits)
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(150)),
            exit = shrinkVertically(tween(180, easing = LinearOutSlowInEasing)) + fadeOut(tween(120))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (depth == 0) Color(0xFF111113) else Color(0xFF080809))
            ) {
                HorizontalDivider(color = ZincBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                val visibleChildren = node.children.filter { it.pendingUnits > 0 }
                visibleChildren.forEachIndexed { i, child ->
                    BacklogNodeRow(
                        node = child,
                        depth = depth + 1,
                        showTrailingDivider = i < visibleChildren.lastIndex
                    )
                }
            }
        }

        if (showTrailingDivider && !expanded) {
            HorizontalDivider(color = ZincBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
        }
    }
}

// ── Leaf backlog row ──────────────────────────────────────────────────────────────────────────

@Composable
private fun LeafBacklogRow(
    title: String,
    pendingUnits: Int,
    showTrailingDivider: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ZincTextPrimary,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        PendingBadge(pendingUnits = pendingUnits)
    }
    if (showTrailingDivider) {
        HorizontalDivider(color = ZincBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
    }
}

// ── Pending badge ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PendingBadge(pendingUnits: Int) {
    // Red when any backlog exists; green when fully cleared.
    val tint = if (pendingUnits == 0) AccentEmerald else AccentRed
    // Fixed-width view box — right-edges align across all rows.
    // Content scrolls horizontally if the number overflows the box.
    Box(
        modifier = Modifier.width(60.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$pendingUnits",
                    color = tint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
