package com.milarm.core

import kotlin.math.min

data class MovementThresholds(
    /** Steps since the check started that count as being up. */
    val steps: Long = 15,
    /** Linear acceleration (m/s², gravity removed) that counts as real movement. */
    val motionMagnitude: Float = 1.5f,
    /** Total time spent above [motionMagnitude] that counts as being up. */
    val activeMotionMillis: Long = 20_000,
    /** Longest gap between two sensor samples that is credited as movement. */
    val maxSampleGapMillis: Long = 1_000,
)

/**
 * Decides from phone sensor readings whether the user is up after dismissing an alarm.
 *
 * Rolling over in bed produces short, weak bursts of motion and no steps, so it stays below
 * the thresholds. Walking with the phone, or picking it up and using it, passes quickly.
 */
class WakeCheckEvaluator(
    private val thresholds: MovementThresholds = MovementThresholds(),
) {
    private var stepBaseline: Long? = null
    private var lastMotionAtMillis: Long? = null

    var steps: Long = 0
        private set
    var activeMotionMillis: Long = 0
        private set
    var userPresent: Boolean = false
        private set

    /** [total] is the cumulative value reported by the step counter sensor. */
    fun onStepCounter(total: Long) {
        val baseline = stepBaseline ?: total.also { stepBaseline = it }
        steps = (total - baseline).coerceAtLeast(0)
    }

    fun onMotion(magnitude: Float, atMillis: Long) {
        val last = lastMotionAtMillis
        if (last != null && magnitude >= thresholds.motionMagnitude) {
            activeMotionMillis += min(atMillis - last, thresholds.maxSampleGapMillis).coerceAtLeast(0)
        }
        lastMotionAtMillis = atMillis
    }

    /** The user unlocked the phone. */
    fun onUserPresent() {
        userPresent = true
    }

    val isAwake: Boolean
        get() = userPresent ||
            steps >= thresholds.steps ||
            activeMotionMillis >= thresholds.activeMotionMillis
}
