package com.mystudytracker.app.ui.splash

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystudytracker.app.ui.theme.AccentBlue
import com.mystudytracker.app.util.AppText
import kotlinx.coroutines.delay

private const val TOTAL_DURATION_MS = 1000L
private const val FADE_IN_MS = 220
private const val FADE_OUT_MS = 200

/**
 * Fixed ~1 second blue splash shown on app launch and every time a date is opened, matching the
 * calendar masthead's title so the two never look out of sync. Text fades in, holds, then the
 * whole screen fades out into whatever comes next - [onFinished] is called once that's done.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var textAlpha by remember { mutableFloatStateOf(0f) }
    var screenAlpha by remember { mutableFloatStateOf(1f) }

    val animatedTextAlpha by animateFloatAsState(
        targetValue = textAlpha,
        animationSpec = tween(FADE_IN_MS, easing = LinearOutSlowInEasing),
        label = "splashTextAlpha"
    )
    val animatedScreenAlpha by animateFloatAsState(
        targetValue = screenAlpha,
        animationSpec = tween(FADE_OUT_MS),
        label = "splashScreenAlpha"
    )

    LaunchedEffect(Unit) {
        textAlpha = 1f
        delay(TOTAL_DURATION_MS - FADE_OUT_MS)
        screenAlpha = 0f
        delay(FADE_OUT_MS.toLong())
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AccentBlue)
            .alpha(animatedScreenAlpha),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = AppText.TITLE_LINE_1,
            color = Color.White,
            fontSize = 15.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.alpha(animatedTextAlpha)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = AppText.TITLE_LINE_2,
            color = Color.White,
            fontSize = 22.sp,
            letterSpacing = 0.4.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alpha(animatedTextAlpha)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = AppText.TITLE_LINE_3,
            color = Color.White,
            fontSize = 15.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.alpha(animatedTextAlpha)
        )
    }
}
