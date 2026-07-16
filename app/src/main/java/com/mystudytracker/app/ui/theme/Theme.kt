package com.mystudytracker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// The app is dark-only by design, matching the approved mockup - no light theme variant.
private val MyStudyTrackerColors = darkColorScheme(
    primary = AccentEmerald,
    secondary = AccentBlue,
    background = ZincBackground,
    surface = ZincSurface,
    surfaceVariant = ZincSurfaceVariant,
    onBackground = ZincTextPrimary,
    onSurface = ZincTextPrimary,
    onSurfaceVariant = ZincTextSecondary,
    outline = ZincBorder,
    error = AccentRed
)

@Composable
fun MyStudyTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MyStudyTrackerColors,
        typography = MyStudyTrackerTypography,
        content = content
    )
}
