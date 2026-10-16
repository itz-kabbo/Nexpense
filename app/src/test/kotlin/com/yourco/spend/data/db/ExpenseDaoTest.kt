package com.yourco.spend.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yourco.spend.data.db.dao.ExpenseDao
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpenseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.expenseDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun createExpense(
        id: String,
        amountMinor: Long,
        localDate: String,
        categoryId: String = "food",
        currency: String = "USD",
        note: String = "test",
        createdAt: Long = 1000L,
        deletedAt: Long? = null,
    ): ExpenseEntity {
        return ExpenseEntity(
            id = id,
            amountMinor = amountMinor,
            currency = currency,
            note = note,
            rawText = "$amountMinor $note",
            categoryId = categoryId,
            createdAt = createdAt,
            localDate = localDate,
            updatedAt = createdAt,
            deletedAt = deletedAt,
        )
    }

    @Test
    fun observeTotal_emptyRange_returnsZero() = runTest {
        dao.observeTotal("2026-10-01", "2026-10-31").test {
            assertEquals(0L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTotal_sumsExpensesWithinRangeAndExcludesSoftDeleted() = runTest {
        dao.upsert(createExpense(id = "1", amountMinor = 1250L, localDate = "2026-10-15"))
        dao.upsert(createExpense(id = "2", amountMinor = 3000L, localDate = "2026-10-16"))
        dao.upsert(createExpense(id = "3", amountMinor = 5000L, localDate = "2026-11-01")) // Out of range
        dao.upsert(createExpense(id = "4", amountMinor = 2000L, localDate = "2026-10-15", deletedAt = 2000L)) // Deleted

        dao.observeTotal("2026-10-01", "2026-10-31").test {
            assertEquals(4250L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeByCategory_groupsAndOrdersByTotalDesc() = runTest {
        dao.upsert(createExpense(id = "1", amountMinor = 1000L, localDate = "2026-10-10", categoryId = "food"))
        dao.upsert(createExpense(id = "2", amountMinor = 1500L, localDate = "2026-10-11", categoryId = "food"))
        dao.upsert(createExpense(id = "3", amountMinor = 4000L, localDate = "2026-10-12", categoryId = "transport"))
        dao.upsert(createExpense(id = "4", amountMinor = 500L, localDate = "2026-10-13", categoryId = "fun"))
        dao.upsert(createExpense(id = "5", amountMinor = 9999L, localDate = "2026-10-14", categoryId = "home", deletedAt = 3000L))

        dao.observeByCategory("2026-10-01", "2026-10-31").test {
            val categories = awaitItem()
            assertEquals(3, categories.size)

            // transport: 4000 (1 count)
            assertEquals("transport", categories[0].categoryId)
            assertEquals(4000L, categories[0].total)
            assertEquals(1, categories[0].count)

            // food: 2500 (2 count)
            assertEquals("food", categories[1].categoryId)
            assertEquals(2500L, categories[1].total)
            assertEquals(2, categories[1].count)

            // fun: 500 (1 count)
            assertEquals("fun", categories[2].categoryId)
            assertEquals(500L, categories[2].total)
            assertEquals(1, categories[2].count)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeBuckets_monthBuckets_groupsByYearMonth() = runTest {
        dao.upsert(createExpense(id = "1", amountMinor = 1000L, localDate = "2026-08-15"))
        dao.upsert(createExpense(id = "2", amountMinor = 2000L, localDate = "2026-09-01"))
        dao.upsert(createExpense(id = "3", amountMinor = 3000L, localDate = "2026-09-30"))
        dao.upsert(createExpense(id = "4", amountMinor = 4000L, localDate = "2026-10-15"))
        dao.upsert(createExpense(id = "5", amountMinor = 5000L, localDate = "2026-10-16", deletedAt = 1000L))

        // len = 7 produces "YYYY-MM"
        dao.observeBuckets("2026-08-01", "2026-10-31", len = 7).test {
            val buckets = awaitItem()
            assertEquals(3, buckets.size)

            assertEquals("2026-08", buckets[0].bucket)
            assertEquals(1000L, buckets[0].total)

            assertEquals("2026-09", buckets[1].bucket)
            assertEquals(5000L, buckets[1].total)

            assertEquals("2026-10", buckets[2].bucket)
            assertEquals(4000L, buckets[2].total)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeBuckets_dayBuckets_groupsByDay() = runTest {
        dao.upsert(createExpense(id = "1", amountMinor = 1000L, localDate = "2026-10-14"))
        dao.upsert(createExpense(id = "2", amountMinor = 2000L, localDate = "2026-10-15"))
        dao.upsert(createExpense(id = "3", amountMinor = 3000L, localDate = "2026-10-15"))

        // len = 10 produces "YYYY-MM-DD"
        dao.observeBuckets("2026-10-01", "2026-10-31", len = 10).test {
            val buckets = awaitItem()
            assertEquals(2, buckets.size)

            assertEquals("2026-10-14", buckets[0].bucket)
            assertEquals(1000L, buckets[0].total)

            assertEquals("2026-10-15", buckets[1].bucket)
            assertEquals(5000L, buckets[1].total)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeBuckets_yearBuckets_groupsByYear() = runTest {
        dao.upsert(createExpense(id = "1", amountMinor = 1000L, localDate = "2024-06-15"))
        dao.upsert(createExpense(id = "2", amountMinor = 2000L, localDate = "2025-01-20"))
        dao.upsert(createExpense(id = "3", amountMinor = 3000L, localDate = "2025-11-05"))
        dao.upsert(createExpense(id = "4", amountMinor = 4000L, localDate = "2026-03-10"))

        // len = 4 produces "YYYY"
        dao.observeBuckets("2024-01-01", "2026-12-31", len = 4).test {
            val buckets = awaitItem()
            assertEquals(3, buckets.size)

            assertEquals("2024", buckets[0].bucket)
            assertEquals(1000L, buckets[0].total)

            assertEquals("2025", buckets[1].bucket)
            assertEquals(5000L, buckets[1].total)

            assertEquals("2026", buckets[2].bucket)
            assertEquals(4000L, buckets[2].total)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun softDeleteAndRestore_updatesDeletedAtAndAffectsQueries() = runTest {
        val expense = createExpense(id = "exp-1", amountMinor = 1500L, localDate = "2026-10-15")
        dao.upsert(expense)

        // Verify initially present in day and total
        dao.observeDay("2026-10-15").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("exp-1", items[0].id)
            cancelAndIgnoreRemainingEvents()
        }

        // Soft delete
        val deleteTime = 5000L
        dao.softDelete("exp-1", deleteTime)

        // Verify excluded from observeDay
        dao.observeDay("2026-10-15").test {
            val items = awaitItem()
            assertTrue(items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // Verify excluded from observeTotal
        dao.observeTotal("2026-10-01", "2026-10-31").test {
            assertEquals(0L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Verify entity still exists in db with deleted_at set
        val softDeletedEntity = dao.getById("exp-1")
        assertNotNull(softDeletedEntity)
        assertEquals(deleteTime, softDeletedEntity?.deletedAt)

        // Restore
        dao.restore("exp-1")

        // Verify restored in db
        val restoredEntity = dao.getById("exp-1")
        assertNotNull(restoredEntity)
        assertNull(restoredEntity?.deletedAt)

        // Verify restored in observeDay and observeTotal
        dao.observeDay("2026-10-15").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("exp-1", items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
        dao.observeTotal("2026-10-01", "2026-10-31").test {
            assertEquals(1500L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun periodBoundaries_monthBoundary_inclusiveAndAccurate() = runTest {
        dao.upsert(createExpense(id = "1", amountMinor = 100L, localDate = "2026-01-31"))
        dao.upsert(createExpense(id = "2", amountMinor = 200L, localDate = "2026-02-01"))
        dao.upsert(createExpense(id = "3", amountMinor = 400L, localDate = "2026-02-02"))

        // Query crossing Jan 31 to Feb 1
        dao.observeTotal("2026-01-31", "2026-02-01").test {
            assertEquals(300L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Query exactly Jan 31
        dao.observeTotal("2026-01-31", "2026-01-31").test {
            assertEquals(100L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun periodBoundaries_yearBoundary_dec31ToJan1() = runTest {
        dao.upsert(createExpense(id = "1", amountMinor = 500L, localDate = "2025-12-31"))
        dao.upsert(createExpense(id = "2", amountMinor = 700L, localDate = "2026-01-01"))
        dao.upsert(createExpense(id = "3", amountMinor = 900L, localDate = "2026-01-02"))

        dao.observeTotal("2025-12-31", "2026-01-01").test {
            assertEquals(1200L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Entire 2026 year range
        dao.observeTotal("2026-01-01", "2026-12-31").test {
            assertEquals(1600L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun periodBoundaries_leapYear_feb28Feb29Mar01() = runTest {
        // 2024 is a leap year (has Feb 29)
        dao.upsert(createExpense(id = "1", amountMinor = 100L, localDate = "2024-02-28"))
        dao.upsert(createExpense(id = "2", amountMinor = 200L, localDate = "2024-02-29"))
        dao.upsert(createExpense(id = "3", amountMinor = 300L, localDate = "2024-03-01"))

        // Exact leap day
        dao.observeTotal("2024-02-29", "2024-02-29").test {
            assertEquals(200L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Range across leap day
        dao.observeTotal("2024-02-28", "2024-03-01").test {
            assertEquals(600L, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Buckets for leap month (len = 7)
        dao.observeBuckets("2024-02-01", "2024-03-31", len = 7).test {
            val buckets = awaitItem()
            assertEquals(2, buckets.size)
            assertEquals("2024-02", buckets[0].bucket)
            assertEquals(300L, buckets[0].total)
            assertEquals("2024-03", buckets[1].bucket)
            assertEquals(300L, buckets[1].total)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
