package com.ubahkan.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubahkan.app.UbahkanService
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.ConversionCapabilityManager
import com.ubahkan.app.core.ConversionJobRequest
import com.ubahkan.app.core.ConversionQueueManager
import com.ubahkan.app.core.SelectedFileItem
import com.ubahkan.app.ui.components.UbahkanSectionLabel
import com.ubahkan.app.ui.components.formatFileSize
import com.ubahkan.app.ui.theme.UbahkanInstantBolt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SwapHoriz

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BottomSheetConverter(
    selectedFiles: List<SelectedFileItem>,
    defaultQuality: Int,
    onDismiss: () -> Unit,
    onConvertStarted: () -> Unit
) {
    if (selectedFiles.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val isBatchMode = selectedFiles.size > 1
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val strings = AppStrings.current

    val totalSize = remember(selectedFiles) { selectedFiles.sumOf { it.sizeBytes } }

    // Kelompokkan berkas berdasarkan format sumber
    val groupedFiles = remember(selectedFiles) {
        selectedFiles.groupBy { file ->
            file.fileName.substringAfterLast('.', "lain").trim().lowercase()
        }
    }

    // Format target per kelompok (key: format sumber, value: target format)
    val targetFormatPerGroup = remember(groupedFiles) {
        mutableStateMapOf<String, String>().apply {
            groupedFiles.forEach { (srcExt, files) ->
                val sampleFile = files.first()
                val availableTargets = ConversionCapabilityManager.getTargetFormats(sampleFile.fileName, sampleFile.mimeType)
                val defaultTarget = availableTargets.firstOrNull { it != srcExt } ?: availableTargets.firstOrNull() ?: ""
                put(srcExt, defaultTarget)
            }
        }
    }

    // Kualitas konversi per kelompok (key: format sumber, value: preset kualitas)
    val qualityPerGroup = remember(groupedFiles) {
        mutableStateMapOf<String, Int>().apply {
            groupedFiles.keys.forEach { srcExt ->
                put(srcExt, defaultQuality)
            }
        }
    }

    // Pilihan Gabung/Pisahkan PDF per grup gambar
    val pdfMergeOptionPerGroup = remember(groupedFiles) {
        mutableStateMapOf<String, Boolean>().apply {
            groupedFiles.keys.forEach { srcExt ->
                put(srcExt, true) // default: gabung satu PDF
            }
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
            val subtitleText = if (isBatchMode) {
                val sizeText = if (totalSize > 0) " • ${formatFileSize(totalSize)}" else ""
                strings.bottomSheetBatchSubtitle(selectedFiles.size, groupedFiles.size, sizeText)
            } else {
                val single = selectedFiles.first()
                val sizeText = if (single.sizeBytes > 0) " • ${formatFileSize(single.sizeBytes)}" else ""
                strings.bottomSheetSingleSubtitle(single.fileName, sizeText)
            }

            // Header
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
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        strings.bottomSheetTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitleText,
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

            UbahkanSectionLabel(text = strings.targetFormatHeader)

            // Konfigurasi per kelompok format
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedFiles.forEach { (srcExt, filesInGroup) ->
                    val sampleFile = filesInGroup.first()
                    val availableTargets = remember(srcExt) {
                        ConversionCapabilityManager.getTargetFormats(sampleFile.fileName, sampleFile.mimeType)
                    }
                    val currentTarget = targetFormatPerGroup[srcExt] ?: ""
                    val isCurrentTargetLossless = remember(srcExt, currentTarget) {
                        ConversionCapabilityManager.isLosslessCompatible(srcExt, currentTarget, sampleFile.mimeType)
                    }
                    val isImageSource = srcExt in listOf("png", "jpg", "jpeg", "webp", "bmp", "gif")
                    val categoryLabel = if (isImageSource) strings.categoryImage else strings.categoryDocument
                    val categoryIcon = if (isImageSource) Icons.Default.Image else Icons.Default.Description

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Category header — tile netral + ikon biru (gaya mockup)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = RoundedCornerShape(11.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = categoryIcon,
                                            contentDescription = categoryLabel,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(categoryLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${srcExt.uppercase()} • ${filesInGroup.size} file${if (filesInGroup.size > 1) strings.filesCountSuffix else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                ) {
                                    Text(
                                        currentTarget.uppercase().ifBlank { strings.chooseFormat },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Target format chips
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableTargets.forEach { targetOption ->
                                    val isSelected = currentTarget.equals(targetOption, ignoreCase = true)
                                    val supportsInstant = ConversionCapabilityManager.isLosslessCompatible(srcExt, targetOption, sampleFile.mimeType)

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { targetFormatPerGroup[srcExt] = targetOption },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    targetOption.uppercase(),
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp
                                                )
                                                if (supportsInstant) Text("⚡", color = UbahkanInstantBolt, fontSize = 11.sp)
                                            }
                                        },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                                            selectedLabelColor = MaterialTheme.colorScheme.background
                                        ),
                                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    )
                                }
                            }

                            // Opsi khusus PNG/Gambar ke PDF: Gabung atau Pisah
                            val isImageToPdf = srcExt in listOf("png", "jpg", "jpeg", "webp") && currentTarget.equals("pdf", ignoreCase = true)
                            if (isImageToPdf && filesInGroup.size > 1) {
                                val isMerge = pdfMergeOptionPerGroup[srcExt] ?: true
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(13.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 11.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Text(strings.mergeToPdfTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                            Text(strings.mergeToPdfDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = isMerge,
                                            onCheckedChange = { pdfMergeOptionPerGroup[srcExt] = it }
                                        )
                                    }
                                }
                            }

                            // Kualitas konversi
                            val currentQuality = qualityPerGroup[srcExt] ?: defaultQuality
                            val qualityOptions = if (isCurrentTargetLossless) {
                                listOf(Pair(strings.qualityLow, 2), Pair(strings.qualityMedium, 1), Pair(strings.qualityHigh, 0), Pair(strings.qualityInstant, 3))
                            } else {
                                listOf(Pair(strings.qualityLow, 2), Pair(strings.qualityMedium, 1), Pair(strings.qualityHigh, 0))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                UbahkanSectionLabel(text = strings.qualityTitle)
                                // Segmented control sesuai mockup: rel redup, segmen aktif putih
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(13.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        qualityOptions.forEach { (label, qIdx) ->
                                            val isSelected = currentQuality == qIdx
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                                                    )
                                                    .clickable { qualityPerGroup[srcExt] = qIdx },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 3.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (qIdx == 3) Text("⚡", color = UbahkanInstantBolt, fontSize = 11.sp)
                                                    Text(
                                                        label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Info Lossless/Instant
                            if (isCurrentTargetLossless) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("⚡", color = UbahkanInstantBolt, fontSize = 15.sp)
                                        Text(
                                            strings.instantLosslessSupported(srcExt, currentTarget),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (!isCurrentTargetLossless && currentQuality == 3) {
                                Text(
                                    strings.instantNotAvailable,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Validasi & CTA
            val allGroupsConfigured = groupedFiles.keys.all { key ->
                !targetFormatPerGroup[key].isNullOrBlank()
            }

            Button(
                onClick = {
                    val requests = mutableListOf<ConversionJobRequest>()

                    groupedFiles.forEach { (srcExt, filesInGroup) ->
                        val targetFormat = targetFormatPerGroup[srcExt] ?: "pdf"
                        val groupQuality = qualityPerGroup[srcExt] ?: defaultQuality
                        val isLossless = ConversionCapabilityManager.isLosslessCompatible(srcExt, targetFormat, filesInGroup.first().mimeType)
                        val effectiveQuality = if (groupQuality == 3 && !isLossless) 0 else groupQuality

                        val isImageToPdf = srcExt in listOf("png", "jpg", "jpeg", "webp") && targetFormat.equals("pdf", ignoreCase = true)
                        val shouldMerge = isImageToPdf && filesInGroup.size > 1 && (pdfMergeOptionPerGroup[srcExt] ?: true)

                        if (shouldMerge) {
                            val first = filesInGroup.first()
                            val extraUris = filesInGroup.drop(1).map { it.uri }
                            val baseName = first.fileName.substringBeforeLast('.', "ubahkan_merged")
                            requests.add(
                                ConversionJobRequest(
                                    inputUri = first.uri,
                                    originalFileName = "$baseName.pdf",
                                    mimeType = "application/pdf",
                                    targetFormat = "pdf",
                                    qualityPreset = effectiveQuality,
                                    extraInputUris = extraUris
                                )
                            )
                        } else {
                            filesInGroup.forEach { file ->
                                requests.add(
                                    ConversionJobRequest(
                                        inputUri = file.uri,
                                        originalFileName = file.fileName,
                                        mimeType = file.mimeType,
                                        targetFormat = targetFormat,
                                        qualityPreset = effectiveQuality
                                    )
                                )
                            }
                        }
                    }

                    ConversionQueueManager.addJobs(requests)

                    val serviceIntent = Intent(context, UbahkanService::class.java).apply {
                        action = UbahkanService.ACTION_START_QUEUE
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }

                    onConvertStarted()
                },
                enabled = allGroupsConfigured,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp
                )
            ) {
                val isMergeActive = groupedFiles.any { (srcExt, files) ->
                    srcExt in listOf("png", "jpg", "jpeg", "webp") &&
                    (targetFormatPerGroup[srcExt] ?: "").equals("pdf", ignoreCase = true) &&
                    files.size > 1 &&
                    (pdfMergeOptionPerGroup[srcExt] ?: true)
                }
                val label = when {
                    !isBatchMode -> strings.btnConvert
                    isMergeActive -> strings.btnMergeAndConvert(selectedFiles.size)
                    else -> strings.btnConvertBatch(selectedFiles.size)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("▶", fontSize = 15.sp)
                    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// Overload untuk kompatibilitas file tunggal
@Composable
fun BottomSheetConverter(
    fileUri: Uri,
    fileName: String,
    mimeType: String,
    defaultQuality: Int,
    onDismiss: () -> Unit,
    onConvertStarted: () -> Unit
) {
    BottomSheetConverter(
        selectedFiles = listOf(SelectedFileItem(fileUri, fileName, mimeType)),
        defaultQuality = defaultQuality,
        onDismiss = onDismiss,
        onConvertStarted = onConvertStarted
    )
}
