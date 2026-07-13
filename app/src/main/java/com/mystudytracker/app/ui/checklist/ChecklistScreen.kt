package com.mystudytracker.app.ui.checklist

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mystudytracker.app.data.AttachmentType
import com.mystudytracker.app.data.DailyAttachment
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
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val attachments by viewModel.attachments.collectAsState()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
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
                // Reserves space for the sticky bottom bar (3dp progress + 52dp content = 55dp)
                // plus real breathing room, on top of the device's own navigation-bar inset - the
                // bar itself is edge-to-edge with navigationBarsPadding(), so without also applying
                // it here, 3-button-nav devices (~48dp inset) could clip the last section behind it.
                .navigationBarsPadding()
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
                // Paperclip button: turns blue when a remark or any file is stored for this day.
                // Lives outside the locked task area so remarks and files can always be added or
                // edited regardless of lock status - locking only freezes the task checkboxes.
                val hasContent = !note.isNullOrBlank() || attachments.isNotEmpty()
                IconButton(onClick = { sheetOpen = true }) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = if (hasContent) "Edit remarks & attachments" else "Add remarks or attachments",
                        tint = if (hasContent) AccentBlue else ZincTextPrimary,
                        modifier = Modifier.size(22.dp)
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

    if (sheetOpen) {
        RemarksAttachmentsSheet(
            date = date,
            initialNote = note,
            attachments = attachments,
            onDismiss = { sheetOpen = false },
            onSaveNote = { text -> viewModel.saveNote(text) },
            onAddAttachment = { filePath, type, displayName ->
                viewModel.addAttachment(filePath, type, displayName)
            },
            onRemoveAttachment = { id -> viewModel.removeAttachment(id) }
        )
    }
}

/**
 * Bottom sheet combining the day's free-form remark and any file attachments into one unified
 * surface. The remark autosaves on a debounce - no explicit save button needed.
 *
 * Sheet content uses [navigationBarsPadding] so it sits cleanly above the navigation bar,
 * matching the same pattern as the sticky bottom progress bar on the checklist screen.
 *
 * Files are copied to internal storage on pick so the paths remain stable indefinitely, unlike
 * raw content:// URIs from the system picker which can expire after the session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemarksAttachmentsSheet(
    date: LocalDate,
    initialNote: String?,
    attachments: List<DailyAttachment>,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit,
    onAddAttachment: (filePath: String, type: AttachmentType, displayName: String) -> Unit,
    onRemoveAttachment: (id: Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(initialNote ?: "") }
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Debounced autosave: waits for a short pause in typing before persisting, so every keystroke
    // doesn't trigger its own database write.
    LaunchedEffect(text) {
        delay(400)
        onSaveNote(text)
    }

    // One launcher per attachment type - each copies the picked file to internal storage then
    // hands the stable path + display name to the ViewModel.
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                copyToInternalStorage(context, it, date.toString(), AttachmentType.IMAGE)
                    ?.let { (path, name) -> onAddAttachment(path, AttachmentType.IMAGE, name) }
            }
        }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                copyToInternalStorage(context, it, date.toString(), AttachmentType.VIDEO)
                    ?.let { (path, name) -> onAddAttachment(path, AttachmentType.VIDEO, name) }
            }
        }
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                copyToInternalStorage(context, it, date.toString(), AttachmentType.AUDIO)
                    ?.let { (path, name) -> onAddAttachment(path, AttachmentType.AUDIO, name) }
            }
        }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                copyToInternalStorage(context, it, date.toString(), AttachmentType.DOCUMENT)
                    ?.let { (path, name) -> onAddAttachment(path, AttachmentType.DOCUMENT, name) }
            }
        }
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remarks & Attachments",
                        color = ZincTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = date.format(DATE_LABEL_FORMAT),
                        color = ZincTextMuted,
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = ZincTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── REMARK ───────────────────────────────────────────────────────────
            Text(
                text = "REMARK",
                color = ZincTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                placeholder = {
                    Text(
                        text = "Add a remark for today...",
                        color = ZincTextMuted,
                        fontSize = 14.sp
                    )
                },
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
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = ZincTextPrimary)
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = ZincBorder, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            // ── ATTACHMENTS ──────────────────────────────────────────────────────
            Text(
                text = "ATTACHMENTS",
                color = ZincTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            // Four type-picker buttons in a uniform row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttachTypeButton(
                    icon = Icons.Outlined.Image,
                    label = "Image",
                    tint = AccentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { imageLauncher.launch("image/*") }
                )
                AttachTypeButton(
                    icon = Icons.Outlined.Videocam,
                    label = "Video",
                    tint = Color(0xFF9C6FE4),
                    modifier = Modifier.weight(1f),
                    onClick = { videoLauncher.launch("video/*") }
                )
                AttachTypeButton(
                    icon = Icons.Outlined.Audiotrack,
                    label = "Audio",
                    tint = Color(0xFFE89D3A),
                    modifier = Modifier.weight(1f),
                    onClick = { audioLauncher.launch("audio/*") }
                )
                AttachTypeButton(
                    icon = Icons.Outlined.Description,
                    label = "Doc",
                    tint = AccentEmerald,
                    modifier = Modifier.weight(1f),
                    onClick = { documentLauncher.launch("*/*") }
                )
            }

            // Horizontally scrollable row of chips for every attached file
            if (attachments.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attachments.forEach { attachment ->
                        AttachmentChip(
                            attachment = attachment,
                            onRemove = { onRemoveAttachment(attachment.id) },
                            onOpen = { openAttachment(context, attachment) }
                        )
                    }
                }
            }
        }
    }
}

/** Outlined square button for selecting a file type. */
@Composable
private fun AttachTypeButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, ZincBorder, RoundedCornerShape(10.dp))
            .background(ZincSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = ZincTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Compact, dismissible chip for a single attachment. Tap to open; × to remove. */
@Composable
private fun AttachmentChip(
    attachment: DailyAttachment,
    onRemove: () -> Unit,
    onOpen: () -> Unit
) {
    val (icon, tint) = when (attachment.type) {
        AttachmentType.IMAGE    -> Icons.Outlined.Image       to AccentBlue
        AttachmentType.VIDEO    -> Icons.Outlined.Videocam    to Color(0xFF9C6FE4)
        AttachmentType.AUDIO    -> Icons.Outlined.Audiotrack  to Color(0xFFE89D3A)
        AttachmentType.DOCUMENT -> Icons.Outlined.Description to AccentEmerald
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ZincSurfaceVariant)
            .border(1.dp, ZincBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = attachment.displayName,
            color = ZincTextPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 130.dp)
        )
        Spacer(Modifier.width(2.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove attachment",
                tint = ZincTextMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * Copies a file from a system-picker content URI into app-internal storage.
 * Returns (absolutePath, displayName) on success, or null if the copy fails.
 *
 * Running on [Dispatchers.IO] keeps the main thread free during the copy.
 */
private suspend fun copyToInternalStorage(
    context: Context,
    uri: Uri,
    date: String,
    @Suppress("UNUSED_PARAMETER") type: AttachmentType
): Pair<String, String>? = withContext(Dispatchers.IO) {
    try {
        val cr = context.contentResolver
        val displayName = cr.query(uri, arrayOf("_display_name"), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: "file_${System.currentTimeMillis()}"
        val ext = displayName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
        val fileName = "${UUID.randomUUID()}${if (ext != null) ".$ext" else ""}"
        val dir = File(context.filesDir, "attachments/$date").also { it.mkdirs() }
        val dest = File(dir, fileName)
        cr.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        Pair(dest.absolutePath, displayName)
    } catch (_: Exception) {
        null
    }
}

/**
 * Opens an internal-storage attachment in an external viewer via [FileProvider].
 * Silently no-ops if the file no longer exists or no app can handle the MIME type.
 */
private fun openAttachment(context: Context, attachment: DailyAttachment) {
    try {
        val file = File(attachment.filePath)
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = when (attachment.type) {
            AttachmentType.IMAGE    -> "image/*"
            AttachmentType.VIDEO    -> "video/*"
            AttachmentType.AUDIO    -> "audio/*"
            AttachmentType.DOCUMENT -> "*/*"
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                },
                "Open with"
            )
        )
    } catch (_: Exception) {
        // No installed app can handle this file type - silently ignore.
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

    val label = when {
        locked      -> "Checklist Locked"
        allComplete -> "Tap to Lock Checklist"
        else        -> "$completedCount/$totalCount completed"
    }

    // Sticky, edge-to-edge bottom bar. It installs its own no-op clickable so it always consumes
    // its own touch events - taps here can never fall through to the checklist row underneath,
    // unlike the old floating pill. Once every task is checked, that same clickable becomes the
    // one-tap, permanent lock action - no new element, no confirmation dialog.
    val absorbTouches = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ZincSurface)
            .navigationBarsPadding()
            .clickable(interactionSource = absorbTouches, indication = null) {
                if (interactive) {
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
