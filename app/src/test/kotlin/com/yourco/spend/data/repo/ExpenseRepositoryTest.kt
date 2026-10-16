package com.yourco.spend.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yourco.spend.data.db.AppDatabase
import com.yourco.spend.data.db.entity.ExpenseEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpenseRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository
    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneId.of("UTC"))

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExpenseRepositoryImpl(
            expenseDao = db.expenseDao(),
            clock = fixedClock,
        )
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        db.close()
    }

    private fun sampleExpense(id: String = "e1", amount: Long = 1000L, localDate: String = "2026-10-15") =
        ExpenseEntity(
            id = id,
            amountMinor = amount,
            currency = "USD",
            note = "coffee",
            rawText = "10 coffee",
            categoryId = "food",
            createdAt = 1000L,
            localDate = localDate,
            updatedAt = 1000L,
        )

    @Test
    fun addAndGetById() = runTest {
        val exp = sampleExpense()
        repository.add(exp)

        val retrieved = repository.getById("e1")
        assertNotNull(retrieved)
        assertEquals(1000L, retrieved?.amountMinor)
        assertEquals("coffee", retrieved?.note)
    }

    @Test
    fun updateExpense() = runTest {
        val exp = sampleExpense()
        repository.add(exp)

        val updated = exp.copy(amountMinor = 1500L, note = "latte")
        repository.update(updated)

        val retrieved = repository.getById("e1")
        assertEquals(1500L, retrieved?.amountMinor)
        assertEquals("latte", retrieved?.note)
    }

    @Test
    fun softDeleteUsesClockAndRestoreClears() = runTest {
        val exp = sampleExpense()
        repository.add(exp)

        // Soft delete without explicit timestamp (uses clock)
        repository.softDelete("e1")

        val deletedEntity = repository.getById("e1")
        assertNotNull(deletedEntity)
        assertEquals(fixedClock.millis(), deletedEntity?.deletedAt)

        repository.observeDay("2026-10-15").test {
            val list = awaitItem()
            assertTrue(list.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // Restore
        repository.restore("e1")

        val restoredEntity = repository.getById("e1")
        assertNotNull(restoredEntity)
        assertNull(restoredEntity?.deletedAt)

        repository.observeDay("2026-10-15").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("e1", list[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTotalsCategoriesAndBuckets() = runTest {
        repository.add(sampleExpense(id = "1", amount = 1000L, localDate = "2026-10-14"))
        repository.add(sampleExpense(id = "2", amount = 2000L, localDate = "2026-10-15"))

        repository.observeTotal("2026-10-01", "2026-10-31").test {
            assertEquals(3000L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        repository.observeByCategory("2026-10-01", "2026-10-31").test {
            val categories = awaitItem()
            assertEquals(1, categories.size)
            assertEquals("food", categories[0].categoryId)
            assertEquals(3000L, categories[0].total)
            cancelAndIgnoreRemainingEvents()
        }

        repository.observeBuckets("2026-10-01", "2026-10-31", len = 10).test {
            val buckets = awaitItem()
            assertEquals(2, buckets.size)
            assertEquals("2026-10-14", buckets[0].bucket)
            assertEquals(1000L, buckets[0].total)
            assertEquals("2026-10-15", buckets[1].bucket)
            assertEquals(2000L, buckets[1].total)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
