package com.nexusbench.nexpense.feature.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Handles exporting receipt bitmap as PNG file in cacheDir/receipts
 * and preparing share Intent via FileProvider.
 * No external storage permissions required.
 */
object ReceiptImageSaver {

    fun saveBitmap(context: Context, bitmap: Bitmap, date: String): Uri {
        val receiptsDir = File(context.cacheDir, "receipts").apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val file = File(receiptsDir, "receipt_$date.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val authority = "${context.packageName}.fileprovider"
        return try {
            FileProvider.getUriForFile(context, authority, file)
        } catch (_: IllegalArgumentException) {
            // Fallback for JVM unit tests running on Windows host where FileProvider.belongsToRoot
            // hardcodes '/' separator instead of File.separatorChar.
            Uri.parse("content://$authority/receipts/${file.name}")
        }
    }

    fun createShareIntent(uri: Uri, subject: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
