package com.ubahkan.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.ToolOperation
import com.ubahkan.app.ui.components.StaggerReveal
import com.ubahkan.app.ui.components.ToolGridCard
import com.ubahkan.app.ui.components.UbahkanSectionLabel

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// TOOLS SCREEN  -  TAB ALAT (UI 1.3.0)
// Daftar berkelompok sesuai mockup: baris berisi ikon garis
// di kotak abu lembut, nama Medium, deskripsi satu baris abu,
// dan chevron kanan. Ketuk baris membuka pemilih berkas untuk
// operasi itu. Di bawahnya tetap ada kartu info kemampuan.
// ============================================================

private data class ToolEntry(
    val operation: ToolOperation,
    val mimeType: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun ToolsScreen(
    onPickFileForTool: (ToolOperation, String) -> Unit,
    onConvertClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { com.ubahkan.app.UbahkanPrefs(context) }
    var viewMode by remember { mutableStateOf(prefs.toolsViewMode) } // 0: List, 1: Grid
    val gridMode = viewMode == 1

    val videoTools = listOf(
        ToolEntry(ToolOperation.COMPRESS_VIDEO, "video/*", strings.toolCompressVideoTitle, strings.toolCompressVideoDesc, Icons.Outlined.Compress),
        ToolEntry(ToolOperation.TRIM_VIDEO, "video/*", strings.toolTrimTitle, strings.toolTrimDesc, Icons.Outlined.ContentCut),
        ToolEntry(ToolOperation.AUTO_SPLIT, "video/*", strings.toolSplitTitle, strings.toolSplitDesc, Icons.Outlined.CallSplit),
        ToolEntry(ToolOperation.MERGE_VIDEO, "video/*", strings.toolMergeVideoTitle, strings.toolMergeVideoDesc, Icons.Outlined.Layers),
        ToolEntry(ToolOperation.HARDSUB, "video/*", strings.toolHardsubTitle, strings.toolHardsubDesc, Icons.Outlined.Subtitles),
        ToolEntry(ToolOperation.CROP_VIDEO, "video/*", strings.toolCropTitle, strings.toolCropDesc, Icons.Outlined.Crop),
        ToolEntry(ToolOperation.RESIZE_VIDEO, "video/*", strings.toolResizeTitle, strings.toolResizeDesc, Icons.Outlined.AspectRatio),
        ToolEntry(ToolOperation.MUTE_VIDEO, "video/*", strings.toolMuteTitle, strings.toolMuteDesc, Icons.Outlined.VolumeOff),
        ToolEntry(ToolOperation.REPLACE_AUDIO, "video/*", strings.toolReplaceAudioTitle, strings.toolReplaceAudioDesc, Icons.Outlined.Audiotrack)
    )
    val documentTools = listOf(
        ToolEntry(ToolOperation.SPLIT_PDF, "application/pdf", strings.toolSplitPdfTitle, strings.toolSplitPdfDesc, Icons.Outlined.ContentCopy),
        ToolEntry(ToolOperation.MERGE_PDF, "application/pdf", strings.toolMergePdfTitle, strings.toolMergePdfDesc, Icons.Outlined.PictureAsPdf)
    )
    val mediaTools = listOf(
        ToolEntry(ToolOperation.COMPRESS_IMAGE, "image/*", strings.toolCompressImageTitle, strings.toolCompressImageDesc, Icons.Outlined.PhotoSizeSelectLarge),
        ToolEntry(ToolOperation.HEIC_TO_JPG, "image/*", strings.toolHeicTitle, strings.toolHeicDesc, Icons.Outlined.Image),
        ToolEntry(ToolOperation.NORMALIZE_VOLUME, "*/*", strings.toolNormalizeTitle, strings.toolNormalizeDesc, Icons.Outlined.GraphicEq),
        ToolEntry(ToolOperation.FILE_INFO, "*/*", strings.toolInfoTitle, strings.toolInfoDesc, Icons.Default.Info)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + saklar tampilan List/Grid (persisten di preferensi)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.toolsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.toolsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ToolsViewToggle(
                    gridMode = gridMode,
                    onSelect = { grid ->
                        viewMode = if (grid) 1 else 0
                        prefs.toolsViewMode = viewMode
                    }
                )
            }
        }

        // Konversi berkas biasa (operasi CONVERT)
        item {
            StaggerReveal(index = 0) {
                if (gridMode) {
                    ToolConvertCard(onClick = onConvertClick)
                } else {
                    ToolRow(
                        title = strings.toolConvertTitle,
                        description = strings.toolConvertDesc,
                        icon = Icons.Outlined.SwapHoriz,
                        onClick = onConvertClick,
                        showDivider = false
                    )
                }
            }
        }

        // Grup Video
        item {
            UbahkanSectionLabel(text = strings.categoryVideo)
        }
        if (gridMode) {
            item { ToolsGridGroup(tools = videoTools, baseIndex = 1, onPickFileForTool = onPickFileForTool) }
        } else {
            items(videoTools.size, key = { "video_${videoTools[it].operation}" }) { idx ->
                val tool = videoTools[idx]
                StaggerReveal(index = 1 + idx) {
                    ToolRow(
                        title = tool.title,
                        description = tool.description,
                        icon = tool.icon,
                        onClick = { onPickFileForTool(tool.operation, tool.mimeType) },
                        showDivider = idx < videoTools.lastIndex
                    )
                }
            }
        }

        // Grup Dokumen
        item {
            UbahkanSectionLabel(
                text = strings.categoryDocument,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (gridMode) {
            item { ToolsGridGroup(tools = documentTools, baseIndex = 2, onPickFileForTool = onPickFileForTool) }
        } else {
            items(documentTools.size, key = { "doc_${documentTools[it].operation}" }) { idx ->
                val tool = documentTools[idx]
                StaggerReveal(index = 2 + idx) {
                    ToolRow(
                        title = tool.title,
                        description = tool.description,
                        icon = tool.icon,
                        onClick = { onPickFileForTool(tool.operation, tool.mimeType) },
                        showDivider = idx < documentTools.lastIndex
                    )
                }
            }
        }

        // Grup Gambar dan Audio
        item {
            UbahkanSectionLabel(
                text = strings.toolsGroupMedia,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (gridMode) {
            item { ToolsGridGroup(tools = mediaTools, baseIndex = 3, onPickFileForTool = onPickFileForTool) }
        } else {
            items(mediaTools.size, key = { "media_${mediaTools[it].operation}" }) { idx ->
                val tool = mediaTools[idx]
                StaggerReveal(index = 3 + idx) {
                    ToolRow(
                        title = tool.title,
                        description = tool.description,
                        icon = tool.icon,
                        onClick = { onPickFileForTool(tool.operation, tool.mimeType) },
                        showDivider = idx < mediaTools.lastIndex
                    )
                }
            }
        }

        // Kartu info kemampuan konversi bawaan
        item {
            SectionHeading(
                title = strings.toolsConversionTitle,
                subtitle = strings.toolsConversionSubtitle
            )
        }

        item {
            ToolFeatureCard(
                title = strings.toolVideoTitle,
                description = strings.toolVideoDesc,
                supportedFormats = "MKV · MP4 · MOV  →  MP4 · MOV · MKV",
                icon = Icons.Default.VideoFile
            )
        }

        item {
            ToolFeatureCard(
                title = strings.toolPdfTitle,
                description = strings.toolPdfDesc,
                supportedFormats = "PDF · JPG · PNG · WEBP  →  PDF · PNG · JPG",
                icon = Icons.Default.PictureAsPdf
            )
        }

        item {
            ToolFeatureCard(
                title = strings.toolAudioTitle,
                description = strings.toolAudioDesc,
                supportedFormats = "MP4 · MKV · MOV · WEBM  →  MP3 · M4A · WAV · FLAC · OGG · OPUS",
                icon = Icons.Default.Audiotrack
            )
        }

        item {
            ToolFeatureCard(
                title = strings.toolSvgTitle,
                description = strings.toolSvgDesc,
                supportedFormats = "SVG  →  PNG · JPG · WEBP",
                icon = Icons.Default.AutoAwesome
            )
        }

        item {
            ToolFeatureCard(
                title = strings.toolEpubTitle,
                description = strings.toolEpubDesc,
                supportedFormats = "EPUB  →  TXT",
                icon = Icons.Default.Book
            )
        }

        item {
            ToolFeatureCard(
                title = strings.toolImageTitle,
                description = strings.toolImageDesc,
                supportedFormats = "PNG · JPG · WEBP · BMP  →  PNG · JPG · WEBP · BMP · PDF",
                icon = Icons.Default.Image
            )
        }

        // Cara lain memakai Ubahkan
        item {
            SectionHeading(
                title = strings.toolsOtherWaysTitle,
                subtitle = strings.toolsOtherWaysSubtitle
            )
        }

        item {
            InfoFeatureCard(
                title = strings.toolShareTitle,
                description = strings.toolShareDesc,
                detail = strings.toolShareDetail,
                icon = Icons.Default.Share
            )
        }

        item {
            InfoFeatureCard(
                title = strings.toolBatchTitle,
                description = strings.toolBatchDesc,
                detail = strings.toolBatchDetail,
                icon = Icons.Default.DynamicFeed
            )
        }

        item {
            InfoFeatureCard(
                title = strings.toolOfflineTitle,
                description = strings.toolOfflineDesc,
                detail = strings.toolOfflineDetail,
                icon = Icons.Default.WifiOff
            )
        }

        item {
            InfoFeatureCard(
                title = strings.toolFormatTitle,
                description = strings.toolFormatDesc,
                detail = strings.toolFormatDetail,
                icon = Icons.Default.Extension
            )
        }
    }
}

// Saklar tampilan List/Grid: pil abu berisi dua ikon Outlined,
// pilihan aktif berupa pil putih kecil (gaya mockup UI 1.4.0).
@Composable
private fun ToolsViewToggle(
    gridMode: Boolean,
    onSelect: (Boolean) -> Unit
) {
    val strings = AppStrings.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(false, true).forEach { grid ->
            val selected = gridMode == grid
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                shadowElevation = if (selected) 1.dp else 0.dp,
                modifier = Modifier.clickable { onSelect(grid) }
            ) {
                Box(
                    modifier = Modifier.size(width = 34.dp, height = 26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (grid) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                        contentDescription = if (grid) strings.toolsViewGrid else strings.toolsViewList,
                        tint = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

// Kartu Konversi Berkas untuk mode Grid: kartu putih selebar layar
// dengan ubin ikon biru pucat, judul, deskripsi, dan chevron.
@Composable
private fun ToolConvertCard(onClick: () -> Unit) {
    val strings = AppStrings.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.toolConvertTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.toolConvertDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// Satu grup alat dalam mode Grid: kartu 2 kolom setinggi seragam.
@Composable
private fun ToolsGridGroup(
    tools: List<ToolEntry>,
    baseIndex: Int,
    onPickFileForTool: (ToolOperation, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tools.chunked(2).forEachIndexed { rowIndex, rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTools.forEachIndexed { colIndex, tool ->
                    StaggerReveal(
                        index = baseIndex + rowIndex * 2 + colIndex,
                        modifier = Modifier.weight(1f)
                    ) {
                        ToolGridCard(
                            title = tool.title,
                            description = tool.description,
                            icon = tool.icon,
                            onClick = { onPickFileForTool(tool.operation, tool.mimeType) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (rowTools.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Baris alat sesuai mockup: ikon garis 16dp di kotak abu lembut
// radius 10dp, nama Medium, deskripsi satu baris abu, chevron.
@Composable
private fun ToolRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(15.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolFeatureCard(
    title: String,
    description: String,
    supportedFormats: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeatureIcon(icon)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FormatLabel(supportedFormats)
        }
    }
}

@Composable
private fun InfoFeatureCard(
    title: String,
    description: String,
    detail: String,
    icon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeatureIcon(icon)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) AppStrings.current.toolCollapse else AppStrings.current.toolExpand,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun FeatureIcon(
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.primaryContainer
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun FormatLabel(
    formats: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = formats,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
