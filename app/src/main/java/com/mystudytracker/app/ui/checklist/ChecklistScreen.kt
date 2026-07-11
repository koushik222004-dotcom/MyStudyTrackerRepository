package com.mystudytracker.app.ui.checklist

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystudytracker.app.data.SectionDefinition
import com.mystudytracker.app.data.TaskCatalog
import com.mystudytracker.app.data.TaskItem
import com.mystudytracker.app.ui.theme.AccentEmerald
import com.mystudytracker.app.ui.theme.ZincBackground
import com.mystudytracker.app.ui.theme.ZincBorder
import com.mystudytracker.app.ui.theme.ZincSurface
import com.mystudytracker.app.ui.theme.ZincSurfaceVariant
import com.mystudytracker.app.ui.theme.ZincTextMuted
import com.mystudytracker.app.ui.theme.ZincTextPrimary
import com.mystudytracker.app.ui.theme.ZincTextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())

@Composable
fun ChecklistScreen(
    date: LocalDate,
    viewModel: ChecklistViewModel,
    onBack: () -> Unit
) {
    val checked by viewModel.checked.collectAsState()
    val completedCount = checked.values.count { it }

    Box(modifier = Modifier.fillMaxSize().background(ZincBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ZincTextPrimary)
                }
                Column {
                    Text(text = date.format(DATE_LABEL_FORMAT), color = ZincTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text(text = "Daily checklist", color = ZincTextMuted, fontSize = 12.sp)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TaskCatalog.sections.forEach { section ->
                    SectionCard(
                        section = section,
                        date = date,
                        checked = checked,
                        onToggle = { taskId -> viewModel.toggle(taskId) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ZincSurface)
                .border(1.dp, ZincBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Completed: $completedCount / ${TaskCatalog.totalTaskCount}",
                color = ZincTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SectionCard(
    section: SectionDefinition,
    date: LocalDate,
    checked: Map<String, Boolean>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ZincSurface)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                tint = section.iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = section.title, color = ZincTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        val ruleText = section.ruleProvider?.invoke(date)
        if (ruleText != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(start = 26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ZincSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(text = ruleText, color = ZincTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
        } else {
            Spacer(Modifier.height(4.dp))
        }

        Column(modifier = Modifier.padding(start = 26.dp)) {
            section.tasks.forEach { task ->
                TaskRow(
                    task = task,
                    checked = checked["${section.key}.${task.key}"] ?: false,
                    onToggle = { onToggle("${section.key}.${task.key}") }
                )
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskItem, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) AccentEmerald else Color.Transparent)
                .border(2.dp, if (checked) AccentEmerald else ZincBorder, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = ZincBackground,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = task.label,
            color = if (checked) ZincTextMuted else ZincTextPrimary,
            fontSize = 15.sp,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}
