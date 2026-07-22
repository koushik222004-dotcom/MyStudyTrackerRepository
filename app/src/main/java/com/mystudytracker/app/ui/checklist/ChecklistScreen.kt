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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
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
    var leafMenuState by remember { mutableStateOf<LeafMenuState?>(null) }
    val allComplete = totalCount > 0 && completedCount >= totalCount
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedProgressFraction by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(250),
        label = "checklistProgress"
    )

    // Hardware/gesture back closes whichever panel is open — R&A first, leaf menu second.
    BackHandler(enabled = sheetOpen || leafMenuState != null) {
        when {
            sheetOpen -> sheetOpen = false
            else -> leafMenuState = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ZincBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Fixed header — stays on screen while content scrolls ─────────
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

            HorizontalDivider(color = ZincBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

            // ── Scrollable body ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 84.dp)
            ) {
                Spacer(Modifier.height(12.dp))

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
                            toggleGroupNotApplicable = viewModel::toggleGroupNotApplicable,
                            openLeafMenu = { state -> leafMenuState = state }
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

        // ── Leaf task options panel ─────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = leafMenuState != null,
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
                    ) { leafMenuState = null }
            )
        }

        AnimatedVisibility(
            visible = leafMenuState != null,
            enter = fadeIn(tween(220)) +
                scaleIn(tween(220), initialScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 1f)),
            exit = fadeOut(tween(160)) +
                scaleOut(tween(160), targetScale = 0.96f, transformOrigin = TransformOrigin(0.5f, 1f)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            leafMenuState?.let { state ->
                LeafActionMenu(
                    title = state.title,
                    notApplicable = state.notApplicable,
                    targetCount = state.targetCount,
                    onDismiss = { leafMenuState = null },
                    onToggleNotApplicable = {
                        viewModel.setNotApplicable(state.fullKey, !state.notApplicable)
                        leafMenuState = state.copy(notApplicable = !state.notApplicable)
                    },
                    onQuantityChange = { qty ->
                        viewModel.setQuantity(state.fullKey, qty)
                    }
                )
            }
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) AccentBlue.copy(alpha = 0.22f) else ZincSurfaceVariant)
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

    // Outer Column carries no horizontal padding so HorizontalDivider spans edge to edge.
    // Each content section manages its own horizontal padding internally.
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
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        // Carries its own horizontal padding.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 18.dp),
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

        // ── REMARK section ───────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ZincBorder,
                    unfocusedContainerColor = ZincSurfaceVariant,
                    focusedTextColor = ZincTextPrimary,
                    unfocusedTextColor = ZincTextPrimary,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = AccentBlue,
                    focusedPlaceholderColor = ZincTextMuted,
                    unfocusedPlaceholderColor = ZincTextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = ZincTextPrimary)
            )
            Spacer(Modifier.height(20.dp))
        }

        // Edge-to-edge divider — no horizontal padding on this element
        HorizontalDivider(color = ZincSurfaceVariant, thickness = 1.dp)

        // ── ATTACHMENTS section ──────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(20.dp))
        }
        Spacer(Modifier.height(6.dp))
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
                    .background(AccentRed.copy(alpha = 0.10f))
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
                // AnimatedContent aligns its outgoing/incoming content independently of the
                // parent Box it sits in - its own default is Alignment.TopStart. The parent Box
                // above centers *this composable's footprint*, but without this, the phase text
                // inside was being anchored top-start of that footprint, not centered. Since each
                // phase's text/row has a different width and height, "top-start of a differently
                // sized box" lands the text in a visibly different spot on every phase change,
                // which is what actually produced the odd/misplaced-looking text transition -
                // independent of the progress bar and independent of the size/fade behavior below.
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    // Pure crossfade — no position change at all. The outgoing phase fades out
                    // with a linear ease so it dissolves evenly; the incoming phase fades in with
                    // FastOutSlowIn so it appears quickly and settles smoothly into full opacity.
                    // The asymmetric durations (outgoing slightly shorter than incoming) keep the
                    // two alpha curves from perfectly mirroring each other, which is what makes a
                    // crossfade feel fluid rather than mechanical.
                    //
                    // SizeTransform is snapped: AnimatedContent animates its bounding box by
                    // default, which re-centers differently-sized content every frame and reads as
                    // a diagonal jump even with pure fades. Snapping kills that artifact entirely.
                    fadeIn(tween(260, delayMillis = 90, easing = FastOutSlowInEasing))
                        .togetherWith(fadeOut(tween(180, easing = LinearOutSlowInEasing)))
                        .using(SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> snap() }))
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
                    // Only the number rolls; "/$totalCount completed" stays fixed so the
                    // surrounding text doesn't jump or resize during the animation.
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = completedCount,
                            label = "completedCountRoll",
                            transitionSpec = {
                                if (targetState > initialState) {
                                    // Ticking up — new number rises in from below, old exits upward.
                                    (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                                        slideInVertically(tween(200, easing = FastOutSlowInEasing)) { it })
                                        .togetherWith(
                                            fadeOut(tween(150, easing = LinearOutSlowInEasing)) +
                                                slideOutVertically(tween(150, easing = LinearOutSlowInEasing)) { -it }
                                        )
                                } else {
                                    // Ticking down (uncheck) — new number drops in from above.
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

/** Bundles every checklist mutation the tree UI can trigger, so composables below only need one parameter. */
private data class LeafMenuState(
    val fullKey: String,
    val title: String,
    val notApplicable: Boolean,
    val targetCount: Int
)

private data class ChecklistActions(
    val toggleLeaf: (String) -> Unit,
    val setQuantity: (String, Int) -> Unit,
    val setNotApplicable: (String, Boolean) -> Unit,
    val toggleGroup: (List<String>) -> Unit,
    val toggleGroupNotApplicable: (List<String>) -> Unit,
    val openLeafMenu: (LeafMenuState) -> Unit
)

// Depth-based child container shading: each level gets a progressively darker surface so
// nested stacks read as distinct without indentation lines or explicit connectors.
private fun childContainerColor(depth: Int): Color = when (depth) {
    0    -> Color(0xFF111113) // Subtle step down from ZincSurface card (0xFF18181B)
    else -> Color(0xFF080809) // Subtle step down from depth-0
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
        // Section header — tap cascades check/uncheck on all leaves; long-press cascades N/A.
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
            // Checkbox — fixed 60dp box so its right edge matches group rows and leaf rows exactly.
            Box(
                modifier = Modifier.width(60.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                UnifiedCheckbox(
                    state = sectionState,
                    modifier = Modifier
                        .clickable(enabled = !locked) { actions.toggleGroup(sectionLeafKeys) }
                )
            }
        }

        HorizontalDivider(
            color = ZincBorder.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            section.children.forEachIndexed { index, node ->
                NodeRow(
                    node = node,
                    pathPrefix = section.key,
                    depth = 0,
                    taskStates = taskStates,
                    locked = locked,
                    actions = actions,
                    showTrailingDivider = index < section.children.lastIndex
                )
            }
        }
    }
}

/** Derived tri-state summary of a group of leaves - never stored, always computed from [taskStates]. */
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
    actions: ChecklistActions,
    showTrailingDivider: Boolean = false
) {
    val fullKey = "$pathPrefix.${node.key}"
    when (node) {
        is TaskLeaf -> LeafRow(fullKey = fullKey, title = node.title, state = taskStates[fullKey], locked = locked, actions = actions, showTrailingDivider = showTrailingDivider)
        is TaskGroup -> GroupRow(node = node, parentPrefix = pathPrefix, fullKey = fullKey, depth = depth, taskStates = taskStates, locked = locked, actions = actions, showTrailingDivider = showTrailingDivider)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupRow(
    node: TaskGroup,
    parentPrefix: String,
    fullKey: String,
    depth: Int,
    taskStates: Map<String, DailyTaskState>,
    locked: Boolean,
    actions: ChecklistActions,
    showTrailingDivider: Boolean = false
) {
    var expanded by remember(fullKey) { mutableStateOf(false) }
    val leafKeys = remember(node, parentPrefix) { TaskCatalog.leafKeysUnder(node, parentPrefix) }
    val groupState = aggregateState(leafKeys, taskStates)
    val labelColor by animateColorAsState(
        targetValue = if (groupState == GroupState.EXCLUDED || groupState == GroupState.DONE)
            ZincTextMuted else ZincTextPrimary,
        animationSpec = tween(200),
        label = "groupLabelColor_$fullKey"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(200),
        label = "chevronRotation_$fullKey"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Tap to expand/collapse; long-press cascades Not Applicable to all leaves underneath.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    enabled = !locked,
                    onClick = { expanded = !expanded },
                    onLongClick = { actions.toggleGroupNotApplicable(leafKeys) }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title — takes remaining space; wraps to a second line for long names,
            // with the row growing to fit via heightIn(min).
            Text(
                text = node.title,
                color = labelColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (groupState == GroupState.DONE) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            // Chevron sits LEFT of checkbox at a fixed gap so checkbox position
            // never shifts regardless of checkbox content width.
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = ZincTextMuted,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(chevronRotation)
            )
            Spacer(Modifier.width(6.dp))
            // Checkbox — same fixed 60dp box as section header and leaf rows.
            Box(
                modifier = Modifier.width(60.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                UnifiedCheckbox(
                    state = groupState,
                    modifier = Modifier
                        .clickable(enabled = !locked) { actions.toggleGroup(leafKeys) }
                )
            }
        }

        // Children — darker background differentiates children from parent header.
        // No internal vertical padding: each child leaf row is exactly the same height as
        // the parent header row so total height = number-of-rows × ROW_HEIGHT (52dp).
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(190, easing = LinearOutSlowInEasing)) + fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(childContainerColor(depth))
            ) {
                // Divider at the top so children open at a clear visual boundary
                HorizontalDivider(
                    color = ZincBorder.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
                node.children.forEachIndexed { i, child ->
                    NodeRow(
                        node = child,
                        pathPrefix = fullKey,
                        depth = depth + 1,
                        taskStates = taskStates,
                        locked = locked,
                        actions = actions,
                        showTrailingDivider = i < node.children.lastIndex
                    )
                }
            }
        }
        // Trailing divider only when collapsed — disappears when children open,
        // reappears when they close, so children expand into the divider's space.
        if (showTrailingDivider && !expanded) {
            HorizontalDivider(
                color = ZincBorder.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
        }
    }
}

/**
 * Unified checkbox for both groups and leaves, always on the trailing right edge.
 * EXCLUDED uses a variable-width "N/A" text pill so it matches the leaf N/A display exactly.
 * All other states use a fixed 22 dp square that matches the leaf boolean checkbox.
 */
@Composable
private fun UnifiedCheckbox(state: GroupState, modifier: Modifier = Modifier) {
    // Scale bounce — fires whenever state transitions to DONE, matching LeafRow behaviour.
    val checkboxScale = remember { Animatable(1f) }
    LaunchedEffect(state) {
        if (state == GroupState.DONE) {
            checkboxScale.animateTo(1.18f, tween(90))
            checkboxScale.animateTo(1f, tween(90))
        }
    }

    // EXCLUDED uses a variable-width pill — solid ZincBorder background creates
    // clear depth against the ZincSurface card without needing an explicit border stroke.
    if (state == GroupState.EXCLUDED) {
        Box(
            modifier = modifier
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ZincBorder)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N/A",
                color = ZincTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
        return
    }

    // Hard color switch — matches the leaf checkbox which also snaps instantly,
    // letting the scale bounce and icon fade carry all the animation weight.
    val bgColor = when (state) {
        GroupState.DONE    -> AccentEmerald
        GroupState.PARTIAL -> AccentBlue.copy(alpha = 0.22f)
        else               -> ZincSurfaceVariant // EMPTY
    }
    Box(
        modifier = modifier
            .size(22.dp)
            .scale(checkboxScale.value)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        // Check icon — fades + scales in when DONE, matching the leaf checkbox animation.
        androidx.compose.animation.AnimatedVisibility(
            visible = state == GroupState.DONE,
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.6f),
            exit  = fadeOut(tween(80))  + scaleOut(tween(80),  targetScale  = 0.6f)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = ZincBackground,
                modifier = Modifier.size(14.dp)
            )
        }
        // Remove icon — same fade + scale for PARTIAL.
        androidx.compose.animation.AnimatedVisibility(
            visible = state == GroupState.PARTIAL,
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.6f),
            exit  = fadeOut(tween(80))  + scaleOut(tween(80),  targetScale  = 0.6f)
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LeafRow(
    fullKey: String,
    title: String,
    state: DailyTaskState?,
    locked: Boolean,
    actions: ChecklistActions,
    showTrailingDivider: Boolean = false
) {
    val notApplicable = state?.notApplicable == true
    val target = state?.targetCount ?: 1
    val completed = state?.completedCount ?: 0
    val done = state.isDone()
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
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                enabled = !locked,
                onClick = {
                    if (notApplicable) actions.setNotApplicable(fullKey, false)
                    else actions.toggleLeaf(fullKey)
                },
                onLongClick = {
                    actions.openLeafMenu(
                        LeafMenuState(fullKey, title, notApplicable, target)
                    )
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title — leading, with strikethrough when done. Wraps to a second line
        // for long names; row grows via heightIn(min).
        Crossfade(
            targetState = done,
            animationSpec = tween(200),
            label = "taskStrike",
            modifier = Modifier.weight(1f)
        ) { isDone ->
            Text(
                text = title,
                color = labelColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
            )
        }
        Spacer(Modifier.width(8.dp))
        // Checkbox — right-edge anchored via widthIn(min). The trailing edge stays pinned even
        // when content grows wide (e.g. "67/100"), which grows inward into the min-width box.
        // Tap area is the whole Row; this box is display-only.
        // 20dp leading spacer = 14dp (chevron) + 6dp (gap) used in GroupRow.
        // Aligns checkbox right-edges of leaf rows with those of group rows.
        Spacer(Modifier.width(20.dp))
        // Fixed-width checkbox area — right-edge always aligned; content scrolls
        // horizontally if quantity text overflows (e.g. very large numbers).
        Box(
            modifier = Modifier.width(60.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    notApplicable -> Box(
                        modifier = Modifier
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ZincBorder)
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "N/A",
                            color = ZincTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    target > 1 -> Box(
                        modifier = Modifier
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (done) AccentEmerald else ZincBorder)
                            .padding(horizontal = 7.dp),
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
                            .size(22.dp)
                            .scale(checkboxScale.value)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (done) AccentEmerald else ZincSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
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
            }
        }
    }

    if (showTrailingDivider) {
        HorizontalDivider(
            color = ZincBorder.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
    }

}

/**
 * Long-press action panel for a single leaf task — overlay-based, not a Dialog.
 * Opening transition and positioning match the R&A panel exactly: fade + scale
 * from the bottom anchor, scrim behind, back gesture aware at the screen level.
 */
@Composable
private fun LeafActionMenu(
    title: String,
    notApplicable: Boolean,
    targetCount: Int,
    onDismiss: () -> Unit,
    onToggleNotApplicable: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    var quantity by remember(targetCount) { mutableStateOf(targetCount) }

    // Outer Column has no horizontal padding so dividers span edge to edge.
    // Content sections carry their own horizontal padding individually.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 32.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(ZincSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .navigationBarsPadding()
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TASK OPTIONS",
                    color = ZincTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = title,
                    color = ZincTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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

        // Edge-to-edge divider
        HorizontalDivider(color = ZincSurfaceVariant, thickness = 1.dp)

        // ── Not Applicable toggle ────────────────────────────────────────────
        // Tapping immediately applies and dismisses — same as N/A on any group row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (notApplicable) AccentBlue.copy(alpha = 0.10f) else Color.Transparent)
                .clickable { onToggleNotApplicable() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status dot — accent when active, muted when inactive
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (notApplicable) AccentBlue else ZincBorder)
            )
            Text(
                text = if (notApplicable) "Clear Not Applicable" else "Mark as Not Applicable",
                color = if (notApplicable) AccentBlue else ZincTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            // Active badge — always reserves its space so the row height never shifts;
            // alpha hides it when N/A is off.
            Box(
                modifier = Modifier
                    .alpha(if (notApplicable) 1f else 0f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentBlue.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "N/A",
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Edge-to-edge divider
        HorizontalDivider(color = ZincSurfaceVariant, thickness = 1.dp)

        // ── Quantity stepper ─────────────────────────────────────────────────
        // Changes apply immediately on each tap (autosave pattern, no Save button).
        // Fixed-width center box prevents +/- from shifting as digit count changes.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Minus — ZincSurfaceVariant lifts above ZincSurface panel without sinking into it
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ZincSurfaceVariant)
                    .clickable(
                        enabled = quantity > 1,
                        onClick = { quantity--; onQuantityChange(quantity) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    color = if (quantity > 1) ZincTextPrimary else ZincTextMuted,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light
                )
            }

            // Fixed-width box keeps +/- anchored regardless of digit count
            Box(
                modifier = Modifier.width(96.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = quantity,
                    label = "quantityRoll",
                    transitionSpec = {
                        if (targetState > initialState) {
                            // Ticking up — new number rises from below, old exits upward
                            (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                                slideInVertically(tween(200, easing = FastOutSlowInEasing)) { it })
                                .togetherWith(
                                    fadeOut(tween(150, easing = LinearOutSlowInEasing)) +
                                        slideOutVertically(tween(150, easing = LinearOutSlowInEasing)) { -it }
                                )
                        } else {
                            // Ticking down — new number drops in from above
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
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Plus — same depth treatment
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ZincSurfaceVariant)
                    .clickable { quantity++; onQuantityChange(quantity) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = ZincTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Light)
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}
