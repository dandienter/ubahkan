package com.ubahkan.app.ui.screens.scan

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.scan.ScanQuad
import com.ubahkan.app.core.scan.ScanSession
import com.ubahkan.app.openFileDirectly
import com.ubahkan.app.shareFileDirectly
import com.ubahkan.app.ui.theme.UbahkanBlue
import java.io.File

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// ALUR PINDAI DOKUMEN  -  layar penuh 5 tahap:
// Tangkap -> Sesuaikan Sudut -> Filter -> Halaman -> Selesai.
// Tombol kembali bergerak mundur per tahap; dari Tangkap
// (atau Halaman) kembali membatalkan sesi. Tombol X selalu
// membatalkan seluruh sesi dan kembali ke tab asal.
// ============================================================

private enum class ScanStage { Capture, Adjust, Filter, Pages, Done }

@Composable
fun ScanFlowScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    var stage by remember { mutableStateOf(ScanStage.Capture) }
    val session = remember { ScanSession() }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var quad by remember { mutableStateOf<ScanQuad?>(null) }
    var savedFile by remember { mutableStateOf<File?>(null) }

    fun finishDone() {
        onSaved()
        onClose()
    }

    fun cancelAll() {
        session.clear()
        photo = null
        quad = null
        onClose()
    }

    fun goBack() {
        when (stage) {
            ScanStage.Capture ->
                if (session.pages.isNotEmpty()) stage = ScanStage.Pages else cancelAll()
            ScanStage.Adjust -> {
                photo = null
                quad = null
                stage = ScanStage.Capture
            }
            ScanStage.Filter -> stage = ScanStage.Adjust
            ScanStage.Pages -> cancelAll()
            ScanStage.Done -> finishDone()
        }
    }

    BackHandler { goBack() }

    val title = when (stage) {
        ScanStage.Capture -> strings.scanTitle
        ScanStage.Adjust -> strings.scanAdjustTitle
        ScanStage.Filter -> strings.scanFilterTitle
        ScanStage.Pages -> strings.scanPagesTitle
        ScanStage.Done -> strings.scanSavedTitle
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { goBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (stage != ScanStage.Done) {
                IconButton(onClick = { cancelAll() }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = strings.scanCancel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (stage) {
                ScanStage.Capture -> ScanCaptureStage(
                    onPhoto = { bmp, detectedQuad ->
                        photo = bmp
                        quad = detectedQuad
                        stage = ScanStage.Adjust
                    }
                )

                ScanStage.Adjust -> {
                    val bmp = photo
                    if (bmp == null) {
                        LaunchedEffect(Unit) { stage = ScanStage.Capture }
                    } else {
                        ScanAdjustStage(
                            bitmap = bmp,
                            initialQuad = quad,
                            onConfirm = { confirmedQuad ->
                                quad = confirmedQuad
                                stage = ScanStage.Filter
                            }
                        )
                    }
                }

                ScanStage.Filter -> {
                    val bmp = photo
                    val confirmedQuad = quad
                    if (bmp == null || confirmedQuad == null) {
                        LaunchedEffect(Unit) { stage = ScanStage.Capture }
                    } else {
                        ScanFilterStage(
                            bitmap = bmp,
                            quad = confirmedQuad,
                            onUsePage = { pageBitmap ->
                                session.addPage(pageBitmap)
                                photo = null
                                quad = null
                                stage = ScanStage.Pages
                            }
                        )
                    }
                }

                ScanStage.Pages -> ScanPagesStage(
                    session = session,
                    onAddPage = { stage = ScanStage.Capture },
                    onSavedFile = { file ->
                        savedFile = file
                        session.clear()
                        stage = ScanStage.Done
                    }
                )

                ScanStage.Done -> ScanDoneContent(
                    file = savedFile,
                    onFinish = { finishDone() }
                )
            }
        }
    }
}

// ============================================================
// TAHAP 5 PINDAI: SELESAI  -  nama berkas + lokasi simpan,
// tombol Buka dan Bagikan mengikuti pola hasil konversi.
// ============================================================

@Composable
private fun ScanDoneContent(
    file: File?,
    onFinish: () -> Unit
) {
    val strings = AppStrings.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = UbahkanBlue,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = strings.scanSavedTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.scanSavedLocation(file?.name ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(26.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { file?.let { openFileDirectly(context, it, strings) } },
                enabled = file != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.scanOpenFile)
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
                onClick = { file?.let { shareFileDirectly(context, it, strings) } },
                enabled = file != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.actionShare)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text(strings.scanDoneButton)
        }
    }
}
