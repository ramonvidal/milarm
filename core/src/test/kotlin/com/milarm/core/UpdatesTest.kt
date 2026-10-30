package com.milarm.core

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdatesTest {
    private fun update(build: Int) = AppUpdate(build, "0.$build", "https://example.com/Milarm.apk", 1)

    @Test
    fun `build number is read from release tags`() {
        assertEquals(12, Updates.buildNumber("v0.12"))
        assertEquals(7, Updates.buildNumber(" v0.7 "))
        assertNull(Updates.buildNumber("v1.2"))
        assertNull(Updates.buildNumber("0.12"))
        assertNull(Updates.buildNumber("v0.12-beta"))
    }

    @Test
    fun `only higher builds are newer`() {
        assertTrue(Updates.isNewer(update(5), installedBuild = 4))
        assertFalse(Updates.isNewer(update(5), installedBuild = 5))
        assertFalse(Updates.isNewer(update(5), installedBuild = 6))
    }

    @Test
    fun `checks are due every six hours`() {
        val now = Instant.parse("2026-10-30T12:00:00Z")
        assertTrue(Updates.isCheckDue(null, now))
        assertFalse(Updates.isCheckDue(now.minus(Duration.ofHours(5)), now))
        assertTrue(Updates.isCheckDue(now.minus(Duration.ofHours(6)), now))
    }

    @Test
    fun `later hides only that version`() {
        assertFalse(Updates.shouldOffer(update(5), installedBuild = 4, dismissedBuild = 5))
        assertTrue(Updates.shouldOffer(update(6), installedBuild = 4, dismissedBuild = 5))
        assertTrue(Updates.shouldOffer(update(5), installedBuild = 4, dismissedBuild = null))
    }
}
