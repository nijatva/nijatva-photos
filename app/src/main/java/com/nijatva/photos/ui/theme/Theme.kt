package com.nijatva.photos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Nijatva brand palette — Green, Blue, White & Black, plus the three accent choices
 * (Green / Saffron / Indigo) and four theme modes (System / Dark / Light / Black)
 * offered on the Appearance screen.
 *
 * Dynamic (Material You) colour is off by default; the Appearance screen can turn it on
 * for Android 12+, in which case wallpaper colours replace the accent.
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

    val Saffron = Color(0xFFE8A317)
    val Indigo = Color(0xFF3F51B5)
}

/** primary / onPrimary / primaryContainer / onPrimaryContainer per accent, per mode. */
private data class Accent(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
)

private val accents: Map<String, Accent> = mapOf(
    "green" to Accent(
        lightPrimary = Color(0xFF119154), lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFB6F2D0), lightOnContainer = Color(0xFF00210F),
        darkPrimary = Color(0xFF7FDBA9), darkOnPrimary = Color(0xFF00391F),
        darkContainer = Color(0xFF00522F), darkOnContainer = Color(0xFFB6F2D0)
    ),
    "saffron" to Accent(
        lightPrimary = Color(0xFFB26A00), lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFFFDFA8), lightOnContainer = Color(0xFF2A1700),
        darkPrimary = Color(0xFFFFC15E), darkOnPrimary = Color(0xFF432C00),
        darkContainer = Color(0xFF5F4100), darkOnContainer = Color(0xFFFFDFA8)
    ),
    "indigo" to Accent(
        lightPrimary = Color(0xFF3F51B5), lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFDDE1FF), lightOnContainer = Color(0xFF00105C),
        darkPrimary = Color(0xFFB9C3FF), darkOnPrimary = Color(0xFF0F1B6B),
        darkContainer = Color(0xFF2A3AA0), darkOnContainer = Color(0xFFDDE1FF)
    )
)

private fun accentFor(name: String): Accent = accents[name] ?: accents.getValue("green")

private fun lightScheme(accent: String) = accentFor(accent).let { a ->
    lightColorScheme(
        primary = a.lightPrimary,
        onPrimary = a.lightOnPrimary,
        primaryContainer = a.lightContainer,
        onPrimaryContainer = a.lightOnContainer,

        secondary = Color(0xFF154AB4),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD8E2FF),
        onSecondaryContainer = Color(0xFF001A41),

        tertiary = Color(0xFF1F8987),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFA7F0ED),
        onTertiaryContainer = Color(0xFF00201F),

        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),

        background = Color(0xFFFBFDFB),
        onBackground = Color(0xFF131313),
        surface = Color(0xFFFBFDFB),
        onSurface = Color(0xFF131313),
        surfaceVariant = Color(0xFFDDE4E0),
        onSurfaceVariant = Color(0xFF414944),

        outline = Color(0xFF717973),
        outlineVariant = Color(0xFFC1C9C3),

        inverseSurface = Color(0xFF2E312F),
        inverseOnSurface = Color(0xFFEFF1EE),
        surfaceTint = a.lightPrimary,
        scrim = Color(0xFF000000)
    )
}

private fun darkScheme(accent: String) = accentFor(accent).let { a ->
    darkColorScheme(
        primary = a.darkPrimary,
        onPrimary = a.darkOnPrimary,
        primaryContainer = a.darkContainer,
        onPrimaryContainer = a.darkOnContainer,

        secondary = Color(0xFFAEC6FF),
        onSecondary = Color(0xFF002E6B),
        secondaryContainer = Color(0xFF003C88),
        onSecondaryContainer = Color(0xFFD8E2FF),

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
        surfaceTint = a.darkPrimary,
        scrim = Color(0xFF000000)
    )
}

/** Pure-black variant for OLED screens: same dark scheme with black surfaces. */
private fun blackScheme(accent: String) = accentFor(accent).let { a ->
    darkColorScheme(
        primary = a.darkPrimary,
        onPrimary = a.darkOnPrimary,
        primaryContainer = a.darkContainer,
        onPrimaryContainer = a.darkOnContainer,

        secondary = Color(0xFFAEC6FF),
        onSecondary = Color(0xFF002E6B),
        secondaryContainer = Color(0xFF003C88),
        onSecondaryContainer = Color(0xFFD8E2FF),

        tertiary = Color(0xFF6FD5D2),
        onTertiary = Color(0xFF003735),
        tertiaryContainer = Color(0xFF004F4D),
        onTertiaryContainer = Color(0xFFA7F0ED),

        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),

        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF141414),
        onSurfaceVariant = Color(0xFFB8B8B8),

        outline = Color(0xFF3A3A3A),
        outlineVariant = Color(0xFF232323),

        inverseSurface = Color(0xFFFFFFFF),
        inverseOnSurface = Color(0xFF000000),
        surfaceTint = a.darkPrimary,
        scrim = Color(0xFF000000)
    )
}

@Composable
fun ZohoPhotosTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { ThemeState.load(context) }

    val systemDark = isSystemInDarkTheme()
    val dark = when (ThemeState.mode) {
        "light" -> false
        "dark", "black" -> true
        else -> systemDark
    }

    val scheme = when {
        ThemeState.dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        ThemeState.mode == "black" -> blackScheme(ThemeState.accent)
        dark -> darkScheme(ThemeState.accent)
        else -> lightScheme(ThemeState.accent)
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
