package com.mochi.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.mochi.audio.AudioPlayer
import com.mochi.resources.Res
import com.mochi.ui.motion.LocalMotionPolicy
import com.mochi.ui.theme.Kurogoma
import com.mochi.ui.theme.MatchaSoft
import com.mochi.ui.theme.RiceFlour
import com.mochi.ui.theme.SakuraPink
import kotlinx.coroutines.launch

// Vector paths from the source artwork (docs/icon/mochi.svg), authored in a 0..64 viewBox.
// Drawn on a Canvas (like SuccessAnimation) so it renders identically on Android and iOS
// without relying on generated image resources.
private const val BODY =
    "M21.3753 10.2262C8.42769 16.5472 6 30.7109 6 37.4416C6.80923 44.465 8.42769 46.2208 " +
        "11.6646 49.7325C13.7277 51.9707 22.1479 55 31.0861 55C38.3692 55 46.0568 53.2442 " +
        "50.5076 50.6104C53.4748 48.8546 59.1601 41.8312 57.7906 32.1741C56.1722 20.7612 " +
        "48.0799 12.5673 43.2245 10.2262C35.9415 6.71448 26.2307 7.85578 21.3753 10.2262Z"
private const val SMILE =
    "M29.6364 35.2105C29.6364 37.0658 30.4242 38.9211 32 38.9211C33.5758 38.9211 34.3636 " +
        "36.4474 34.3636 35.2105"
private const val EYE_LEFT =
    "M24.2747 32.7368C21.969 32.7368 21.9691 38.9211 24.2747 38.9211C26.8097 38.9211 26.5803 " +
        "32.7368 24.2747 32.7368Z"
private const val EYE_RIGHT =
    "M39.6383 32.7368C37.3327 32.7368 37.3327 38.9211 39.6383 38.9211C42.1733 38.9211 41.9439 " +
        "32.7368 39.6383 32.7368Z"
private const val CHEEK_RIGHT =
    "M46.9148 40.2057C46.9493 39.1256 41.6676 38.9275 41.6332 40.0075C41.5953 41.195 46.8804 " +
        "41.2857 46.9148 40.2057Z"
private const val CHEEK_LEFT =
    "M22.638 40.2057C22.6724 39.1256 17.3908 38.9275 17.3563 40.0075C17.3185 41.195 22.6035 " +
        "41.2857 22.638 40.2057Z"
private const val BELLY =
    "M54.8202 24.2705C61.5263 38.6726 53.8148 49.2209 46.8339 52.0273C40.9067 54.0016 33.4301 " +
        "54.7721 26.83 54.2559C23.5304 53.9977 20.4678 53.4194 17.9384 52.5205C17.5555 52.3844 " +
        "17.1865 52.2398 16.831 52.0898C20.0195 53.0844 23.5735 53.4319 27.1542 53.1416C32.8544 " +
        "52.6795 38.6676 50.6004 43.245 46.8896C45.439 45.1111 48.1661 41.7821 50.4462 " +
        "37.5371C52.4898 33.7324 54.1887 29.1633 54.8202 24.2705Z"

private fun path(data: String): Path = PathParser().parsePathString(data).toPath()

private val bodyPath = path(BODY)
private val smilePath = path(SMILE)
private val eyeLeftPath = path(EYE_LEFT)
private val eyeRightPath = path(EYE_RIGHT)
private val cheekRightPath = path(CHEEK_RIGHT)
private val cheekLeftPath = path(CHEEK_LEFT)
private val bellyPath = path(BELLY)

private val Highlight = Color(0xFFFFFFFF)

/**
 * The Mochi mascot, drawn from vector paths and scaled to fill the given [modifier] size.
 * When [animateEntry] is true it drops in and settles with a springy little bounce.
 */
@Composable
fun MochiLogo(
    modifier: Modifier = Modifier,
    animateEntry: Boolean = true,
    interactive: Boolean = true,
) {
    val motionPolicy = LocalMotionPolicy.current
    val drop = remember { Animatable(if (animateEntry && motionPolicy.allowSpatialMotion) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val player = remember { AudioPlayer() }
    var clickSound by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(Unit) {
        clickSound = runCatching { Res.readBytes("files/click.wav") }.getOrNull()
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    suspend fun bounce() {
        drop.snapTo(1f)
        drop.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    fun onTap() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        clickSound?.let { player.play(it) }
        if (motionPolicy.allowSpatialMotion) scope.launch { bounce() }
    }
    LaunchedEffect(animateEntry, motionPolicy.reduced) {
        if (motionPolicy.reduced) {
            drop.snapTo(0f)
        } else if (animateEntry) {
            bounce()
        }
    }
    Canvas(
        modifier
            .graphicsLayer {
                val activeDrop = if (motionPolicy.allowSpatialMotion) drop.value else 0f
                translationY = activeDrop * -size.height * 0.35f
                val popScale = 1f - activeDrop * 0.18f
                scaleX = popScale
                scaleY = popScale
            }
            .then(
                if (interactive) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onTap() }
                } else {
                    Modifier
                },
            ),
    ) {
        val s = size.minDimension / 64f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(bodyPath, RiceFlour)
            drawPath(
                smilePath,
                SakuraPink,
                style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(eyeLeftPath, Kurogoma)
            drawPath(eyeRightPath, Kurogoma)
            drawPath(cheekRightPath, SakuraPink)
            drawPath(cheekLeftPath, SakuraPink)
            drawPath(bellyPath, SakuraPink)
            // Specular highlights: small ovals under the source's rotation matrices.
            highlight(
                0.588097f, -0.80879f, 0.759755f, 0.650209f, 10.351f, 25.0898f,
                radius = 2.13328f, radiusY = 1.39706f,
            )
            highlight(
                0.674438f, -0.738331f, 0.681063f, 0.732224f, 14.0923f, 19.3511f,
                radius = 4.39583f, radiusY = 2.76135f,
            )
            drawPath(bodyPath, MatchaSoft, style = Stroke(width = 1f))
        }
    }
}

@Suppress("LongParameterList")
private fun DrawScope.highlight(
    a: Float,
    b: Float,
    c: Float,
    d: Float,
    e: Float,
    f: Float,
    radius: Float,
    radiusY: Float,
) {
    val m = Matrix()
    m.values[0] = a
    m.values[1] = b
    m.values[4] = c
    m.values[5] = d
    m.values[12] = e
    m.values[13] = f
    withTransform({ transform(m) }) {
        // The source ellipse's center equals its radii, so its top-left is the origin.
        drawOval(Highlight, topLeft = Offset.Zero, size = Size(radius * 2, radiusY * 2))
    }
}
