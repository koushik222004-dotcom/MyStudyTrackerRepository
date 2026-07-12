package com.mystudytracker.app.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Sync
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
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
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
    val bannerMessage by viewModel.bannerMessage.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val justSynced by viewModel.justSynced.collectAsState()

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

    if (bannerMessage != null) {
        LaunchedEffect(bannerMessage) {
            delay(4500)
            viewModel.clearBanner()
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
                lastSyncedLabel = lastSyncedLabel,
                onClick = { viewModel.syncDate() }
            )
        }

        if (bannerMessage != null) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ZincSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = bannerMessage ?: "",
                    color = ZincTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Calendar block is vertically centered in the remaining space between the
        // title above and the legend below, so the screen doesn't feel top-heavy.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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

@Composable
private fun SyncPill(
    syncing: Boolean,
    justSynced: Boolean,
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

    val label = when {
        syncing -> "Syncing..."
        else -> "Last synced: ${lastSyncedLabel ?: "never"}"
    }
    val tint = if (justSynced) AccentEmerald else AccentBlue

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ZincSurfaceVariant)
            .clickable(enabled = !syncing) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(targetState = justSynced, label = "syncIcon") { showCheck ->
            Icon(
                imageVector = if (showCheck) Icons.Filled.Check else Icons.Filled.Sync,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(15.dp)
                    .rotate(if (syncing && !showCheck) angle else 0f)
            )
        }
        Text(
            text = label,
            color = ZincTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

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

/** Builds a 7-wide grid of weeks for [month], with null placeholders for leading/trailing empty cells. */
private fun buildWeeks(month: YearMonth): List<List<LocalDate?>> {
    val firstOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    // LocalDate.dayOfWeek: MONDAY=1..SUNDAY=7. We want Sunday-first columns, so map SUNDAY -> 0.
    val startOffset = firstOfMonth.dayOfWeek.value % 7

    val cells = mutableListOf<LocalDate?>()
    repeat(startOffset) { cells.add(null) }
    for (day in 1..daysInMonth) cells.add(month.atDay(day))
    while (cells.size % 7 != 0) cells.add(null)

    return cells.chunked(7)
}
