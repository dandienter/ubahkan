package com.ubahkan.app.core.scan

import android.graphics.PointF

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// QUAD DOKUMEN  -  empat sudut dokumen (TL, TR, BR, BL) dalam
// koordinat piksel bitmap sumber. Dipakai detektor OpenCV,
// tahap sesuaikan sudut, dan warp perspektif.
// ============================================================

data class ScanQuad(
    val tl: PointF,
    val tr: PointF,
    val br: PointF,
    val bl: PointF
) {
    fun toList(): List<PointF> = listOf(tl, tr, br, bl)

    /** Petakan quad ini (relatif terhadap ukuran lama) ke ukuran bitmap lain. */
    fun scaledTo(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): ScanQuad {
        if (srcWidth <= 0 || srcHeight <= 0) return this
        val sx = dstWidth.toFloat() / srcWidth.toFloat()
        val sy = dstHeight.toFloat() / srcHeight.toFloat()
        fun map(p: PointF) = PointF(p.x * sx, p.y * sy)
        return ScanQuad(map(tl), map(tr), map(br), map(bl))
    }

    companion object {
        fun fromList(points: List<PointF>): ScanQuad =
            ScanQuad(points[0], points[1], points[2], points[3])

        /** Quad seluruh bingkai dengan sedikit inset agar gagang mudah digeser. */
        fun fullFrame(width: Int, height: Int): ScanQuad {
            val ix = width * 0.04f
            val iy = height * 0.04f
            return ScanQuad(
                PointF(ix, iy),
                PointF(width - ix, iy),
                PointF(width - ix, height - iy),
                PointF(ix, height - iy)
            )
        }

        /** Quad dari koordinat ternormalisasi 0..1 ke piksel bitmap tujuan. */
        fun fromNormalized(points: List<PointF>, width: Int, height: Int): ScanQuad {
            fun map(p: PointF) = PointF(
                (p.x * width).coerceIn(0f, width.toFloat()),
                (p.y * height).coerceIn(0f, height.toFloat())
            )
            return ScanQuad(map(points[0]), map(points[1]), map(points[2]), map(points[3]))
        }
    }
}

/** Urutkan 4 titik bebas menjadi TL, TR, BR, BL yang konsisten. */
fun orderQuadPoints(points: List<PointF>): ScanQuad {
    require(points.size == 4)
    val tl = points.minByOrNull { it.x + it.y }!!
    val br = points.maxByOrNull { it.x + it.y }!!
    val tr = points.maxByOrNull { it.x - it.y }!!
    val bl = points.minByOrNull { it.x - it.y }!!
    return ScanQuad(tl, tr, br, bl)
}
