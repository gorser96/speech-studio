package com.speech_studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = AccentLight,
    onPrimary = OnAccentLight,
    primaryContainer = AccentContainerLight,
    onPrimaryContainer = OnAccentContainerLight,
    inversePrimary = AccentDark,
    secondary = OnAccentContainerLight,
    onSecondary = OnAccentLight,
    secondaryContainer = AccentContainerLight,
    onSecondaryContainer = OnAccentContainerLight,
    tertiary = AccentLight,
    onTertiary = OnAccentLight,
    tertiaryContainer = SurfaceVariantLight,
    onTertiaryContainer = OnSurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = AccentLight,
    inverseSurface = OnSurfaceLight,
    inverseOnSurface = SurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = OutlineLight,
    outlineVariant = LineLight,
    surfaceBright = SurfaceLight,
    surfaceDim = BackgroundLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceContainerLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceVariantLight,
    surfaceContainerHighest = SurfaceVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    primaryContainer = AccentContainerDark,
    onPrimaryContainer = OnAccentContainerDark,
    inversePrimary = AccentLight,
    secondary = OnAccentContainerDark,
    onSecondary = OnAccentDark,
    secondaryContainer = AccentContainerDark,
    onSecondaryContainer = OnAccentContainerDark,
    tertiary = AccentDark,
    onTertiary = OnAccentDark,
    tertiaryContainer = SurfaceVariantDark,
    onTertiaryContainer = OnSurfaceDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = AccentDark,
    inverseSurface = OnSurfaceDark,
    inverseOnSurface = SurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = LineDark,
    surfaceBright = SurfaceVariantDark,
    surfaceDim = BackgroundDark,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceContainerDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceVariantDark,
    surfaceContainerHighest = SurfaceVariantDark
)

/**
 * Шкала скруглений стиля Bento: плоские карточки различаются линией и фоном,
 * а не тенью, поэтому форма задаётся одним рядом значений.
 */
private val SpeechStudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

/**
 * Dynamic color is available only on Android 12+; other platforms return `null`
 * and use the common light/dark schemes below.
 */
@Composable
expect fun platformColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
fun SpeechStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = (if (dynamicColor) platformColorScheme(darkTheme) else null)
        ?: if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SpeechStudioShapes,
        content = content
    )
}
