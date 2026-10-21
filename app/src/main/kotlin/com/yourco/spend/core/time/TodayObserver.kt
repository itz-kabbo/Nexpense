package com.yourco.spend.core.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exposes a reactive flow of the current LocalDate.
 * When midnight occurs, device time/date changes, or timezone changes,
 * emits the new current LocalDate so screens update automatically without app restart.
 */
interface TodayObserver {
    val todayFlow: Flow<LocalDate>
    val today: Flow<LocalDate> get() = todayFlow
    fun currentLocalDate(): LocalDate

    companion object {
        fun of(flow: Flow<LocalDate>, currentProvider: () -> LocalDate): TodayObserver =
            object : TodayObserver {
                override val todayFlow: Flow<LocalDate> = flow.distinctUntilChanged()
                override fun currentLocalDate(): LocalDate = currentProvider()
            }

        fun fixed(date: LocalDate): TodayObserver =
            object : TodayObserver {
                override val todayFlow: Flow<LocalDate> = flowOf(date)
                override fun currentLocalDate(): LocalDate = date
            }

        fun from(clock: Clock): TodayObserver =
            object : TodayObserver {
                override val todayFlow: Flow<LocalDate> = flow {
                    emit(LocalDate.now(clock))
                }
                override fun currentLocalDate(): LocalDate = LocalDate.now(clock)
            }
    }
}

/**
 * Default implementation combining:
 * 1) Initial date emission
 * 2) BroadcastReceiver listening for ACTION_DATE_CHANGED, ACTION_TIME_CHANGED, ACTION_TIMEZONE_CHANGED
 * 3) Periodic ticking flow to ensure midnight rollover even if broadcasts are delayed by system power-saving
 */
@Singleton
class DefaultTodayObserver(
    private val context: Context,
    private val clock: Clock,
    private val tickIntervalMs: Long = 30_000L,
) : TodayObserver {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        clock: Clock,
    ) : this(context, clock, 30_000L)

    override val todayFlow: Flow<LocalDate> = merge(
        flow {
            emit(currentLocalDate())
        },
        callbackFlow {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    trySend(currentLocalDate())
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            awaitClose {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
            }
        },
        flow {
            while (true) {
                delay(tickIntervalMs)
                emit(currentLocalDate())
            }
        },
    ).distinctUntilChanged()

    override fun currentLocalDate(): LocalDate = LocalDate.now(clock)
}
