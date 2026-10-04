package com.example.lifeorganizer.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun `versions compare numerically`() {
        assertTrue(Updater.isNewer("0.10", "0.9"))
        assertTrue(Updater.isNewer("1.0", "0.9.5"))
        assertFalse(Updater.isNewer("0.9", "0.9"))
        assertFalse(Updater.isNewer("0.8", "0.9"))
    }
}
