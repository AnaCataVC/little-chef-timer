package com.littlechef.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerLogicTest {

    @Test
    fun runningTimerCountsDownFromEndTime() {
        val timer = TimerState("r", totalMillis = 60_000, endAtMillis = 10_000)
        assertFalse(timer.isPaused)
        assertEquals(4_000, timer.remaining(now = 6_000))
    }

    @Test
    fun runningTimerNeverGoesNegative() {
        val timer = TimerState("r", totalMillis = 60_000, endAtMillis = 10_000)
        assertEquals(0, timer.remaining(now = 15_000))
    }

    @Test
    fun pausedTimerIgnoresClock() {
        val timer = TimerState("r", totalMillis = 60_000, remainingMillis = 25_000)
        assertTrue(timer.isPaused)
        assertEquals(25_000, timer.remaining(now = 0))
        assertEquals(25_000, timer.remaining(now = 999_999))
    }

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("10:00", formatDuration(600_000))
        assertEquals("00:05", formatDuration(5_000))
        assertEquals("00:00", formatDuration(0))
    }

    @Test
    fun formatsHoursWhenNeeded() {
        assertEquals("1:05:09", formatDuration((3_600 + 5 * 60 + 9) * 1000L))
    }

    @Test
    fun roundsUpPartialSeconds() {
        assertEquals("00:01", formatDuration(1))
        assertEquals("00:02", formatDuration(1_001))
    }
}
