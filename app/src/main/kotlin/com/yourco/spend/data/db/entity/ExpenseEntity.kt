package com.yourco.spend.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense",
    indices = [
        Index("local_date"),
        Index(value = ["category_id", "local_date"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,

    val currency: String,

    val note: String,

    @ColumnInfo(name = "raw_text")
    val rawText: String,

    @ColumnInfo(name = "category_id")
    val categoryId: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "local_date")
    val localDate: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
)
