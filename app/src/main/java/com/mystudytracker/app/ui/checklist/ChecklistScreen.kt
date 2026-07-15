package com.mystudytracker.app.ui.checklist

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mystudytracker.app.data.AttachmentType
import com.mystudytracker.app.data.CatalogNode
import com.mystudytracker.app.data.DailyAttachment
import com.mystudytracker.app.data.DailyTaskState
import com.mystudytracker.app.data.SectionDefinition
import com.mystudytracker.app.data.TaskCatalog
import com.mystudytracker.app.data.TaskGroup
import com.mystudytracker.app.data.TaskLeaf
import com.mystudytracker.app.data.isDone
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
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── MIME support list ──────────────────────────────────────────────────────────────────────────
// Broad prefix matches cover all image/video/audio sub-formats automatically. DOCUMENT covers
// office files, PDFs, archives, and plain text. Everything else - APKs, executables, system
// files, etc. - is rejected with a user-facing error rather than silently failing.

private val SUPPORTED_MIME_PREFIXES = setOf("image/", "video/", "audio/")

private val SUPPORTED_MIME_EXACT = setOf(
    // Documents
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "text/plain",
    "text/csv",
    // Archives
    "application/zip",
    "application/x-zip-compressed",
    "application/x-rar-compressed",
    "application/x-7z-compressed",
    "application/gzip",
    "application/x-gzip",
    "application/x-tar",
    "application/x-bzip2",
)

private fun isSupportedMime(mime: String): Boolean =
    SUPPORTED_MIME_PREFIXES.any { mime.startsWith(it) } || mime in SUPPORTED_MIME_EXACT

private fun attachmentTypeFromMime(mime: String): AttachmentType = when {
    mime.startsWith("image/") -> AttachmentType.IMAGE
    mime.startsWith("video/") -> AttachmentType.VIDEO
    mime.startsWith("audio/") -> AttachmentType.AUDIO
    else                      -> AttachmentType.DOCUMENT
}

// ── Screen ────────────────────────────────────────────────────────────────────────────────────

private val DATE_LABEL_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    date: LocalDate,
    viewModel: ChecklistViewModel,
    onBack: () -> Unit
) {
    val taskStates by viewModel.taskStates.collectAsState()
    val locked by viewModel.locked.collectAsState()
    val note by viewModel.note.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val completedCount by viewModel.completedUnits.collectAsState()
    val totalCount by viewModel.totalUnits.collectAsState()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val allComplete = totalCount > 0 && completedCount >= totalCount
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedProgressFraction by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(250),
        label = "checklistProgress"
    )

    // Hardware/gesture back closes the panel instead of leaving the screen while it's open.
    BackHandler(enabled = sheetOpen) { sheetOpen = false }

    Box(modifier = Modifier.fillMaxSize().background(ZincBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 84.dp)
        ) {
            // ── Top bar ──────────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ZincTextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = date.format(DATE_LABEL_FORMAT),
                        color = ZincTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Daily checklist",
                        color = ZincTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                val hasContent = !note.isNullOrBlank() || attachments.isNotEmpty()
                RemarkAttachmentBadge(active = hasContent, onClick = { sheetOpen = true })
            }

            Spacer(Modifier.height(8.dp))

            // ── Section list ─────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .alpha(if (locked) 0.55f else 1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val actions = remember(viewModel) {
                    ChecklistActions(
                        toggleLeaf = viewModel::toggleLeaf,
                        setQuantity = viewModel::setQuantity,
                        setNotApplicable = viewModel::setNotApplicable,
                        toggleGroup = viewModel::toggleGroup,
                        toggleGroupNotApplicable = viewModel::toggleGroupNotApplicable
                    )
                }
                TaskCatalog.sections.forEach { section ->
                    SectionCard(
                        section = section,
                        date = date,
                        taskStates = taskStates,
                        locked = locked,
                        actions = actions
                    )
                }
            }
        }

        LockableBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            completedCount = completedCount,
            totalCount = totalCount,
            allComplete = allComplete,
            locked = locked,
            animatedProgressFraction = animatedProgressFraction,
            onLock = { viewModel.lockDay() }
        )

        // ── Remark & Attachments scrim ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = sheetOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Close",
                        role = Role.Button
                    ) { sheetOpen = false }
            )
        }

        AnimatedVisibility(
            visible = sheetOpen,
            enter = fadeIn(tween(220)) +
                scaleIn(tween(220), initialScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 1f)),
            exit = fadeOut(tween(160)) +
                scaleOut(tween(160), targetScale = 0.96f, transformOrigin = TransformOrigin(0.5f, 1f)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            RemarkAttachmentsPanel(
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
}

// ── Remark & Attachments badge ─────────────────────────────────────────────────────────────────

@Composable
private fun RemarkAttachmentBadge(active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) AccentBlue.copy(alpha = 0.14f) else ZincSurfaceVariant)
            .let { if (active) it.border(1.dp, AccentBlue, RoundedCornerShape(8.dp)) else it }
            .clickable(
                onClickLabel = if (active) "Edit remark & attachments" else "Add remark or attachments",
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "R&A",
            color = if (active) AccentBlue else ZincTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}

// ── Remark & Attachments panel ─────────────────────────────────────────────────────────────────

@Composable
private fun RemarkAttachmentsPanel(
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
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf(initialNote ?: "") }
    var fileError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(text) {
        delay(400)
        onSaveNote(text)
    }

    val currentText by rememberUpdatedState(text)
    val currentOnSaveNote by rememberUpdatedState(onSaveNote)
    DisposableEffect(Unit) {
        onDispose { currentOnSaveNote(currentText) }
    }

    LaunchedEffect(fileError) {
        if (fileError != null) {
            delay(3000)
            fileError = null
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val mime = context.contentResolver.getType(it) ?: ""
            if (isSupportedMime(mime)) {
                fileError = null
                val type = attachmentTypeFromMime(mime)
                scope.launch {
                    copyToInternalStorage(context, it, date.toString(), type)
                        ?.let { (path, name) -> onAddAttachment(path, type, name) }
                }
            } else {
                fileError = "Unsupported file format"
            }
        }
    }

    val remarkFieldMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.20f).dp
        .coerceIn(80.dp, 180.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .shadow(elevation = 32.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(ZincSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Remark & Attachments",
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
                .heightIn(min = 80.dp, max = remarkFieldMaxHeight),
            maxLines = Int.MAX_VALUE,
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

        Text(
            text = "ATTACHMENTS",
            color = ZincTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ZincSurfaceVariant)
                .clickable {
                    focusManager.clearFocus()
                    fileLauncher.launch("*/*")
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FileUpload,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Upload File",
                color = ZincTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        AnimatedVisibility(
            visible = fileError != null,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1FE05252))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFE05252),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = fileError ?: "",
                        color = Color(0xFFE05252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (attachments.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                attachments.forEach { attachment ->
                    key(attachment.id) {
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

// ── Attachment chip ────────────────────────────────────────────────────────────────────────────

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
    var confirmingDelete by remember { mutableStateOf(false) }

    AnimatedContent(targetState = confirmingDelete, label = "chipDeleteConfirm") { confirming ->
        if (confirming) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ZincSurfaceVariant)
                    .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Delete?", color = ZincTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Confirm delete",
                        tint = AccentRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(onClick = { confirmingDelete = false }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = ZincTextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ZincSurfaceVariant)
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
                    onClick = { confirmingDelete = true },
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
    }
}

// ── File utilities ────────────────────────────────────────────────────────────────────────────

private suspend fun copyToInternalStorage(
    context: Context,
    uri: Uri,
    date: String,
    type: AttachmentType
): Pair<String, String>? = withContext(Dispatchers.IO) {
    try {
        val cr = context.contentResolver
        val displayName = cr.query(uri, arrayOf("_display_name"), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: "file_${System.currentTimeMillis()}"
        val ext = displayName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
        val fileName = "${UUID.randomUUID()}${if (ext != null) ".$ext" else ""}"
        val dir = File(context.filesDir, "attachments/$date/${type.name.lowercase()}").also { it.mkdirs() }
        val dest = File(dir, fileName)
        cr.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        Pair(dest.absolutePath, displayName)
    } catch (_: Exception) {
        null
    }
}

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
    } catch (_: Exception) { }
}

// ── Bottom bar ────────────────────────────────────────────────────────────────────────────────

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
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(locked) { if (locked) confirming = false }

    val barPhase = when {
        locked      -> "locked"
        confirming  -> "confirming"
        allComplete -> "complete"
        else        -> "progress"
    }

    val absorbTouches = remember { MutableInteractionSource() }
    val yesInteraction = remember { MutableInteractionSource() }
    val noInteraction  = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ZincSurface)
            .navigationBarsPadding()
            .clickable(interactionSource = absorbTouches, indication = null) {
                when {
                    confirming  -> confirming = false
                    interactive -> confirming = true
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ZincBorder.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(ZincSurfaceVariant)
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
            AnimatedContent(
                targetState = barPhase,
                label = "bottomBarPhase",
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    fadeIn(tween(260, delayMillis = 90, easing = FastOutSlowInEasing))
                        .togetherWith(fadeOut(tween(180, easing = LinearOutSlowInEasing)))
                        .using(SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> snap() }))
                }
            ) { phase ->
                when (phase) {
                    "locked" -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = ZincTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Checklist Locked",
                            color = ZincTextMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    "confirming" -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Are you sure?",
                            color = ZincTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentEmerald)
                                .clickable(
                                    interactionSource = yesInteraction,
                                    indication = null
                                ) { confirming = false; onLock() }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Yes",
                                color = ZincBackground,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ZincSurfaceVariant)
                                .clickable(
                                    interactionSource = noInteraction,
                                    indication = null
                                ) { confirming = false }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No",
                                color = ZincTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    "complete" -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = ZincTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Tap to Lock Checklist",
                            color = ZincTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = completedCount,
                            label = "completedCountRoll",
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                                        slideInVertically(tween(200, easing = FastOutSlowInEasing)) { it })
                                        .togetherWith(
                                            fadeOut(tween(150, easing = LinearOutSlowInEasing)) +
                                                slideOutVertically(tween(150, easing = LinearOutSlowInEasing)) { -it }
                                        )
                                } else {
                                    (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                                        slideInVertically(tween(200, easing = FastOutSlowInEasing)) { -it })
                                        .togetherWith(
                                            fadeOut(tween(150, easing = LinearOutSlowInEasing)) +
                                                slideOutVertically(tween(150, easing = LinearOutSlowInEasing)) { it }
                                        )
                                }.using(SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> snap() }))
                            }
                        ) { count ->
                            Text(
                                text = "$count",
                                color = ZincTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "/$totalCount completed",
                            color = ZincTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ── Section & task tree ───────────────────────────────────────────────────────────────────────

private data class ChecklistActions(
    val toggleLeaf: (String) -> Unit,
    val setQuantity: (String, Int) -> Unit,
    val setNotApplicable: (String, Boolean) -> Unit,
    val toggleGroup: (List<String>) -> Unit,
    val toggleGroupNotApplicable: (List<String>) -> Unit
)

// ── Depth-based child container shading ──────────────────────────────────────────────────────
// Each depth level gets a progressively slightly darker surface so nested stacks are visually
// distinct without needing indentation lines or explicit connectors.
private fun childContainerColor(depth: Int): Color = when (depth) {
    0    -> Color(0xFF27272A) // ZincSurfaceVariant - first level children
    else -> Color(0xFF1F1F22) // Slightly darker - deeper nesting
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionCard(
    section: SectionDefinition,
    date: LocalDate,
    taskStates: Map<String, DailyTaskState>,
    locked: Boolean,
    actions: ChecklistActions
) {
    val sectionLeafKeys = remember(section) { TaskCatalog.leafKeysUnder(section.children, section.key) }
    val sectionState = aggregateState(sectionLeafKeys, taskStates)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(ZincSurface)
    ) {
        // ── Section header ────────────────────────────────────────────────────────
        // Tap cascades check/uncheck on all leaves; long-press cascades "doesn't apply".
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = !locked,
                    onClick = { actions.toggleGroup(sectionLeafKeys) },
                    onLongClick = { actions.toggleGroupNotApplicable(sectionLeafKeys) }
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in a tinted pill container
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(section.iconTint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = section.iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    color = ZincTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val ruleText = section.ruleProvider?.invoke(date)
                if (ruleText != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = ruleText,
                        color = section.iconTint.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            AggregateStateIcon(state = sectionState)
        }

        // Hairline divider between header and children
        HorizontalDivider(
            color = ZincBorder.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )

        // ── Children ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            section.children.forEachIndexed { index, node ->
                NodeRow(
                    node = node,
                    pathPrefix = section.key,
                    depth = 0,
                    taskStates = taskStates,
                    locked = locked,
                    actions = actions
                )
                // Thin separator between sibling items (not after last)
                if (index < section.children.lastIndex) {
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

private enum class GroupState { EMPTY, PARTIAL, DONE, EXCLUDED }

private fun aggregateState(leafKeys: List<String>, taskStates: Map<String, DailyTaskState>): GroupState {
    if (leafKeys.isEmpty()) return GroupState.EMPTY
    val states = leafKeys.map { taskStates[it] }
    if (states.all { it?.notApplicable == true }) return GroupState.EXCLUDED
    val applicable = states.filterNot { it?.notApplicable == true }
    if (applicable.isEmpty()) return GroupState.EXCLUDED
    val allDone = applicable.all { it.isDone() }
    val noneStarted = applicable.all { (it?.completedCount ?: 0) == 0 }
    return when {
        allDone -> GroupState.DONE
        noneStarted -> GroupState.EMPTY
        else -> GroupState.PARTIAL
    }
}

@Composable
private fun NodeRow(
    node: CatalogNode,
    pathPrefix: String,
    depth: Int,
    taskStates: Map<String, DailyTaskState>,
    locked: Boolean,
    actions: ChecklistActions
) {
    val fullKey = "$pathPrefix.${node.key}"
    when (node) {
        is TaskLeaf  -> LeafRow(fullKey = fullKey, title = node.title, state = taskStates[fullKey], locked = locked, actions = actions)
        is TaskGroup -> GroupRow(node = node, parentPrefix = pathPrefix, fullKey = fullKey, depth = depth, taskStates = taskStates, locked = locked, actions = actions)
    }
}

// ── GroupRow ──────────────────────────────────────────────────────────────────────────────────
//
// Design: tapping the parent row toggles expand/collapse. Children slide in below with a shaded
// background container — a "child stack" rather than an indented tree accordion. Long-pressing
// the parent cascades "doesn't apply" to all leaves underneath.
//
// The chevron rotates 90° to indicate open/closed state; it is part of the row itself, not a
// separate icon button, so the entire row is the single interaction target.

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupRow(
    node: TaskGroup,
    parentPrefix: String,
    fullKey: String,
    depth: Int,
    taskStates: Map<String, DailyTaskState>,
    locked: Boolean,
    actions: ChecklistActions
) {
    var expanded by remember(fullKey) { mutableStateOf(false) }
    val leafKeys = remember(node, parentPrefix) { TaskCatalog.leafKeysUnder(node, parentPrefix) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(200),
        label = "chevron_$fullKey"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Parent row — tap to expand/collapse, long-press to cascade N/A
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    enabled = !locked,
                    onClick = { expanded = !expanded },
                    onLongClick = { actions.toggleGroupNotApplicable(leafKeys) }
                )
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = node.title,
                color = ZincTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            AggregateStateIcon(state = aggregateState(leafKeys, taskStates))
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = ZincTextMuted,
                modifier = Modifier
                    .size(15.dp)
                    .rotate(chevronRotation)
            )
        }

        // Children container — different shade background = child stack
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(190, easing = LinearOutSlowInEasing)) + fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(childContainerColor(depth))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                node.children.forEachIndexed { i, child ->
                    NodeRow(
                        node = child,
                        pathPrefix = fullKey,
                        depth = depth + 1,
                        taskStates = taskStates,
                        locked = locked,
                        actions = actions
                    )
                    if (i < node.children.lastIndex) {
                        HorizontalDivider(
                            color = ZincBorder.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AggregateStateIcon(state: GroupState) {
    val (icon, tint) = when (state) {
        GroupState.DONE     -> Icons.Filled.Check               to AccentEmerald
        GroupState.PARTIAL  -> Icons.Filled.Remove              to AccentBlue
        GroupState.EXCLUDED -> Icons.Filled.Block               to ZincTextMuted
        GroupState.EMPTY    -> Icons.Outlined.RadioButtonUnchecked to ZincTextMuted
    }
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
}

// ── LeafRow ───────────────────────────────────────────────────────────────────────────────────
//
// A single checkable task leaf. Tap to toggle done/pending; long-press opens the action menu
// (set quantity, mark N/A). The checkbox bounces subtly on completion and the label fades +
// strikes through when done.

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LeafRow(
    fullKey: String,
    title: String,
    state: DailyTaskState?,
    locked: Boolean,
    actions: ChecklistActions
) {
    val notApplicable = state?.notApplicable == true
    val target = state?.targetCount ?: 1
    val completed = state?.completedCount ?: 0
    val done = state.isDone()
    var menuOpen by remember { mutableStateOf(false) }

    val checkboxScale = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done) {
            checkboxScale.animateTo(1.18f, tween(90))
            checkboxScale.animateTo(1f, tween(90))
        }
    }

    val labelColor by animateColorAsState(
        targetValue = if (notApplicable) ZincTextMuted else if (done) ZincTextMuted else ZincTextPrimary,
        animationSpec = tween(200),
        label = "labelColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                enabled = !locked,
                onClick = { actions.toggleLeaf(fullKey) },
                onLongClick = { menuOpen = true }
            )
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            notApplicable -> Box(
                modifier = Modifier
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(2.dp, ZincBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "N/A", color = ZincTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            target > 1 -> Box(
                modifier = Modifier
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (done) AccentEmerald else ZincSurfaceVariant)
                    .border(2.dp, if (done) AccentEmerald else ZincBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$completed/$target",
                    color = if (done) ZincBackground else ZincTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            else -> Box(
                modifier = Modifier
                    .size(20.dp)
                    .scale(checkboxScale.value)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (done) AccentEmerald else Color.Transparent)
                    .border(2.dp, if (done) AccentEmerald else ZincBorder, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = done,
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
        }
        Spacer(Modifier.width(12.dp))
        Crossfade(
            targetState = done,
            animationSpec = tween(200),
            label = "taskStrike"
        ) { isDone ->
            Text(
                text = title,
                color = labelColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }

    if (menuOpen) {
        LeafActionMenu(
            title = title,
            notApplicable = notApplicable,
            targetCount = target,
            onDismiss = { menuOpen = false },
            onToggleNotApplicable = { actions.setNotApplicable(fullKey, !notApplicable) },
            onSetQuantity = { qty -> actions.setQuantity(fullKey, qty) }
        )
    }
}

@Composable
private fun LeafActionMenu(
    title: String,
    notApplicable: Boolean,
    targetCount: Int,
    onDismiss: () -> Unit,
    onToggleNotApplicable: () -> Unit,
    onSetQuantity: (Int) -> Unit
) {
    var quantity by remember(targetCount) { mutableStateOf(targetCount) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZincSurface,
        title = { Text(text = title, color = ZincTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleNotApplicable(); onDismiss() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (notApplicable) "Remove \"Doesn't Apply\"" else "Mark as \"Doesn't Apply\"",
                        color = ZincTextPrimary,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = ZincBorder, thickness = 1.dp)
                Spacer(Modifier.height(12.dp))
                Text(text = "QUANTITY", color = ZincTextMuted, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity -= 1 },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(ZincSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) { Text(text = "-", color = ZincTextPrimary, fontSize = 16.sp) }
                    }
                    Text(text = "$quantity", color = ZincTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(
                        onClick = { quantity += 1 },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(ZincSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) { Text(text = "+", color = ZincTextPrimary, fontSize = 16.sp) }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onSetQuantity(quantity); onDismiss() }) {
                Text(text = "Save Quantity", color = AccentBlue)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = ZincTextMuted)
            }
        }
    )
}
