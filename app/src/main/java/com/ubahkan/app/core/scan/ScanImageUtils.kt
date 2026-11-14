package com.ubahkan.app.core.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import kotlin.math.max

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// UTILITAS BITMAP PINDAI  -  muat dari galeri (EXIF ditangani
// ImageDecoder), putar hasil jepret kamera, dan kecilkan
// bitmap agar pemrosesan OpenCV tetap ringan.
// ============================================================

object ScanImageUtils {

    private const val MAX_DIMENSION = 3000

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            var bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
            if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
            scaleToMaxDimension(bitmap, MAX_DIMENSION)
        } catch (t: Throwable) {
            // Cadangan untuk format yang tidak didukung ImageDecoder.
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val decoded = BitmapFactory.decodeStream(input) ?: return null
                    val argb = if (decoded.config == Bitmap.Config.ARGB_8888) decoded
                    else decoded.copy(Bitmap.Config.ARGB_8888, false)
                    scaleToMaxDimension(argb, MAX_DIMENSION)
                }
            } catch (t2: Throwable) {
                null
            }
        }
    }

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return bitmap
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val factor = maxDimension.toFloat() / longest.toFloat()
        val w = (bitmap.width * factor).toInt().coerceAtLeast(1)
        val h = (bitmap.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    fun downscaleToWidth(bitmap: Bitmap, targetWidth: Int): Bitmap {
        if (bitmap.width <= targetWidth) return bitmap
        val factor = targetWidth.toFloat() / bitmap.width.toFloat()
        val h = (bitmap.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, h, true)
    }
}
