package com.yourco.spend.data.repo

import com.yourco.spend.data.db.dao.BucketTotal
import com.yourco.spend.data.db.dao.CategoryTotal
import com.yourco.spend.data.db.dao.ExpenseDao
import com.yourco.spend.data.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface ExpenseRepository {
    suspend fun add(expense: ExpenseEntity)
    suspend fun update(expense: ExpenseEntity)
    suspend fun softDelete(id: String, now: Long? = null)
    suspend fun restore(id: String)
    fun observeDay(date: String): Flow<List<ExpenseEntity>>
    fun observeTotal(from: String, to: String): Flow<Long>
    fun observeByCategory(from: String, to: String): Flow<List<CategoryTotal>>
    fun observeBuckets(from: String, to: String, len: Int): Flow<List<BucketTotal>>
    suspend fun getById(id: String): ExpenseEntity?
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
        expenseDao.restore(id)
    }

    override fun observeDay(date: String): Flow<List<ExpenseEntity>> {
        return expenseDao.observeDay(date)
    }

    override fun observeTotal(from: String, to: String): Flow<Long> {
        return expenseDao.observeTotal(from, to)
    }

    override fun observeByCategory(from: String, to: String): Flow<List<CategoryTotal>> {
        return expenseDao.observeByCategory(from, to)
    }

    override fun observeBuckets(from: String, to: String, len: Int): Flow<List<BucketTotal>> {
        return expenseDao.observeBuckets(from, to, len)
    }

    override suspend fun getById(id: String): ExpenseEntity? {
        return expenseDao.getById(id)
    }
}
