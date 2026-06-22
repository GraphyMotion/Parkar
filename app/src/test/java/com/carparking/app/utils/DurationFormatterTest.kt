package com.carparking.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatterTest {

    private val now = 1_000_000_000L

    @Test
    fun `moins d'une minute donne a l'instant`() {
        assertEquals("à l'instant", formatElapsedDuration(now - 30_000, now))
    }

    @Test
    fun `quelques minutes avec prefixe`() {
        assertEquals("il y a 5min", formatElapsedDuration(now - 5 * 60_000, now))
    }

    @Test
    fun `quelques minutes sans prefixe`() {
        assertEquals("5min", formatElapsedDuration(now - 5 * 60_000, now, withPrefix = false))
    }

    @Test
    fun `heures et minutes avec prefixe`() {
        val twoHoursTenMin = (2 * 60 + 10) * 60_000L
        assertEquals("il y a 2h 10min", formatElapsedDuration(now - twoHoursTenMin, now))
    }

    @Test
    fun `heures et minutes sans prefixe (widget)`() {
        val twoHoursTenMin = (2 * 60 + 10) * 60_000L
        assertEquals("2h10min", formatElapsedDuration(now - twoHoursTenMin, now, withPrefix = false))
    }

    @Test
    fun `jours et heures`() {
        val oneDayFiveHours = (29 * 60) * 60_000L
        assertEquals("il y a 1j 5h", formatElapsedDuration(now - oneDayFiveHours, now))
    }
}
