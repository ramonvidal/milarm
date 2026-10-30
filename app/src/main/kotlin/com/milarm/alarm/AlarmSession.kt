package com.milarm.alarm

import com.milarm.core.Alarm
import com.milarm.core.AlarmPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The alarm currently ringing or being checked, shared between [AlarmService] and the UI. */
object AlarmSession {
    data class State(val alarm: Alarm, val phase: AlarmPhase)

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    internal fun update(state: State?) {
        _state.value = state
    }
}
