package com.mystudytracker.app.ui.calendar

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystudytracker.app.data.TaskCatalog
import com.mystudytracker.app.ui.theme.AccentAmber
import com.mystudytracker.app.ui.theme.AccentBlue
import com.mystudytracker.app.ui.theme.AccentEmerald
import com.mystudytracker.app.ui.theme.AccentRed
import com.mystudytracker.app.ui.theme.ZincBackground
import com.mystudytracker.app.ui.theme.ZincBorder
import com.mystudytracker.app.ui.theme.ZincSurface
import com.mystudytracker.app.ui.theme.ZincTextMuted
import com.mystudytracker.app.ui.theme.ZincTextPrimary
import com.mystudytracker.app.ui.theme.ZincTextSecondary
import com.mystudytracker.app.util.DateRules
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val WEEKDAY_LABELS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDateSelected: (LocalDate) -> Unit
) {
    val completedCounts by viewModel.completedCountByDate.collectAsState()
    val today = remember { LocalDate.now() }
    val startMonth = remember { YearMonth.from(DateRules.START_DATE) }
    val endMonth = remember { YearMonth.from(DateRules.END_DATE) }
    val initialMonth = remember {
        val current = YearMonth.from(today)
        when {
            current.isBefore(startMonth) -> startMonth
            current.isAfter(endMonth) -> endMonth
            else -> current
        }
    }
    var cursorMonth by rememberSaveable { mutableStateOf(initialMonth.toString()) }
    val month = YearMonth.parse(cursorMonth)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZincBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = "KOUSHIK'S NEET 2027 TRACKER",
            color = ZincTextMuted,
            fontSize = 13.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (month.isAfter(startMonth)) cursorMonth = month.minusMonths(1).toString() },
                    enabled = month.isAfter(startMonth)
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = if (month.isAfter(startMonth)) ZincTextPrimary else ZincBorder
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

            val weeks = remember(month) { buildWeeks(month) }
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

        LegendCard()
        Spacer(Modifier.height(20.dp))
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

    val (background, contentColor, borderColor) = when (status) {
        DayStatus.FUTURE -> Triple(Color.Transparent, ZincTextPrimary, ZincBorder)
        DayStatus.TODAY_INCOMPLETE -> Triple(AccentBlue, Color.White, null)
        DayStatus.TODAY_COMPLETE -> Triple(AccentEmerald, Color.White, null)
        DayStatus.GREEN -> Triple(AccentEmerald.copy(alpha = 0.9f), Color(0xFF022C22), null)
        DayStatus.YELLOW -> Triple(AccentAmber.copy(alpha = 0.9f), Color(0xFF451A03), null)
        DayStatus.RED -> Triple(AccentRed.copy(alpha = 0.85f), Color(0xFFFEF2F2), null)
        DayStatus.OUTSIDE -> Triple(Color.Transparent, Color.Transparent, null)
    }

    if (status == DayStatus.OUTSIDE) return

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
