package com.ubahkan.app.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubahkan.app.UbahkanEngine
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.ConversionQueueManager
import com.ubahkan.app.core.FileCategory
import com.ubahkan.app.core.FileCategoryManager
import com.ubahkan.app.core.JobStatus
import com.ubahkan.app.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ripple
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.ui.unit.sp

// ============================================================
// HOME SCREEN — STATE MODEL
// Sesuai UBAHKAN_UI_UX_SPEC.md Section 7.5
// ============================================================

data class CategoryFilterItem(val id: String, val label: String)

sealed class HomeHistoryState {
    object NoHistory : HomeHistoryState()
    data class HistoryAvailable(val files: List<File>) : HomeHistoryState()
    object FilteredEmpty : HomeHistoryState()
    object Loading : HomeHistoryState()
    data class Error(val message: String) : HomeHistoryState()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    ubahkanFiles: List<File>,
    isRefreshing: Boolean,
    hasNotificationPermission: Boolean,
    onRefresh: () -> Unit,
    onPickFileClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onFileClick: (File) -> Unit,
    onShareFile: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    isBatteryUnrestricted: Boolean = true,
    onRetryConversion: (android.net.Uri, String) -> Unit,
    onBannerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val strings = AppStrings.current

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { onRefresh() }
    )

    val engineState by UbahkanEngine.currentStatus.collectAsState()
    val queueState by ConversionQueueManager.queueState.collectAsState()

    val activeJob = remember(queueState) { queueState.firstOrNull { it.status == JobStatus.PROCESSING } }
    val failedJobs = remember(queueState) { queueState.filter { it.status == JobStatus.FAILED } }
    val completedCount = remember(queueState) { queueState.count { it.status == JobStatus.COMPLETED } }
    val totalQueueCount = remember(queueState) { queueState.size }
    
    val context = androidx.compose.ui.platform.LocalContext.current
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
    
    // Auto-refresh when conversion completes
    LaunchedEffect(engineState, completedCount) {
        if (engineState is UbahkanEngine.ConvertState.Success || completedCount > 0) {
            onRefresh()
            if (engineState is UbahkanEngine.ConvertState.Success) {
                delay(1500)
                UbahkanEngine.currentStatus.value = UbahkanEngine.ConvertState.Idle
            }
        }
    }

    // --- FILTER STATE ---
    var selectedCategoryId by remember { mutableStateOf("all") }
    val categories = listOf(
        CategoryFilterItem("all", strings.categoryAll),
        CategoryFilterItem("document", strings.categoryDocument),
        CategoryFilterItem("image", strings.categoryImage),
        CategoryFilterItem("video", strings.categoryVideo),
        CategoryFilterItem("audio", strings.categoryAudio)
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

    // --- HISTORY STATE MODEL ---
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- 0. HERO MINIMALIS (sesuai mockup) ---
            item {
                HomeHero(onPickFileClick = onPickFileClick)
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
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BatteryAlert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.batteryBannerTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = strings.batteryBannerDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
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
                                    Text(strings.btnLater, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showBatteryGuideDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(strings.btnLearnHow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. GRID KATEGORI (pengganti chip filter, sesuai mockup) ---
            // Ketuk kategori untuk memfilter riwayat; ketuk lagi untuk reset.
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    UbahkanSectionLabel(text = strings.homeCategoryHeader)
                    HomeCategoryGrid(
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = { id ->
                            selectedCategoryId = if (selectedCategoryId == id) "all" else id
                        }
                    )
                }
            }

            // --- 4b. LABEL RIWAYAT ---
            if (historyState is HomeHistoryState.HistoryAvailable ||
                historyState is HomeHistoryState.FilteredEmpty
            ) {
                item {
                    UbahkanSectionLabel(
                        text = strings.homeHistoryHeader,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // --- 5. MAIN CONTENT AREA ---
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
                    items(historyState.files, key = { it.absolutePath }) { file ->
                        FileHistoryItemCard(
                            file = file,
                            onClick = { onFileClick(file) },
                            onShare = { onShareFile(file) },
                            onDelete = { onDeleteFile(file) }
                        )
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

            // --- 6. FOOTER LOKASI HASIL ---
            if (historyState is HomeHistoryState.HistoryAvailable) {
                item {
                    Text(
                        text = strings.homeSavedFooter,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }

        //Battery Dialog
        if (showBatteryGuideDialog) {
            BatteryOptimizationGuideDialog(
                onDismiss = { showBatteryGuideDialog = false },
                onOpenSettings = {
                    showBatteryGuideDialog = false
                    com.ubahkan.app.openAppBatterySettings(context, strings)
                }
            )
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
// CATEGORY FILTER CHIPS
// Sesuai UBAHKAN_UI_UX_SPEC.md Section 7.2
// ============================================================

@Composable
fun UbahkanCategoryFilter(
    categories: List<CategoryFilterItem>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(categories, key = { it.id }) { categoryItem ->
            val isSelected = selectedCategoryId == categoryItem.id
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(categoryItem.id) },
                label = {
                    Text(
                        text = categoryItem.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = Color.Transparent,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    selectedBorderWidth = 0.dp
                )
            )
        }
    }
}

// ============================================================
// EMPTY STATE — logo + teks, gaya minimalis
// ============================================================

@Composable
fun UbahkanEmptyFileState(
    onPickFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    // Empty state minimalis: logo + teks, tanpa lingkaran animasi.
    // Aksi pilih file utama ada di tombol hero; logo di sini juga bisa diketuk.
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
            fontWeight = FontWeight.SemiBold,
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
// HERO BERANDA — kicker, judul besar, tombol Pilih File hitam
// Sesuai mockup minimalis layar 1
// ============================================================

@Composable
private fun HomeHero(
    onPickFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = strings.homeKicker,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = strings.homeHeroTitle,
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

        Button(
            onClick = onPickFileClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = strings.selectFile,
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(modifier = Modifier.height(13.dp))

        Text(
            text = strings.homeShareHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================
// GRID KATEGORI 2 KOLOM — ikon garis biru, gaya mockup
// Ketuk = filter riwayat, ketuk lagi = reset ke semua
// ============================================================

@Composable
private fun HomeCategoryGrid(
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val tiles = listOf(
        HomeCategoryTile("video", strings.categoryVideo, strings.homeFormatsCount(5), Icons.Default.Movie),
        HomeCategoryTile("audio", strings.categoryAudio, strings.homeFormatsCount(7), Icons.Default.Audiotrack),
        HomeCategoryTile("image", strings.categoryImage, strings.homeFormatsCount(6), Icons.Default.Image),
        HomeCategoryTile("document", strings.categoryDocument, "PDF · EPUB", Icons.Default.Description)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tiles.chunked(2).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTiles.forEach { tile ->
                    HomeCategoryCard(
                        tile = tile,
                        isSelected = selectedCategoryId == tile.id,
                        onClick = { onCategorySelected(tile.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowTiles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class HomeCategoryTile(
    val id: String,
    val label: String,
    val sublabel: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun HomeCategoryCard(
    tile: HomeCategoryTile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = tile.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tile.sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================
// FILTERED EMPTY STATE
// Sesuai UBAHKAN_UI_UX_SPEC.md Section 7.4 & 7.5
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
            fontWeight = FontWeight.SemiBold,
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
                    fontWeight = FontWeight.SemiBold,
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
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
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
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = strings.batteryStep2,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = strings.batteryStep3,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
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
                Text(strings.btnOpenAppInfo, fontWeight = FontWeight.Bold)
            }
        }
    )
}