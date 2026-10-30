package com.milarm.data

import com.milarm.core.Alarm
import com.milarm.core.SoundMode
import com.milarm.core.WakeCheckConfig
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

object AlarmJson {
    fun encode(alarms: List<Alarm>): String =
        JSONArray().apply { alarms.forEach { put(toJson(it)) } }.toString()

    fun decode(text: String): List<Alarm> = runCatching {
        val array = JSONArray(text)
        (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun toJson(alarm: Alarm) = JSONObject().apply {
        put("id", alarm.id)
        put("hour", alarm.hour)
        put("minute", alarm.minute)
        put("days", JSONArray(alarm.days.map { it.name }))
        put("label", alarm.label)
        put("enabled", alarm.enabled)
        put("soundMode", alarm.soundMode.name)
        put("snoozeMinutes", alarm.snoozeMinutes)
        put("wakeCheck", JSONObject().apply {
            put("enabled", alarm.wakeCheck.enabled)
            put("checkAfterMinutes", alarm.wakeCheck.checkAfterMinutes)
            put("responseSeconds", alarm.wakeCheck.responseSeconds)
            put("maxRounds", alarm.wakeCheck.maxRounds)
        })
    }

    private fun fromJson(json: JSONObject): Alarm {
        val defaults = WakeCheckConfig()
        val days = json.optJSONArray("days")
        val wake = json.optJSONObject("wakeCheck") ?: JSONObject()
        return Alarm(
            id = json.getInt("id"),
            hour = json.getInt("hour"),
            minute = json.getInt("minute"),
            days = (0 until (days?.length() ?: 0)).mapNotNull { i ->
                runCatching { DayOfWeek.valueOf(days!!.getString(i)) }.getOrNull()
            }.toSet(),
            label = json.optString("label"),
            enabled = json.optBoolean("enabled", true),
            soundMode = runCatching { SoundMode.valueOf(json.getString("soundMode")) }
                .getOrDefault(SoundMode.RINGTONE_AND_VIBRATE),
            snoozeMinutes = json.optInt("snoozeMinutes", 9),
            wakeCheck = WakeCheckConfig(
                enabled = wake.optBoolean("enabled", defaults.enabled),
                checkAfterMinutes = wake.optInt("checkAfterMinutes", defaults.checkAfterMinutes),
                responseSeconds = wake.optInt("responseSeconds", defaults.responseSeconds),
                maxRounds = wake.optInt("maxRounds", defaults.maxRounds),
            ),
        )
    }
}
