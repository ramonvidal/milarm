package com.milarm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakeCheckEvaluatorTest {
    @Test
    fun `no readings means not awake`() {
        assertFalse(WakeCheckEvaluator().isAwake)
    }

    @Test
    fun `steps are counted from the first reading`() {
        val evaluator = WakeCheckEvaluator()
        evaluator.onStepCounter(10_000)
        evaluator.onStepCounter(10_010)
        assertEquals(10, evaluator.steps)
        assertFalse(evaluator.isAwake)

        evaluator.onStepCounter(10_015)
        assertTrue(evaluator.isAwake)
    }

    @Test
    fun `step counter reset after reboot does not go negative`() {
        val evaluator = WakeCheckEvaluator()
        evaluator.onStepCounter(500)
        evaluator.onStepCounter(3)
        assertEquals(0, evaluator.steps)
    }

    @Test
    fun `rolling over in bed is not enough`() {
        val evaluator = WakeCheckEvaluator()
        // A 3-second burst of strong motion, sampled every 200 ms, then stillness.
        var t = 0L
        repeat(15) { evaluator.onMotion(4f, t); t += 200 }
        repeat(300) { evaluator.onMotion(0.1f, t); t += 200 }
        assertFalse(evaluator.isAwake)
    }

    @Test
    fun `sustained movement counts as awake`() {
        val evaluator = WakeCheckEvaluator()
        var t = 0L
        repeat(110) { evaluator.onMotion(2.5f, t); t += 200 }
        assertTrue(evaluator.isAwake)
    }

    @Test
    fun `weak motion is ignored`() {
        val evaluator = WakeCheckEvaluator()
        var t = 0L
        repeat(1_000) { evaluator.onMotion(1.0f, t); t += 200 }
        assertEquals(0, evaluator.activeMotionMillis)
    }

    @Test
    fun `long sensor gaps are capped`() {
        val evaluator = WakeCheckEvaluator()
        evaluator.onMotion(3f, 0)
        evaluator.onMotion(3f, 60_000)
        assertEquals(1_000, evaluator.activeMotionMillis)
    }

    @Test
    fun `unlocking the phone counts as awake`() {
        val evaluator = WakeCheckEvaluator()
        evaluator.onUserPresent()
        assertTrue(evaluator.isAwake)
    }
}
