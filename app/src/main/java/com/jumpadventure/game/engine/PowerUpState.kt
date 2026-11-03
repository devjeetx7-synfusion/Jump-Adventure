package com.jumpadventure.game.engine

/**
 * Pure, Android-free state machine for the timed power-ups.
 *
 * This exists so the activation / duration / charge rules have exactly one
 * implementation that can be unit-tested on the JVM. [GameView] owns an
 * instance and delegates every mutation to it.
 *
 * Rules encoded here:
 *  - A power-up is either **idle**, **active** (running down a timer) or
 *    **ready** (a stored charge waiting to be spent).
 *  - Activating an already-active power-up **refreshes** its remaining time
 *    instead of being ignored, so a second pickup is never silently wasted.
 *  - Timers tick only while active and never go negative.
 *  - Charges are capped at [MAX_CHARGES] so the HUD badge stays readable.
 *  - Shield additionally tracks discrete hits ("plates") which can be
 *    consumed independently of the remaining time.
 */
class PowerUpState {

    /** One timed power-up slot. */
    class Slot(private val maxCharges: Int = MAX_CHARGES) {
        var isActive: Boolean = false
            private set
        var timeRemaining: Float = 0f
            private set
        var duration: Float = 0f
            private set
        var charges: Int = 0
            private set

        /** Fraction of the full duration still remaining, in 0..1. */
        val progress: Float
            get() = if (duration <= 0f) 0f else (timeRemaining / duration).coerceIn(0f, 1f)

        fun addCharge(): Int {
            if (charges < maxCharges) charges++
            return charges
        }

        fun activate(newDuration: Float) {
            isActive = true
            duration = newDuration.coerceAtLeast(0f)
            timeRemaining = duration
        }

        /**
         * Spends one stored charge and activates the slot.
         * @return true if a charge was available and consumed.
         */
        fun consumeChargeAndActivate(newDuration: Float): Boolean {
            if (charges <= 0) return false
            charges--
            activate(newDuration)
            return true
        }

        fun tick(deltaTime: Float) {
            if (!isActive) return
            timeRemaining -= deltaTime
            if (timeRemaining <= 0f) {
                timeRemaining = 0f
                isActive = false
                duration = 0f
            }
        }

        fun deactivate() {
            isActive = false
            timeRemaining = 0f
            duration = 0f
        }

        fun reset() {
            deactivate()
            charges = 0
        }
    }

    val magnet = Slot()
    val shield = Slot()
    val speed = Slot()
    val highJump = Slot()
    val power = Slot()

    /** Remaining shield plates; 0 means no protection even if [shield] is active. */
    var shieldHitsRemaining: Int = 0
        private set

    /** Grants the shield plates and starts (or refreshes) the shield timer. */
    fun activateShield(duration: Float, hitCapacity: Int) {
        shield.activate(duration)
        shieldHitsRemaining = hitCapacity.coerceAtLeast(1)
    }

    /**
     * Spends one stored shield charge, then starts the timer and plates.
     * @return true if a charge was available and consumed.
     */
    fun consumeShieldChargeAndActivate(duration: Float, hitCapacity: Int): Boolean {
        if (!shield.consumeChargeAndActivate(duration)) return false
        shieldHitsRemaining = hitCapacity.coerceAtLeast(1)
        return true
    }

    /**
     * Consumes one shield plate. The shield deactivates when the last plate goes.
     * @return true if a plate absorbed the hit.
     */
    fun absorbShieldHit(): Boolean {
        if (!shield.isActive || shieldHitsRemaining <= 0) return false
        shieldHitsRemaining--
        if (shieldHitsRemaining <= 0) {
            shieldHitsRemaining = 0
            shield.deactivate()
        }
        return true
    }

    /** True when the shield is both timed-in and still has plates left. */
    val isShieldProtecting: Boolean
        get() = shield.isActive && shieldHitsRemaining > 0

    fun tick(deltaTime: Float) {
        magnet.tick(deltaTime)
        shield.tick(deltaTime)
        speed.tick(deltaTime)
        highJump.tick(deltaTime)
        power.tick(deltaTime)

        // Shield plates are meaningless once the timer has run out.
        if (!shield.isActive) shieldHitsRemaining = 0
    }

    fun resetAll() {
        magnet.reset()
        shield.reset()
        speed.reset()
        highJump.reset()
        power.reset()
        shieldHitsRemaining = 0
    }

    companion object {
        const val MAX_CHARGES = 9
    }
}
