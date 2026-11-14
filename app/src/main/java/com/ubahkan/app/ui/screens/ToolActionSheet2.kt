package com.ubahkan.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubahkan.app.UbahkanService
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.getExplicitMimeType
import com.ubahkan.app.getFileNameFromUri
import com.ubahkan.app.getFileSizeFromUri
import com.ubahkan.app.core.ConversionQueueManager
import com.ubahkan.app.core.FileCategory
import com.ubahkan.app.core.FileCategoryManager
import com.ubahkan.app.core.SelectedFileItem
import com.ubahkan.app.core.ToolOperation
import com.ubahkan.app.ui.components.UbahkanSectionLabel
import com.ubahkan.app.ui.components.formatFileSize

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// Sheet konfigurasi alat Batch 2: gabung video dan alat lanjutan lain.
// Sama seperti Batch 1, menekan Mulai memasukkan job ke antrean yang sama
// (ConversionQueueManager + UbahkanService), hasil ke Downloads/Ubahkan.

private fun startToolJob2(
    context: Context,
    files: List<SelectedFileItem>,
    targetFormat: String,
    operation: ToolOperation,
    extraUrisOverride: List<Uri>? = null,
    pdfPageRange: String = "",
    cropRatio: String = "",
    resizeHeight: Int = 0
) {
    val first = files.first()
    ConversionQueueManager.addJob(
        inputUri = first.uri,
        originalFileName = first.fileName,
        mimeType = first.mimeType,
        targetFormat = targetFormat,
        qualityPreset = 0,
        extraInputUris = extraUrisOverride ?: files.drop(1).map { it.uri },
        operation = operation,
        pdfPageRange = pdfPageRange,
        cropRatio = cropRatio,
        resizeHeight = resizeHeight
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
fun ToolActionSheet2(
    files: List<SelectedFileItem>,
    operation: ToolOperation,
    onDismiss: () -> Unit,
    onStarted: () -> Unit
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val file = files.first()
    val ext = remember(file) { file.fileName.substringAfterLast('.', "").trim().lowercase() }
    val category = remember(file) { FileCategoryManager.getCategory(ext, file.mimeType) }
    val isVideo = category == FileCategory.VIDEO
    val isPdf = category == FileCategory.PDF
    val isHeic = ext in listOf("heic", "heif") ||
        file.mimeType.contains("heic", ignoreCase = true) ||
        file.mimeType.contains("heif", ignoreCase = true)

    val title = when (operation) {
        ToolOperation.MERGE_VIDEO -> strings.toolMergeVideoTitle
        ToolOperation.SPLIT_PDF -> strings.toolSplitPdfTitle
        ToolOperation.MERGE_PDF -> strings.toolMergePdfTitle
        ToolOperation.HARDSUB -> strings.toolHardsubTitle
        ToolOperation.CROP_VIDEO -> strings.toolCropTitle
        ToolOperation.RESIZE_VIDEO -> strings.toolResizeTitle
        ToolOperation.MUTE_VIDEO -> strings.toolMuteTitle
        ToolOperation.REPLACE_AUDIO -> strings.toolReplaceAudioTitle
        ToolOperation.HEIC_TO_JPG -> strings.toolHeicTitle
        ToolOperation.NORMALIZE_VOLUME -> strings.toolNormalizeTitle
        else -> strings.bottomSheetTitle
    }
    val icon: ImageVector = when (operation) {
        ToolOperation.MERGE_VIDEO -> Icons.Default.MergeType
        ToolOperation.SPLIT_PDF -> Icons.Default.ContentCopy
        ToolOperation.MERGE_PDF -> Icons.Default.PictureAsPdf
        ToolOperation.HARDSUB -> Icons.Default.Subtitles
        ToolOperation.CROP_VIDEO -> Icons.Default.Crop
        ToolOperation.RESIZE_VIDEO -> Icons.Default.AspectRatio
        ToolOperation.MUTE_VIDEO -> Icons.Default.VolumeOff
        ToolOperation.REPLACE_AUDIO -> Icons.Default.Audiotrack
        ToolOperation.HEIC_TO_JPG -> Icons.Default.Image
        ToolOperation.NORMALIZE_VOLUME -> Icons.Default.GraphicEq
        else -> Icons.Default.SwapHoriz
    }

    var errorText by remember { mutableStateOf<String?>(null) }
    var pdfRangeText by remember { mutableStateOf("") }
    var cropRatioSel by remember { mutableStateOf("9:16") }
    var resizeHeightSel by remember { mutableStateOf(720) }

    // Berkas pendamping untuk hardsub (.srt) dan ganti audio, dipilih dari dalam sheet
    var companion by remember { mutableStateOf<SelectedFileItem?>(null) }
    val companionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = getFileNameFromUri(context, uri)
            val mime = context.contentResolver.getType(uri)
                ?: getExplicitMimeType(name.substringAfterLast('.', ""))
            val size = getFileSizeFromUri(context, uri)
            companion = SelectedFileItem(uri, name, mime, size)
        }
    }

    val guardError = when (operation) {
        ToolOperation.MERGE_VIDEO -> when {
            !isVideo -> strings.toolErrNotVideo
            files.size < 2 -> strings.toolErrNeedTwoFiles
            else -> null
        }
        ToolOperation.SPLIT_PDF -> if (!isPdf) strings.toolErrNotPdf else null
        ToolOperation.MERGE_PDF -> when {
            !isPdf -> strings.toolErrNotPdf
            files.size < 2 -> strings.toolErrNeedTwoFiles
            else -> null
        }
        ToolOperation.HARDSUB -> if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.CROP_VIDEO -> if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.RESIZE_VIDEO -> if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.MUTE_VIDEO -> if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.REPLACE_AUDIO -> if (!isVideo) strings.toolErrNotVideo else null
        ToolOperation.HEIC_TO_JPG -> if (!isHeic) strings.toolErrNotHeic else null
        ToolOperation.NORMALIZE_VOLUME ->
            if (!isVideo && category != FileCategory.AUDIO) strings.toolErrNotMedia else null
        else -> null
    }
    val canStart = guardError == null

    val handleStart: () -> Unit = {
        errorText = null
        when (operation) {
            ToolOperation.MERGE_VIDEO -> {
                startToolJob2(
                    context, files,
                    ext.ifEmpty { "mp4" },
                    ToolOperation.MERGE_VIDEO
                )
                onStarted()
            }
            ToolOperation.SPLIT_PDF -> {
                startToolJob2(
                    context, files, "pdf",
                    ToolOperation.SPLIT_PDF,
                    pdfPageRange = pdfRangeText.trim()
                )
                onStarted()
            }
            ToolOperation.MERGE_PDF -> {
                startToolJob2(
                    context, files, "pdf",
                    ToolOperation.MERGE_PDF
                )
                onStarted()
            }
            ToolOperation.CROP_VIDEO -> {
                startToolJob2(
                    context, files,
                    ext.ifEmpty { "mp4" },
                    ToolOperation.CROP_VIDEO,
                    cropRatio = cropRatioSel
                )
                onStarted()
            }
            ToolOperation.RESIZE_VIDEO -> {
                startToolJob2(
                    context, files,
                    ext.ifEmpty { "mp4" },
                    ToolOperation.RESIZE_VIDEO,
                    resizeHeight = resizeHeightSel
                )
                onStarted()
            }
            ToolOperation.MUTE_VIDEO -> {
                startToolJob2(
                    context, files,
                    ext.ifEmpty { "mp4" },
                    ToolOperation.MUTE_VIDEO
                )
                onStarted()
            }
            ToolOperation.HEIC_TO_JPG -> {
                startToolJob2(
                    context, files, "jpg",
                    ToolOperation.HEIC_TO_JPG
                )
                onStarted()
            }
            ToolOperation.NORMALIZE_VOLUME -> {
                startToolJob2(
                    context, files,
                    ext.ifEmpty { "mp3" },
                    ToolOperation.NORMALIZE_VOLUME
                )
                onStarted()
            }
            ToolOperation.REPLACE_AUDIO -> {
                val comp = companion
                if (comp == null) {
                    errorText = strings.toolErrCompanionMissing
                } else {
                    startToolJob2(
                        context, files,
                        ext.ifEmpty { "mp4" },
                        ToolOperation.REPLACE_AUDIO,
                        extraUrisOverride = listOf(comp.uri)
                    )
                    onStarted()
                }
            }
            ToolOperation.HARDSUB -> {
                val comp = companion
                when {
                    comp == null -> errorText = strings.toolErrCompanionMissing
                    !comp.fileName.endsWith(".srt", ignoreCase = true) ->
                        errorText = strings.toolErrSubtitleExt
                    else -> {
                        startToolJob2(
                            context, files,
                            ext.ifEmpty { "mp4" },
                            ToolOperation.HARDSUB,
                            extraUrisOverride = listOf(comp.uri)
                        )
                        onStarted()
                    }
                }
            }
            else -> Unit
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
                        fontWeight = FontWeight.Bold,
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
                        Text("×", fontSize = 23.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            when (operation) {
                ToolOperation.MERGE_VIDEO, ToolOperation.MERGE_PDF -> {
                    Text(
                        text = strings.toolSelectedFilesInfo(files.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    files.forEachIndexed { index, item ->
                        Text(
                            text = "${index + 1}. ${item.fileName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                ToolOperation.SPLIT_PDF -> {
                    OutlinedTextField(
                        value = pdfRangeText,
                        onValueChange = { pdfRangeText = it },
                        label = { Text(strings.toolPdfRangeLabel) },
                        placeholder = { Text("1-3") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = strings.toolPdfRangeHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ToolOperation.CROP_VIDEO -> {
                    UbahkanSectionLabel(text = strings.toolCropRatioLabel)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("9:16", "1:1", "16:9").forEach { ratio ->
                            ToolOptionChip2(
                                label = ratio,
                                selected = cropRatioSel == ratio,
                                onClick = { cropRatioSel = ratio }
                            )
                        }
                    }
                }
                ToolOperation.RESIZE_VIDEO -> {
                    UbahkanSectionLabel(text = strings.toolResizeLabel)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1080, 720, 480, 360).forEach { height ->
                            ToolOptionChip2(
                                label = "${height}p",
                                selected = resizeHeightSel == height,
                                onClick = { resizeHeightSel = height }
                            )
                        }
                    }
                }
                ToolOperation.HARDSUB -> {
                    UbahkanSectionLabel(text = strings.toolSubtitleFileLabel)
                    OutlinedButton(
                        onClick = { companionLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = companion?.fileName ?: strings.toolPickCompanionButton,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                ToolOperation.REPLACE_AUDIO -> {
                    UbahkanSectionLabel(text = strings.toolAudioFileLabel)
                    OutlinedButton(
                        onClick = { companionLauncher.launch("audio/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = companion?.fileName ?: strings.toolPickCompanionButton,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                else -> Unit
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
                Text(text = strings.toolStartButton, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ToolOptionChip2(
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
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
