package com.ubahkan.app.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.scan.ScanDetector
import com.ubahkan.app.core.scan.ScanImageUtils
import com.ubahkan.app.core.scan.ScanQuad
import com.ubahkan.app.ui.theme.UbahkanBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// TAHAP 1 PINDAI: TANGKAP
// Pratinjau CameraX + analisis deteksi tepi terthrottle
// (maks sekitar 3x per detik) yang menggambar poligon
// dokumen sebagai overlay. Tombol jepret memakai
// ImageCapture; quad deteksi terakhir dipetakan ke foto
// penuh. Impor galeri memakai Photo Picker tanpa izin
// penyimpanan. Bila izin kamera ditolak, tampil pilihan
// impor galeri saja.
// ============================================================

@Composable
fun ScanCaptureStage(
    onPhoto: (android.graphics.Bitmap, ScanQuad?) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            importing = true
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    ScanImageUtils.loadBitmapFromUri(context, uri)
                }
                if (bmp != null) {
                    val detected = withContext(Dispatchers.IO) {
                        runCatching { ScanDetector.detect(bmp) }.getOrNull()
                    }
                    onPhoto(bmp, detected)
                } else {
                    Toast.makeText(context, strings.scanCaptureFailed, Toast.LENGTH_SHORT).show()
                }
                importing = false
            }
        }
    }
    val launchGallery = {
        galleryLauncher.launch(
            ActivityResultContracts.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            CameraCaptureContent(
                onPhoto = onPhoto,
                onImportGallery = launchGallery
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.scanCameraDeniedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.scanCameraDeniedDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(strings.scanAllowCamera)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = launchGallery) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.scanImportGallery)
                }
            }
        }

        if (importing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UbahkanBlue)
            }
        }
    }
}

@Composable
private fun CameraCaptureContent(
    onPhoto: (android.graphics.Bitmap, ScanQuad?) -> Unit,
    onImportGallery: () -> Unit
) {
    val strings = AppStrings.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val selector43 = remember {
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setResolutionSelector(selector43)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val providerHolder = remember { arrayOfNulls<ProcessCameraProvider>(1) }

    var normQuad by remember { mutableStateOf<List<PointF>?>(null) }
    var frameAspect by remember { mutableFloatStateOf(3f / 4f) }
    var lastDetectMs by remember { mutableLongStateOf(0L) }
    var flashOn by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var openCvReady by remember { mutableStateOf(true) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        openCvReady = withContext(Dispatchers.IO) { ScanDetector.ensureOpenCv() }
    }

    LaunchedEffect(previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = runCatching { providerFuture.get() }.getOrNull()
                ?: return@addListener
            providerHolder[0] = provider
            val preview = Preview.Builder()
                .setResolutionSelector(selector43)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(selector43)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor) { image ->
                try {
                    val now = System.currentTimeMillis()
                    if (now - lastDetectMs >= 300L) {
                        lastDetectMs = now
                        val raw = image.toBitmap()
                        val upright = ScanImageUtils.rotate(raw, image.imageInfo.rotationDegrees)
                        if (upright.height > 0) {
                            frameAspect = upright.width.toFloat() / upright.height.toFloat()
                        }
                        val detected = runCatching { ScanDetector.detect(upright) }.getOrNull()
                        normQuad = detected?.let { quad ->
                            quad.toList().map { point ->
                                PointF(
                                    point.x / upright.width.toFloat(),
                                    point.y / upright.height.toFloat()
                                )
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // Bingkai yang gagal dianalisis dilewati saja.
                } finally {
                    image.close()
                }
            }
            try {
                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    analysis
                )
                hasFlash = camera.cameraInfo.hasFlashUnit()
            } catch (_: Throwable) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { providerHolder[0]?.unbindAll() }
            analysisExecutor.shutdown()
        }
    }

    fun takePhoto() {
        if (capturing) return
        capturing = true
        imageCapture.flashMode =
            if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val raw = image.toBitmap()
                    val upright = ScanImageUtils.rotate(raw, image.imageInfo.rotationDegrees)
                    image.close()
                    val mapped = normQuad?.let { points ->
                        ScanQuad.fromNormalized(points, upright.width, upright.height)
                    }
                    capturing = false
                    onPhoto(upright, mapped)
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    Toast.makeText(context, strings.scanCaptureFailed, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
        )

        // Overlay poligon dokumen terdeteksi (biru + isi transparan)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val points = normQuad ?: return@Canvas
            if (containerSize.width == 0) return@Canvas
            val rect = fitContentRect(size.width, size.height, frameAspect)
            val path = Path().apply {
                points.forEachIndexed { index, point ->
                    val x = rect.left + point.x * rect.width
                    val y = rect.top + point.y * rect.height
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path = path, color = UbahkanBlue, alpha = 0.15f)
            drawPath(path = path, color = UbahkanBlue, style = Stroke(width = 3.dp.toPx()))
        }

        // Petunjuk di atas pratinjau
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.scanCaptureHint,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
            if (!openCvReady) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.scanOpenCvFailed,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        // Kontrol bawah: galeri, jepret, lampu kilat
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable { onImportGallery() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = strings.scanImportGallery,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable { takePhoto() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (capturing) Color.White.copy(alpha = 0.5f) else Color.White)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(enabled = hasFlash) { flashOn = !flashOn },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (flashOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    contentDescription = strings.scanFlash,
                    tint = if (hasFlash) Color.White else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/** Rect konten FIT_CENTER untuk aliran kamera tegak di dalam penampung. */
private fun fitContentRect(width: Float, height: Float, aspect: Float): Rect {
    var contentWidth = width
    var contentHeight = contentWidth / aspect
    if (contentHeight > height) {
        contentHeight = height
        contentWidth = contentHeight * aspect
    }
    val left = (width - contentWidth) / 2f
    val top = (height - contentHeight) / 2f
    return Rect(
        Offset(left, top),
        Offset(left + contentWidth, top + contentHeight)
    )
}
