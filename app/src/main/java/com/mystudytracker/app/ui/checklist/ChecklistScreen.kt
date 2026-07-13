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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import com.mystudytracker.app.ui.theme.AccentRed

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "Daily checklist", color = ZincTextMuted, fontSize = 12.sp)
                }
                // "R&A" text badge instead of a generic icon - reads clearly at a glance as its own
                // labelled control. Fills with the accent color once a remark or attachment exists.
                val hasContent = !note.isNullOrBlank() || attachments.isNotEmpty()
                RemarkAttachmentBadge(active = hasContent, onClick = { sheetOpen = true })
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .alpha(if (locked) 0.6f else 1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

        LockableBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            completedCount = completedCount,
            totalCount = totalCount,
            allComplete = allComplete,
            locked = locked,
            animatedProgressFraction = animatedProgressFraction,
            onLock = { viewModel.lockDay() }
        )

        // ── Remark & Attachments panel ──────────────────────────────────────────────────────
        // A fixed-position overlay instead of a draggable bottom sheet: its bottom edge sits
        // pinned exactly above the navigation bar (via navigationBarsPadding on the panel itself)
        // for its entire lifetime, so there is no slide-through-the-nav-bar animation frame to get
        // wrong in the first place. Opening/closing is a fade + gentle scale from that fixed
        // anchor rather than a translation, which reads as a deliberate, professional micro-
        // transition instead of a sheet "sliding" into system UI territory.
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

/**
 * Compact "R&A" badge button that opens the Remark & Attachments sheet. A rounded-rectangle text
 * badge reads clearly as its own labelled control rather than relying on a generic icon glyph.
 * Fills with the accent color and border once a remark or attachment exists, matching the accent
 * language used elsewhere in the app (today's calendar highlight, focused text field border).
 */
@Composable
private fun RemarkAttachmentBadge(active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            // Mirrors the back arrow's built-in inset: that IconButton's 24dp glyph sits centered
            // in a 48dp touch target, so its visible edge is 12dp further in than this pill's box
            // would otherwise be. This padding pulls the pill in by the same amount so both
            // visible edges sit equally far from the screen edge.
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) AccentBlue.copy(alpha = 0.14f) else ZincSurfaceVariant)
            // Border only when active — accent signal that content already exists.
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

/**
 * Fixed-position panel (not a draggable sheet) for the day's free-form remark and file
 * attachments. It is composed directly in [ChecklistScreen]'s own overlay - not as a separate
 * platform dialog/window - anchored to the bottom of the screen with [navigationBarsPadding], so
 * its position never overlaps the system navigation bar at any point, including during its
 * fade/scale open and close transitions (see the [AnimatedVisibility] calls at the call site).
 *
 * A single "Upload File" button opens the system file picker with no MIME pre-filter so users
 * see all their files naturally. After selection the MIME type is validated: images, videos,
 * audio, documents (PDF/Office/text), and archives are accepted; anything else - APKs,
 * executables, etc. - shows an inline "Unsupported file format" error that auto-dismisses.
 *
 * The remark autosaves on a 400ms debounce. Height is capped with internal scrolling so a long
 * remark plus many attachments never pushes the panel off-screen - it always stays reachable and
 * dismissible. Closing works three ways: the header's close button, tapping the scrim behind the
 * panel, or the device back button/gesture (see [BackHandler] at the call site).
 */
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

    // Debounced autosave - waits for typing to pause before writing to the database.
    LaunchedEffect(text) {
        delay(400)
        onSaveNote(text)
    }

    // Eager flush on dismiss - covers the case where the panel is closed within the 400 ms
    // debounce window and the latest edit would otherwise be silently discarded.
    // rememberUpdatedState ensures onDispose always captures the current text value, not the
    // one from the first composition.
    val currentText by rememberUpdatedState(text)
    val currentOnSaveNote by rememberUpdatedState(onSaveNote)
    DisposableEffect(Unit) {
        onDispose { currentOnSaveNote(currentText) }
    }

    // Error banner auto-dismisses after 3 seconds so the user doesn't have to manually clear it.
    LaunchedEffect(fileError) {
        if (fileError != null) {
            delay(3000)
            fileError = null
        }
    }

    // Single launcher - opens all files (*/*). MIME validation happens after selection so the
    // system picker shows the user's full file library without artificial restrictions.
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

    // 20 % of screen height, clamped to [80 dp, 180 dp] so the field scales
    // proportionally on every device — small phones, normal, and large.
    val remarkFieldMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.20f).dp
        .coerceIn(80.dp, 180.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .shadow(elevation = 32.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(ZincSurface)
            // Swallow taps so they don't fall through to the scrim behind this panel and dismiss it.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 20.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
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

        // ── REMARK ──────────────────────────────────────────────────────────
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

        // ── ATTACHMENTS ─────────────────────────────────────────────────────
        Text(
            text = "ATTACHMENTS",
            color = ZincTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(10.dp))

        // Single upload button - cleaner than 4 separate type buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ZincSurfaceVariant)
                .clickable {
                    // Drop focus (and the keyboard) before handing off to the system picker.
                    // Without this, focus silently returns to the remark field the moment the
                    // picker closes, which reopens the keyboard and makes its cursor handle
                    // jump for a frame while everything resyncs. Clearing focus up front means
                    // that automatic refocus-and-reopen never happens - the field stays put,
                    // unfocused, until the user deliberately taps back into it - so there's no
                    // resize/refocus sequence left for the handle to glitch during. The panel's
                    // own position and size are completely untouched by this.
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

        // Unsupported format error - slides in below the button, auto-dismisses after 3s
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

        // Horizontally scrollable row of chips for each attached file
        if (attachments.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                attachments.forEach { attachment ->
                    // Keyed on the stable attachment id so each chip's remembered
                    // confirmingDelete state stays bound to that attachment - without this,
                    // removing an item mid-confirmation would leak "Delete?" onto whichever
                    // attachment slides into the same list position afterward.
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

/**
 * Compact dismissible chip for one attachment. Tap to open in the system viewer.
 * Tapping × morphs the chip inline via AnimatedContent to a 'Delete?' confirmation
 * row with a red ✓ (confirm) and × (cancel). Cancelling reverts with no side effects.
 */
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
            // ── Confirmation state ──────────────────────────────────────────
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
            // ── Normal state ────────────────────────────────────────────────
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

/**
 * Copies a picked content URI into app-internal storage on the IO dispatcher.
 * Returns (absolutePath, displayName) or null on failure.
 */
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
        // Files are stored under attachments/<date>/<type>/ so the folder stays organised even
        // when a single day has a mix of images, videos, audio, and documents.
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

/**
 * Opens an internal-storage file in an external viewer via FileProvider.
 * Silently no-ops if the file is gone or no installed app handles the MIME type.
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
        // No installed app can handle this file type.
    }
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

    // Inline confirmation state — same pattern as the attachment chip delete confirm.
    var confirming by remember { mutableStateOf(false) }
    // If the day becomes locked from outside, reset confirming state.
    LaunchedEffect(locked) { if (locked) confirming = false }

    // Four distinct phases drive AnimatedContent.
    val barPhase = when {
        locked     -> "locked"
        confirming -> "confirming"
        allComplete -> "complete"
        else        -> "progress"
    }

    val absorbTouches = remember { MutableInteractionSource() }
    // Hoisted above AnimatedContent so they survive transitions — creating them inside the
    // AnimatedContent lambda causes new instances to be allocated on every phase change.
    val yesInteraction = remember { MutableInteractionSource() }
    val noInteraction  = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ZincSurface)
            .navigationBarsPadding()
            .clickable(interactionSource = absorbTouches, indication = null) {
                when {
                    confirming  -> confirming = false  // tap outside = cancel
                    interactive -> confirming = true   // enter confirmation mode
                }
            }
    ) {
        // Hairline top separator — clean edge between scrollable content and the bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ZincBorder.copy(alpha = 0.5f))
        )
        // Progress track — ZincSurfaceVariant as the unfilled rail, AccentEmerald as fill.
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
                transitionSpec = {
                    // Absolute basic micro-transition: a plain in-place crossfade, nothing else.
                    // No slide, no scale, no drift - the old phase just dissolves into the new one
                    // at the same position, quickly enough to read as a small, deliberate touch
                    // rather than an animation you notice.
                    fadeIn(tween(150)).togetherWith(fadeOut(tween(150)))
                }
            ) { phase ->
                when (phase) {

                    // ── Locked ────────────────────────────────────────────────
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

                    // ── Are you sure? ─────────────────────────────────────────
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
                        // Yes — confirm lock
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
                        // No — cancel
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

                    // ── All complete, ready to lock ───────────────────────────
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

                    // ── In progress ───────────────────────────────────────────
                    else -> Text(
                        text = "$completedCount/$totalCount completed",
                        color = ZincTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Section & task rows ───────────────────────────────────────────────────────────────────────

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
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(ZincSurface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                tint = section.iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = section.title,
                color = ZincTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        val ruleText = section.ruleProvider?.invoke(date)
        if (ruleText != null) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .padding(start = 26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ZincSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = ruleText,
                    color = ZincTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(4.dp))
        } else {
            Spacer(Modifier.height(3.dp))
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
    val checkboxScale = remember { Animatable(1f) }
    LaunchedEffect(checked) {
        if (checked) {
            checkboxScale.animateTo(1.18f, tween(90))
            checkboxScale.animateTo(1f, tween(90))
        }
    }

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
            .padding(vertical = 6.dp),
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
        // Crossfade between plain and struck-through text: at rest only one Text node exists,
        // vs the previous Box approach which always kept both in the tree (50 extra nodes for
        // 25 tasks). Smooth fade still provided by Crossfade's animated alpha transition.
        Crossfade(
            targetState = checked,
            animationSpec = tween(200),
            label = "taskStrike"
        ) { isChecked ->
            Text(
                text = task.label,
                color = labelColor,
                fontSize = 15.sp,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}
