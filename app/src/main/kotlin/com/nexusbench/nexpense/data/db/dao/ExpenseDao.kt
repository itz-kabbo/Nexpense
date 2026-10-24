package com.nexusbench.nexpense.data.db.dao

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nexusbench.nexpense.data.db.entity.CategoryEntity
import com.nexusbench.nexpense.data.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

data class ImportBatchCounts(
    val expensesImported: Int,
    val categoriesImported: Int,
)

data class CurrencyTotal(
    @ColumnInfo(name = "currency") val currencyCode: String,
    @ColumnInfo(name = "total") val totalMinor: Long,
)

data class ExpenseWithCategoryName(
    @Embedded val expense: ExpenseEntity,
    @ColumnInfo(name = "category_name_override") val categoryNameOverride: String?,
)

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Transaction
    suspend fun applyImportBatch(
        categories: List<CategoryEntity>,
        expenses: List<ExpenseEntity>,
    ): ImportBatchCounts {
        upsertCategories(categories)
        var importedExpenses = 0
        for (incoming in expenses) {
            val existing = getById(incoming.id)
            val existingChangeAt = existing?.let { maxOf(it.updatedAt, it.deletedAt ?: Long.MIN_VALUE) }
            val incomingChangeAt = maxOf(incoming.updatedAt, incoming.deletedAt ?: Long.MIN_VALUE)
            if (existing == null || incomingChangeAt > existingChangeAt!!) {
                upsert(incoming)
                importedExpenses++
            }
        }
        return ImportBatchCounts(importedExpenses, categories.size)
    }

    @Query("DELETE FROM expense")
    suspend fun deleteAllExpenses()

    @Query("DELETE FROM category WHERE is_builtin = 0")
    suspend fun deleteCustomCategories()

    @Query("DELETE FROM learned_mapping")
    suspend fun deleteLearnedMappings()

    @Transaction
    suspend fun clearUserData() {
        deleteAllExpenses()
        deleteCustomCategories()
        deleteLearnedMappings()
        upsertCategories(CategoryEntity.DEFAULT_CATEGORIES)
    }

    @Query("UPDATE expense SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE expense SET deleted_at = NULL, updated_at = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM expense WHERE local_date = :date AND deleted_at IS NULL ORDER BY created_at DESC")
    fun observeDay(date: String): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT currency, SUM(amount_minor) AS total FROM expense " +
            "WHERE local_date = :date AND deleted_at IS NULL GROUP BY currency ORDER BY currency"
    )
    fun observeDailyCurrencyTotals(date: String): Flow<List<CurrencyTotal>>

    @Query(
        "SELECT expense.*, category.name_override AS category_name_override " +
            "FROM expense LEFT JOIN category ON category.id = expense.category_id " +
            "WHERE expense.local_date = :date AND expense.deleted_at IS NULL " +
            "ORDER BY expense.created_at DESC"
    )
    fun observeDayWithCategoryNames(date: String): Flow<List<ExpenseWithCategoryName>>

    // VERIFY: Scoped by currency so totals never mix units across different currencies
    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM expense WHERE local_date BETWEEN :from AND :to AND deleted_at IS NULL AND currency = :currency")
    fun observeTotal(from: String, to: String, currency: String): Flow<Long>

    // VERIFY: Scoped by currency so totals never mix units across different currencies
    @Query(
        "SELECT category_id, SUM(amount_minor) AS total, COUNT(*) AS count FROM expense " +
            "WHERE local_date BETWEEN :from AND :to AND deleted_at IS NULL AND currency = :currency " +
            "GROUP BY category_id ORDER BY total DESC"
    )
    fun observeByCategory(from: String, to: String, currency: String): Flow<List<CategoryTotal>>

    // len = 7 groups by month (yyyy-MM), len = 10 groups by day (yyyy-MM-dd)
    // VERIFY: Scoped by currency so totals never mix units across different currencies
    @Query(
        "SELECT substr(local_date, 1, :len) AS bucket, SUM(amount_minor) AS total FROM expense " +
            "WHERE local_date BETWEEN :from AND :to AND deleted_at IS NULL AND currency = :currency GROUP BY bucket ORDER BY bucket"
    )
    fun observeBuckets(from: String, to: String, len: Int, currency: String): Flow<List<BucketTotal>>

    @Query("SELECT * FROM expense WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expense")
    suspend fun getAll(): List<ExpenseEntity>
}
