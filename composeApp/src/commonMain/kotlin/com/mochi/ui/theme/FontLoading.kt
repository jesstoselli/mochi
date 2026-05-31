package com.mochi.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight

/**
 * Builds a [Font] from raw bytes. Platform-specific because the byte-based Font
 * constructor only exists on skiko (iOS/desktop); on Android we write the bytes to a
 * temp file and use the File-based Font.
 */
expect fun loadFont(identity: String, bytes: ByteArray, weight: FontWeight): Font
