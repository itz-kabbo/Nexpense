package com.nexusbench.nexpense.feature.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReceiptImageSaverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun saveBitmap_createsPngFileInCacheDirReceipts_andReturnsContentUri() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val testDate = "2026-10-15"

        val uri = ReceiptImageSaver.saveBitmap(context, bitmap, testDate)

        // Verify file is saved in cacheDir/receipts
        val expectedFile = File(context.cacheDir, "receipts/receipt_$testDate.png")
        assertTrue("Receipt file should exist", expectedFile.exists())
        assertTrue("Receipt file size should be > 0 bytes", expectedFile.length() > 0)

        // Verify content URI format
        assertNotNull(uri)
        assertEquals("content", uri.scheme)
        assertTrue(uri.authority?.contains("fileprovider") == true)
        assertTrue(uri.toString().contains("receipt_$testDate.png"))
    }

    @Test
    fun createShareIntent_setsActionSendAndExtraStream() {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val uri = ReceiptImageSaver.saveBitmap(context, bitmap, "2026-10-14")

        val intent = ReceiptImageSaver.createShareIntent(uri, "Nexpense Receipt - Thursday, Oct 15")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/png", intent.type)
        @Suppress("DEPRECATION")
        val streamUri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        assertEquals(uri, streamUri)
        assertEquals("Nexpense Receipt - Thursday, Oct 15", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }
}
