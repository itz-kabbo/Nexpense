package com.yourco.spend.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ClockProviderTest {

    @Test
    fun todayLocalDate_withFixedClock() {
        val fixedInstant = Instant.parse("2026-10-15T12:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

        assertEquals("2026-10-15", fixedClock.todayLocalDate())
    }

    @Test
    fun epochMillisToLocalDate_convertsCorrectly() {
        val instant = Instant.parse("2026-10-15T00:00:00Z")
        val localDate = epochMillisToLocalDate(instant.toEpochMilli(), ZoneId.of("UTC"))
        assertEquals("2026-10-15", localDate)
    }

    @Test
    fun localDateToEpochMillis_convertsCorrectly() {
        val millis = localDateToEpochMillis("2026-10-15", ZoneId.of("UTC"))
        val expected = Instant.parse("2026-10-15T00:00:00Z").toEpochMilli()
        assertEquals(expected, millis)
    }
}
