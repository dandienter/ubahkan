package com.ubahkan.app.ui.screens.scan

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.scan.ScanOcr
import com.ubahkan.app.core.scan.ScanPdfWriter
import com.ubahkan.app.core.scan.ScanSession
import com.ubahkan.app.ui.theme.UbahkanBlue
import kotlinx.coroutines.launch
import java.io.File

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// TAHAP 4 DAN 5 PINDAI: HALAMAN + SIMPAN PDF
// Daftar halaman sesi dengan urut ulang dan hapus, Baca Teks
// (OCR ML Kit) dengan sheet hasil + tombol Salin, Tanda
// Tangan lewat pad gambar yang ditempel ke halaman terpilih,
// dan Simpan PDF ke Downloads/Ubahkan lewat ScanPdfWriter.
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPagesStage(
    session: ScanSession,
    onAddPage: () -> Unit,
    onSavedFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var selectedId by remember { mutableStateOf<Long?>(null) }
    var ocrRunning by remember { mutableStateOf(false) }
    var showOcrSheet by remember { mutableStateOf(false) }
    var ocrResult by remember { mutableStateOf("") }
    var showSignaturePad by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val selectedPage = session.pages.firstOrNull { it.id == selectedId }

    fun runOcr() {
        if (ocrRunning || session.pages.isEmpty()) return
        val targets = selectedPage?.let { listOf(it) } ?: session.pages.toList()
        ocrRunning = true
        scope.launch {
            val texts = targets.map { page ->
                runCatching { ScanOcr.recognize(page.bitmap) }.getOrDefault("")
            }
            targets.forEachIndexed { index, page ->
                if (texts[index].isNotBlank()) page.ocrText = texts[index]
            }
            ocrResult = texts.filter { it.isNotBlank() }.joinToString("\n\n")
            ocrRunning = false
            showOcrSheet = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (session.pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.scanNoPages,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                items(session.pages, key = { it.id }) { page ->
                    val index = session.pages.indexOf(page)
                    val isSelected = page.id == selectedId
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, UbahkanBlue, RoundedCornerShape(22.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { selectedId = page.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = page.bitmap.asImageBitmap(),
                                contentDescription = strings.scanPageNumber(index + 1),
                                modifier = Modifier
                                    .size(width = 56.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.scanPageNumber(index + 1),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${page.bitmap.width} x ${page.bitmap.height}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (page.ocrText != null) {
                                    Text(
                                        text = strings.scanOcrTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = UbahkanBlue
                                    )
                                }
                            }
                            Column {
                                IconButton(
                                    onClick = { session.movePage(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowUp,
                                        contentDescription = strings.scanMoveUp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { session.movePage(index, index + 1) },
                                    enabled = index < session.pages.lastIndex,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = strings.scanMoveDown,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    session.removePage(page)
                                    if (selectedId == page.id) selectedId = null
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = strings.scanDeletePage,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            if (session.pages.isNotEmpty()) {
                Text(
                    text = strings.scanSelectPageHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddPage,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.scanAddPage)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { runOcr() },
                    enabled = session.pages.isNotEmpty() && !ocrRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TextFields,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.scanReadText)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        if (selectedPage == null) {
                            Toast.makeText(context, strings.scanSelectPageHint, Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            showSignaturePad = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Gesture,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.scanSignature)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    if (saving || session.pages.isEmpty()) return@Button
                    saving = true
                    scope.launch {
                        val file = ScanPdfWriter.saveToDownloads(
                            context, session.pages.map { it.bitmap }
                        )
                        saving = false
                        if (file != null) {
                            onSavedFile(file)
                        } else {
                            Toast.makeText(
                                context, strings.scanSaveFailedToast, Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                enabled = session.pages.isNotEmpty() && !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(strings.scanProcessing)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.scanSavePdf)
                }
            }
        }
    }

    if (ocrRunning) {
        Dialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = UbahkanBlue)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = strings.scanOcrRunning,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showOcrSheet) {
        ModalBottomSheet(onDismissRequest = { showOcrSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = strings.scanOcrTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = ocrResult.ifBlank { strings.scanOcrEmpty },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(ocrResult))
                            Toast.makeText(context, strings.scanCopiedToast, Toast.LENGTH_SHORT)
                                .show()
                        },
                        enabled = ocrResult.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.scanCopyText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showOcrSheet = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text(strings.btnClose)
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    if (showSignaturePad) {
        val target = selectedPage
        SignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onDone = { signature ->
                showSignaturePad = false
                if (target != null) {
                    val composited = compositeSignature(target.bitmap, signature)
                    val index = session.pages.indexOf(target)
                    if (index >= 0) {
                        session.pages[index] = target.copy(bitmap = composited)
                    }
                    Toast.makeText(
                        context, strings.scanSignatureAppliedToast, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

/** Tempel tanda tangan transparan ke kanan bawah halaman (28% lebar halaman). */
private fun compositeSignature(page: Bitmap, signature: Bitmap): Bitmap {
    val result = page.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(result)
    val targetWidth = page.width * 0.28f
    val targetHeight = targetWidth * signature.height.toFloat() / signature.width.toFloat()
    val margin = page.width * 0.04f
    val dest = RectF(
        page.width - targetWidth - margin,
        page.height - targetHeight - margin,
        page.width - margin,
        page.height - margin
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    canvas.drawBitmap(signature, null, dest, paint)
    return result
}

// ============================================================
// PAD TANDA TANGAN  -  kanvas gambar bebas di atas latar
// transparan. Hasil dirender ke bitmap 1000x500 dengan
// latar transparan penuh.
// ============================================================

@Composable
private fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onDone: (Bitmap) -> Unit
) {
    val strings = AppStrings.current
    val paths = remember { mutableStateListOf<Path>() }
    var strokeVersion by remember { mutableIntStateOf(0) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = strings.scanSignatureTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.scanSignatureHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .onSizeChanged { padSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    val path = Path().apply { moveTo(start.x, start.y) }
                                    currentPath = path
                                    paths.add(path)
                                    strokeVersion++
                                },
                                onDrag = { change, _ ->
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    strokeVersion++
                                },
                                onDragEnd = { currentPath = null }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        @Suppress("UNUSED_EXPRESSION")
                        strokeVersion
                        paths.forEach { path ->
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            paths.clear()
                            strokeVersion++
                        }
                    ) {
                        Text(strings.scanSignatureClear)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(strings.btnCancel)
                    }
                    Button(
                        onClick = {
                            if (paths.isNotEmpty() && padSize.width > 0 && padSize.height > 0) {
                                onDone(renderSignature(paths, padSize))
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text(strings.scanDoneButton)
                    }
                }
            }
        }
    }
}

private fun renderSignature(paths: List<Path>, padSize: IntSize): Bitmap {
    val width = 1000
    val height = 500
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 9f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val scaleX = width.toFloat() / padSize.width.toFloat()
    val scaleY = height.toFloat() / padSize.height.toFloat()
    val matrix = android.graphics.Matrix().apply { setScale(scaleX, scaleY) }
    paths.forEach { composePath ->
        val androidPath = android.graphics.Path(composePath.asAndroidPath())
        androidPath.transform(matrix)
        canvas.drawPath(androidPath, paint)
    }
    return bitmap
}
