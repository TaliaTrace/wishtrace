package com.wishtrace.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WishTraceLightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = SurfaceWhite,
    primaryContainer = LavenderSurface,
    onPrimaryContainer = Ink,
    secondary = BrandBlue,
    onSecondary = SurfaceWhite,
    secondaryContainer = BlueSurface,
    onSecondaryContainer = Ink,
    tertiary = Success,
    onTertiary = SurfaceWhite,
    tertiaryContainer = SuccessSurface,
    onTertiaryContainer = Success,
    error = ErrorRed,
    onError = SurfaceWhite,
    errorContainer = ErrorSoft,
    onErrorContainer = ErrorRed,
    background = Canvas,
    onBackground = Ink,
    surface = SurfaceWhite,
    onSurface = Ink,
    surfaceVariant = BlueSurface,
    onSurfaceVariant = InkMuted,
    outline = OutlineCool,
    outlineVariant = OutlineCool.copy(alpha = 0.72f),
)

@Composable
fun WishTraceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WishTraceLightColors,
        typography = WishTraceTypography,
        shapes = WishTraceShapes,
        content = content,
    )
}
