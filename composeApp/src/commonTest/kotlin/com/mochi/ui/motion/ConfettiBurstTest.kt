package com.mochi.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfettiBurstTest {

    private fun particle(pos: Offset, vel: Offset) =
        Particle(position = pos, velocity = vel, color = Color.Red, size = 4f, rotation = 0f)

    @Test
    fun advanceMovesByVelocityScaledByDeltaAndAddsGravity() {
        val p = particle(Offset(0f, 0f), Offset(10f, 0f))
        // dt = 1.0 so math is easy to assert; gravity pulls y down (positive).
        val next = p.advance(dtSeconds = 1f, gravity = 100f)
        assertEquals(10f, next.position.x)
        assertEquals(0f, next.position.y) // velocity.y was 0 at the start of this step
        assertEquals(100f, next.velocity.y) // gravity accelerated it
    }

    @Test
    fun advanceIntegratesGravityOverTwoSteps() {
        var p = particle(Offset(0f, 0f), Offset(0f, 0f))
        p = p.advance(dtSeconds = 1f, gravity = 100f) // v.y -> 100
        p = p.advance(dtSeconds = 1f, gravity = 100f) // pos.y += 100, v.y -> 200
        assertEquals(100f, p.position.y)
        assertEquals(200f, p.velocity.y)
    }

    @Test
    fun rotationAdvancesEachStep() {
        val p = particle(Offset.Zero, Offset.Zero).copy(spin = 90f)
        val next = p.advance(dtSeconds = 1f, gravity = 0f)
        assertTrue(next.rotation == 90f)
    }
}
