package com.yourco.spend.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourco.spend.data.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ExpenseEntity>)

    @Query("UPDATE expense SET deleted_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE expense SET deleted_at = NULL WHERE id = :id")
    suspend fun restore(id: String)

    @Query("SELECT * FROM expense WHERE local_date = :date AND deleted_at IS NULL ORDER BY created_at DESC")
    fun observeDay(date: String): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM expense WHERE local_date BETWEEN :from AND :to AND deleted_at IS NULL")
    fun observeTotal(from: String, to: String): Flow<Long>

    @Query(
        "SELECT category_id, SUM(amount_minor) AS total, COUNT(*) AS count FROM expense " +
            "WHERE local_date BETWEEN :from AND :to AND deleted_at IS NULL " +
            "GROUP BY category_id ORDER BY total DESC"
    )
    fun observeByCategory(from: String, to: String): Flow<List<CategoryTotal>>

    // len = 7 groups by month (yyyy-MM), len = 10 groups by day (yyyy-MM-dd)
    @Query(
        "SELECT substr(local_date, 1, :len) AS bucket, SUM(amount_minor) AS total FROM expense " +
            "WHERE local_date BETWEEN :from AND :to AND deleted_at IS NULL GROUP BY bucket ORDER BY bucket"
    )
    fun observeBuckets(from: String, to: String, len: Int): Flow<List<BucketTotal>>

    @Query("SELECT * FROM expense WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expense")
    suspend fun getAll(): List<ExpenseEntity>
}
