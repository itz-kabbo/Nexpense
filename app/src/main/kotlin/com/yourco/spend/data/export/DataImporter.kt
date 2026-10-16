package com.yourco.spend.data.export

import com.yourco.spend.core.money.fractionDigits
import com.yourco.spend.data.db.dao.CategoryDao
import com.yourco.spend.data.db.dao.ExpenseDao
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import org.json.JSONObject
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(
    val expensesImported: Int,
    val categoriesImported: Int,
    val success: Boolean,
    val error: String? = null,
)

@Singleton
open class DataImporter @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val clock: Clock,
) {
    private val zoneId: ZoneId get() = clock.zone
    open suspend fun importJson(jsonString: String): ImportResult {
        return try {
            val root = JSONObject(jsonString)
            val schema = root.optInt("schema", 0)
            if (schema < 1) {
                return ImportResult(0, 0, success = false, error = "Unsupported schema version: $schema")
            }

            var categoriesCount = 0
            val categoriesArray = root.optJSONArray("categories")
            if (categoriesArray != null) {
                for (i in 0 until categoriesArray.length()) {
                    val catObj = categoriesArray.getJSONObject(i)
                    val id = catObj.getString("id")
                    val nameOverride = if (catObj.isNull("name_override")) null else catObj.getString("name_override")
                    val icon = catObj.optString("icon", "category")
                    val sortOrder = catObj.optInt("sort_order", i)
                    val isHidden = catObj.optBoolean("is_hidden", false)
                    val isBuiltin = catObj.optBoolean("is_builtin", false)

                    val category = CategoryEntity(
                        id = id,
                        nameOverride = nameOverride,
                        icon = icon,
                        sortOrder = sortOrder,
                        isHidden = isHidden,
                        isBuiltin = isBuiltin,
                    )
                    categoryDao.upsert(category)
                    categoriesCount++
                }
            }

            var expensesCount = 0
            val expensesArray = root.optJSONArray("expenses")
            if (expensesArray != null) {
                for (i in 0 until expensesArray.length()) {
                    val expObj = expensesArray.getJSONObject(i)
                    val id = expObj.getString("id")
                    val amountMinor = expObj.getLong("amount_minor")
                    val currency = expObj.getString("currency")
                    val note = expObj.optString("note", "")
                    val rawText = expObj.optString("raw_text", note)
                    val categoryId = expObj.getString("category_id")
                    val createdAt = expObj.getLong("created_at")
                    val localDate = expObj.getString("local_date")
                    val updatedAt = expObj.getLong("updated_at")
                    val deletedAt = if (expObj.isNull("deleted_at")) null else expObj.getLong("deleted_at")

                    val incoming = ExpenseEntity(
                        id = id,
                        amountMinor = amountMinor,
                        currency = currency,
                        note = note,
                        rawText = rawText,
                        categoryId = categoryId,
                        createdAt = createdAt,
                        localDate = localDate,
                        updatedAt = updatedAt,
                        deletedAt = deletedAt,
                    )

                    val existing = expenseDao.getById(id)
                    if (existing == null || incoming.updatedAt > existing.updatedAt) {
                        expenseDao.upsert(incoming)
                        expensesCount++
                    }
                }
            }

            ImportResult(
                expensesImported = expensesCount,
                categoriesImported = categoriesCount,
                success = true,
            )
        } catch (e: Exception) {
            ImportResult(0, 0, success = false, error = e.message)
        }
    }

    open suspend fun importCsv(csvString: String): ImportResult {
        return try {
            val rows = parseCsv(csvString)
            if (rows.isEmpty()) {
                return ImportResult(0, 0, success = true)
            }

            val header = rows[0].map { it.trim().lowercase() }
            val dateIdx = header.indexOf("date")
            val timeIdx = header.indexOf("time")
            val amountIdx = header.indexOf("amount")
            val currencyIdx = header.indexOf("currency")
            val categoryIdx = header.indexOf("category")
            val noteIdx = header.indexOf("note")

            if (dateIdx == -1 || amountIdx == -1 || currencyIdx == -1 || categoryIdx == -1) {
                return ImportResult(0, 0, success = false, error = "Missing required CSV columns in header")
            }

            var expensesCount = 0
            for (rowIndex in 1 until rows.size) {
                val row = rows[rowIndex]
                if (row.size <= maxOf(dateIdx, amountIdx, currencyIdx, categoryIdx)) continue

                val dateStr = row[dateIdx].trim()
                val timeStr = if (timeIdx != -1 && timeIdx < row.size) row[timeIdx].trim() else "12:00:00"
                val amountStr = row[amountIdx].trim()
                val currency = row[currencyIdx].trim().uppercase()
                val categoryId = row[categoryIdx].trim()
                val note = if (noteIdx != -1 && noteIdx < row.size) row[noteIdx].trim() else ""

                if (dateStr.isBlank() || amountStr.isBlank()) continue

                val localDate = try {
                    LocalDate.parse(dateStr)
                } catch (_: Exception) {
                    continue
                }

                val localTime = try {
                    LocalTime.parse(timeStr)
                } catch (_: Exception) {
                    LocalTime.NOON
                }

                val createdAt = localDate.atTime(localTime).atZone(zoneId).toInstant().toEpochMilli()
                val digits = fractionDigits(currency)
                val amountMinor = try {
                    BigDecimal(amountStr)
                        .setScale(digits, RoundingMode.HALF_UP)
                        .movePointRight(digits)
                        .longValueExact()
                } catch (_: Exception) {
                    continue
                }

                val id = UUID.nameUUIDFromBytes("$dateStr-$timeStr-$amountMinor-$currency-$categoryId-$note".toByteArray(Charsets.UTF_8)).toString()
                val incoming = ExpenseEntity(
                    id = id,
                    amountMinor = amountMinor,
                    currency = currency,
                    note = note,
                    rawText = if (note.isBlank()) amountStr else "$amountStr $note",
                    categoryId = categoryId,
                    createdAt = createdAt,
                    localDate = dateStr,
                    updatedAt = createdAt,
                    deletedAt = null,
                )

                val existing = expenseDao.getById(id)
                if (existing == null || incoming.updatedAt > existing.updatedAt) {
                    expenseDao.upsert(incoming)
                    expensesCount++
                }
            }

            ImportResult(
                expensesImported = expensesCount,
                categoriesImported = 0,
                success = true,
            )
        } catch (e: Exception) {
            ImportResult(0, 0, success = false, error = e.message)
        }
    }

    open suspend fun importFromStream(inputStream: InputStream): ImportResult {
        val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val trimmed = content.trim()
        return if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            importJson(trimmed)
        } else {
            importCsv(trimmed)
        }
    }

    private fun parseCsv(csvString: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = csvString.length

        while (i < len) {
            val c = csvString[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && csvString[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < len && csvString[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }
        if (currentRow.isNotEmpty() || currentField.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow.toList())
        }
        return rows.filter { row -> row.any { it.isNotBlank() } }
    }
}
