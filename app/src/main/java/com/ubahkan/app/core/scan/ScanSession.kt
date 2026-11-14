package com.ubahkan.app.core.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.mutableStateListOf
import com.ubahkan.app.core.OutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// SESI PINDAI  -  daftar halaman hasil pindai selama alur
// berjalan, plus penulis PDF yang menyimpan semua halaman
// ke Downloads/Ubahkan lewat OutputManager (mekanisme yang
// sama dengan hasil konversi).
// ============================================================

data class ScanPage(
    val id: Long,
    var bitmap: Bitmap,
    var ocrText: String? = null
)

class ScanSession {
    val pages = mutableStateListOf<ScanPage>()
    private var nextId = 1L

    fun addPage(bitmap: Bitmap) {
        pages.add(ScanPage(nextId++, bitmap))
    }

    fun removePage(page: ScanPage) {
        pages.remove(page)
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in pages.indices || toIndex !in pages.indices) return
        val item = pages.removeAt(fromIndex)
        pages.add(toIndex, item)
    }

    fun clear() {
        pages.clear()
    }
}

object ScanPdfWriter {

    /**
     * Gabung semua halaman menjadi satu PDF. Tiap halaman mengikuti
     * rasio bitmapnya dan bitmapnya lebih dulu dikompresi JPEG
     * kualitas 85. Berkas disimpan lewat OutputManager.finalizeOutput
     * ke Downloads/Ubahkan. Mengembalikan File tujuan atau null.
     */
    suspend fun saveToDownloads(context: Context, pageBitmaps: List<Bitmap>): File? =
        withContext(Dispatchers.IO) {
            if (pageBitmaps.isEmpty()) return@withContext null
            val appContext = context.applicationContext
            val document = PdfDocument()
            var tempFile: File? = null
            try {
                pageBitmaps.forEachIndexed { index, bitmap ->
                    val pageBitmap = jpegRoundTrip(bitmap, 85)
                    val pageInfo = PdfDocument.PageInfo
                        .Builder(pageBitmap.width, pageBitmap.height, index + 1)
                        .create()
                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(pageBitmap, 0f, 0f, null)
                    document.finishPage(page)
                }
                val stamp = SimpleDateFormat("dd MMM yyyy HH.mm", Locale.getDefault())
                    .format(Date())
                tempFile = OutputManager.createTempFile(appContext, "scan_pdf", "pdf")
                FileOutputStream(tempFile).use { out -> document.writeTo(out) }
                val destPath = OutputManager.finalizeOutput(
                    appContext, tempFile, "Pindai $stamp", "pdf"
                )
                destPath?.let { File(it) }
            } catch (t: Throwable) {
                tempFile?.let { runCatching { it.delete() } }
                null
            } finally {
                document.close()
            }
        }

    private fun jpegRoundTrip(bitmap: Bitmap, quality: Int): Bitmap {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val bytes = stream.toByteArray()
        return android.graphics.BitmapFactory.decodeStream(ByteArrayInputStream(bytes))
            ?: bitmap
    }
}
