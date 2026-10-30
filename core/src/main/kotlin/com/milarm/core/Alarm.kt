package com.milarm.core

import java.time.DayOfWeek

enum class SoundMode {
    /** Phone plays the alarm sound and vibrates; the band buzzes too. */
    RINGTONE_AND_VIBRATE,

    /** Phone vibrates silently; the band buzzes too. */
    VIBRATE_ONLY,

    /** Phone stays silent; only the band buzzes. */
    BAND_ONLY,
}

data class WakeCheckConfig(
    val enabled: Boolean = false,
    /** How long after dismissing the alarm the user has to show movement. */
    val checkAfterMinutes: Int = 5,
    /** How long the user has to answer "Are you up?" before the alarm rings again. */
    val responseSeconds: Int = 30,
    /** Maximum number of wake-up checks after one alarm, so it can never loop forever. */
    val maxRounds: Int = 3,
)

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    /** Days the alarm repeats on. Empty means it rings once. */
    val days: Set<DayOfWeek> = emptySet(),
    val label: String = "",
    val enabled: Boolean = true,
    val soundMode: SoundMode = SoundMode.RINGTONE_AND_VIBRATE,
    val snoozeMinutes: Int = 9,
    val wakeCheck: WakeCheckConfig = WakeCheckConfig(),
) {
    val isRepeating: Boolean get() = days.isNotEmpty()
}
