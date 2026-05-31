package com.mochi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

// TODO: swap these to the bundled fonts once the .ttf files are added under
// composeResources/font/ — Nunito for UI/Latin text, Zen Maru Gothic for Japanese.
// e.g. FontFamily(Font(Res.font.Nunito_Regular), Font(Res.font.Nunito_Bold, FontWeight.Bold))
val UiFontFamily: FontFamily = FontFamily.Default
val JapaneseFontFamily: FontFamily = FontFamily.Default

private val Base = Typography()

/** Material typography with the UI font applied across every style. */
val MochiTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = UiFontFamily),
    displayMedium = Base.displayMedium.copy(fontFamily = UiFontFamily),
    displaySmall = Base.displaySmall.copy(fontFamily = UiFontFamily),
    headlineLarge = Base.headlineLarge.copy(fontFamily = UiFontFamily),
    headlineMedium = Base.headlineMedium.copy(fontFamily = UiFontFamily),
    headlineSmall = Base.headlineSmall.copy(fontFamily = UiFontFamily),
    titleLarge = Base.titleLarge.copy(fontFamily = UiFontFamily),
    titleMedium = Base.titleMedium.copy(fontFamily = UiFontFamily),
    titleSmall = Base.titleSmall.copy(fontFamily = UiFontFamily),
    bodyLarge = Base.bodyLarge.copy(fontFamily = UiFontFamily),
    bodyMedium = Base.bodyMedium.copy(fontFamily = UiFontFamily),
    bodySmall = Base.bodySmall.copy(fontFamily = UiFontFamily),
    labelLarge = Base.labelLarge.copy(fontFamily = UiFontFamily),
    labelMedium = Base.labelMedium.copy(fontFamily = UiFontFamily),
    labelSmall = Base.labelSmall.copy(fontFamily = UiFontFamily),
)
