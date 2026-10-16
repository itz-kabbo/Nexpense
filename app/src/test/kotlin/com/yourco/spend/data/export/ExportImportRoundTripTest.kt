package com.yourco.spend.data.export

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yourco.spend.data.db.AppDatabase
import com.yourco.spend.data.db.dao.CategoryDao
import com.yourco.spend.data.db.dao.ExpenseDao
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.data.repo.CategoryRepositoryImpl
import com.yourco.spend.data.repo.ExpenseRepositoryImpl
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
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExportImportRoundTripTest {

    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var jsonExporter: JsonExporter
    private lateinit var csvExporter: CsvExporter
    private lateinit var dataImporter: DataImporter

    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(1729000000000L), ZoneId.of("UTC"))
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = database.expenseDao()
        categoryDao = database.categoryDao()

        val expenseRepo = ExpenseRepositoryImpl(expenseDao, fixedClock)
        val categoryRepo = CategoryRepositoryImpl(categoryDao)

        jsonExporter = JsonExporter(expenseRepo, categoryRepo, fixedClock)
        csvExporter = CsvExporter(expenseRepo, fixedClock)
        dataImporter = DataImporter(expenseDao, categoryDao, fixedClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun jsonExportAndImport_restoresIdenticalRows() = runTest {
        // Seed categories
        categoryDao.upsertAll(CategoryEntity.DEFAULT_CATEGORIES)
        val customCat = CategoryEntity(
            id = "books",
            nameOverride = "Books & Media",
            icon = "book",
            sortOrder = 9,
            isHidden = false,
            isBuiltin = false,
        )
        categoryDao.upsert(customCat)

        // Seed expenses
        val e1 = ExpenseEntity(
            id = "exp-1",
            amountMinor = 600L,
            currency = "USD",
            note = "coffee",
            rawText = "6 coffee",
            categoryId = "food",
            createdAt = 1729000100000L,
            localDate = "2026-10-15",
            updatedAt = 1729000100000L,
            deletedAt = null,
        )
        val e2 = ExpenseEntity(
            id = "exp-2",
            amountMinor = 1250L,
            currency = "USD",
            note = "lunch with, \"friends\"",
            rawText = "12.5 lunch with, \"friends\"",
            categoryId = "food",
            createdAt = 1729000200000L,
            localDate = "2026-10-15",
            updatedAt = 1729000250000L,
            deletedAt = null,
        )
        val e3 = ExpenseEntity(
            id = "exp-3",
            amountMinor = 3500L,
            currency = "EUR",
            note = "train ticket",
            rawText = "35 train ticket",
            categoryId = "transport",
            createdAt = 1729000300000L,
            localDate = "2026-10-16",
            updatedAt = 1729000300000L,
            deletedAt = 1729000400000L, // Soft-deleted
        )
        expenseDao.upsertAll(listOf(e1, e2, e3))

        // 1. Export to JSON
        val exportedJson = jsonExporter.exportJson()
        assertTrue(exportedJson.contains("\"schema\": 1"))

        // 2. Wipe database
        database.clearAllTables()
        assertEquals(0, categoryDao.getAll().size)
        assertEquals(0, expenseDao.getAll().size)

        // 3. Import JSON back
        val result = dataImporter.importJson(exportedJson)
        assertTrue(result.success)
        assertEquals(3, result.expensesImported)
        assertEquals(10, result.categoriesImported) // 9 default + 1 custom

        // 4. Verify identical rows restored
        val restoredCategories = categoryDao.getAll().associateBy { it.id }
        assertEquals(10, restoredCategories.size)
        val restoredCustom = restoredCategories["books"]
        assertNotNull(restoredCustom)
        assertEquals("Books & Media", restoredCustom?.nameOverride)
        assertEquals("book", restoredCustom?.icon)
        assertEquals(9, restoredCustom?.sortOrder)

        val restoredExpenses = expenseDao.getAll().associateBy { it.id }
        assertEquals(3, restoredExpenses.size)

        assertEquals(e1, restoredExpenses["exp-1"])
        assertEquals(e2, restoredExpenses["exp-2"])
        assertEquals(e3, restoredExpenses["exp-3"])
    }

    @Test
    fun deDuplication_neverOverwritesRowWithNewerUpdatedAt() = runTest {
        val existing = ExpenseEntity(
            id = "exp-1",
            amountMinor = 1000L,
            currency = "USD",
            note = "New Note",
            rawText = "10 New Note",
            categoryId = "food",
            createdAt = 1000L,
            localDate = "2026-10-15",
            updatedAt = 5000L, // Current version has updatedAt = 5000
            deletedAt = null,
        )
        expenseDao.upsert(existing)

        // 1. Attempt to import an incoming row with older updatedAt (3000L)
        val olderJson = """
        {
            "schema": 1,
            "expenses": [
                {
                    "id": "exp-1",
                    "amount_minor": 800,
                    "currency": "USD",
                    "note": "Older Note",
                    "raw_text": "8 Older Note",
                    "category_id": "food",
                    "created_at": 1000,
                    "local_date": "2026-10-15",
                    "updated_at": 3000,
                    "deleted_at": null
                }
            ]
        }
        """.trimIndent()

        val resultOlder = dataImporter.importJson(olderJson)
        assertTrue(resultOlder.success)
        assertEquals(0, resultOlder.expensesImported) // Not overwritten

        val afterOlder = expenseDao.getById("exp-1")
        assertEquals(1000L, afterOlder?.amountMinor)
        assertEquals("New Note", afterOlder?.note)
        assertEquals(5000L, afterOlder?.updatedAt)

        // 2. Attempt to import incoming with equal updatedAt (5000L)
        val equalJson = """
        {
            "schema": 1,
            "expenses": [
                {
                    "id": "exp-1",
                    "amount_minor": 900,
                    "currency": "USD",
                    "note": "Equal Note",
                    "raw_text": "9 Equal Note",
                    "category_id": "food",
                    "created_at": 1000,
                    "local_date": "2026-10-15",
                    "updated_at": 5000,
                    "deleted_at": null
                }
            ]
        }
        """.trimIndent()

        val resultEqual = dataImporter.importJson(equalJson)
        assertTrue(resultEqual.success)
        assertEquals(0, resultEqual.expensesImported) // Not overwritten

        val afterEqual = expenseDao.getById("exp-1")
        assertEquals(1000L, afterEqual?.amountMinor)
        assertEquals("New Note", afterEqual?.note)

        // 3. Import incoming with newer updatedAt (6000L)
        val newerJson = """
        {
            "schema": 1,
            "expenses": [
                {
                    "id": "exp-1",
                    "amount_minor": 1500,
                    "currency": "USD",
                    "note": "Even Newer Note",
                    "raw_text": "15 Even Newer Note",
                    "category_id": "food",
                    "created_at": 1000,
                    "local_date": "2026-10-15",
                    "updated_at": 6000,
                    "deleted_at": null
                }
            ]
        }
        """.trimIndent()

        val resultNewer = dataImporter.importJson(newerJson)
        assertTrue(resultNewer.success)
        assertEquals(1, resultNewer.expensesImported) // Overwritten

        val afterNewer = expenseDao.getById("exp-1")
        assertEquals(1500L, afterNewer?.amountMinor)
        assertEquals("Even Newer Note", afterNewer?.note)
        assertEquals(6000L, afterNewer?.updatedAt)
    }

    @Test
    fun csvExportAndImport_roundTrip() = runTest {
        val e1 = ExpenseEntity(
            id = "exp-1",
            amountMinor = 650L,
            currency = "USD",
            note = "latte",
            rawText = "6.5 latte",
            categoryId = "food",
            createdAt = 1729000100000L,
            localDate = "2026-10-15",
            updatedAt = 1729000100000L,
            deletedAt = null,
        )
        val e2 = ExpenseEntity(
            id = "exp-2",
            amountMinor = 1500L,
            currency = "JPY",
            note = "ramen, with egg",
            rawText = "1500 ramen",
            categoryId = "food",
            createdAt = 1729000200000L,
            localDate = "2026-10-15",
            updatedAt = 1729000200000L,
            deletedAt = null,
        )
        expenseDao.upsertAll(listOf(e1, e2))

        val exportedCsv = csvExporter.exportCsv()
        assertTrue(exportedCsv.startsWith("date,time,amount,currency,category,note"))
        assertTrue(exportedCsv.contains("6.50,USD,food,latte"))
        assertTrue(exportedCsv.contains("1500,JPY,food,\"ramen, with egg\""))

        // Wipe DB
        database.clearAllTables()
        assertEquals(0, expenseDao.getAll().size)

        // Import CSV back
        val result = dataImporter.importCsv(exportedCsv)
        assertTrue(result.success)
        assertEquals(2, result.expensesImported)

        val restored = expenseDao.getAll()
        assertEquals(2, restored.size)

        val restoredLatte = restored.find { it.currency == "USD" }
        assertNotNull(restoredLatte)
        assertEquals(650L, restoredLatte?.amountMinor)
        assertEquals("latte", restoredLatte?.note)
        assertEquals("2026-10-15", restoredLatte?.localDate)

        val restoredRamen = restored.find { it.currency == "JPY" }
        assertNotNull(restoredRamen)
        assertEquals(1500L, restoredRamen?.amountMinor)
        assertEquals("ramen, with egg", restoredRamen?.note)
    }

    @Test
    fun importFromStream_autoDetectsFormat() = runTest {
        val json = """
        {
            "schema": 1,
            "expenses": [
                {
                    "id": "json-1",
                    "amount_minor": 100,
                    "currency": "USD",
                    "note": "tea",
                    "raw_text": "1 tea",
                    "category_id": "food",
                    "created_at": 1000,
                    "local_date": "2026-10-15",
                    "updated_at": 1000,
                    "deleted_at": null
                }
            ]
        }
        """.trimIndent()

        val jsonResult = dataImporter.importFromStream(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
        assertTrue(jsonResult.success)
        assertEquals(1, jsonResult.expensesImported)

        val csv = """
        date,time,amount,currency,category,note
        2026-10-15,10:00:00,2.50,USD,food,bagel
        """.trimIndent()

        val csvResult = dataImporter.importFromStream(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertTrue(csvResult.success)
        assertEquals(1, csvResult.expensesImported)

        assertEquals(2, expenseDao.getAll().size)
    }

    @Test
    fun csvImport_skipsMalformedAmountRow_andImportsValidRows() = runTest {
        val csv = """
        date,time,amount,currency,category,note
        2026-10-15,10:00:00,12.50,USD,food,valid coffee
        2026-10-15,10:05:00,not_a_number,USD,food,broken row
        2026-10-15,10:10:00,45.00,USD,transport,valid cab
        """.trimIndent()

        val result = dataImporter.importCsv(csv)
        assertTrue(result.success)
        assertEquals(2, result.expensesImported)

        val expenses = expenseDao.getAll()
        assertEquals(2, expenses.size)
        assertTrue(expenses.any { it.note == "valid coffee" && it.amountMinor == 1250L })
        assertTrue(expenses.any { it.note == "valid cab" && it.amountMinor == 4500L })
    }
}
