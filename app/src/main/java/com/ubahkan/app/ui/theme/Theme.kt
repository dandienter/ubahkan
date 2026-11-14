package com.ubahkan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================================
// UBAHKAN DESIGN SYSTEM  -  THEME v2.0 (MINIMALIS)
// Skema warna mengikuti token di Color.kt: satu aksen biru,
// netral hangat, tombol primer hitam ditangani di level layar.
// ============================================================

private val LightColorScheme = lightColorScheme(
    primary = UbahkanBlue,
    onPrimary = Color.White,
    primaryContainer = UbahkanBlueContainer,
    onPrimaryContainer = UbahkanBlueDark,

    secondary = UbahkanGray,
    onSecondary = Color.White,
    secondaryContainer = UbahkanSegmentTrack,
    onSecondaryContainer = UbahkanInk,

    tertiary = UbahkanBlue,
    onTertiary = Color.White,
    tertiaryContainer = UbahkanBlueContainer,
    onTertiaryContainer = UbahkanBlueDark,

    background = UbahkanLightBackground,
    onBackground = UbahkanLightTextPrimary,

    surface = UbahkanLightSurface,
    onSurface = UbahkanLightTextPrimary,

    surfaceVariant = UbahkanLightSurfaceVariant,
    onSurfaceVariant = UbahkanLightTextSecondary,

    outline = UbahkanLightOutline,
    outlineVariant = UbahkanLightDivider,

    error = UbahkanError,
    onError = Color.White,
    errorContainer = UbahkanErrorContainerLight,
    onErrorContainer = Color(0xFF8B1B24),

    surfaceTint = UbahkanBlue,
)

private val DarkColorScheme = darkColorScheme(
    primary = UbahkanBlueLight,
    onPrimary = Color(0xFF0A1E38),
    primaryContainer = UbahkanBlueContainerDark,
    onPrimaryContainer = Color(0xFFBFD9F7),

    secondary = UbahkanDarkTextSecondary,
    onSecondary = UbahkanDarkBackground,
    secondaryContainer = Color(0xFF2A2A27),
    onSecondaryContainer = UbahkanDarkTextPrimary,

    tertiary = UbahkanBlueLight,
    onTertiary = Color(0xFF0A1E38),
    tertiaryContainer = UbahkanBlueContainerDark,
    onTertiaryContainer = Color(0xFFBFD9F7),

    background = UbahkanDarkBackground,
    onBackground = UbahkanDarkTextPrimary,

    surface = UbahkanDarkSurface,
    onSurface = UbahkanDarkTextPrimary,

    surfaceVariant = UbahkanDarkSurfaceVariant,
    onSurfaceVariant = UbahkanDarkTextSecondary,

    outline = UbahkanDarkOutline,
    outlineVariant = Color(0xFF2A2A27),

    error = Color(0xFFEF7F87),
    onError = Color(0xFF3D0A0E),
    errorContainer = UbahkanErrorContainerDark,
    onErrorContainer = Color(0xFFFAB8BC),

    surfaceTint = UbahkanBlueLight,
)

// ============================================================
// SHAPE TOKENS  -  selaras mockup minimalis
// kartu 16dp, tombol 16dp, chip pill, sheet 28dp di atas
// ============================================================
val UbahkanShapes = Shapes(
    // ExtraSmall: label format kecil
    extraSmall = RoundedCornerShape(8.dp),
    // Small: chip dan kontrol kecil
    small = RoundedCornerShape(14.dp),
    // Medium: kartu dan tombol
    medium = RoundedCornerShape(16.dp),
    // Large: kartu fitur
    large = RoundedCornerShape(16.dp),
    // ExtraLarge: bottom sheet, dialog
    extraLarge = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
)

// ============================================================
// UBAHKAN THEME COMPOSABLE
// themeOption: 0 = System, 1 = Light, 2 = Dark
// ============================================================
@Composable
fun UbahkanTheme(
    themeOption: Int = 0,
    language: String = "id",
    content: @Composable () -> Unit
) {
    val isDark = when (themeOption) {
        1    -> false
        2    -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val strings = com.ubahkan.app.core.AppStrings.get(language)

    androidx.compose.runtime.CompositionLocalProvider(
        com.ubahkan.app.core.LocalAppStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = UbahkanShapes,
            typography = UbahkanTypography,
            content = content
        )
    }
}
