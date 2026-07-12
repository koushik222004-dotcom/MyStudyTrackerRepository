package com.mystudytracker.app.ui.checklist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystudytracker.app.data.SectionDefinition
import com.mystudytracker.app.data.TaskCatalog
import com.mystudytracker.app.data.TaskItem
import com.mystudytracker.app.ui.theme.AccentBlue
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
import kotlinx.coroutines.delay

private val DATE_LABEL_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    date: LocalDate,
    viewModel: ChecklistViewModel,
    onBack: () -> Unit
) {
    val checked by viewModel.checked.collectAsState()
    val locked by viewModel.locked.collectAsState()
    val note by viewModel.note.collectAsState()
    var noteSheetOpen by rememberSaveable { mutableStateOf(false) }
    val completedCount = checked.values.count { it }
    val totalCount = TaskCatalog.totalTaskCount
    val allComplete = totalCount > 0 && completedCount == totalCount
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedProgressFraction by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(250),
        label = "checklistProgress"
    )

    Box(modifier = Modifier.fillMaxSize().background(ZincBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 84.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ZincTextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = date.format(DATE_LABEL_FORMAT), color = ZincTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text(text = "Daily checklist", color = ZincTextMuted, fontSize = 12.sp)
                }
                // Mirrors the back button on the opposite side, balancing the header. Lives outside
                // the dimmed/locked task area below, so a note can always be added or edited even
                // once the day is locked - locking only freezes tasks, never the note.
                val hasNote = !note.isNullOrBlank()
                IconButton(onClick = { noteSheetOpen = true }) {
                    Icon(
                        imageVector = if (hasNote) Icons.Filled.StickyNote2 else Icons.Outlined.StickyNote2,
                        contentDescription = if (hasNote) "Edit note" else "Add note",
                        tint = if (hasNote) AccentBlue else ZincTextPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .alpha(if (locked) 0.6f else 1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TaskCatalog.sections.forEach { section ->
                    SectionCard(
                        section = section,
                        date = date,
                        checked = checked,
                        locked = locked,
                        onToggle = { taskId -> viewModel.toggle(taskId) }
                    )
                }
            }
        }

        // Sticky, edge-to-edge bottom bar. Doubles as the permanent-lock control: once every task
        // is checked its own label becomes a one-tap "lock" affordance, so no new element is
        // introduced into the layout - it is always the same bar, just adapting.
        LockableBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            completedCount = completedCount,
            totalCount = totalCount,
            allComplete = allComplete,
            locked = locked,
            animatedProgressFraction = animatedProgressFraction,
            onLock = { viewModel.lockDay() }
        )
    }

    if (noteSheetOpen) {
        NoteSheet(
            date = date,
            initialNote = note,
            onDismiss = { noteSheetOpen = false },
            onSave = { text -> viewModel.saveNote(text) }
        )
    }
}

/**
 * Bottom sheet for the day's free-form note. Autosaves on a short debounce as the user types -
 * there is no explicit save button, matching the checklist's own no-friction, always-persisted
 * feel. The date is repeated as the sheet's own header so it's unambiguous which day's note is
 * being edited even after scrolling the checklist behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteSheet(
    date: LocalDate,
    initialNote: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialNote ?: "") }
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Debounced autosave: waits for a short pause in typing before persisting, so every keystroke
    // doesn't trigger its own database write.
    LaunchedEffect(text) {
        delay(400)
        onSave(text)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ZincSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(text = date.format(DATE_LABEL_FORMAT), color = ZincTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = "Note", color = ZincTextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                placeholder = { Text("Add a note for today...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ZincSurfaceVariant,
                    unfocusedContainerColor = ZincSurfaceVariant,
                    focusedTextColor = ZincTextPrimary,
                    unfocusedTextColor = ZincTextPrimary,
                    focusedIndicatorColor = AccentBlue,
                    unfocusedIndicatorColor = ZincBorder,
                    cursorColor = AccentBlue,
                    focusedPlaceholderColor = ZincTextMuted,
                    unfocusedPlaceholderColor = ZincTextMuted
                )
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LockableBottomBar(
    modifier: Modifier = Modifier,
    completedCount: Int,
    totalCount: Int,
    allComplete: Boolean,
    locked: Boolean,
    animatedProgressFraction: Float,
    onLock: () -> Unit
) {
    val interactive = allComplete && !locked
    val haptic = LocalHapticFeedback.current

    val label = when {
        locked -> "Checklist Locked"
        allComplete -> "Tap to Lock Checklist"
        else -> "$completedCount/$totalCount completed"
    }

    // Sticky, edge-to-edge bottom bar. It installs its own no-op clickable so it always consumes
    // its own touch events - taps here can never fall through to the checklist row underneath,
    // unlike the old floating pill. Once every task is checked, that same clickable becomes the
    // one-tap, permanent lock action - no new element, no confirmation dialog.
    val absorbTouches = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(ZincSurface)
            .clickable(interactionSource = absorbTouches, indication = null) {
                if (interactive) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLock()
                }
                // Otherwise intentionally empty: the bar exists to be a solid, tappable surface
                // that never passes touches through to whatever is rendered behind it.
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(ZincBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgressFraction)
                    .height(3.dp)
                    .background(AccentEmerald)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(targetState = label, label = "bottomBarLabel") { currentLabel ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Same lock glyph throughout - hollow before the day is finalized, filled solid
                    // once it is - so the two states read as before/after of one action rather than
                    // needing a wording change to tell them apart.
                    if (locked) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = ZincTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    } else if (allComplete) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = ZincTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = currentLabel,
                        color = if (locked) ZincTextMuted else ZincTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    section: SectionDefinition,
    date: LocalDate,
    checked: Map<String, Boolean>,
    locked: Boolean,
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
                    locked = locked,
                    onToggle = { onToggle("${section.key}.${task.key}") }
                )
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskItem, checked: Boolean, locked: Boolean, onToggle: () -> Unit) {
    // Independent, self-contained animation state for this one row's checkbox bounce - each row
    // animates on its own and never competes with the progress bar / strikethrough animations
    // that fire from the same tap.
    val checkboxScale = remember { Animatable(1f) }
    LaunchedEffect(checked) {
        if (checked) {
            checkboxScale.animateTo(1.18f, tween(90))
            checkboxScale.animateTo(1f, tween(90))
        }
    }

    // Fade between a plain label and a struck-through copy stacked in the same place, so the
    // strikethrough appears to fade in/out instead of snapping on.
    val strikeAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "strikeAlpha"
    )
    val labelColor by animateColorAsState(
        targetValue = if (checked) ZincTextMuted else ZincTextPrimary,
        animationSpec = tween(200),
        label = "labelColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !locked) { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .scale(checkboxScale.value)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) AccentEmerald else Color.Transparent)
                .border(2.dp, if (checked) AccentEmerald else ZincBorder, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = checked,
                enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.6f),
                exit = fadeOut(tween(80)) + scaleOut(tween(80), targetScale = 0.6f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = ZincBackground,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box {
            Text(text = task.label, color = labelColor, fontSize = 15.sp, modifier = Modifier.alpha(1f - strikeAlpha))
            Text(
                text = task.label,
                color = labelColor,
                fontSize = 15.sp,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.alpha(strikeAlpha)
            )
        }
    }
}
