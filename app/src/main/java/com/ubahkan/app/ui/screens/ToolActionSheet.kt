package com.ubahkan.app.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arthenica.ffmpegkit.FFprobeKit
import com.ubahkan.app.UbahkanService
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.ConversionQueueManager
import com.ubahkan.app.core.FileCategory
import com.ubahkan.app.core.FileCategoryManager
import com.ubahkan.app.core.OutputManager
import com.ubahkan.app.core.SelectedFileItem
import com.ubahkan.app.core.ToolOperation
import com.ubahkan.app.ui.components.UbahkanSectionLabel
import com.ubahkan.app.ui.components.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Locale

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// Sheet konfigurasi alat Batch 1: kompres target, potong, potong otomatis.
// Memilih opsi lalu menekan Mulai akan memasukkan job ke antrean yang sama
// dengan konversi biasa (ConversionQueueManager + UbahkanService).

// Parse "mm:ss", "hh:mm:ss", atau detik polos menjadi detik. Null bila tidak valid.
private fun parseTimeToSeconds(raw: String): Double? {
    val text = raw.trim().replace(',', '.')
    if (text.isEmpty()) return null
    if (text.contains(':')) {
        val parts = text.split(':')
        if (parts.size !in 2..3) return null
        var total = 0.0
        for ((index, part) in parts.withIndex()) {
            val value = part.trim().toDoubleOrNull() ?: return null
            if (value < 0) return null
            // Bagian menit dan detik (sesudah jam) tidak boleh 60 atau lebih
            if (index > 0 && value >= 60.0) return null
            total = total * 60 + value
        }
        return total
    }
    return text.toDoubleOrNull()?.takeIf { it >= 0.0 }
}

private fun formatSecondsClock(totalSeconds: Double): String {
    val total = totalSeconds.toInt()
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

// Probe durasi berkas dari Uri lewat salinan temp, karena FFprobeKit butuh path berkas
private fun probeUriDurationSeconds(context: Context, file: SelectedFileItem): Double? {
    val ext = file.fileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
    val temp = OutputManager.createTempFile(context, "probe", ext)
    return try {
        val copied = context.contentResolver.openInputStream(file.uri)?.use { input ->
            FileOutputStream(temp).use { output -> input.copyTo(output) }
            true
        } ?: false
        if (!copied || temp.length() <= 0) {
            null
        } else {
            FFprobeKit.getMediaInformation(temp.absolutePath).mediaInformation?.duration?.toDoubleOrNull()
        }
    } catch (e: Exception) {
        null
    } finally {
        OutputManager.cleanTempFiles(temp)
    }
}

private fun startToolJob(
    context: Context,
    file: SelectedFileItem,
    targetFormat: String,
    operation: ToolOperation,
    targetSizeBytes: Long = 0L,
    trimStartSec: Double = 0.0,
    trimEndSec: Double = 0.0,
    splitSeconds: Int = 0
) {
    ConversionQueueManager.addJob(
        inputUri = file.uri,
        originalFileName = file.fileName,
        mimeType = file.mimeType,
        targetFormat = targetFormat,
        qualityPreset = 0,
        operation = operation,
        targetSizeBytes = targetSizeBytes,
        trimStartSec = trimStartSec,
        trimEndSec = trimEndSec,
        splitSeconds = splitSeconds
    )
    val serviceIntent = Intent(context, UbahkanService::class.java).apply {
        action = UbahkanService.ACTION_START_QUEUE
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
    } else {
        context.startService(serviceIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolActionSheet(
    file: SelectedFileItem,
    operation: ToolOperation,
    onDismiss: () -> Unit,
    onStarted: () -> Unit
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val ext = remember(file) { file.fileName.substringAfterLast('.', "").trim().lowercase() }
    val category = remember(file) { FileCategoryManager.getCategory(ext, file.mimeType) }
    val isVideo = category == FileCategory.VIDEO
    val isImage = category == FileCategory.IMAGE

    val title = when (operation) {
        ToolOperation.COMPRESS_VIDEO -> strings.toolCompressVideoTitle
        ToolOperation.COMPRESS_IMAGE -> strings.toolCompressImageTitle
        ToolOperation.TRIM_VIDEO -> strings.toolTrimTitle
        ToolOperation.AUTO_SPLIT -> strings.toolSplitTitle
        ToolOperation.MERGE_VIDEO -> strings.toolMergeVideoTitle
        ToolOperation.SPLIT_PDF -> strings.toolSplitPdfTitle
        ToolOperation.MERGE_PDF -> strings.toolMergePdfTitle
        ToolOperation.HARDSUB -> strings.toolHardsubTitle
        ToolOperation.CROP_VIDEO -> strings.toolCropTitle
        ToolOperation.RESIZE_VIDEO -> strings.toolResizeTitle
        ToolOperation.MUTE_VIDEO -> strings.toolMuteTitle
        ToolOperation.REPLACE_AUDIO -> strings.toolReplaceAudioTitle
        ToolOperation.HEIC_TO_JPG -> strings.toolHeicTitle
        ToolOperation.FILE_INFO -> strings.toolInfoTitle
        ToolOperation.NORMALIZE_VOLUME -> strings.toolNormalizeTitle
        ToolOperation.CONVERT -> strings.bottomSheetTitle
    }
    val icon: ImageVector = when (operation) {
        ToolOperation.COMPRESS_VIDEO -> Icons.Outlined.Compress
        ToolOperation.COMPRESS_IMAGE -> Icons.Outlined.PhotoSizeSelectLarge
        ToolOperation.TRIM_VIDEO -> Icons.Outlined.ContentCut
        ToolOperation.AUTO_SPLIT -> Icons.Outlined.CallSplit
        ToolOperation.MERGE_VIDEO -> Icons.Outlined.MergeType
        ToolOperation.SPLIT_PDF -> Icons.Outlined.ContentCopy
        ToolOperation.MERGE_PDF -> Icons.Outlined.PictureAsPdf
        ToolOperation.HARDSUB -> Icons.Outlined.Subtitles
        ToolOperation.CROP_VIDEO -> Icons.Outlined.Crop
        ToolOperation.RESIZE_VIDEO -> Icons.Outlined.AspectRatio
        ToolOperation.MUTE_VIDEO -> Icons.Outlined.VolumeOff
        ToolOperation.REPLACE_AUDIO -> Icons.Outlined.Audiotrack
        ToolOperation.HEIC_TO_JPG -> Icons.Outlined.Image
        ToolOperation.FILE_INFO -> Icons.Default.Info
        ToolOperation.NORMALIZE_VOLUME -> Icons.Outlined.GraphicEq
        ToolOperation.CONVERT -> Icons.Outlined.SwapHoriz
    }

    // State konfigurasi per operasi
    var videoPresetMb by remember { mutableStateOf(10) }
    var videoCustomMb by remember { mutableStateOf("") }
    var imagePresetKb by remember { mutableStateOf(200) }
    var imageCustomKb by remember { mutableStateOf("") }
    var trimStartText by remember { mutableStateOf("") }
    var trimEndText by remember { mutableStateOf("") }
    var splitSec by remember { mutableStateOf(30) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var durationSec by remember { mutableStateOf<Double?>(null) }

    // Durasi sumber untuk info dan validasi trim
    LaunchedEffect(file.uri, operation) {
        if (operation == ToolOperation.TRIM_VIDEO || operation == ToolOperation.COMPRESS_VIDEO) {
            durationSec = withContext(Dispatchers.IO) {
                probeUriDurationSeconds(context, file)
            }
        }
    }

    val guardError = when (operation) {
        ToolOperation.COMPRESS_IMAGE -> if (!isImage) strings.toolErrNotImage else null
        ToolOperation.COMPRESS_VIDEO, ToolOperation.TRIM_VIDEO, ToolOperation.AUTO_SPLIT,
        ToolOperation.MERGE_VIDEO ->
            if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.SPLIT_PDF ->
            if (category != FileCategory.PDF) strings.toolErrNotPdf else null
        ToolOperation.MERGE_PDF ->
            if (category != FileCategory.PDF) strings.toolErrNotPdf else null
        ToolOperation.HARDSUB ->
            if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.CROP_VIDEO ->
            if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.RESIZE_VIDEO ->
            if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.MUTE_VIDEO ->
            if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.REPLACE_AUDIO ->
            if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.HEIC_TO_JPG -> null
        ToolOperation.FILE_INFO -> null
        ToolOperation.NORMALIZE_VOLUME -> null
        ToolOperation.CONVERT -> null
    }
    val canStart = guardError == null

    val handleStart: () -> Unit = {
        errorText = null
        when (operation) {
            ToolOperation.COMPRESS_VIDEO -> {
                val mb = if (videoCustomMb.isNotBlank()) {
                    videoCustomMb.replace(',', '.').toDoubleOrNull()
                } else {
                    videoPresetMb.toDouble()
                }
                if (mb == null || mb <= 0.0) {
                    errorText = strings.toolErrCustomSize
                } else {
                    startToolJob(
                        context, file, "mp4", ToolOperation.COMPRESS_VIDEO,
                        targetSizeBytes = (mb * 1024 * 1024).toLong()
                    )
                    onStarted()
                }
            }
            ToolOperation.COMPRESS_IMAGE -> {
                val kb = if (imageCustomKb.isNotBlank()) {
                    imageCustomKb.replace(',', '.').toDoubleOrNull()
                } else {
                    imagePresetKb.toDouble()
                }
                if (kb == null || kb <= 0.0) {
                    errorText = strings.toolErrCustomSize
                } else {
                    startToolJob(
                        context, file, "jpg", ToolOperation.COMPRESS_IMAGE,
                        targetSizeBytes = (kb * 1024).toLong()
                    )
                    onStarted()
                }
            }
            ToolOperation.TRIM_VIDEO -> {
                val start = parseTimeToSeconds(trimStartText)
                val end = parseTimeToSeconds(trimEndText)
                val duration = durationSec
                when {
                    start == null || end == null -> errorText = strings.toolErrTimeFormat
                    duration == null -> errorText = strings.engineErrNoDuration
                    end <= start || end > duration -> errorText = strings.toolErrTrimRange
                    else -> {
                        startToolJob(
                            context, file, ext.ifEmpty { "mp4" }, ToolOperation.TRIM_VIDEO,
                            trimStartSec = start, trimEndSec = end
                        )
                        onStarted()
                    }
                }
            }
            ToolOperation.AUTO_SPLIT -> {
                startToolJob(
                    context, file, ext.ifEmpty { "mp4" }, ToolOperation.AUTO_SPLIT,
                    splitSeconds = splitSec
                )
                onStarted()
            }
            ToolOperation.CONVERT -> Unit
            // Operasi Batch 2 ditangani ToolActionSheet2 dan FileInfoSheet, bukan sheet ini
            ToolOperation.MERGE_VIDEO, ToolOperation.SPLIT_PDF, ToolOperation.MERGE_PDF,
            ToolOperation.HARDSUB, ToolOperation.CROP_VIDEO, ToolOperation.RESIZE_VIDEO,
            ToolOperation.MUTE_VIDEO, ToolOperation.REPLACE_AUDIO, ToolOperation.HEIC_TO_JPG,
            ToolOperation.FILE_INFO, ToolOperation.NORMALIZE_VOLUME -> Unit
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 4.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: ikon + judul alat + nama berkas + tombol tutup
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val sizeText = if (file.sizeBytes > 0) " - ${formatFileSize(file.sizeBytes)}" else ""
                    Text(
                        text = "${file.fileName}$sizeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    modifier = Modifier.size(34.dp).clickable { onDismiss() },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            when (operation) {
                ToolOperation.COMPRESS_VIDEO -> {
                    UbahkanSectionLabel(text = strings.toolTargetSizeLabel)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 25).forEach { mb ->
                            ToolOptionChip(
                                label = "$mb MB",
                                selected = videoCustomMb.isBlank() && videoPresetMb == mb,
                                onClick = {
                                    videoPresetMb = mb
                                    videoCustomMb = ""
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = videoCustomMb,
                        onValueChange = { videoCustomMb = it },
                        label = { Text(strings.toolCustomSizeMbLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    durationSec?.let { dur ->
                        Text(
                            text = strings.toolVideoDurationInfo(formatSecondsClock(dur)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ToolOperation.COMPRESS_IMAGE -> {
                    UbahkanSectionLabel(text = strings.toolTargetSizeLabel)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(100, 200, 500).forEach { kb ->
                            ToolOptionChip(
                                label = "$kb KB",
                                selected = imageCustomKb.isBlank() && imagePresetKb == kb,
                                onClick = {
                                    imagePresetKb = kb
                                    imageCustomKb = ""
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = imageCustomKb,
                        onValueChange = { imageCustomKb = it },
                        label = { Text(strings.toolCustomSizeKbLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ToolOperation.TRIM_VIDEO -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = trimStartText,
                            onValueChange = { trimStartText = it },
                            label = { Text(strings.toolTrimStartLabel) },
                            placeholder = { Text("0:00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = trimEndText,
                            onValueChange = { trimEndText = it },
                            label = { Text(strings.toolTrimEndLabel) },
                            placeholder = { Text(durationSec?.let { formatSecondsClock(it) } ?: "0:30") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = strings.toolTrimTimeHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    durationSec?.let { dur ->
                        Text(
                            text = strings.toolVideoDurationInfo(formatSecondsClock(dur)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ToolOperation.AUTO_SPLIT -> {
                    UbahkanSectionLabel(text = strings.toolSplitEveryLabel)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30, 60, 90).forEach { sec ->
                            ToolOptionChip(
                                label = strings.toolSecondsUnit(sec),
                                selected = splitSec == sec,
                                onClick = { splitSec = sec }
                            )
                        }
                    }
                }

                ToolOperation.CONVERT -> Unit
                // Operasi Batch 2 ditangani ToolActionSheet2 dan FileInfoSheet, bukan sheet ini
                ToolOperation.MERGE_VIDEO, ToolOperation.SPLIT_PDF, ToolOperation.MERGE_PDF,
                ToolOperation.HARDSUB, ToolOperation.CROP_VIDEO, ToolOperation.RESIZE_VIDEO,
                ToolOperation.MUTE_VIDEO, ToolOperation.REPLACE_AUDIO, ToolOperation.HEIC_TO_JPG,
                ToolOperation.FILE_INFO, ToolOperation.NORMALIZE_VOLUME -> Unit
            }

            if (guardError != null) {
                Text(
                    text = guardError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (errorText != null) {
                Text(
                    text = errorText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = handleStart,
                enabled = canStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                    disabledContentColor = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(text = strings.toolStartButton, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ToolOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Medium,
                fontSize = 12.sp
            )
        },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.onBackground,
            selectedLabelColor = MaterialTheme.colorScheme.background
        ),
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    )
}
