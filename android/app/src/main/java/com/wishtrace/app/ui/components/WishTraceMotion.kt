package com.wishtrace.app.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberWishTraceMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }.getOrDefault(true)
    }
}

@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val motionEnabled = rememberWishTraceMotionEnabled()
    var visible by remember { mutableStateOf(!motionEnabled) }
    LaunchedEffect(motionEnabled) {
        visible = true
    }
    val enter = if (motionEnabled) {
        val delay = index * 55
        fadeIn(
            animationSpec = tween(
                durationMillis = 260,
                delayMillis = delay,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 340,
                delayMillis = delay,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetY = { fullHeight -> fullHeight / 7 },
        ) + scaleIn(
            initialScale = 0.985f,
            animationSpec = tween(
                durationMillis = 340,
                delayMillis = delay,
                easing = FastOutSlowInEasing,
            ),
        )
    } else {
        EnterTransition.None
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
    ) {
        content()
    }
}
