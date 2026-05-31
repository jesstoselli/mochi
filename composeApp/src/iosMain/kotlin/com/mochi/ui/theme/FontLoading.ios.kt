package com.mochi.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight

actual fun loadFont(identity: String, bytes: ByteArray, weight: FontWeight): Font =
    Font(identity = identity, data = bytes, weight = weight)
