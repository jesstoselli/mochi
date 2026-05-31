package com.mochi.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import java.io.File

actual fun loadFont(identity: String, bytes: ByteArray, weight: FontWeight): Font {
    // java.io.tmpdir maps to the app cache dir on Android, so no Context is needed.
    val file = File.createTempFile(identity, ".ttf")
    file.writeBytes(bytes)
    file.deleteOnExit()
    return Font(file = file, weight = weight)
}
