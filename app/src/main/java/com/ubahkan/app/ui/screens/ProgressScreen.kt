package com.ubahkan.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.ConversionJob
import com.ubahkan.app.core.ConversionQueueManager
import com.ubahkan.app.core.JobStatus
import com.ubahkan.app.core.ToolOperation
import com.ubahkan.app.sharePartsToWhatsAppStatus
import com.ubahkan.app.ui.components.UbahkanEaseOut
import com.ubahkan.app.ui.components.UbahkanLogo
import com.ubahkan.app.ui.components.UbahkanSectionLabel
import com.ubahkan.app.ui.components.formatFileSize
import com.ubahkan.app.ui.components.rememberPressScale
import com.ubahkan.app.ui.theme.UbahkanBlue
import com.ubahkan.app.ui.theme.UbahkanDarkBackground
import com.ubahkan.app.ui.theme.UbahkanProgressTrack
import com.ubahkan.app.ui.theme.UbahkanProgressTrackDark
import kotlin.math.roundToInt
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onMinimize: () -> Unit,
    onFinish: () -> Unit,
    onViewResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val context = LocalContext.current
    val queueState by ConversionQueueManager.queueState.collectAsState()

    val totalCount = queueState.size
    val completedCount = queueState.count { it.status == JobStatus.COMPLETED }
    val failedCount = queueState.count { it.status == JobStatus.FAILED }
    val activeJob = queueState.firstOrNull { it.status == JobStatus.PROCESSING }

    val isFinished = totalCount > 0 && queueState.none { it.status == JobStatus.PROCESSING || it.status == JobStatus.WAITING }
    val isAllSuccess = isFinished && failedCount == 0

    // Berkas hasil Potong Otomatis yang selesai, untuk tombol Bagikan ke Status WA
    val splitPartFiles = remember(queueState) {
        queueState
            .filter { it.operation == ToolOperation.AUTO_SPLIT && it.status == JobStatus.COMPLETED }
            .flatMap { it.outputFilePaths }
            .map { File(it) }
            .filter { it.exists() }
    }

    // Konfirmasi pembatalan
    var showCancelDialog by remember { mutableStateOf(false) }
    val (viewPressSource, viewPressModifier) = rememberPressScale()
    val (backPressSource, backPressModifier) = rememberPressScale()

    // Intercept back button to minimize
    BackHandler {
        onMinimize()
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(strings.cancelDialogTitle) },
            text = { Text(strings.cancelDialogDesc) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        ConversionQueueManager.cancelAllJobs()
                        onFinish()
                    }
                ) {
                    Text(strings.btnConfirmCancel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(strings.btnContinue)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isFinished) strings.titleFinished else strings.titleConversionProgress,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.btnMinimize
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (!isFinished) {
                // --- STATE PROSES BERJALAN ---
                val currentJob = activeJob ?: queueState.firstOrNull()
                val currentPercent = currentJob?.progress ?: 0
                val animatedProgress by animateFloatAsState(
                    targetValue = currentPercent / 100f,
                    animationSpec = tween(durationMillis = 300, easing = UbahkanEaseOut),
                    label = "progressAnim"
                )

                // Cincin progres besar: persen Regular di tengah cincin
                // biru, teks tahap di atas, frasa acak berganti di bawah.
                val inputSize = remember(currentJob?.id) {
                    currentJob?.let { com.ubahkan.app.getFileSizeFromUri(context, it.inputUri) } ?: 0L
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.convertingFileProgress,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ProgressRing(
                        progress = animatedProgress,
                        centerText = "${(animatedProgress * 100).roundToInt()}%",
                        subText = if (inputSize > 0) {
                            strings.progressFromSize(formatFileSize(inputSize))
                        } else null
                    )

                    // Label tahap dari sinyal nyata antrean: belum ada job
                    // aktif berarti menyiapkan; progres akhir atau pesan
                    // finalisasi engine berarti menyimpan hasil.
                    val stageLabel = when {
                        activeJob == null -> strings.progressStagePreparing
                        currentJob != null && (currentJob.progress >= 95 ||
                            currentJob.progressMessage == strings.engineFinalizingValidating) ->
                            strings.progressStageSaving
                        else -> strings.progressStageConverting
                    }
                    Text(
                        text = stageLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (currentJob != null) {
                        Text(
                            text = currentJob.originalFileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val stageMessage = currentJob.progressMessage
                        if (stageMessage.isNotBlank()) {
                            Text(
                                text = stageMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    RandomProgressPhrase(phrases = strings.progressPhrases)

                    if (totalCount > 1) {
                        Text(
                            text = strings.progressBatchFilesCompleted(completedCount, totalCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Antrean pekerjaan: baris ikon + nama + status (gaya mockup)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    UbahkanSectionLabel(text = strings.progressQueueHeader)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(queueState, key = { it.id }) { job ->
                            QueueJobRow(job = job)
                        }
                    }
                }

                // Tombol Batal
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.cancelConversion)
                }

            } else if (isAllSuccess) {
                // --- STATE SUKSES LENGKAP ---
                Spacer(modifier = Modifier.weight(0.5f))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = strings.titleFinished,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = strings.conversionSuccess,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (totalCount == 1) strings.singleFileSuccessDesc else strings.batchFilesSuccessDesc(completedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Laporan banding ukuran sebelum dan sesudah, dari data job
                // yang sama dengan akumulasi statistik Riwayat. Bila ukuran
                // tidak terbaca, hanya nama berkas dan lokasi yang tampil.
                val completedJobs = queueState.filter { it.status == JobStatus.COMPLETED }
                val reportData = remember(queueState) {
                    var beforeTotal = 0L
                    var afterTotal = 0L
                    completedJobs.forEach { job ->
                        val inSize = com.ubahkan.app.getFileSizeFromUri(context, job.inputUri)
                        val outPaths = (listOfNotNull(job.outputFilePath) + job.outputFilePaths).distinct()
                        val outSize = outPaths.sumOf { path ->
                            File(path).takeIf { it.exists() }?.length() ?: 0L
                        }
                        if (inSize > 0 && outSize > 0) {
                            beforeTotal += inSize
                            afterTotal += outSize
                        }
                    }
                    val firstOutputName = completedJobs.firstOrNull()
                        ?.let { job ->
                            (listOfNotNull(job.outputFilePath) + job.outputFilePaths)
                                .firstOrNull()
                                ?.let { File(it).name }
                        }
                    Triple(beforeTotal, afterTotal, firstOutputName)
                }
                val (reportBefore, reportAfter, reportName) = reportData
                if (reportName != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = strings.reportTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (reportBefore > 0 && reportAfter > 0) {
                                Text(
                                    text = strings.reportBeforeAfter(
                                        formatFileSize(reportBefore),
                                        formatFileSize(reportAfter)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val percentText = when {
                                    reportAfter < reportBefore ->
                                        strings.reportSavedPercent(
                                            ((reportBefore - reportAfter) * 100 / reportBefore).toInt()
                                        )
                                    reportAfter > reportBefore ->
                                        strings.reportGrownPercent(
                                            ((reportAfter - reportBefore) * 100 / reportBefore).toInt()
                                        )
                                    else -> strings.reportSameSize
                                }
                                Text(
                                    text = percentText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (completedJobs.size == 1) {
                                Text(
                                    text = reportName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = strings.homeSavedFooter,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (splitPartFiles.isNotEmpty()) {
                        WhatsAppStatusShareButton(files = splitPartFiles)
                    }

                    Button(
                        onClick = onViewResult,
                        interactionSource = viewPressSource,
                        modifier = viewPressModifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(strings.btnViewResult, fontWeight = FontWeight.Medium)
                    }

                    OutlinedButton(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(strings.btnBackToHome)
                    }
                }

            } else {
                // --- STATE SEBAGIAN GAGAL / SEMUA GAGAL ---
                Spacer(modifier = Modifier.weight(0.5f))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (completedCount > 0) strings.conversionPartialSuccess else strings.conversionFailed,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                val summaryText = if (completedCount > 0) {
                    strings.summaryPartial(completedCount, failedCount)
                } else {
                    queueState.firstOrNull { it.status == JobStatus.FAILED }?.errorMessage ?: strings.defaultErrorMsg
                }

                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                if (splitPartFiles.isNotEmpty()) {
                    WhatsAppStatusShareButton(files = splitPartFiles)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = onFinish,
                    interactionSource = backPressSource,
                    modifier = backPressModifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(strings.btnBackToHome, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Tombol hitam berbagi semua potongan hasil Potong Otomatis ke WhatsApp (Status WA)
@Composable
private fun WhatsAppStatusShareButton(files: List<File>) {
    val context = LocalContext.current
    val strings = AppStrings.current

    Button(
        onClick = { sharePartsToWhatsAppStatus(context, files, strings) },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(strings.btnShareToWhatsAppStatus, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    centerText: String,
    subText: String?
) {
    val isDark = MaterialTheme.colorScheme.background == UbahkanDarkBackground
    val trackColor = if (isDark) UbahkanProgressTrackDark else UbahkanProgressTrack
    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            if (progress > 0f) {
                drawArc(
                    color = UbahkanBlue,
                    startAngle = -90f,
                    sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp),
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subText != null) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Frasa santai berganti tiap sekitar 2,8 detik dengan fade,
// urutan acak tanpa mengulang frasa yang sama berurutan.
@Composable
private fun RandomProgressPhrase(phrases: List<String>) {
    if (phrases.isEmpty()) return
    var index by remember { mutableStateOf(phrases.indices.random()) }
    LaunchedEffect(phrases) {
        while (true) {
            kotlinx.coroutines.delay(2800)
            var next = phrases.indices.random()
            if (phrases.size > 1) {
                while (next == index) {
                    next = phrases.indices.random()
                }
            }
            index = next
        }
    }
    AnimatedContent(
        targetState = index,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "progressPhrase"
    ) { i ->
        Text(
            text = "\"${phrases.getOrElse(i) { "" }}\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Baris satu pekerjaan dalam antrean layar proses:
// ikon berkas di kotak abu, nama + format tujuan, teks status,
// dan persen kecil untuk yang sedang diproses.
@Composable
private fun QueueJobRow(job: ConversionJob) {
    val strings = AppStrings.current
    val statusText = when (job.status) {
        JobStatus.PROCESSING -> job.progressMessage
        JobStatus.WAITING -> strings.queueWaiting
        JobStatus.COMPLETED -> strings.queueCompleted
        JobStatus.FAILED -> job.errorMessage ?: strings.queueFailed
        JobStatus.CANCELLED -> strings.queueCancelled
    }
    val titleText = if (job.targetFormat.isNotBlank()) {
        "${job.originalFileName} \u2192 ${job.targetFormat.uppercase()}"
    } else {
        job.originalFileName
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (job.status == JobStatus.PROCESSING) {
                Text(
                    text = "${job.progress}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
