package com.mochi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

/** Provides the Japanese font to composables that render kana/kanji (e.g. the card). */
val LocalJapaneseFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** The two font families used across the app. */
data class MochiFonts(val ui: FontFamily, val japanese: FontFamily)

/**
 * Loads the app fonts. Platform-specific: Android bundles Nunito + Zen Maru Gothic;
 * iOS falls back to the system fonts (which already render Japanese well).
 */
@Composable
expect fun rememberMochiFonts(): MochiFonts

/** Material typography with the UI font applied across every style. */
fun mochiTypography(ui: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = ui),
        displayMedium = base.displayMedium.copy(fontFamily = ui),
        displaySmall = base.displaySmall.copy(fontFamily = ui),
        headlineLarge = base.headlineLarge.copy(fontFamily = ui),
        headlineMedium = base.headlineMedium.copy(fontFamily = ui),
        headlineSmall = base.headlineSmall.copy(fontFamily = ui),
        titleLarge = base.titleLarge.copy(fontFamily = ui),
        titleMedium = base.titleMedium.copy(fontFamily = ui),
        titleSmall = base.titleSmall.copy(fontFamily = ui),
        bodyLarge = base.bodyLarge.copy(fontFamily = ui),
        bodyMedium = base.bodyMedium.copy(fontFamily = ui),
        bodySmall = base.bodySmall.copy(fontFamily = ui),
        labelLarge = base.labelLarge.copy(fontFamily = ui),
        labelMedium = base.labelMedium.copy(fontFamily = ui),
        labelSmall = base.labelSmall.copy(fontFamily = ui),
    )
}
