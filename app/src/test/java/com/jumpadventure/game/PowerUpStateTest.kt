package com.jumpadventure.game

import com.jumpadventure.game.engine.PowerUpState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the activation / duration / charge rules that previously lived as five
 * hand-rolled counters inside GameView.
 */
class PowerUpStateTest {

    private fun state() = PowerUpState()

    // ---------------------------------------------------------------- charges

    @Test
    fun `collecting adds a charge but does not start the timer`() {
        val s = state()
        s.magnet.addCharge()

        assertEquals(1, s.magnet.charges)
        assertFalse("collecting must only bank a charge", s.magnet.isActive)
        assertEquals(0f, s.magnet.timeRemaining, 0.0001f)
    }

    @Test
    fun `charges are capped at the documented maximum`() {
        val s = state()
        repeat(20) { s.magnet.addCharge() }
        assertEquals(PowerUpState.MAX_CHARGES, s.magnet.charges)
    }

    @Test
    fun `spending a charge when none are banked fails and starts nothing`() {
        val s = state()
        assertFalse(s.speed.consumeChargeAndActivate(8f))
        assertFalse(s.speed.isActive)
        assertEquals(0, s.speed.charges)
    }

    @Test
    fun `spending a charge activates the slot and decrements the rack`() {
        val s = state()
        s.speed.addCharge()
        s.speed.addCharge()

        assertTrue(s.speed.consumeChargeAndActivate(8f))
        assertEquals(1, s.speed.charges)
        assertTrue(s.speed.isActive)
        assertEquals(8f, s.speed.timeRemaining, 0.0001f)
    }

    // --------------------------------------------------------------- duration

    @Test
    fun `timer counts down and deactivates exactly at zero`() {
        val s = state()
        s.magnet.activate(2f)

        s.tick(1.0f)
        assertTrue(s.magnet.isActive)
        assertEquals(1.0f, s.magnet.timeRemaining, 0.0001f)

        s.tick(1.0f)
        assertFalse("must deactivate when the timer reaches zero", s.magnet.isActive)
        assertEquals(0f, s.magnet.timeRemaining, 0.0001f)
    }

    @Test
    fun `timer never goes negative when overshooting`() {
        val s = state()
        s.highJump.activate(1f)
        s.tick(5f)
        assertEquals(0f, s.highJump.timeRemaining, 0.0001f)
        assertFalse(s.highJump.isActive)
    }

    @Test
    fun `re-activating an active slot refreshes to full duration`() {
        val s = state()
        s.speed.activate(8f)
        s.tick(6f)
        assertEquals(2f, s.speed.timeRemaining, 0.0001f)

        s.speed.activate(8f)
        assertEquals(8f, s.speed.timeRemaining, 0.0001f)
        assertTrue(s.speed.isActive)
    }

    @Test
    fun `progress reports the fraction of duration remaining`() {
        val s = state()
        s.power.activate(10f)
        assertEquals(1f, s.power.progress, 0.0001f)
        s.tick(5f)
        assertEquals(0.5f, s.power.progress, 0.0001f)
        s.tick(5f)
        assertEquals(0f, s.power.progress, 0.0001f)
    }

    @Test
    fun `inactive slots do not tick`() {
        val s = state()
        s.speed.tick(3f)
        assertFalse(s.speed.isActive)
        assertEquals(0f, s.speed.timeRemaining, 0.0001f)
    }

    // ----------------------------------------------------------------- shield

    @Test
    fun `shield activates with plates and absorbs hits one at a time`() {
        val s = state()
        s.activateShield(10f, 3)

        assertTrue(s.isShieldProtecting)
        assertEquals(3, s.shieldHitsRemaining)

        assertTrue(s.absorbShieldHit())
        assertEquals(2, s.shieldHitsRemaining)
        assertTrue("still protecting with plates left", s.isShieldProtecting)

        assertTrue(s.absorbShieldHit())
        assertTrue(s.absorbShieldHit())
        assertEquals(0, s.shieldHitsRemaining)
        assertFalse("last plate consumed must drop the shield", s.shield.isActive)
    }

    @Test
    fun `shield with no plates does not absorb a hit`() {
        val s = state()
        assertFalse(s.absorbShieldHit())
    }

    @Test
    fun `shield plates expire with the timer`() {
        val s = state()
        s.activateShield(1f, 3)
        s.tick(1.5f)

        assertFalse(s.shield.isActive)
        assertEquals("plates must not outlive the timer", 0, s.shieldHitsRemaining)
        assertFalse(s.isShieldProtecting)
    }

    @Test
    fun `shield charge spending grants plates`() {
        val s = state()
        s.shield.addCharge()

        assertTrue(s.consumeShieldChargeAndActivate(10f, 2))
        assertEquals(0, s.shield.charges)
        assertEquals(2, s.shieldHitsRemaining)
        assertTrue(s.isShieldProtecting)
    }

    @Test
    fun `shield charge spending without a charge is a no-op`() {
        val s = state()
        assertFalse(s.consumeShieldChargeAndActivate(10f, 2))
        assertEquals(0, s.shieldHitsRemaining)
        assertFalse(s.shield.isActive)
    }

    // ------------------------------------------------------------------ reset

    @Test
    fun `reset clears timers charges and plates`() {
        val s = state()
        s.magnet.activate(5f)
        s.speed.addCharge()
        s.activateShield(5f, 2)
        s.power.activate(5f)

        s.resetAll()

        assertFalse(s.magnet.isActive)
        assertFalse(s.shield.isActive)
        assertFalse(s.speed.isActive)
        assertFalse(s.power.isActive)
        assertEquals(0, s.magnet.charges)
        assertEquals(0, s.speed.charges)
        assertEquals(0, s.shieldHitsRemaining)
    }

    // ------------------------------------------------------------- independence

    @Test
    fun `each slot ticks independently`() {
        val s = state()
        s.magnet.activate(1f)
        s.power.activate(5f)

        s.tick(1.2f)

        assertFalse(s.magnet.isActive)
        assertTrue("power must be unaffected by magnet expiry", s.power.isActive)
    }

    @Test
    fun `tick applies to every slot in one call`() {
        val s = state()
        s.magnet.activate(1f)
        s.shield.activate(1f)
        s.speed.activate(1f)
        s.highJump.activate(1f)
        s.power.activate(1f)

        s.tick(2f)

        assertFalse(s.magnet.isActive)
        assertFalse(s.shield.isActive)
        assertFalse(s.speed.isActive)
        assertFalse(s.highJump.isActive)
        assertFalse(s.power.isActive)
    }
}
