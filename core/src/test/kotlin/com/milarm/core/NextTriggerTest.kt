package com.milarm.core

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class NextTriggerTest {
    private val zone = ZoneId.of("Europe/Madrid")

    // 2026-10-28 is a Wednesday.
    private fun at(day: Int, hour: Int, minute: Int, month: Int = 10) =
        ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, zone)

    @Test
    fun `one-time alarm later today rings today`() {
        val alarm = Alarm(id = 1, hour = 7, minute = 30)
        assertEquals(at(28, 7, 30), NextTrigger.after(alarm, at(28, 6, 0)))
    }

    @Test
    fun `one-time alarm already passed rings tomorrow`() {
        val alarm = Alarm(id = 1, hour = 7, minute = 30)
        assertEquals(at(29, 7, 30), NextTrigger.after(alarm, at(28, 8, 0)))
    }

    @Test
    fun `alarm exactly now rings tomorrow`() {
        val alarm = Alarm(id = 1, hour = 7, minute = 30)
        assertEquals(at(29, 7, 30), NextTrigger.after(alarm, at(28, 7, 30)))
    }

    @Test
    fun `weekday alarm on friday evening skips the weekend`() {
        val alarm = Alarm(
            id = 1, hour = 7, minute = 0,
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        )
        // Friday 30 Oct, 20:00 -> Monday 2 Nov, 07:00
        assertEquals(at(2, 7, 0, month = 11), NextTrigger.after(alarm, at(30, 20, 0)))
    }

    @Test
    fun `single weekday already passed today waits a full week`() {
        val alarm = Alarm(id = 1, hour = 7, minute = 0, days = setOf(DayOfWeek.WEDNESDAY))
        assertEquals(at(4, 7, 0, month = 11), NextTrigger.after(alarm, at(28, 9, 0)))
    }

    @Test
    fun `time inside the spring daylight saving gap moves forward`() {
        // Europe/Madrid skips 02:00-03:00 on 29 March 2026.
        val alarm = Alarm(id = 1, hour = 2, minute = 30)
        val now = ZonedDateTime.of(2026, 3, 28, 23, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 3, 29, 3, 30, 0, 0, zone), NextTrigger.after(alarm, now))
    }

    @Test
    fun `autumn daylight saving change keeps local time`() {
        // Clocks go back on 25 October 2026.
        val alarm = Alarm(id = 1, hour = 7, minute = 0)
        val now = ZonedDateTime.of(2026, 10, 24, 22, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 10, 25, 7, 0, 0, 0, zone), NextTrigger.after(alarm, now))
    }
}
