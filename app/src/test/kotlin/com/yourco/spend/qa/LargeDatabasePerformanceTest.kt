package com.yourco.spend.qa

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yourco.spend.data.db.AppDatabase
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.data.repo.ExpenseRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
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
class LargeDatabasePerformanceTest {

    private lateinit var database: AppDatabase
    private lateinit var expenseRepo: ExpenseRepositoryImpl
    private val clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseRepo = ExpenseRepositoryImpl(database.expenseDao(), clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fiveThousandRows_insightAggregationUnder100ms() = runTest {
        val categories = CategoryEntity.DEFAULT_CATEGORIES.map { it.id }
        val startDate = LocalDate.of(2026, 1, 1)

        val expenses = (1..5000).map { i ->
            val dayOffset = (i % 365).toLong()
            val date = startDate.plusDays(dayOffset).toString()
            val category = categories[i % categories.size]
            val amountMinor = ((i % 50) + 1) * 100L // 1.00 to 50.00
            val timestamp = 1735689600000L + (dayOffset * 86400000L) + (i * 1000L)

            ExpenseEntity(
                id = "perf-exp-$i",
                amountMinor = amountMinor,
                currency = "USD",
                note = "item $i $category",
                rawText = "${amountMinor / 100} item $i $category",
                categoryId = category,
                createdAt = timestamp,
                localDate = date,
                updatedAt = timestamp,
                deletedAt = null,
            )
        }

        // Insert 5000 rows in chunks of 1000
        expenses.chunked(1000).forEach { chunk ->
            database.expenseDao().upsertAll(chunk)
        }

        assertEquals(5000, database.expenseDao().getAll().size)

        // Warm up query cache
        expenseRepo.observeTotal("2026-01-01", "2026-12-31", "USD").first()

        // 1. Measure Year-level full Insight aggregation
        val yearStartNano = System.nanoTime()
        val totalYear = expenseRepo.observeTotal("2026-01-01", "2026-12-31", "USD").first()
        val byCategoryYear = expenseRepo.observeByCategory("2026-01-01", "2026-12-31", "USD").first()
        val bucketsYear = expenseRepo.observeBuckets("2026-01-01", "2026-12-31", 7, "USD").first()
        val yearDurationMs = (System.nanoTime() - yearStartNano) / 1_000_000.0

        println("5,000-row Year Insight aggregation duration: $yearDurationMs ms")
        assertTrue(
            "Year aggregation must complete under 100ms, took $yearDurationMs ms",
            yearDurationMs < 100.0,
        )

        // Validate data integrity
        val expectedSum = expenses.sumOf { it.amountMinor }
        assertEquals(expectedSum, totalYear)
        assertEquals(totalYear, byCategoryYear.sumOf { it.total })
        assertEquals(totalYear, bucketsYear.sumOf { it.total })
        assertEquals(9, byCategoryYear.size)
        assertEquals(12, bucketsYear.size) // 12 monthly buckets

        // 2. Measure Month-level Insight aggregation
        val monthStartNano = System.nanoTime()
        val totalMonth = expenseRepo.observeTotal("2026-06-01", "2026-06-30", "USD").first()
        val byCategoryMonth = expenseRepo.observeByCategory("2026-06-01", "2026-06-30", "USD").first()
        val bucketsMonth = expenseRepo.observeBuckets("2026-06-01", "2026-06-30", 10, "USD").first()
        val monthDurationMs = (System.nanoTime() - monthStartNano) / 1_000_000.0

        println("5,000-row Month Insight aggregation duration: $monthDurationMs ms")
        assertTrue(
            "Month aggregation must complete under 100ms, took $monthDurationMs ms",
            monthDurationMs < 100.0,
        )
        assertEquals(totalMonth, byCategoryMonth.sumOf { it.total })
        assertEquals(totalMonth, bucketsMonth.sumOf { it.total })
    }
}
