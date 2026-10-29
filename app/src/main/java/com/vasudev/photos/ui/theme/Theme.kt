package com.vasudev.photos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Nijatva brand palette — Green, Blue, White & Black.
 *
 * Sampled from the brand sheet:
 *   Greens  Forest #16522D · Emerald #119154 · Teal #1F8987 · Mint #8DE9BD · Lime #95EF78
 *   Blues   Navy   #0A377B · Royal   #154AB4 · Azure #2B9FF4 · Sky  #84CFF7 · Cyan #1EE6F6
 *   Neutrals Matte Black #131313 · Charcoal #3A3D42 · Slate #485661 · Silver #BCC0C3 · White #FFFFFF
 *
 * Dynamic (Material You) colour is intentionally not used: the app always renders in the
 * brand palette so it looks the same on every device.
 */

// Brand constants, handy for one-off accents in screens.
object BrandColors {
    val Forest = Color(0xFF16522D)
    val Emerald = Color(0xFF119154)
    val Teal = Color(0xFF1F8987)
    val Mint = Color(0xFF8DE9BD)
    val Lime = Color(0xFF95EF78)

    val Navy = Color(0xFF0A377B)
    val Royal = Color(0xFF154AB4)
    val Azure = Color(0xFF2B9FF4)
    val Sky = Color(0xFF84CFF7)
    val Cyan = Color(0xFF1EE6F6)

    val MatteBlack = Color(0xFF131313)
    val Charcoal = Color(0xFF3A3D42)
    val Slate = Color(0xFF485661)
    val Silver = Color(0xFFBCC0C3)
    val White = Color(0xFFFFFFFF)
}

private val BrandLightScheme = lightColorScheme(
    primary = Color(0xFF154AB4),             // Royal Blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),

    secondary = Color(0xFF119154),           // Emerald Green
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB6F2D0),
    onSecondaryContainer = Color(0xFF00210F),

    tertiary = Color(0xFF1F8987),            // Teal
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA7F0ED),
    onTertiaryContainer = Color(0xFF00201F),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFBFDFB),
    onBackground = Color(0xFF131313),        // Matte Black
    surface = Color(0xFFFBFDFB),
    onSurface = Color(0xFF131313),
    surfaceVariant = Color(0xFFDDE4E0),
    onSurfaceVariant = Color(0xFF414944),

    outline = Color(0xFF717973),
    outlineVariant = Color(0xFFC1C9C3),

    inverseSurface = Color(0xFF2E312F),
    inverseOnSurface = Color(0xFFEFF1EE),
    surfaceTint = Color(0xFF154AB4),
    scrim = Color(0xFF000000)
)

private val BrandDarkScheme = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002E6B),
    primaryContainer = Color(0xFF003C88),
    onPrimaryContainer = Color(0xFFD8E2FF),

    secondary = Color(0xFF7FDBA9),
    onSecondary = Color(0xFF00391F),
    secondaryContainer = Color(0xFF00522F),
    onSecondaryContainer = Color(0xFFB6F2D0),

    tertiary = Color(0xFF6FD5D2),
    onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF004F4D),
    onTertiaryContainer = Color(0xFFA7F0ED),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF0E1512),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF0E1512),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF414944),
    onSurfaceVariant = Color(0xFFC1C9C3),

    outline = Color(0xFF8B938D),
    outlineVariant = Color(0xFF414944),

    inverseSurface = Color(0xFFE1E3DF),
    inverseOnSurface = Color(0xFF2E312F),
    surfaceTint = Color(0xFFAEC6FF),
    scrim = Color(0xFF000000)
)

@Composable
fun ZohoPhotosTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) BrandDarkScheme else BrandLightScheme,
        content = content
    )
}
