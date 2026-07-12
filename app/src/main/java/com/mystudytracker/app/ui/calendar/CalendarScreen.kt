package com.mystudytracker.app.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition

private val WEEKDAY_LABELS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDateSelected: (LocalDate) -> Unit
) {
    val completedCounts by viewModel.completedCountByDate.collectAsState()
    val trackedToday by viewModel.today.collectAsState()
    val lastSyncedLabel by viewModel.lastSyncedLabel.collectAsState()
    val rebootDetected by viewModel.rebootDetected.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val justSynced by viewModel.justSynced.collectAsState()
    val syncFailed by viewModel.syncFailed.collectAsState()

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
    // "today" now falls in - but only if the user isn't already looking at it. Reuses the exact
    // same cursorMonth state that the prev/next arrows drive, so AnimatedContent below plays the
    // identical slide transition regardless of what changed the month.
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

        // Three-line masthead - the focal title of the screen, always first.
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

        // Sync status/action as a single centered pill - one cohesive tappable element instead of
        // a stray button floating in a corner.
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

        // Calendar block is vertically centered in the remaining space between the title above
        // and the legend below, so the screen doesn't feel top-heavy. Also scrollable: in
        // portrait this area's content is always shorter than its allotted space so scrolling
        // never engages and centering looks identical to before, but in landscape the day grid
        // (sized off the much wider available width) can be taller than the remaining vertical
        // space - without this, it would overflow past this block and overlap the legend below
        // instead of scaling down or scrolling.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            // Month navigator
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

            // Weekday header
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
                    completedCounts = completedCounts,
                    onDateSelected = onDateSelected
                )
            }
        }

        LegendCard()
        Spacer(Modifier.height(20.dp))
    }
}

/**
 * Isolated in its own composable so recomposing unrelated screen state (sync pill, banner) never
 * forces the whole month grid to re-lay-out - only [month], [today], or [completedCounts]
 * actually changing triggers work here.
 */
@Composable
private fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    completedCounts: Map<String, Int>,
    onDateSelected: (LocalDate) -> Unit
) {
    val weeks = remember(month) { buildWeeks(month) }
    Column {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                today = today,
                                completedCount = completedCounts[date.toString()],
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

/**
 * Single adaptive control that communicates every sync-related state through its own icon, tint,
 * and label - never-synced, syncing, just-succeeded, failed, and reboot-detected all render here
 * instead of a separate banner, so the pill's fixed size means nothing else on screen ever shifts
 * position when its state changes.
 */
@Composable
private fun SyncPill(
    syncing: Boolean,
    justSynced: Boolean,
    syncFailed: Boolean,
    rebootDetected: Boolean,
    lastSyncedLabel: String?,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "syncAngle"
    )

    // Failed and reboot-detected are deliberately different colors even though both are
    // "attention" states - red means the sync you just attempted failed outright, amber means the
    // date is merely unverified since a restart. Keeping them visually distinct avoids reading one
    // as the other at a glance.
    //
    // Icon, tint, rotation, and label are bundled into ONE immutable value and driven through a
    // single AnimatedContent, rather than the icon and text crossfading independently. They used
    // to be two separate AnimatedContent-esque updates reading the same outer state, which meant
    // the icon's crossfade animation (a few hundred ms) and the text's instant swap were not the
    // same transition - so for a split second you could see the new label next to the old icon
    // (e.g. "Synced" still paired with the spinning blue sync icon) before the icon caught up.
    // Treating icon+label as one atomic snapshot inside a single AnimatedContent means they always
    // appear and disappear together as a single entity.
    // `syncing` must be checked before `rebootDetected` - an active sync attempt is a live,
    // real-time state and should always visually override the stale "reboot happened, date
    // unknown" flag from before the tap. Otherwise, tapping sync right after a reboot would show
    // the amber "Sync Status: Unknown" state for the whole attempt (since rebootDetected doesn't
    // clear until a sync actually succeeds) instead of the blue "Syncing..." spinner, making a
    // real, in-progress attempt look like it never started before jumping straight to "Failed".
    val visualState = when {
        justSynced -> SyncVisualState(Icons.Filled.Check, AccentEmerald, rotating = false, label = "Synced")
        syncFailed -> SyncVisualState(Icons.Filled.WarningAmber, AccentRed, rotating = false, label = "Sync Failed")
        syncing -> SyncVisualState(Icons.Filled.Sync, AccentBlue, rotating = true, label = "Syncing...")
        rebootDetected -> SyncVisualState(Icons.Filled.WarningAmber, AccentAmber, rotating = false, label = "Sync Status: Unknown")
        lastSyncedLabel == null -> SyncVisualState(Icons.Filled.Sync, AccentBlue, rotating = false, label = "Tap to Sync Date")
        else -> SyncVisualState(Icons.Filled.Sync, AccentBlue, rotating = false, label = "Last synced: $lastSyncedLabel")
    }

    Row(
        modifier = Modifier
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

/** Bundles a sync pill's icon, tint, rotation flag, and label as one atomic snapshot - see [SyncPill]. */
private data class SyncVisualState(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val rotating: Boolean,
    val label: String
)

@Composable
private fun DayCell(
    date: LocalDate,
    today: LocalDate,
    completedCount: Int?,
    onClick: () -> Unit
) {
    val status = computeDayStatus(date, today, completedCount)
    val clickable = isDayClickable(status)

    if (status == DayStatus.OUTSIDE) return

    val (targetBackground, contentColor, borderColor) = when (status) {
        DayStatus.FUTURE -> Triple(Color.Transparent, ZincTextPrimary, ZincBorder)
        DayStatus.TODAY_INCOMPLETE -> Triple(AccentBlue, Color.White, null)
        DayStatus.TODAY_COMPLETE -> Triple(AccentEmerald, Color.White, null)
        DayStatus.GREEN -> Triple(AccentEmerald.copy(alpha = 0.9f), Color(0xFF022C22), null)
        DayStatus.YELLOW -> Triple(AccentAmber.copy(alpha = 0.9f), Color(0xFF451A03), null)
        DayStatus.RED -> Triple(AccentRed.copy(alpha = 0.85f), Color(0xFFFEF2F2), null)
        // Unreachable in practice - the early return above already exits for OUTSIDE. Kept only
        // because DayStatus is an enum and this `when` must stay exhaustive.
        DayStatus.OUTSIDE -> Triple(Color.Transparent, Color.Transparent, null)
    }

    // Cross-fades between colors when a day's status actually changes (e.g. finishing the last
    // task) - has no effect on first mount, so swiping to a new month never triggers a fade here.
    val background by animateColorAsState(targetValue = targetBackground, animationSpec = tween(200), label = "dayColor")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .let { if (borderColor != null) it.border(1.dp, borderColor, RoundedCornerShape(16.dp)) else it }
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
private fun LegendCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ZincSurface)
            .padding(16.dp)
    ) {
        Text(
            text = "LEGEND",
            color = ZincTextMuted,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(12.dp))
        val rows = listOf(
            AccentBlue to "Today (incomplete)",
            AccentEmerald to "Fully completed",
            AccentAmber to "Partially completed",
            AccentRed to "Nothing completed"
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
                    .border(1.dp, ZincBorder, CircleShape)
            )
            Text(text = "Future (locked)", color = ZincTextSecondary, fontSize = 13.sp)
        }
    }
}

/**
 * Builds a 7-wide grid of weeks for [month], with null placeholders for leading/trailing empty
 * cells. Always returns exactly [WEEKS_PER_GRID] rows (padding with an extra all-null week when a
 * month only spans 5 calendar rows) so the grid's total height never changes between months - this
 * keeps the month navigator and everything below it from shifting position when switching months.
 */
private fun buildWeeks(month: YearMonth): List<List<LocalDate?>> {
    val firstOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    // LocalDate.dayOfWeek: MONDAY=1..SUNDAY=7. We want Sunday-first columns, so map SUNDAY -> 0.
    val startOffset = firstOfMonth.dayOfWeek.value % 7

    val cells = mutableListOf<LocalDate?>()
    repeat(startOffset) { cells.add(null) }
    for (day in 1..daysInMonth) cells.add(month.atDay(day))
    while (cells.size % 7 != 0) cells.add(null)
    while (cells.size < WEEKS_PER_GRID * 7) cells.add(null)

    return cells.chunked(7)
}

private const val WEEKS_PER_GRID = 6
