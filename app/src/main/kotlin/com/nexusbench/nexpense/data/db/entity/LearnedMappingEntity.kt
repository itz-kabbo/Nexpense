package com.nexusbench.nexpense.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learned_mapping")
data class LearnedMappingEntity(
    @PrimaryKey
    @ColumnInfo(name = "normalized_note")
    val normalizedNote: String,

    @ColumnInfo(name = "category_id")
    val categoryId: String,

    val hits: Int = 1,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
