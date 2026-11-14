package com.ubahkan.app.ui.screens.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.Strings
import com.ubahkan.app.core.scan.ScanDetector
import com.ubahkan.app.core.scan.ScanFilter
import com.ubahkan.app.core.scan.ScanFilters
import com.ubahkan.app.core.scan.ScanImageUtils
import com.ubahkan.app.core.scan.ScanQuad
import com.ubahkan.app.ui.theme.UbahkanBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// TAHAP 3 PINDAI: FILTER
// Warp perspektif dihitung sekali, lalu strip 12 filter
// dengan pratinjau thumbnail langsung dari versi kecil
// hasil warp. Dua slider kecerahan dan kontras diterapkan
// di atas hasil filter (pratinjau lewat ColorMatrix, hasil
// akhir lewat OpenCV agar sama persis).
// ============================================================

@Composable
fun ScanFilterStage(
    bitmap: Bitmap,
    quad: ScanQuad,
    onUsePage: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val scope = rememberCoroutineScope()

    var warped by remember(bitmap, quad) { mutableStateOf<Bitmap?>(null) }
    var selectedFilter by remember { mutableStateOf(ScanFilter.ORIGINAL) }
    var filtered by remember { mutableStateOf<Bitmap?>(null) }
    var thumbnails by remember { mutableStateOf<Map<ScanFilter, Bitmap>>(emptyMap()) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var applying by remember { mutableStateOf(false) }

    LaunchedEffect(bitmap, quad) {
        warped = withContext(Dispatchers.IO) {
            runCatching { ScanDetector.warp(bitmap, quad) }.getOrNull()
        }
    }

    LaunchedEffect(warped) {
        val base = warped ?: return@LaunchedEffect
        thumbnails = withContext(Dispatchers.IO) {
            val small = ScanImageUtils.downscaleToWidth(base, 220)
            ScanFilter.entries.associateWith { filter ->
                runCatching { ScanFilters.apply(small, filter) }.getOrNull() ?: small
            }
        }
    }

    LaunchedEffect(warped, selectedFilter) {
        val base = warped ?: return@LaunchedEffect
        filtered = null
        filtered = withContext(Dispatchers.IO) {
            runCatching { ScanFilters.apply(base, selectedFilter) }.getOrNull() ?: base
        }
    }

    val previewBitmap = filtered ?: warped
    val previewFilter = if (brightness != 0f || contrast != 1f) {
        val c = contrast
        val offset = (128f * (1f - c) + brightness) / 255f
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, offset,
                    0f, c, 0f, 0f, offset,
                    0f, 0f, c, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    } else {
        null
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = previewFilter
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = UbahkanBlue)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.scanProcessing,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {
            items(ScanFilter.entries) { filter ->
                val isSelected = filter == selectedFilter
                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { selectedFilter = filter },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val thumb = thumbnails[filter]
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, UbahkanBlue, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                }
                            )
                    ) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = filter.label(strings),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = filter.label(strings),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            AdjustmentSlider(
                label = strings.scanBrightnessLabel,
                value = brightness,
                valueRange = -50f..50f,
                onValueChange = { brightness = it }
            )
            AdjustmentSlider(
                label = strings.scanContrastLabel,
                value = contrast,
                valueRange = 0.5f..1.5f,
                onValueChange = { contrast = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val base = filtered ?: warped ?: return@Button
                    if (applying) return@Button
                    applying = true
                    scope.launch {
                        val out = withContext(Dispatchers.IO) {
                            runCatching {
                                ScanFilters.applyBrightnessContrast(
                                    base, brightness.roundToInt(), contrast
                                )
                            }.getOrNull() ?: base
                        }
                        applying = false
                        onUsePage(out)
                    }
                },
                enabled = warped != null && !applying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text(strings.scanUsePage)
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = UbahkanBlue,
                activeTrackColor = UbahkanBlue,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

private fun ScanFilter.label(strings: Strings): String = when (this) {
    ScanFilter.ORIGINAL -> strings.scanFilterOriginal
    ScanFilter.GRAYSCALE -> strings.scanFilterGrayscale
    ScanFilter.BLACK_WHITE -> strings.scanFilterBlackWhite
    ScanFilter.WHITE_PAPER -> strings.scanFilterWhitePaper
    ScanFilter.SHADOW_REMOVE -> strings.scanFilterShadow
    ScanFilter.SHARPEN_TEXT -> strings.scanFilterSharpen
    ScanFilter.CONTRAST_BOOST -> strings.scanFilterContrastBoost
    ScanFilter.COLOR_POP -> strings.scanFilterColorPop
    ScanFilter.SEPIA -> strings.scanFilterSepia
    ScanFilter.NEGATIVE -> strings.scanFilterNegative
    ScanFilter.BLUEPRINT -> strings.scanFilterBlueprint
    ScanFilter.PHOTOCOPY -> strings.scanFilterPhotocopy
}
