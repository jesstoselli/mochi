package com.mochi.ui.theme

import androidx.compose.runtime.Composable

/**
 * Syncs the system bars (status/navigation) icon appearance with the app theme, so the
 * clock/wifi/battery icons stay readable when the app theme is forced light or dark.
 * No-op on platforms where it doesn't apply.
 */
@Composable
expect fun SystemBarsEffect(darkTheme: Boolean)
