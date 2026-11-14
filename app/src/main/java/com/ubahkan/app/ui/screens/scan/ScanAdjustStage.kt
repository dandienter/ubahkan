package com.ubahkan.app.ui.screens.scan

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.scan.ScanDetector
import com.ubahkan.app.core.scan.ScanQuad
import com.ubahkan.app.ui.theme.UbahkanBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// TAHAP 2 PINDAI: SESUAIKAN SUDUT
// Foto tampil penuh dengan 4 gagang titik yang bisa digeser
// satu per satu; garis poligon diperbarui langsung mengikuti
// titik. Atur Ulang menjalankan ulang deteksi (atau kembali
// ke quad penuh). Lanjut menyimpan quad pilihan pengguna.
// ============================================================

@Composable
fun ScanAdjustStage(
    bitmap: Bitmap,
    initialQuad: ScanQuad?,
    onConfirm: (ScanQuad) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val scope = rememberCoroutineScope()
    var points by remember(bitmap) {
        mutableStateOf(
            initialQuad?.toList()
                ?: ScanQuad.fullFrame(bitmap.width, bitmap.height).toList()
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val scaleDp = min(
                maxWidth.value / bitmap.width.toFloat(),
                maxHeight.value / bitmap.height.toFloat()
            )
            val displayWidthDp = bitmap.width * scaleDp
            val displayHeightDp = bitmap.height * scaleDp
            val density = LocalDensity.current.density
            val scalePx = scaleDp * density

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(displayWidthDp.dp, displayHeightDp.dp)) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path().apply {
                            points.forEachIndexed { index, point ->
                                val x = point.x * scalePx
                                val y = point.y * scalePx
                                if (index == 0) moveTo(x, y) else lineTo(x, y)
                            }
                            close()
                        }
                        drawPath(path = path, color = UbahkanBlue, alpha = 0.15f)
                        drawPath(
                            path = path,
                            color = UbahkanBlue,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                    points.forEachIndexed { index, point ->
                        val handleSize = 34.dp
                        Box(
                            modifier = Modifier
                                .offset {
                                    val half = (handleSize.toPx() / 2f).roundToInt()
                                    IntOffset(
                                        (point.x * scalePx).roundToInt() - half,
                                        (point.y * scalePx).roundToInt() - half
                                    )
                                }
                                .size(handleSize)
                                .pointerInput(index, scalePx) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val current = points[index]
                                        val nextX = (current.x + dragAmount.x / scalePx)
                                            .coerceIn(0f, bitmap.width.toFloat())
                                        val nextY = (current.y + dragAmount.y / scalePx)
                                            .coerceIn(0f, bitmap.height.toFloat())
                                        points = points.toMutableList().also { list ->
                                            list[index] = PointF(nextX, nextY)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.5.dp, UbahkanBlue, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = strings.scanAdjustHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val detected = withContext(Dispatchers.IO) {
                                runCatching { ScanDetector.detect(bitmap) }.getOrNull()
                            }
                            points = (detected
                                ?: ScanQuad.fullFrame(bitmap.width, bitmap.height)).toList()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.scanResetCorners)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = { onConfirm(ScanQuad.fromList(points)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text(strings.scanNext)
                }
            }
        }
    }
}
