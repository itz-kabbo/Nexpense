package com.yourco.spend.core.time

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideZoneId(clock: Clock): ZoneId = clock.zone

    @Provides
    @Singleton
    fun provideTodayObserver(
        @ApplicationContext context: Context,
        clock: Clock,
    ): TodayObserver = DefaultTodayObserver(context, clock)
}

/**
 * Extension and helper functions for getting and formatting local_date (ISO YYYY-MM-DD).
 * A day is stored as local_date text (for example 2026-10-15), computed in the device
 * time zone at write time, so travel across time zones never moves history.
 */
fun Clock.todayLocalDate(): String = LocalDate.now(this).toString()

fun Clock.currentEpochMillis(): Long = millis()

fun epochMillisToLocalDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().toString()

fun localDateToEpochMillis(localDate: String, zoneId: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.parse(localDate).atStartOfDay(zoneId).toInstant().toEpochMilli()
