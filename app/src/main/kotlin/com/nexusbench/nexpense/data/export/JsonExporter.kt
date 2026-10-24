package com.nexusbench.nexpense.data.export

import com.nexusbench.nexpense.data.db.entity.CategoryEntity
import com.nexusbench.nexpense.data.db.entity.ExpenseEntity
import com.nexusbench.nexpense.data.repo.CategoryRepository
import com.nexusbench.nexpense.data.repo.ExpenseRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonExporter @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val clock: Clock,
) {
    suspend fun exportJson(): String {
        val categories = categoryRepository.getAll()
        val expenses = expenseRepository.getAll()

        val root = JSONObject()
        root.put("schema", 1)
        root.put("exported_at", clock.millis())

        val categoriesArray = JSONArray()
        for (cat in categories) {
            val catObj = JSONObject()
            catObj.put("id", cat.id)
            if (cat.nameOverride != null) {
                catObj.put("name_override", cat.nameOverride)
            } else {
                catObj.put("name_override", JSONObject.NULL)
            }
            catObj.put("icon", cat.icon)
            catObj.put("sort_order", cat.sortOrder)
            catObj.put("is_hidden", cat.isHidden)
            catObj.put("is_builtin", cat.isBuiltin)
            categoriesArray.put(catObj)
        }
        root.put("categories", categoriesArray)

        val expensesArray = JSONArray()
        for (exp in expenses) {
            val expObj = JSONObject()
            expObj.put("id", exp.id)
            expObj.put("amount_minor", exp.amountMinor)
            expObj.put("currency", exp.currency)
            expObj.put("note", exp.note)
            expObj.put("raw_text", exp.rawText)
            expObj.put("category_id", exp.categoryId)
            expObj.put("created_at", exp.createdAt)
            expObj.put("local_date", exp.localDate)
            expObj.put("updated_at", exp.updatedAt)
            if (exp.deletedAt != null) {
                expObj.put("deleted_at", exp.deletedAt)
            } else {
                expObj.put("deleted_at", JSONObject.NULL)
            }
            expensesArray.put(expObj)
        }
        root.put("expenses", expensesArray)

        return root.toString(2)
    }

    suspend fun export(outputStream: OutputStream) {
        val json = exportJson()
        outputStream.write(json.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }
}
