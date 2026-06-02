package com.mochi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mochi.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.File

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun rememberMochiFonts(): MochiFonts {
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

private fun loadFont(identity: String, bytes: ByteArray, weight: FontWeight): Font {
    // java.io.tmpdir maps to the app cache dir on Android, so no Context is needed.
    val file = File.createTempFile(identity, ".ttf")
    file.writeBytes(bytes)
    file.deleteOnExit()
    return Font(file = file, weight = weight)
}
