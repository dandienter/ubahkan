package com.ubahkan.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import com.ubahkan.app.ui.components.rememberPressScale
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

                // Visual Transformation: Source File → Ubahkan Ribbon → Target File
                FileTransformationVisual(
                    sourceExt = currentJob?.originalFileName?.substringAfterLast('.', "")?.uppercase() ?: "FILE",
                    targetExt = currentJob?.targetFormat?.uppercase() ?: "OUTPUT"
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Progress Info
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

                    Text(
                        text = "${(animatedProgress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (totalCount > 1) {
                        Text(
                            text = strings.progressBatchFilesCompleted(completedCount, totalCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentJob != null) {
                        Text(
                            text = currentJob.originalFileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

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
private fun FileTransformationVisual(
    sourceExt: String,
    targetExt: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "transformPulse")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Source File Box
        Surface(
            modifier = Modifier
                .size(76.dp)
                .offset(y = floatOffset.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceExt,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Center Ubahkan Logo
        UbahkanLogo(size = 56.dp)

        // Target File Box
        Surface(
            modifier = Modifier
                .size(76.dp)
                .offset(y = (-floatOffset).dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = targetExt,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
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
