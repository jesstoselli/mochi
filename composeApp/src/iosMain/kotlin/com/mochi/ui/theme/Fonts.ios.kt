package com.mochi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * iOS uses the system fonts — it already renders Japanese with the system CJK font.
 * Bundling Nunito/Zen Maru on iOS is a follow-up (font-from-bytes interop).
 */
@Composable
actual fun rememberMochiFonts(): MochiFonts = MochiFonts(
    ui = FontFamily.Default,
    japanese = FontFamily.Default,
)
