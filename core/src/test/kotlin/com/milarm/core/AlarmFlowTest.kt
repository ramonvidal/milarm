package com.milarm.core

import com.milarm.core.AlarmPhase.Asking
import com.milarm.core.AlarmPhase.Done
import com.milarm.core.AlarmPhase.Ringing
import com.milarm.core.AlarmPhase.WakeCheck
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmFlowTest {
    private val off = WakeCheckConfig(enabled = false)
    private val on = WakeCheckConfig(enabled = true, maxRounds = 3)

    @Test
    fun `dismiss without wake check ends the alarm`() {
        assertEquals(Done, AlarmFlow.onDismiss(Ringing(0), off))
    }

    @Test
    fun `dismiss with wake check starts the first round`() {
        assertEquals(WakeCheck(1), AlarmFlow.onDismiss(Ringing(0), on))
    }

    @Test
    fun `movement during the check ends it`() {
        assertEquals(Done, AlarmFlow.onAwake(WakeCheck(1)))
        assertEquals(Done, AlarmFlow.onCheckDeadline(WakeCheck(1), awake = true))
    }

    @Test
    fun `no movement asks, and no answer rings again`() {
        val asking = AlarmFlow.onCheckDeadline(WakeCheck(1), awake = false)
        assertEquals(Asking(1), asking)
        assertEquals(Ringing(1), AlarmFlow.onAskTimeout(asking))
    }

    @Test
    fun `answering I'm up ends the alarm`() {
        assertEquals(Done, AlarmFlow.onAwake(Asking(2)))
    }

    @Test
    fun `full sequence stops after max rounds`() {
        var phase: AlarmPhase = AlarmFlow.start(0)
        repeat(3) {
            phase = AlarmFlow.onDismiss(phase, on)
            phase = AlarmFlow.onCheckDeadline(phase, awake = false)
            phase = AlarmFlow.onAskTimeout(phase)
        }
        assertEquals(Ringing(3), phase)
        assertEquals(Done, AlarmFlow.onDismiss(phase, on))
    }

    @Test
    fun `snooze only applies while ringing`() {
        assertEquals(Done, AlarmFlow.onSnooze(Ringing(0)))
        assertEquals(WakeCheck(1), AlarmFlow.onSnooze(WakeCheck(1)))
    }

    @Test
    fun `events in the wrong phase are ignored`() {
        assertEquals(Ringing(0), AlarmFlow.onAwake(Ringing(0)))
        assertEquals(Ringing(0), AlarmFlow.onAskTimeout(Ringing(0)))
        assertEquals(WakeCheck(1), AlarmFlow.onDismiss(WakeCheck(1), on))
        assertEquals(Done, AlarmFlow.onRingTimeout(Ringing(2)))
    }
}
