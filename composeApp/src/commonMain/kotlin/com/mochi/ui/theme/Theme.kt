package com.mochi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = MatchaSoft,
    onPrimary = Color(0xFF18341C),
    primaryContainer = Color(0xFFD6EBD9),
    onPrimaryContainer = Color(0xFF18341C),
    secondary = SakuraPink,
    onSecondary = Color(0xFF5A2A35),
    secondaryContainer = Color(0xFFFFE0E7),
    onSecondaryContainer = Color(0xFF7A3344),
    background = RiceFlour,
    onBackground = Kurogoma,
    surface = PureWhite,
    onSurface = Kurogoma,
    surfaceVariant = Color(0xFFEFEFF2),
    onSurfaceVariant = Color(0xFF5C5C60),
    outline = Color(0xFFD9D9DE),
    outlineVariant = Color(0xFFE9E9ED),
)

private val DarkColors = darkColorScheme(
    primary = MatchaVibrant,
    onPrimary = Color(0xFF0A2410),
    primaryContainer = Color(0xFF274A2E),
    onPrimaryContainer = Color(0xFFBFE6C5),
    secondary = SakuraIntense,
    onSecondary = Color(0xFF3A0E16),
    secondaryContainer = Color(0xFF3A2A2E),
    onSecondaryContainer = Color(0xFFFFC9D2),
    background = AzukiNight,
    onBackground = PowderedSugar,
    surface = DarkBeanPaste,
    onSurface = PowderedSugar,
    surfaceVariant = Color(0xFF2E2E35),
    onSurfaceVariant = Color(0xFFB9B9C0),
    outline = Color(0xFF3A3A42),
    outlineVariant = Color(0xFF2E2E35),
)

// Generous, soft corners — the "mochi" feel. The card uses extraLarge (32dp).
private val MochiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MochiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MochiTypography,
        shapes = MochiShapes,
        content = content,
    )
}
