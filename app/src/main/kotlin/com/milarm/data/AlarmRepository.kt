package com.milarm.data

import android.content.Context
import com.milarm.core.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps the alarm list in SharedPreferences as JSON; the list is small. */
class AlarmRepository(context: Context) {
    private val prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    private val _alarms = MutableStateFlow(load())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    fun get(id: Int): Alarm? = _alarms.value.firstOrNull { it.id == id }

    fun nextId(): Int = (_alarms.value.maxOfOrNull { it.id } ?: 0) + 1

    @Synchronized
    fun upsert(alarm: Alarm) {
        save(_alarms.value.filterNot { it.id == alarm.id } + alarm)
    }

    @Synchronized
    fun delete(id: Int) {
        save(_alarms.value.filterNot { it.id == id })
    }

    private fun save(alarms: List<Alarm>) {
        val sorted = alarms.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id }))
        _alarms.value = sorted
        prefs.edit().putString(KEY, AlarmJson.encode(sorted)).apply()
    }

    private fun load(): List<Alarm> =
        prefs.getString(KEY, null)?.let(AlarmJson::decode) ?: emptyList()

    private companion object {
        const val KEY = "alarms"
    }
}
