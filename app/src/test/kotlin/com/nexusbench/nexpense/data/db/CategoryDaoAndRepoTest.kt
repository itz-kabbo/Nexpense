package com.nexusbench.nexpense.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.nexusbench.nexpense.data.db.dao.CategoryDao
import com.nexusbench.nexpense.data.db.entity.CategoryEntity
import com.nexusbench.nexpense.data.repo.CategoryRepository
import com.nexusbench.nexpense.data.repo.CategoryRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryDaoAndRepoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CategoryDao
    private lateinit var repository: CategoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.createCallback())
            .allowMainThreadQueries()
            .build()
        dao = db.categoryDao()
        repository = CategoryRepositoryImpl(dao)
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        db.close()
    }

    @Test
    fun databaseCreationSeedsDefaultCategories() = runTest {
        // Trigger DB creation by querying count
        val count = dao.count()
        assertEquals(9, count)

        val categories = listOf(
            "food", "groceries", "transport", "shopping",
            "home", "health", "fun", "travel", "other"
        )
        for (id in categories) {
            val cat = dao.getById(id)
            assertNotNull("Category $id should exist", cat)
            assertTrue("Category $id should be builtin", cat!!.isBuiltin)
            assertEquals(false, cat.isHidden)
        }
    }

    @Test
    fun seedDefaultsIfEmpty_doesNotOverwriteExisting() = runTest {
        assertEquals(9, dao.count())
        // Modify a category
        dao.updateNameOverride("food", "Dining Out")

        // Calling seedDefaultsIfEmpty should keep existing
        repository.seedDefaultsIfEmpty()

        val food = dao.getById("food")
        assertEquals("Dining Out", food?.nameOverride)
        assertEquals(9, dao.count())
    }

    @Test
    fun observeActive_excludesHiddenCategories() = runTest {
        repository.setHidden("fun", true)

        repository.observeActive().test {
            val list = awaitItem()
            assertEquals(8, list.size)
            assertTrue(list.none { it.id == "fun" })
            cancelAndIgnoreRemainingEvents()
        }

        // Unhide
        repository.setHidden("fun", false)
        repository.observeActive().test {
            val list = awaitItem()
            assertEquals(9, list.size)
            assertTrue(list.any { it.id == "fun" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateNameOverride_updatesSuccessfully() = runTest {
        repository.updateNameOverride("coffee", "Coffee & Tea")
        repository.upsert(
            CategoryEntity(
                id = "custom",
                nameOverride = "Custom Cat",
                icon = "star",
                sortOrder = 10,
                isHidden = false,
                isBuiltin = false,
            )
        )

        val custom = repository.getById("custom")
        assertNotNull(custom)
        assertEquals("Custom Cat", custom?.nameOverride)
    }
}
