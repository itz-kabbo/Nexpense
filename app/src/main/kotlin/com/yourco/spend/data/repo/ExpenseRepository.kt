package com.yourco.spend.data.repo

import com.yourco.spend.data.db.dao.BucketTotal
import com.yourco.spend.data.db.dao.CategoryTotal
import com.yourco.spend.data.db.dao.CurrencyTotal
import com.yourco.spend.data.db.dao.ExpenseDao
import com.yourco.spend.data.db.dao.ExpenseWithCategoryName
import com.yourco.spend.data.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface ExpenseRepository {
    suspend fun add(expense: ExpenseEntity)
    suspend fun update(expense: ExpenseEntity)
    suspend fun softDelete(id: String, now: Long? = null)
    suspend fun restore(id: String)
    suspend fun restore(id: String, now: Long) = restore(id)
    fun observeDay(date: String): Flow<List<ExpenseEntity>>
    fun observeDailyCurrencyTotals(date: String): Flow<List<CurrencyTotal>> =
        observeDay(date).map { expenses ->
            expenses.groupBy { it.currency }.map { (currency, rows) ->
                CurrencyTotal(currency, rows.sumOf { it.amountMinor })
            }.sortedBy { it.currencyCode }
        }
    fun observeDayWithCategoryNames(date: String): Flow<List<ExpenseWithCategoryName>> =
        observeDay(date).map { expenses -> expenses.map { ExpenseWithCategoryName(it, null) } }
    fun observeTotal(from: String, to: String, currency: String): Flow<Long>
    fun observeByCategory(from: String, to: String, currency: String): Flow<List<CategoryTotal>>
    fun observeBuckets(from: String, to: String, len: Int, currency: String): Flow<List<BucketTotal>>
    suspend fun getById(id: String): ExpenseEntity?
    suspend fun getAll(): List<ExpenseEntity>
    suspend fun clearUserData() = Unit
}

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val clock: Clock,
) : ExpenseRepository {

    override suspend fun add(expense: ExpenseEntity) {
        expenseDao.upsert(expense)
    }

    override suspend fun update(expense: ExpenseEntity) {
        expenseDao.upsert(expense)
    }

    override suspend fun softDelete(id: String, now: Long?) {
        val deleteTime = now ?: clock.millis()
        expenseDao.softDelete(id, deleteTime)
    }

    override suspend fun restore(id: String) {
        restore(id, clock.millis())
    }

    override suspend fun restore(id: String, now: Long) {
        expenseDao.restore(id, now)
    }

    override fun observeDay(date: String): Flow<List<ExpenseEntity>> {
        return expenseDao.observeDay(date)
    }

    override fun observeDailyCurrencyTotals(date: String): Flow<List<CurrencyTotal>> {
        return expenseDao.observeDailyCurrencyTotals(date)
    }

    override fun observeDayWithCategoryNames(date: String): Flow<List<ExpenseWithCategoryName>> {
        return expenseDao.observeDayWithCategoryNames(date)
    }

    override fun observeTotal(from: String, to: String, currency: String): Flow<Long> {
        return expenseDao.observeTotal(from, to, currency)
    }

    override fun observeByCategory(from: String, to: String, currency: String): Flow<List<CategoryTotal>> {
        return expenseDao.observeByCategory(from, to, currency)
    }

    override fun observeBuckets(from: String, to: String, len: Int, currency: String): Flow<List<BucketTotal>> {
        return expenseDao.observeBuckets(from, to, len, currency)
    }

    override suspend fun getById(id: String): ExpenseEntity? {
        return expenseDao.getById(id)
    }

    override suspend fun getAll(): List<ExpenseEntity> {
        return expenseDao.getAll()
    }

    override suspend fun clearUserData() {
        expenseDao.clearUserData()
    }
}
