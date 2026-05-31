package com.mochi.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mochi.resources.Res
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Lottie celebration animation (e.g. confetti on a correct answer).
 * Requires a LottieFiles JSON at composeResources/files/celebration.json.
 * Not wired into the first screen yet — this is the next Lottie study step.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun SuccessAnimation(modifier: Modifier = Modifier, loop: Boolean = false) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/celebration.json").decodeToString(),
        )
    }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (loop) Compottie.IterateForever else 1,
    )
    Image(
        painter = rememberLottiePainter(composition = composition, progress = { progress }),
        contentDescription = "Celebration",
        modifier = modifier,
    )
}
