package com.mochi.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mochi.settings.MotionPreference

@Immutable
data class MotionPolicy(val reduced: Boolean) {
    val allowSpatialMotion: Boolean get() = !reduced
    val allowDecorativeMotion: Boolean get() = !reduced
    val allowInfiniteMotion: Boolean get() = !reduced
    val allowShortFades: Boolean get() = true

    companion object {
        val Full = MotionPolicy(reduced = false)
        val Reduced = MotionPolicy(reduced = true)
    }
}

fun resolveMotionPolicy(
    preference: MotionPreference,
    systemReduced: Boolean,
): MotionPolicy = when (preference) {
    MotionPreference.FULL -> MotionPolicy.Full
    MotionPreference.SYSTEM -> if (systemReduced) MotionPolicy.Reduced else MotionPolicy.Full
    MotionPreference.REDUCED -> MotionPolicy.Reduced
}

val LocalMotionPolicy = staticCompositionLocalOf { MotionPolicy.Full }

@Composable
fun MochiMotionProvider(
    preference: MotionPreference,
    content: @Composable () -> Unit,
) {
    val systemReduced = if (preference == MotionPreference.SYSTEM) {
        rememberSystemReducedMotion()
    } else {
        false
    }
    val policy = remember(preference, systemReduced) {
        resolveMotionPolicy(preference, systemReduced)
    }
    CompositionLocalProvider(LocalMotionPolicy provides policy, content = content)
}
