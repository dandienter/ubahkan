package com.ubahkan.app.core.scan

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// OCR PINDAI (ML Kit, model bundled di dalam APK sehingga
// bekerja penuh tanpa internet).
// ============================================================

object ScanOcr {

    suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                runCatching { recognizer.close() }
                if (cont.isActive) cont.resume(result.text.trim())
            }
            .addOnFailureListener { error ->
                runCatching { recognizer.close() }
                if (cont.isActive) cont.resumeWithException(error)
            }
        cont.invokeOnCancellation { runCatching { recognizer.close() } }
    }
}
