package com.ubahkan.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubahkan.app.UbahkanEngine
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.ConversionQueueManager
import com.ubahkan.app.core.FileCategory
import com.ubahkan.app.core.FileCategoryManager
import com.ubahkan.app.core.JobStatus
import com.ubahkan.app.ui.components.CategoryFilterItem
import com.ubahkan.app.ui.components.FileHistoryItemCard
import com.ubahkan.app.ui.components.UbahkanLogo
import com.ubahkan.app.ui.components.UbahkanScreenHeader
import com.ubahkan.app.ui.components.UbahkanSectionLabel
import com.ubahkan.app.ui.components.UbahkanTextTabs
import com.ubahkan.app.ui.components.formatFileSize
import java.io.File

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// HISTORY SCREEN  -  TAB RIWAYAT (UI 1.3.0)
// Daftar berkas hasil konversi yang sebelumnya berada di
// Beranda, kini di tab sendiri sesuai mockup: chip kategori
// menyaring daftar, tarik ke bawah untuk menyegarkan.
// ============================================================

sealed class HomeHistoryState {
    object NoHistory : HomeHistoryState()
    data class HistoryAvailable(val files: List<File>) : HomeHistoryState()
    object FilteredEmpty : HomeHistoryState()
    object Loading : HomeHistoryState()
    data class Error(val message: String) : HomeHistoryState()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HistoryScreen(
    ubahkanFiles: List<File>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPickFileClick: () -> Unit,
    onFileClick: (File) -> Unit,
    onShareFile: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { onRefresh() }
    )

    val engineState by UbahkanEngine.currentStatus.collectAsState()
    val queueState by ConversionQueueManager.queueState.collectAsState()
    val completedCount = remember(queueState) { queueState.count { it.status == JobStatus.COMPLETED } }

    // Segarkan otomatis ketika ada konversi yang selesai
    LaunchedEffect(engineState, completedCount) {
        if (engineState is UbahkanEngine.ConvertState.Success || completedCount > 0) {
            onRefresh()
        }
    }

    // --- STATISTIK HEMAT (UI 1.4.0) ---
    // Pekerjaan selesai yang ukurannya diketahui menyumbang selisih
    // masukan-keluaran ke akumulasi hemat persisten. Berkas lama tanpa
    // data ukuran tidak dihitung (dihitung yang ada saja).
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { com.ubahkan.app.UbahkanPrefs(context) }
    var savedTotal by remember { mutableStateOf(prefs.savedBytesTotal) }

    LaunchedEffect(queueState) {
        val counted = prefs.countedJobIds.toMutableSet()
        var changed = false
        queueState.filter { it.status == JobStatus.COMPLETED }.forEach { job ->
            if (job.id !in counted) {
                counted += job.id
                changed = true
                val inputSize = com.ubahkan.app.getFileSizeFromUri(context, job.inputUri)
                val outputPaths = (listOfNotNull(job.outputFilePath) + job.outputFilePaths).distinct()
                val outputSize = outputPaths.sumOf { path -> File(path).takeIf { it.exists() }?.length() ?: 0L }
                if (inputSize > 0 && outputSize > 0 && outputSize < inputSize) {
                    prefs.savedBytesTotal += inputSize - outputSize
                }
            }
        }
        if (changed) {
            prefs.countedJobIds = counted
            savedTotal = prefs.savedBytesTotal
        }
    }

    // Meta konversi sesi ini per berkas hasil: label "MP4 ke MP3" + persen hemat.
    val sessionMeta = remember(queueState) {
        val map = mutableMapOf<String, Pair<String, Int?>>()
        queueState.filter { it.status == JobStatus.COMPLETED }.forEach { job ->
            val fromExt = job.originalFileName.substringAfterLast('.', "").uppercase()
            if (fromExt.isEmpty() || job.targetFormat.isBlank()) return@forEach
            val label = strings.historyConversionLabel(fromExt, job.targetFormat.uppercase())
            val inputSize = com.ubahkan.app.getFileSizeFromUri(context, job.inputUri)
            val outputPaths = (listOfNotNull(job.outputFilePath) + job.outputFilePaths).distinct()
            outputPaths.forEach { path ->
                val outFile = File(path)
                val percent = if (inputSize > 0 && outFile.exists() && outFile.length() in 1 until inputSize) {
                    ((inputSize - outFile.length()) * 100 / inputSize).toInt()
                } else null
                map[outFile.absolutePath] = label to percent
            }
        }
        map
    }

    // --- FILTER STATE ---
    var selectedCategoryId by remember { mutableStateOf("all") }
    val categories = listOf(
        CategoryFilterItem("all", strings.categoryAll),
        CategoryFilterItem("video", strings.categoryVideo),
        CategoryFilterItem("audio", strings.categoryAudio),
        CategoryFilterItem("image", strings.categoryImage),
        CategoryFilterItem("document", strings.categoryDocument)
    )

    val filteredFiles = remember(ubahkanFiles, selectedCategoryId) {
        when (selectedCategoryId) {
            "all" -> ubahkanFiles
            "document" -> ubahkanFiles.filter {
                val c = FileCategoryManager.getCategory(it.extension)
                c == FileCategory.DOCUMENT || c == FileCategory.PDF || c == FileCategory.EBOOK || c == FileCategory.SPREADSHEET
            }
            "image" -> ubahkanFiles.filter { FileCategoryManager.getCategory(it.extension) == FileCategory.IMAGE }
            "video" -> ubahkanFiles.filter { FileCategoryManager.getCategory(it.extension) == FileCategory.VIDEO }
            "audio" -> ubahkanFiles.filter { FileCategoryManager.getCategory(it.extension) == FileCategory.AUDIO }
            else -> ubahkanFiles
        }
    }

    val historyState: HomeHistoryState = when {
        isRefreshing -> HomeHistoryState.Loading
        ubahkanFiles.isEmpty() -> HomeHistoryState.NoHistory
        filteredFiles.isEmpty() -> HomeHistoryState.FilteredEmpty
        else -> HomeHistoryState.HistoryAvailable(filteredFiles)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                UbahkanScreenHeader(
                    title = strings.tabHistory,
                    subtitle = strings.homeSavedFooter
                )
            }

            if (ubahkanFiles.isNotEmpty()) {
                item {
                    HistoryStatsCard(
                        filesCount = ubahkanFiles.size,
                        savedBytes = savedTotal
                    )
                }
            }

            item {
                UbahkanTextTabs(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it }
                )
            }

            when (historyState) {
                is HomeHistoryState.NoHistory -> {
                    item {
                        UbahkanEmptyFileState(
                            onPickFileClick = onPickFileClick,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }

                is HomeHistoryState.FilteredEmpty -> {
                    item {
                        val currentLabel = categories.find { it.id == selectedCategoryId }?.label ?: strings.categoryAll
                        UbahkanFilteredEmptyState(
                            category = currentLabel,
                            onClearFilter = { selectedCategoryId = "all" }
                        )
                    }
                }

                is HomeHistoryState.HistoryAvailable -> {
                    val groups = listOf(
                        strings.historyGroupToday to historyState.files.filter { dayBucket(it) == 0 },
                        strings.historyGroupYesterday to historyState.files.filter { dayBucket(it) == 1 },
                        strings.historyGroupOlder to historyState.files.filter { dayBucket(it) == 2 }
                    ).filter { it.second.isNotEmpty() }

                    groups.forEach { (groupLabel, groupFiles) ->
                        item(key = "label_$groupLabel") {
                            UbahkanSectionLabel(
                                text = groupLabel,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        item(key = "card_$groupLabel") {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    groupFiles.forEachIndexed { idx, file ->
                                        val meta = sessionMeta[file.absolutePath]
                                        FileHistoryItemCard(
                                            file = file,
                                            onClick = { onFileClick(file) },
                                            onShare = { onShareFile(file) },
                                            onDelete = { onDeleteFile(file) },
                                            conversionLabel = meta?.first,
                                            savedPercent = meta?.second,
                                            showDivider = idx < groupFiles.lastIndex
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is HomeHistoryState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                is HomeHistoryState.Error -> {
                    item {
                        Text(
                            text = historyState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

// ============================================================
// KARTU STATISTIK RIWAYAT (UI 1.4.0)
// Kartu putih melayang radius 22dp: tiga kolom sama lebar
// dipisah garis vertikal tipis (berkas, dihemat, offline).
// ============================================================

@Composable
private fun HistoryStatsCard(
    filesCount: Int,
    savedBytes: Long,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryStatColumn(
                value = filesCount.toString(),
                label = strings.historyStatFiles,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            HistoryStatColumn(
                value = formatFileSize(savedBytes),
                label = strings.historyStatSaved,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            HistoryStatColumn(
                value = "100%",
                label = strings.historyStatOffline,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HistoryStatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

// Kelompok hari berkas: 0 = hari ini, 1 = kemarin, 2 = lebih lama.
private fun dayBucket(file: File): Int {
    val fileCal = java.util.Calendar.getInstance().apply { timeInMillis = file.lastModified() }
    val today = java.util.Calendar.getInstance()
    fun sameDay(a: java.util.Calendar, b: java.util.Calendar): Boolean =
        a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
            a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
    if (sameDay(fileCal, today)) return 0
    today.add(java.util.Calendar.DAY_OF_YEAR, -1)
    if (sameDay(fileCal, today)) return 1
    return 2
}

// ============================================================
// EMPTY STATE  -  logo + teks, gaya minimalis
// ============================================================

@Composable
fun UbahkanEmptyFileState(
    onPickFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        UbahkanLogo(
            size = 64.dp,
            modifier = Modifier.clickable { onPickFileClick() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.selectFile,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = strings.selectFileSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// FILTERED EMPTY STATE
// ============================================================

@Composable
fun UbahkanFilteredEmptyState(
    category: String,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.noFilesInCategory(category),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = strings.noFilesInCategoryDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onClearFilter,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = strings.btnShowAll,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
