package com.mochi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mochi.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Provides the Japanese font to composables that render kana/kanji (e.g. the card). */
val LocalJapaneseFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** The two font families used across the app. */
data class MochiFonts(val ui: FontFamily, val japanese: FontFamily)

/**
 * Loads the bundled fonts from their raw bytes (under composeResources/files/fonts/).
 * We read bytes rather than use the generated `Res.font.*` accessors, which aren't
 * reliably generated with the current AGP 9 KMP-library + Compose Resources setup.
 * Falls back to system fonts for the first frame, then swaps in once loaded.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberMochiFonts(): MochiFonts {
    val fonts by produceState(MochiFonts(FontFamily.Default, FontFamily.Default)) {
        value = MochiFonts(
            ui = FontFamily(
                loadFont("nunito_regular", Res.readBytes("files/fonts/nunito_regular.ttf"), FontWeight.Normal),
                loadFont("nunito_medium", Res.readBytes("files/fonts/nunito_medium.ttf"), FontWeight.Medium),
                loadFont("nunito_bold", Res.readBytes("files/fonts/nunito_bold.ttf"), FontWeight.Bold),
            ),
            japanese = FontFamily(
                loadFont("zmg_regular", Res.readBytes("files/fonts/zen_maru_gothic_regular.ttf"), FontWeight.Normal),
                loadFont("zmg_medium", Res.readBytes("files/fonts/zen_maru_gothic_medium.ttf"), FontWeight.Medium),
            ),
        )
    }
    return fonts
}

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
