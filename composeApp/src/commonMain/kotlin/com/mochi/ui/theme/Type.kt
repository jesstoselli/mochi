package com.mochi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mochi.resources.Res
import org.jetbrains.compose.resources.Font

/** Nunito — UI, translations and romaji (Latin text). */
@Composable
fun rememberUiFontFamily(): FontFamily = FontFamily(
    Font(Res.font.nunito_regular, FontWeight.Normal),
    Font(Res.font.nunito_medium, FontWeight.Medium),
    Font(Res.font.nunito_bold, FontWeight.Bold),
)

/** Zen Maru Gothic — Japanese characters (kana + kanji). Subset to the deck's glyphs. */
@Composable
fun rememberJapaneseFontFamily(): FontFamily = FontFamily(
    Font(Res.font.zen_maru_gothic_regular, FontWeight.Normal),
    Font(Res.font.zen_maru_gothic_medium, FontWeight.Medium),
)

/** Provides the Japanese font to composables that render kana/kanji (e.g. the card). */
val LocalJapaneseFont = staticCompositionLocalOf { FontFamily.Default }

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
