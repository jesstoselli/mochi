package com.mochi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mochi.resources.Res
import com.mochi.resources.nunito_bold
import com.mochi.resources.nunito_medium
import com.mochi.resources.nunito_regular
import com.mochi.resources.zen_maru_gothic_medium
import com.mochi.resources.zen_maru_gothic_regular
import org.jetbrains.compose.resources.Font

/** Provides the Japanese font to composables that render kana/kanji (e.g. the card). */
val LocalJapaneseFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** The two font families used across the app. */
data class MochiFonts(val ui: FontFamily, val japanese: FontFamily)

/**
 * Loads the bundled Mochi fonts (Nunito for UI, Zen Maru Gothic for Japanese) straight from the
 * multiplatform `Res.font` accessors, so Android and iOS share one implementation with no
 * platform-specific font loading.
 */
@Composable
fun rememberMochiFonts(): MochiFonts = MochiFonts(
    ui = FontFamily(
        Font(Res.font.nunito_regular, FontWeight.Normal),
        Font(Res.font.nunito_medium, FontWeight.Medium),
        Font(Res.font.nunito_bold, FontWeight.Bold),
    ),
    japanese = FontFamily(
        Font(Res.font.zen_maru_gothic_regular, FontWeight.Normal),
        Font(Res.font.zen_maru_gothic_medium, FontWeight.Medium),
    ),
)

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
