package com.mystudytracker.app.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystudytracker.app.data.DailyProgress
import com.mystudytracker.app.ui.theme.AccentAmber
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
import com.mystudytracker.app.util.AppText
import com.mystudytracker.app.util.DateRules
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val WEEKDAY_LABELS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDateSelected: (LocalDate) -> Unit,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val progressByDate by viewModel.progressByDate.collectAsState()
    val trackedToday by viewModel.today.collectAsState()
    val lastSyncedLabel by viewModel.lastSyncedLabel.collectAsState()
    val rebootDetected by viewModel.rebootDetected.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val justSynced by viewModel.justSynced.collectAsState()
    val syncFailed by viewModel.syncFailed.collectAsState()
    val totalBacklogUnits by viewModel.totalBacklogUnits.collectAsState()

    // Only used to pick which month is shown first - the actual "today" used for highlighting and
    // unlocking days always comes from the uptime-anchored tracker above, never the wall clock.
    val wallClockNow = remember { LocalDate.now() }
    val startMonth = remember { YearMonth.from(DateRules.START_DATE) }
    val endMonth = remember { YearMonth.from(DateRules.END_DATE) }
    val initialMonth = remember {
        val current = YearMonth.from(wallClockNow)
        when {
            current.isBefore(startMonth) -> startMonth
            current.isAfter(endMonth) -> endMonth
            else -> current
        }
    }
    var cursorMonth by rememberSaveable { mutableStateOf(initialMonth.toString()) }
    val month = YearMonth.parse(cursorMonth)

    // Before the first-ever sync, there is no verified "today" - fall back to a date before the
    // tracked range so every day renders as locked/future rather than guessing from the wall clock.
    val today = trackedToday ?: DateRules.START_DATE.minusDays(1)

    // After a successful manual sync, jump the visible month back to whatever month the verified
    // "today" now falls in - but only if the user isn't already looking at it.
    LaunchedEffect(justSynced) {
        if (justSynced && trackedToday != null) {
            val syncedMonth = YearMonth.from(trackedToday).let {
                when {
                    it.isBefore(startMonth) -> startMonth
                    it.isAfter(endMonth) -> endMonth
                    else -> it
                }
            }
            if (syncedMonth != month) {
                cursorMonth = syncedMonth.toString()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZincBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // Three-line masthead
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = AppText.TITLE_LINE_1,
                color = ZincTextMuted,
                fontSize = 14.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = AppText.TITLE_LINE_2,
                color = AccentBlue,
                fontSize = 21.sp,
                letterSpacing = 0.4.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = AppText.TITLE_LINE_3,
                color = ZincTextMuted,
                fontSize = 14.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SyncPill(
                syncing = syncing,
                justSynced = justSynced,
                syncFailed = syncFailed,
                rebootDetected = rebootDetected,
                lastSyncedLabel = lastSyncedLabel,
                onClick = { viewModel.syncDate() }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            val canGoNext = month.isBefore(endMonth)
            val canGoPrev = month.isAfter(startMonth)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (canGoPrev) cursorMonth = month.minusMonths(1).toString() },
                    enabled = canGoPrev
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = if (canGoPrev) ZincTextPrimary else ZincBorder
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        color = ZincTextPrimary,
                        fontSize = 22.sp
                    )
                    Text(text = month.year.toString(), color = ZincTextMuted, fontSize = 14.sp)
                }
                IconButton(
                    onClick = { if (canGoNext) cursorMonth = month.plusMonths(1).toString() },
                    enabled = canGoNext
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Next month",
                        tint = if (canGoNext) ZincTextPrimary else ZincBorder
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_LABELS.forEach { label ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = label, color = ZincTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(200)) { width -> width } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(200)) { width -> -width } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(200)) { width -> -width } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(200)) { width -> width } + fadeOut(tween(200)))
                    }
                },
                label = "monthGrid"
            ) { targetMonth ->
                CalendarGrid(
                    month = targetMonth,
                    today = today,
                    progressByDate = progressByDate,
                    onDateSelected = onDateSelected
                )
            }
        }

        // Backlog Report + Settings button — share the same horizontal row.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ReportButton(totalBacklogUnits = totalBacklogUnits, onClick = onOpenReport)
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ZincSurface)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = ZincTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        KeyCard()
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ReportButton(totalBacklogUnits: Int?, onClick: () -> Unit) {
    // null = not synced yet (treat as interactive); 0 = all clear (green, disabled); >0 = has backlog
    val allClear = totalBacklogUnits == 0
    val interactive = !allClear

    val bgColor = if (allClear) AccentEmerald.copy(alpha = 0.16f) else ZincSurface
    val iconTint = if (allClear) AccentEmerald else AccentBlue
    val labelColor = if (allClear) AccentEmerald else ZincTextPrimary
    val labelText = if (allClear) "No Backlogs ✓" else "Backlog Report"
    val trailingTint = if (allClear) AccentEmerald.copy(alpha = 0.5f) else ZincTextMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (allClear) 0.dp else 6.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(if (interactive) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = labelText,
                color = labelColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = trailingTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Isolated in its own composable so recomposing unrelated screen state (sync pill, banner) never
 * forces the whole month grid to re-lay-out.
 */
@Composable
private fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    progressByDate: Map<String, DailyProgress>,
    onDateSelected: (LocalDate) -> Unit
) {
    val weeks = remember(month) { buildWeeks(month) }
    Column {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        if (date != null) {
                            val progress = progressByDate[date.toString()]
                            DayCell(
                                date = date,
                                today = today,
                                completedUnits = progress?.completedUnits,
                                totalUnits = progress?.totalUnits,
                                onClick = { onDateSelected(date) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SyncPill(
    syncing: Boolean,
    justSynced: Boolean,
    syncFailed: Boolean,
    rebootDetected: Boolean,
    lastSyncedLabel: String?,
    onClick: () -> Unit
) {
    val angle: Float = if (syncing) {
        val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
        val a by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
            label = "syncAngle"
        )
        a
    } else {
        0f
    }

    val visualState = when {
        justSynced      -> SyncVisualState(Icons.Filled.Check, AccentEmerald, rotating = false, label = "Synced")
        syncFailed      -> SyncVisualState(Icons.Filled.WarningAmber, AccentRed, rotating = false, label = "Sync Failed")
        syncing         -> SyncVisualState(Icons.Filled.Sync, AccentBlue, rotating = true, label = "Syncing...")
        rebootDetected  -> SyncVisualState(Icons.Filled.WarningAmber, AccentAmber, rotating = false, label = "Sync Status: Unknown")
        lastSyncedLabel == null -> SyncVisualState(Icons.Filled.Sync, AccentBlue, rotating = false, label = "Tap to Sync Date")
        else            -> SyncVisualState(Icons.Filled.Sync, AccentBlue, rotating = false, label = "Last synced: $lastSyncedLabel")
    }

    Row(
        modifier = Modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(ZincSurfaceVariant)
            .clickable(enabled = !syncing) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(targetState = visualState, label = "syncPillContent") { state ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = state.icon,
                    contentDescription = null,
                    tint = state.tint,
                    modifier = Modifier
                        .size(15.dp)
                        .rotate(if (state.rotating) angle else 0f)
                )
                Text(
                    text = state.label,
                    color = ZincTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

private data class SyncVisualState(
    val icon: ImageVector,
    val tint: Color,
    val rotating: Boolean,
    val label: String
)

@Composable
private fun DayCell(
    date: LocalDate,
    today: LocalDate,
    completedUnits: Int?,
    totalUnits: Int?,
    onClick: () -> Unit
) {
    val status = computeDayStatus(date, today, completedUnits, totalUnits)
    val clickable = isDayClickable(status)

    if (status == DayStatus.OUTSIDE) return

    // FUTURE uses a solid ZincSurface background (one shade above ZincBackground)
    // instead of a border — depth through tone, not stroke.
    val (targetBackground, contentColor) = when (status) {
        DayStatus.FUTURE           -> Pair(ZincSurface, ZincTextSecondary)
        DayStatus.TODAY_INCOMPLETE -> Pair(AccentBlue, Color.White)
        DayStatus.TODAY_COMPLETE   -> Pair(AccentEmerald, Color.White)
        DayStatus.GREEN            -> Pair(AccentEmerald.copy(alpha = 0.9f), Color(0xFF022C22))
        DayStatus.YELLOW           -> Pair(AccentAmber.copy(alpha = 0.9f), Color(0xFF451A03))
        DayStatus.RED              -> Pair(AccentRed.copy(alpha = 0.85f), Color(0xFFFEF2F2))
        DayStatus.OUTSIDE          -> Pair(Color.Transparent, Color.Transparent)
    }

    val background by animateColorAsState(targetValue = targetBackground, animationSpec = tween(200), label = "dayColor")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(enabled = clickable) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = if (status == DayStatus.GREEN || status == DayStatus.YELLOW || status == DayStatus.RED) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun KeyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(ZincSurface)
            .padding(16.dp)
    ) {
        Text(
            text = "KEY",
            color = ZincTextMuted,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(12.dp))
        val rows = listOf(
            AccentBlue    to "Today (incomplete)",
            AccentEmerald to "Fully completed",
            AccentAmber   to "Partially completed",
            AccentRed     to "Nothing completed"
        )
        rows.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                pair.forEach { (color, label) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(text = label, color = ZincTextSecondary, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(ZincSurfaceVariant)
            )
            Text(text = "Future (locked)", color = ZincTextSecondary, fontSize = 13.sp)
        }
    }
}

private fun buildWeeks(month: YearMonth): List<List<LocalDate?>> {
    val firstOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = firstOfMonth.dayOfWeek.value % 7

    val cells = mutableListOf<LocalDate?>()
    repeat(startOffset) { cells.add(null) }
    for (day in 1..daysInMonth) cells.add(month.atDay(day))
    while (cells.size % 7 != 0) cells.add(null)
    while (cells.size < WEEKS_PER_GRID * 7) cells.add(null)

    return cells.chunked(7)
}

private const val WEEKS_PER_GRID = 6
