package com.yourco.spend.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourco.spend.data.db.entity.LearnedMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnedMappingDao {
    @Query("SELECT * FROM learned_mapping")
    fun observeAll(): Flow<List<LearnedMappingEntity>>

    @Query("SELECT * FROM learned_mapping WHERE normalized_note = :normalizedNote")
    suspend fun get(normalizedNote: String): LearnedMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: LearnedMappingEntity)
}
