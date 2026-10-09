package com.example.lifeorganizer.update

import com.example.lifeorganizer.update.AutoUpdateCheck.IDLE_MS
import com.example.lifeorganizer.update.AutoUpdateCheck.MIN_GAP_MS
import com.example.lifeorganizer.update.AutoUpdateCheck.SNOOZE_MS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoUpdateCheckTest {
    private val now = 1_000_000_000L

    @Test
    fun `fresh start checks unless a check just ran`() {
        assertTrue(AutoUpdateCheck.shouldCheck(now, lastCheck = 0, backgroundedAt = null))
        assertFalse(AutoUpdateCheck.shouldCheck(now, lastCheck = now - MIN_GAP_MS + 1, backgroundedAt = null))
    }

    @Test
    fun `coming back checks only after a long time in the background`() {
        val longAgo = now - 2 * MIN_GAP_MS
        assertFalse(AutoUpdateCheck.shouldCheck(now, longAgo, backgroundedAt = now - 60_000))
        assertTrue(AutoUpdateCheck.shouldCheck(now, longAgo, backgroundedAt = now - IDLE_MS))
    }

    @Test
    fun `later keeps the same release quiet for a day`() {
        assertFalse(AutoUpdateCheck.shouldShow("1.2", snoozedVersion = "1.2", snoozedAt = now - 3_600_000, now = now))
        assertTrue(AutoUpdateCheck.shouldShow("1.2", snoozedVersion = "1.2", snoozedAt = now - SNOOZE_MS, now = now))
        assertTrue(AutoUpdateCheck.shouldShow("1.3", snoozedVersion = "1.2", snoozedAt = now, now = now))
        assertTrue(AutoUpdateCheck.shouldShow("1.2", snoozedVersion = null, snoozedAt = 0, now = now))
    }
}
