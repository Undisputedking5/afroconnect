package com.example.africonnect.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand palette ───────────────────────────────────────────────────────────
val Teal = Color(0xFF0E7C6B)
val TealDark = Color(0xFF075E51)
val TealLight = Color(0xFF5ED6C0)
val Sunset = Color(0xFFE8621A)
val SunsetLight = Color(0xFFFF9A62)
val Gold = Color(0xFFE0A526)
val Success = Color(0xFF16A34A)
val Danger = Color(0xFFDC2626)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEDE6),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Sunset,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1D2),
    onSecondaryContainer = Color(0xFF3A1300),
    tertiary = Gold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDC2),
    onTertiaryContainer = Color(0xFF3A2A00),
    background = Color(0xFFF5F7F6),
    onBackground = Color(0xFF111917),
    surface = Color.White,
    onSurface = Color(0xFF111917),
    surfaceVariant = Color(0xFFEBF0EE),
    onSurfaceVariant = Color(0xFF5B6764),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAF9),
    surfaceContainer = Color(0xFFF1F4F3),
    surfaceContainerHigh = Color(0xFFEBF0EE),
    surfaceContainerHighest = Color(0xFFE4EAE8),
    outline = Color(0xFFC4CDCA),
    outlineVariant = Color(0xFFE1E7E5),
    error = Danger,
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF075E51),
    onPrimaryContainer = Color(0xFFCDEDE6),
    secondary = SunsetLight,
    onSecondary = Color(0xFF4A1A00),
    secondaryContainer = Color(0xFF6B2A05),
    onSecondaryContainer = Color(0xFFFFDBCB),
    tertiary = Color(0xFFF2C766),
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFEDC2),
    background = Color(0xFF0D1311),
    onBackground = Color(0xFFE3EAE7),
    surface = Color(0xFF141B19),
    onSurface = Color(0xFFE3EAE7),
    surfaceVariant = Color(0xFF222B28),
    onSurfaceVariant = Color(0xFFA9B6B2),
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceContainerLow = Color(0xFF141B19),
    surfaceContainer = Color(0xFF18201E),
    surfaceContainerHigh = Color(0xFF1F2826),
    surfaceContainerHighest = Color(0xFF283230),
    outline = Color(0xFF3D4845),
    outlineVariant = Color(0xFF2A3431),
    error = Color(0xFFFF8A80),
)

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Immutable
data class ExtraColors(val success: Color, val gold: Color, val online: Color)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(success = Success, gold = Gold, online = Color(0xFF22C55E))
}

@Composable
fun AfriconnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

val CaptionStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
