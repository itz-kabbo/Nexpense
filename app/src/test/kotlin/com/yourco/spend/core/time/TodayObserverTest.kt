package com.yourco.spend.core.time

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodayObserverTest {

    private class MutableClock(
        private var currentInstant: Instant,
        private val zone: ZoneId = ZoneId.of("UTC"),
    ) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId?): Clock = MutableClock(currentInstant, zone ?: this.zone)
        override fun instant(): Instant = currentInstant

        fun setInstant(newInstant: Instant) {
            currentInstant = newInstant
        }
    }

    @Test
    fun initialDate_emitsCurrentClockDate() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clock = MutableClock(Instant.parse("2026-10-15T12:00:00Z"))
        val observer = DefaultTodayObserver(app, clock, tickIntervalMs = 10_000L)

        observer.todayFlow.test {
            assertEquals(LocalDate.parse("2026-10-15"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun actionDateChanged_emitsNewDate() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clock = MutableClock(Instant.parse("2026-10-15T12:00:00Z"))
        val observer = DefaultTodayObserver(app, clock, tickIntervalMs = 60_000L)

        observer.todayFlow.test {
            assertEquals(LocalDate.parse("2026-10-15"), awaitItem())

            // Advance clock to next day (midnight rollover)
            clock.setInstant(Instant.parse("2026-10-16T00:01:00Z"))
            // Send ACTION_DATE_CHANGED broadcast
            app.sendBroadcast(Intent(Intent.ACTION_DATE_CHANGED))

            assertEquals(LocalDate.parse("2026-10-16"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun actionTimeChanged_emitsNewDate() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clock = MutableClock(Instant.parse("2026-10-15T23:59:00Z"))
        val observer = DefaultTodayObserver(app, clock, tickIntervalMs = 60_000L)

        observer.todayFlow.test {
            assertEquals(LocalDate.parse("2026-10-15"), awaitItem())

            // Advance clock to next day
            clock.setInstant(Instant.parse("2026-10-16T00:05:00Z"))
            app.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED))

            assertEquals(LocalDate.parse("2026-10-16"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun actionTimezoneChanged_emitsNewDate() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clock = MutableClock(Instant.parse("2026-10-15T22:00:00Z"), ZoneId.of("UTC"))
        val observer = DefaultTodayObserver(app, clock, tickIntervalMs = 60_000L)

        observer.todayFlow.test {
            assertEquals(LocalDate.parse("2026-10-15"), awaitItem())

            // Shift clock instant such that it lands in Oct 16
            clock.setInstant(Instant.parse("2026-10-16T03:00:00Z"))
            app.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))

            assertEquals(LocalDate.parse("2026-10-16"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tickingFlow_emitsNewDateOnTick() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clock = MutableClock(Instant.parse("2026-10-15T23:59:50Z"))
        val observer = DefaultTodayObserver(app, clock, tickIntervalMs = 1_000L)

        observer.todayFlow.test {
            assertEquals(LocalDate.parse("2026-10-15"), awaitItem())

            // Advance clock to next day
            clock.setInstant(Instant.parse("2026-10-16T00:00:10Z"))
            // Advance virtual time for ticker
            advanceTimeBy(1_500L)

            assertEquals(LocalDate.parse("2026-10-16"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun companionFactories_fixedAndOf() = runTest {
        val fixed = TodayObserver.fixed(LocalDate.parse("2026-10-15"))
        assertEquals(LocalDate.parse("2026-10-15"), fixed.currentLocalDate())
        fixed.todayFlow.test {
            assertEquals(LocalDate.parse("2026-10-15"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
