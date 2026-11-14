package com.ubahkan.app.core.scan

import android.graphics.Bitmap
import android.graphics.PointF
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.sqrt

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// MESIN DETEKSI DOKUMEN (OpenCV)
// ensureOpenCv(): inisialisasi pustaka native sekali saja.
// detect(): pipa murni bitmap -> quad dokumen atau null.
// warp(): luruskan perspektif mengikuti quad.
// ============================================================

object ScanDetector {

    @Volatile
    private var initTried = false

    @Volatile
    private var initOk = false

    fun ensureOpenCv(): Boolean {
        if (initTried) return initOk
        synchronized(this) {
            if (initTried) return initOk
            initTried = true
            initOk = try {
                OpenCVLoader.initLocal()
            } catch (t: Throwable) {
                false
            }
            return initOk
        }
    }

    private fun asArgb(bitmap: Bitmap): Bitmap =
        if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
        else bitmap.copy(Bitmap.Config.ARGB_8888, false)

    /**
     * Deteksi quad dokumen pada bitmap. Mengembalikan null bila
     * OpenCV belum siap atau tidak ada kontur 4 titik yang lolos
     * rasio luas minimal. Aman dipanggil dari thread latar.
     */
    fun detect(source: Bitmap): ScanQuad? {
        if (!ensureOpenCv()) return null
        val bitmap = asArgb(source)
        val mats = mutableListOf<Mat>()
        fun <T : Mat> T.track(): T {
            mats.add(this)
            return this
        }
        return try {
            val src = Mat().track()
            Utils.bitmapToMat(bitmap, src)

            val maxSide = max(src.cols(), src.rows())
            val scale = if (maxSide > 800) 800.0 / maxSide else 1.0
            val small = Mat().track()
            if (scale < 1.0) {
                Imgproc.resize(
                    src, small,
                    Size(src.cols() * scale, src.rows() * scale),
                    0.0, 0.0, Imgproc.INTER_AREA
                )
            } else {
                src.copyTo(small)
            }

            val gray = Mat().track()
            Imgproc.cvtColor(small, gray, Imgproc.COLOR_RGBA2GRAY)
            val blurred = Mat().track()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            val edges = Mat().track()
            Imgproc.Canny(blurred, edges, 50.0, 150.0)
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0)).track()
            Imgproc.dilate(edges, edges, kernel)

            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat().track()
            Imgproc.findContours(
                edges, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
            )
            val imageArea = small.cols().toDouble() * small.rows().toDouble()
            val sorted = contours.sortedByDescending { Imgproc.contourArea(it) }

            var found: ScanQuad? = null
            for (contour in sorted.take(5)) {
                val area = Imgproc.contourArea(contour)
                if (area < imageArea * 0.12) break
                val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)
                if (approx.rows() == 4) {
                    val pts = approx.toArray().map { p ->
                        PointF((p.x / scale).toFloat(), (p.y / scale).toFloat())
                    }
                    found = orderQuadPoints(pts)
                    approx.release()
                    break
                }
                approx.release()
            }
            contours.forEach { it.release() }
            found
        } catch (t: Throwable) {
            null
        } finally {
            mats.forEach { it.release() }
        }
    }

    /**
     * Warp perspektif bitmap mengikuti quad. Sisi terpanjang hasil
     * dibatasi sekitar 2000px agar berkas dan memori tetap wajar.
     */
    fun warp(source: Bitmap, quad: ScanQuad, maxSide: Int = 2000): Bitmap {
        check(ensureOpenCv()) { "OpenCV belum siap" }
        val bitmap = asArgb(source)
        val srcMat = Mat()
        val dstMat = Mat()
        try {
            Utils.bitmapToMat(bitmap, srcMat)

            fun dist(a: PointF, b: PointF): Double {
                val dx = (a.x - b.x).toDouble()
                val dy = (a.y - b.y).toDouble()
                return sqrt(dx * dx + dy * dy)
            }
            var outW = max(dist(quad.tl, quad.tr), dist(quad.bl, quad.br))
            var outH = max(dist(quad.tl, quad.bl), dist(quad.tr, quad.br))
            val longest = max(outW, outH)
            if (longest > maxSide) {
                val f = maxSide / longest
                outW *= f
                outH *= f
            }
            val w = outW.toInt().coerceAtLeast(1)
            val h = outH.toInt().coerceAtLeast(1)

            val srcPts = MatOfPoint2f(
                Point(quad.tl.x.toDouble(), quad.tl.y.toDouble()),
                Point(quad.tr.x.toDouble(), quad.tr.y.toDouble()),
                Point(quad.br.x.toDouble(), quad.br.y.toDouble()),
                Point(quad.bl.x.toDouble(), quad.bl.y.toDouble())
            )
            val dstPts = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(w - 1.0, 0.0),
                Point(w - 1.0, h - 1.0),
                Point(0.0, h - 1.0)
            )
            val transform = Imgproc.getPerspectiveTransform(srcPts, dstPts)
            Imgproc.warpPerspective(srcMat, dstMat, transform, Size(w.toDouble(), h.toDouble()))
            srcPts.release()
            dstPts.release()
            transform.release()

            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(dstMat, out)
            return out
        } finally {
            srcMat.release()
            dstMat.release()
        }
    }

    /** Mat RGBA dari bitmap, pemanggil wajib melepaskannya. */
    internal fun bitmapToRgbaMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(asArgb(bitmap), mat)
        return mat
    }

    /** Bitmap ARGB_8888 dari Mat (3 atau 4 kanal). */
    internal fun matToBitmap(mat: Mat): Bitmap {
        val rgba = Mat()
        try {
            when (mat.channels()) {
                1 -> Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_GRAY2RGBA)
                3 -> Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_RGB2RGBA)
                else -> mat.copyTo(rgba)
            }
            val out = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgba, out)
            return out
        } finally {
            rgba.release()
        }
    }
}
