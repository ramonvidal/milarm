package com.milarm.core

import java.time.LocalTime
import java.time.ZonedDateTime

object NextTrigger {
    /**
     * The first time strictly after [now] at which [alarm] should ring.
     *
     * Times that fall into a daylight-saving gap are moved forward by the length of the gap,
     * following [ZonedDateTime.of].
     */
    fun after(alarm: Alarm, now: ZonedDateTime): ZonedDateTime {
        val time = LocalTime.of(alarm.hour, alarm.minute)
        // Eight days covers "same weekday next week" when today's time has already passed.
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (alarm.isRepeating && date.dayOfWeek !in alarm.days) continue
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        error("No trigger found for alarm ${alarm.id}")
    }
}
