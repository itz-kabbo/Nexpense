package com.nexusbench.nexpense.data.db.dao

import androidx.room.ColumnInfo

data class CategoryTotal(
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    val total: Long,
    val count: Int,
)

data class BucketTotal(
    val bucket: String,
    val total: Long,
)
