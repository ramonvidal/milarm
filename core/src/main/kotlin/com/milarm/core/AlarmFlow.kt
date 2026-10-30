package com.milarm.core

/**
 * What an alarm is doing right now. [round] counts the wake-up checks already started for it.
 */
sealed interface AlarmPhase {
    data class Ringing(val round: Int) : AlarmPhase

    /** Alarm dismissed; watching for movement until the check deadline. */
    data class WakeCheck(val round: Int) : AlarmPhase

    /** No movement seen; waiting for the user to tap "I'm up". */
    data class Asking(val round: Int) : AlarmPhase

    data object Done : AlarmPhase
}

/** Transitions between [AlarmPhase]s. Timers and sensors live in the Android service. */
object AlarmFlow {
    fun start(round: Int): AlarmPhase = AlarmPhase.Ringing(round)

    fun onDismiss(phase: AlarmPhase, config: WakeCheckConfig): AlarmPhase = when (phase) {
        is AlarmPhase.Ringing ->
            if (config.enabled && phase.round < config.maxRounds) AlarmPhase.WakeCheck(phase.round + 1)
            else AlarmPhase.Done
        else -> phase
    }

    fun onSnooze(phase: AlarmPhase): AlarmPhase =
        if (phase is AlarmPhase.Ringing) AlarmPhase.Done else phase

    /** Movement detected, or the user tapped "I'm up". */
    fun onAwake(phase: AlarmPhase): AlarmPhase = when (phase) {
        is AlarmPhase.WakeCheck, is AlarmPhase.Asking -> AlarmPhase.Done
        else -> phase
    }

    fun onCheckDeadline(phase: AlarmPhase, awake: Boolean): AlarmPhase = when {
        phase !is AlarmPhase.WakeCheck -> phase
        awake -> AlarmPhase.Done
        else -> AlarmPhase.Asking(phase.round)
    }

    fun onAskTimeout(phase: AlarmPhase): AlarmPhase =
        if (phase is AlarmPhase.Asking) AlarmPhase.Ringing(phase.round) else phase

    fun onRingTimeout(phase: AlarmPhase): AlarmPhase =
        if (phase is AlarmPhase.Ringing) AlarmPhase.Done else phase
}
