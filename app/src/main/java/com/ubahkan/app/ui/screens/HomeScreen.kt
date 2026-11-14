package com.ubahkan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubahkan.app.UbahkanEngine
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.ConversionQueueManager
import com.ubahkan.app.core.JobStatus
import com.ubahkan.app.core.ToolOperation
import com.ubahkan.app.ui.components.CategoryFilterItem
import com.ubahkan.app.ui.components.ConversionBanner
import com.ubahkan.app.ui.components.ErrorCard
import com.ubahkan.app.ui.components.StaggerReveal
import com.ubahkan.app.ui.components.ToolGridCard
import com.ubahkan.app.ui.components.UbahkanSectionLabel
import com.ubahkan.app.ui.components.UbahkanTextTabs
import com.ubahkan.app.ui.components.rememberPressScale
import kotlinx.coroutines.delay

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// HOME SCREEN  -  BERANDA sesuai mockup UI 1.3.0
// Hero ringan, tombol Pilih File hitam, chip kategori,
// lalu grid Alat Populer. Riwayat berkas pindah ke tab Riwayat.
// ============================================================

@Composable
fun HomeScreen(
    hasNotificationPermission: Boolean,
    onPickFileClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onToolClick: (ToolOperation, String) -> Unit,
    onRetryConversion: (android.net.Uri, String) -> Unit,
    onBannerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    val engineState by UbahkanEngine.currentStatus.collectAsState()
    val queueState by ConversionQueueManager.queueState.collectAsState()

    val activeJob = remember(queueState) { queueState.firstOrNull { it.status == JobStatus.PROCESSING } }
    val failedJobs = remember(queueState) { queueState.filter { it.status == JobStatus.FAILED } }
    val totalQueueCount = remember(queueState) { queueState.size }

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val prefs = remember { com.ubahkan.app.UbahkanPrefs(context) }

    var batteryUnrestricted by remember {
        mutableStateOf(com.ubahkan.app.checkBatteryOptimizationDisabled(context))
    }
    var batteryBannerDismissed by remember { mutableStateOf(prefs.dismissBatteryWarning) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                batteryUnrestricted = com.ubahkan.app.checkBatteryOptimizationDisabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Reset status error engine sesaat sesudah tampil
    LaunchedEffect(engineState) {
        if (engineState is UbahkanEngine.ConvertState.Success) {
            delay(1500)
            UbahkanEngine.currentStatus.value = UbahkanEngine.ConvertState.Idle
        }
    }

    // --- FILTER ALAT POPULER ---
    var selectedToolCategory by remember { mutableStateOf("all") }
    val toolCategories = listOf(
        CategoryFilterItem("all", strings.categoryAll),
        CategoryFilterItem("video", strings.categoryVideo),
        CategoryFilterItem("audio", strings.categoryAudio),
        CategoryFilterItem("image", strings.categoryImage),
        CategoryFilterItem("document", strings.categoryDocument)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 0. HERO MINIMALIS (sesuai mockup) ---
        item {
            StaggerReveal(index = 0) {
                HomeHero(onPickFileClick = onPickFileClick)
            }
        }

        // --- 1. CONVERSION PROGRESS BANNER (jika ada job aktif) ---
        if (activeJob != null) {
            val currentJobNumber = queueState.indexOfFirst { it.id == activeJob.id } + 1
            val queueInfo = if (totalQueueCount > 1) {
                strings.convertingBatchInfo(currentJobNumber, totalQueueCount)
            } else {
                strings.convertingTo(activeJob.targetFormat)
            }

            item {
                ConversionBanner(
                    format = activeJob.targetFormat,
                    progress = activeJob.progress,
                    fileName = activeJob.originalFileName,
                    queueInfo = queueInfo,
                    stepMessage = activeJob.progressMessage,
                    onCancel = { ConversionQueueManager.cancelAllJobs() },
                    onClick = onBannerClick
                )
            }
        }

        // --- 2. ERROR BANNER ---
        if (failedJobs.isNotEmpty()) {
            failedJobs.forEach { failedJob ->
                item(key = "failed_${failedJob.id}") {
                    ErrorCard(
                        message = "${failedJob.originalFileName}: ${failedJob.errorMessage ?: strings.failedToConvertDefault}",
                        onDismiss = { ConversionQueueManager.dismissJob(failedJob.id) },
                        onRetry = {
                            ConversionQueueManager.dismissJob(failedJob.id)
                            onRetryConversion(failedJob.inputUri, failedJob.targetFormat)
                        }
                    )
                }
            }
        } else if (engineState is UbahkanEngine.ConvertState.Error) {
            val error = engineState as UbahkanEngine.ConvertState.Error
            item {
                ErrorCard(
                    message = error.message,
                    onDismiss = { UbahkanEngine.currentStatus.value = UbahkanEngine.ConvertState.Idle },
                    onRetry = if (error.originalUri != null && error.targetFormat.isNotEmpty()) {
                        {
                            UbahkanEngine.currentStatus.value = UbahkanEngine.ConvertState.Idle
                            onRetryConversion(error.originalUri, error.targetFormat)
                        }
                    } else null
                )
            }
        }

        // --- 3. NOTIFICATION PERMISSION PROMPT ---
        if (!hasNotificationPermission) {
            item {
                NotificationPermissionBanner(onRequestPermission = onRequestPermission)
            }
        }

        if (!batteryUnrestricted && !batteryBannerDismissed) {
            item {
                // Banner baterai gaya modern: kartu putih melayang, ikon di
                // ubin biru pucat, tombol Lihat Cara hitam kecil.
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BatteryAlert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(11.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.batteryBannerTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.batteryBannerDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    batteryBannerDismissed = true
                                    prefs.dismissBatteryWarning = true
                                }
                            ) {
                                Text(
                                    strings.btnLater,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showBatteryGuideDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = MaterialTheme.colorScheme.background
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                            ) {
                                Text(strings.btnLearnHow, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. TAB KATEGORI + ALAT POPULER ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StaggerReveal(index = 1) {
                    UbahkanTextTabs(
                        categories = toolCategories,
                        selectedCategoryId = selectedToolCategory,
                        onCategorySelected = { selectedToolCategory = it }
                    )
                }
                UbahkanSectionLabel(
                    text = strings.homePopularHeader,
                    modifier = Modifier.padding(top = 4.dp)
                )
                PopularToolsGrid(
                    selectedCategoryId = selectedToolCategory,
                    onToolClick = onToolClick
                )
            }
        }
    }

    // Battery Dialog
    if (showBatteryGuideDialog) {
        BatteryOptimizationGuideDialog(
            onDismiss = { showBatteryGuideDialog = false },
            onOpenSettings = {
                showBatteryGuideDialog = false
                com.ubahkan.app.openAppBatterySettings(context, strings)
            }
        )
    }
}

// ============================================================
// HERO BERANDA  -  judul besar ringan dengan kata aksen biru
// dan tombol Pilih File hitam selebar layar (mockup layar 1)
// ============================================================

@Composable
private fun HomeHero(
    onPickFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val accentColor = MaterialTheme.colorScheme.primary

    // Kata "internet" pada judul diberi warna aksen, sisanya tinta.
    val heroTitle = remember(strings.homeHeroTitle, accentColor) {
        val raw = strings.homeHeroTitle
        val accentWord = "internet"
        val start = raw.indexOf(accentWord, ignoreCase = true)
        if (start >= 0) {
            buildAnnotatedString {
                append(raw.substring(0, start))
                withStyle(SpanStyle(color = accentColor)) {
                    append(raw.substring(start, start + accentWord.length))
                }
                append(raw.substring(start + accentWord.length))
            }
        } else {
            AnnotatedString(raw)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = heroTitle,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = strings.homeHeroSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(26.dp))

        val (pressSource, pressModifier) = rememberPressScale()
        Button(
            onClick = onPickFileClick,
            interactionSource = pressSource,
            modifier = pressModifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = strings.selectFile,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

// ============================================================
// GRID ALAT POPULER  -  kartu putih bergaris, ikon biru di
// kotak biru muda. Kategori "Semua" menampilkan empat alat
// teratas; kategori lain menyaring dari daftar yang sama.
// ============================================================

private data class PopularTool(
    val operation: ToolOperation,
    val mimeType: String,
    val categoryId: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
private fun PopularToolsGrid(
    selectedCategoryId: String,
    onToolClick: (ToolOperation, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val popularTools = listOf(
        PopularTool(ToolOperation.COMPRESS_VIDEO, "video/*", "video", strings.toolCompressVideoTitle, strings.toolCompressVideoDesc, Icons.Outlined.Compress),
        PopularTool(ToolOperation.AUTO_SPLIT, "video/*", "video", strings.toolSplitTitle, strings.toolSplitDesc, Icons.Outlined.CallSplit),
        PopularTool(ToolOperation.MERGE_VIDEO, "video/*", "video", strings.toolMergeVideoTitle, strings.toolMergeVideoDesc, Icons.Outlined.Layers),
        PopularTool(ToolOperation.COMPRESS_IMAGE, "image/*", "image", strings.toolCompressImageTitle, strings.toolCompressImageDesc, Icons.Outlined.PhotoSizeSelectLarge),
        PopularTool(ToolOperation.HEIC_TO_JPG, "image/*", "image", strings.toolHeicTitle, strings.toolHeicDesc, Icons.Outlined.Image),
        PopularTool(ToolOperation.NORMALIZE_VOLUME, "*/*", "audio", strings.toolNormalizeTitle, strings.toolNormalizeDesc, Icons.Outlined.GraphicEq),
        PopularTool(ToolOperation.SPLIT_PDF, "application/pdf", "document", strings.toolSplitPdfTitle, strings.toolSplitPdfDesc, Icons.Outlined.ContentCopy),
        PopularTool(ToolOperation.MERGE_PDF, "application/pdf", "document", strings.toolMergePdfTitle, strings.toolMergePdfDesc, Icons.Outlined.PictureAsPdf)
    )
    val visibleTools = if (selectedCategoryId == "all") {
        popularTools.take(4)
    } else {
        popularTools.filter { it.categoryId == selectedCategoryId }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        visibleTools.chunked(2).forEachIndexed { rowIndex, rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTools.forEachIndexed { colIndex, tool ->
                    key(tool.operation) {
                        StaggerReveal(
                            index = 2 + rowIndex * 2 + colIndex,
                            modifier = Modifier.weight(1f)
                        ) {
                            ToolGridCard(
                                title = tool.title,
                                description = tool.description,
                                icon = tool.icon,
                                onClick = { onToolClick(tool.operation, tool.mimeType) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                if (rowTools.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ============================================================
// NOTIFICATION PERMISSION BANNER
// ============================================================

@Composable
fun NotificationPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.notifBannerTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = strings.notifBannerDesc,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onRequestPermission) {
                Text(
                    text = strings.btnAllow,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun BatteryOptimizationGuideDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = AppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = strings.batteryDialogTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.batteryDialogIntro,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.batteryStep1,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = strings.batteryStep2,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = strings.batteryStep3,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = strings.batteryNotes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.btnClose)
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(strings.btnOpenAppInfo)
            }
        }
    )
}
