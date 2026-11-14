package com.ubahkan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ubahkan.app.core.AppStrings

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// Halaman Panduan permanen: daftar seksi topik yang bisa dibuka-tutup,
// tiap seksi berisi langkah bernomor singkat, ditutup Glosarium Format
// yang penjelasannya memakai ulang formatExplain dari AppStrings.
@Composable
fun GuideScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = strings.guideBack
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.guideTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.guideSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GuideSectionCard(
                    icon = Icons.Outlined.SwapHoriz,
                    title = strings.guideConvertTitle,
                    steps = strings.guideConvertSteps,
                    initiallyExpanded = true
                )
            }
            item {
                GuideSectionCard(
                    icon = Icons.Outlined.Compress,
                    title = strings.guideCompressTitle,
                    steps = strings.guideCompressSteps
                )
            }
            item {
                GuideSectionCard(
                    icon = Icons.Outlined.ContentCut,
                    title = strings.guideSplitTitle,
                    steps = strings.guideSplitSteps
                )
            }
            item {
                GuideSectionCard(
                    icon = Icons.Outlined.DocumentScanner,
                    title = strings.scanTitle,
                    steps = strings.guideScanSteps
                )
            }
            item {
                GuideSectionCard(
                    icon = Icons.Outlined.History,
                    title = strings.guideHistoryTitle,
                    steps = strings.guideHistorySteps
                )
            }
            item {
                GuideSectionCard(
                    icon = Icons.Outlined.SystemUpdate,
                    title = strings.guideUpdateTitle,
                    steps = strings.guideUpdateSteps
                )
            }
            item {
                GuideGlossaryCard()
            }
        }
    }
}

@Composable
private fun GuideSectionCard(
    icon: ImageVector,
    title: String,
    steps: List<String>,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    steps.forEachIndexed { index, step ->
                        Row {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(22.dp)
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideGlossaryCard() {
    val strings = AppStrings.current
    var expanded by remember { mutableStateOf(false) }
    val formats = remember {
        listOf(
            "MP4", "MKV", "MOV", "WebM", "GIF",
            "MP3", "M4A", "WAV", "FLAC", "OGG", "Opus",
            "JPG", "PNG", "WebP", "BMP", "HEIC",
            "PDF", "TXT"
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Text(
                    text = strings.guideGlossaryTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    formats.forEach { format ->
                        Column {
                            Text(
                                text = format,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.formatExplain(format),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
