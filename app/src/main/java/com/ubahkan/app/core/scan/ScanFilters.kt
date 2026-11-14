package com.ubahkan.app.core.scan

import android.graphics.Bitmap
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.CLAHE
import org.opencv.imgproc.Imgproc
import kotlin.math.min

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// FILTER DOKUMEN (OpenCV)  -  12 filter diterapkan pada hasil
// warp. Semua fungsi aman dipanggil dari thread latar dan
// mengembalikan bitmap baru (bitmap masukan tidak diubah).
// ============================================================

enum class ScanFilter {
    ORIGINAL,
    GRAYSCALE,
    BLACK_WHITE,
    WHITE_PAPER,
    SHADOW_REMOVE,
    SHARPEN_TEXT,
    CONTRAST_BOOST,
    COLOR_POP,
    SEPIA,
    NEGATIVE,
    BLUEPRINT,
    PHOTOCOPY
}

object ScanFilters {

    fun apply(source: Bitmap, filter: ScanFilter): Bitmap {
        if (filter == ScanFilter.ORIGINAL) return source
        check(ScanDetector.ensureOpenCv()) { "OpenCV belum siap" }
        val src = ScanDetector.bitmapToRgbaMat(source)
        val result: Mat = try {
            when (filter) {
                ScanFilter.ORIGINAL -> src
                ScanFilter.GRAYSCALE -> grayOf(src)
                ScanFilter.BLACK_WHITE -> blackWhite(src)
                ScanFilter.WHITE_PAPER -> normalizeBackground(src, strong = false)
                ScanFilter.SHADOW_REMOVE -> normalizeBackground(src, strong = true)
                ScanFilter.SHARPEN_TEXT -> sharpen(src)
                ScanFilter.CONTRAST_BOOST -> claheContrast(src)
                ScanFilter.COLOR_POP -> colorPop(src)
                ScanFilter.SEPIA -> sepia(src)
                ScanFilter.NEGATIVE -> negative(src)
                ScanFilter.BLUEPRINT -> blueprint(src)
                ScanFilter.PHOTOCOPY -> photocopy(src)
            }
        } catch (t: Throwable) {
            src.release()
            throw t
        }
        val out = ScanDetector.matToBitmap(result)
        if (result !== src) result.release()
        src.release()
        return out
    }

    /**
     * Kecerahan (-50..50) dan kontras (0.5..1.5) di atas hasil filter.
     * Rumus: hasil = (piksel - 128) * kontras + 128 + kecerahan.
     */
    fun applyBrightnessContrast(source: Bitmap, brightness: Int, contrast: Float): Bitmap {
        if (brightness == 0 && contrast == 1f) return source
        check(ScanDetector.ensureOpenCv()) { "OpenCV belum siap" }
        val src = ScanDetector.bitmapToRgbaMat(source)
        val rgb = Mat()
        val dst = Mat()
        try {
            Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB)
            val beta = 128.0 * (1.0 - contrast) + brightness
            rgb.convertTo(dst, -1, contrast.toDouble(), beta)
            return ScanDetector.matToBitmap(dst)
        } finally {
            src.release()
            rgb.release()
            dst.release()
        }
    }

    private fun grayOf(rgba: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        val out = Mat()
        Imgproc.cvtColor(gray, out, Imgproc.COLOR_GRAY2RGBA)
        gray.release()
        return out
    }

    private fun blackWhite(rgba: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        val bw = Mat()
        val rawBlock = min(gray.cols(), gray.rows()) / 12
        val blockSize = rawBlock.coerceIn(11, 61).let { if (it % 2 == 0) it + 1 else it }
        Imgproc.adaptiveThreshold(
            gray, bw, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
            blockSize, 10.0
        )
        val out = Mat()
        Imgproc.cvtColor(bw, out, Imgproc.COLOR_GRAY2RGBA)
        gray.release()
        bw.release()
        return out
    }

    /**
     * Normalisasi latar: latar diestimasi dari closing morfologi
     * besar + blur, lalu citra abu dibagi latar itu sehingga
     * kertas menjadi putih bersih. Varian kuat memakai kernel
     * lebih besar dan regangan kontras tambahan.
     */
    private fun normalizeBackground(rgba: Mat, strong: Boolean): Mat {
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        val shortSide = min(gray.cols(), gray.rows())
        val kernelSize = (if (strong) shortSide / 4 else shortSide / 8)
            .coerceIn(15, 151).let { if (it % 2 == 0) it + 1 else it }
        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE, Size(kernelSize.toDouble(), kernelSize.toDouble())
        )
        val bg = Mat()
        Imgproc.morphologyEx(gray, bg, Imgproc.MORPH_CLOSE, kernel)
        val blurSize = (kernelSize / 2).let { if (it % 2 == 0) it + 1 else it }.coerceAtLeast(3)
        Imgproc.GaussianBlur(bg, bg, Size(blurSize.toDouble(), blurSize.toDouble()), 0.0)
        val divided = Mat()
        Core.divide(gray, bg, divided, 255.0)
        if (strong) {
            val stretched = Mat()
            divided.convertTo(stretched, -1, 1.18, -14.0)
            stretched.copyTo(divided)
            stretched.release()
        }
        val out = Mat()
        Imgproc.cvtColor(divided, out, Imgproc.COLOR_GRAY2RGBA)
        gray.release()
        kernel.release()
        bg.release()
        divided.release()
        return out
    }

    private fun sharpen(rgba: Mat): Mat {
        val blurred = Mat()
        Imgproc.GaussianBlur(rgba, blurred, Size(0.0, 0.0), 3.0)
        val out = Mat()
        Core.addWeighted(rgba, 1.6, blurred, -0.6, 0.0, out)
        blurred.release()
        return out
    }

    private fun claheContrast(rgba: Mat): Mat {
        val rgb = Mat()
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
        val lab = Mat()
        Imgproc.cvtColor(rgb, lab, Imgproc.COLOR_RGB2Lab)
        val channels = mutableListOf<Mat>()
        Core.split(lab, channels)
        val clahe: CLAHE = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(channels[0], channels[0])
        Core.merge(channels, lab)
        val outRgb = Mat()
        Imgproc.cvtColor(lab, outRgb, Imgproc.COLOR_Lab2RGB)
        val out = Mat()
        Imgproc.cvtColor(outRgb, out, Imgproc.COLOR_RGB2RGBA)
        channels.forEach { it.release() }
        rgb.release()
        lab.release()
        outRgb.release()
        return out
    }

    private fun colorPop(rgba: Mat): Mat {
        val rgb = Mat()
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
        val hsv = Mat()
        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV)
        val channels = mutableListOf<Mat>()
        Core.split(hsv, channels)
        channels[1].convertTo(channels[1], -1, 1.45, 0.0)
        Core.merge(channels, hsv)
        val outRgb = Mat()
        Imgproc.cvtColor(hsv, outRgb, Imgproc.COLOR_HSV2RGB)
        val out = Mat()
        Imgproc.cvtColor(outRgb, out, Imgproc.COLOR_RGB2RGBA)
        channels.forEach { it.release() }
        rgb.release()
        hsv.release()
        outRgb.release()
        return out
    }

    private fun sepia(rgba: Mat): Mat {
        val rgb = Mat()
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
        val kernel = Mat(3, 3, CvType.CV_64F)
        kernel.put(
            0, 0,
            0.393, 0.769, 0.189,
            0.349, 0.686, 0.168,
            0.272, 0.534, 0.131
        )
        val transformed = Mat()
        Core.transform(rgb, transformed, kernel)
        val out = Mat()
        Imgproc.cvtColor(transformed, out, Imgproc.COLOR_RGB2RGBA)
        rgb.release()
        kernel.release()
        transformed.release()
        return out
    }

    private fun negative(rgba: Mat): Mat {
        val rgb = Mat()
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
        Core.bitwise_not(rgb, rgb)
        val out = Mat()
        Imgproc.cvtColor(rgb, out, Imgproc.COLOR_RGB2RGBA)
        rgb.release()
        return out
    }

    private fun blueprint(rgba: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        val inverted = Mat()
        Core.bitwise_not(gray, inverted)
        val colored = Mat()
        Imgproc.applyColorMap(inverted, colored, Imgproc.COLORMAP_WINTER)
        val out = Mat()
        Imgproc.cvtColor(colored, out, Imgproc.COLOR_BGR2RGBA)
        gray.release()
        inverted.release()
        colored.release()
        return out
    }

    private fun photocopy(rgba: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.medianBlur(gray, gray, 3)
        val bw = Mat()
        Imgproc.threshold(gray, bw, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
        val out = Mat()
        Imgproc.cvtColor(bw, out, Imgproc.COLOR_GRAY2RGBA)
        gray.release()
        bw.release()
        return out
    }
}
