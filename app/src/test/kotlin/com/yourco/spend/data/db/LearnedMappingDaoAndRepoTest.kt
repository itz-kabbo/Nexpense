package com.yourco.spend.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yourco.spend.data.db.dao.LearnedMappingDao
import com.yourco.spend.data.repo.LearnedMappingRepository
import com.yourco.spend.data.repo.LearnedMappingRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class LearnedMappingDaoAndRepoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: LearnedMappingDao
    private lateinit var repository: LearnedMappingRepository
    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000_000L), ZoneId.of("UTC"))

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.learnedMappingDao()
        repository = LearnedMappingRepositoryImpl(dao, clock)
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        db.close()
    }

    @Test
    fun recordAndGetMapping() = runTest {
        assertNull(repository.getMapping("boba"))

        // Record first time
        repository.recordMapping("Boba", "groceries")
        assertEquals("groceries", repository.getMapping("boba"))
        assertEquals("groceries", repository.getMapping(" BOBA "))

        val entity = dao.get("boba")
        assertNotNull(entity)
        assertEquals(1, entity?.hits)
        assertEquals(1_000_000L, entity?.updatedAt)

        // Record again (e.g. user re-confirms or re-corrects)
        repository.recordMapping("boba", "food")
        assertEquals("food", repository.getMapping("boba"))

        val updatedEntity = dao.get("boba")
        assertEquals(2, updatedEntity?.hits)
        assertEquals("food", updatedEntity?.categoryId)
    }

    @Test
    fun observeAllAsMap() = runTest {
        repository.recordMapping("latte", "food")
        repository.recordMapping("metro", "transport")

        repository.observeAllAsMap().test {
            val map = awaitItem()
            assertEquals(2, map.size)
            assertEquals("food", map["latte"])
            assertEquals("transport", map["metro"])
            cancelAndIgnoreRemainingEvents()
        }
    }
}
