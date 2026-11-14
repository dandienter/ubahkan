package com.ubahkan.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arthenica.ffmpegkit.FFprobeKit
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.OutputManager
import com.ubahkan.app.core.SelectedFileItem
import com.ubahkan.app.ui.components.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Locale

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// Lembar info berkas: membaca MediaInformation lewat FFprobeKit dari salinan
// temp berkas terpilih, lalu menampilkan format, durasi, ukuran, resolusi,
// codec video/audio, dan bitrate. Sifatnya hanya tampil, tidak membuat job.

internal data class MediaInfoData(
    val format: String,
    val durationText: String?,
    val resolution: String?,
    val videoCodec: String?,
    val audioCodec: String?,
    val bitrateText: String?
)

private fun formatInfoDuration(totalSeconds: Double): String {
    val total = totalSeconds.toLong()
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

internal fun probeMediaInfo(
    context: android.content.Context,
    file: SelectedFileItem
): MediaInfoData? {
    val ext = file.fileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "bin" }
    val temp = OutputManager.createTempFile(context, "info", ext)
    return try {
        val copied = context.contentResolver.openInputStream(file.uri)?.use { input ->
            FileOutputStream(temp).use { output -> input.copyTo(output) }
            true
        } ?: false
        if (!copied || temp.length() <= 0) {
            null
        } else {
            val info = FFprobeKit.getMediaInformation(temp.absolutePath).mediaInformation
            if (info == null) {
                null
            } else {
                val streams = info.streams ?: emptyList()
                val videoStream = streams.firstOrNull { it.type.equals("video", ignoreCase = true) }
                val audioStream = streams.firstOrNull { it.type.equals("audio", ignoreCase = true) }
                val width = videoStream?.width ?: 0L
                val height = videoStream?.height ?: 0L
                val bitrateBps = info.bitrate?.toDoubleOrNull()
                MediaInfoData(
                    format = info.format ?: ext.uppercase(),
                    durationText = info.duration?.toDoubleOrNull()?.let { formatInfoDuration(it) },
                    resolution = if (width > 0 && height > 0) "$width x $height" else null,
                    videoCodec = videoStream?.codec,
                    audioCodec = audioStream?.codec,
                    bitrateText = bitrateBps?.let { bps ->
                        if (bps >= 1000) "${(bps / 1000).toLong()} kbps" else "${bps.toLong()} bps"
                    }
                )
            }
        }
    } catch (e: Exception) {
        null
    } finally {
        OutputManager.cleanTempFiles(temp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInfoSheet(
    file: SelectedFileItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = AppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var info by remember { mutableStateOf<MediaInfoData?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(file.uri) {
        info = withContext(Dispatchers.IO) { probeMediaInfo(context, file) }
        loaded = true
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = strings.toolInfoTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = file.fileName,
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

            if (!loaded) {
                Text(
                    text = strings.infoLoading,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (info == null) {
                InfoRow(label = strings.infoSizeLabel, value = formatFileSize(file.sizeBytes))
                Text(
                    text = strings.infoFailed,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                val data = info!!
                InfoRow(label = strings.infoFormatLabel, value = data.format)
                InfoRow(label = strings.infoSizeLabel, value = formatFileSize(file.sizeBytes))
                data.durationText?.let { InfoRow(label = strings.infoDurationLabel, value = it) }
                data.resolution?.let { InfoRow(label = strings.infoResolutionLabel, value = it) }
                data.videoCodec?.let { InfoRow(label = strings.infoVideoCodecLabel, value = it) }
                data.audioCodec?.let { InfoRow(label = strings.infoAudioCodecLabel, value = it) }
                data.bitrateText?.let { InfoRow(label = strings.infoBitrateLabel, value = it) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
