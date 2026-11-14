package com.ubahkan.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.ubahkan.app.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

object UbahkanEngine {

    private const val TAG = "UbahkanEngine"

    sealed class ConvertState {
        object Idle : ConvertState()
        data class Converting(val format: String, val progress: Int) : ConvertState()
        object Success : ConvertState()
        data class Error(val message: String, val originalUri: Uri? = null, val targetFormat: String = "") : ConvertState()
    }

    val currentStatus = MutableStateFlow<ConvertState>(ConvertState.Idle)

    // Managed Coroutine Scope tied to the Engine lifecycle
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    suspend fun executeConversion(
        context: Context,
        inputUri: Uri,
        outputExtension: String,
        originalFileName: String,
        qualityPreset: Int,
        onProgress: (String, Int) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase()
        val targetExt = outputExtension.trim().removePrefix(".").lowercase()
        activeJob = coroutineContext[Job]

        val resultDeferred = CompletableDeferred<String>()

        try {
            when {
                // Route 1: PDF Document Input
                inputExt == "pdf" && targetExt in listOf("jpg", "jpeg", "png", "webp") -> {
                    convertPdfToImages(context, inputUri, targetExt, originalFileName, qualityPreset, onProgress,
                        { path -> resultDeferred.complete(path) },
                        { err -> resultDeferred.completeExceptionally(IOException(err)) }
                    )
                }
                inputExt == "pdf" && targetExt == "pdf" -> {
                    compressPdf(context, inputUri, originalFileName, qualityPreset, onProgress,
                        { path -> resultDeferred.complete(path) },
                        { err -> resultDeferred.completeExceptionally(IOException(err)) }
                    )
                }

                // Route 2: Image to PDF
                targetExt == "pdf" && inputExt in listOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif") -> {
                    convertImageToPdf(context, inputUri, originalFileName, onProgress,
                        { path -> resultDeferred.complete(path) },
                        { err -> resultDeferred.completeExceptionally(IOException(err)) }
                    )
                }

                // Route 3: Vector SVG to Bitmap Image
                inputExt == "svg" && targetExt in listOf("png", "jpg", "jpeg", "webp") -> {
                    convertSvgToImage(context, inputUri, targetExt, originalFileName, qualityPreset, onProgress,
                        { path -> resultDeferred.complete(path) },
                        { err -> resultDeferred.completeExceptionally(IOException(err)) }
                    )
                }

                // Route 4: E-Book EPUB to TXT
                inputExt == "epub" && targetExt == "txt" -> {
                    convertEpubToTxt(context, inputUri, originalFileName, onProgress,
                        { path -> resultDeferred.complete(path) },
                        { err -> resultDeferred.completeExceptionally(IOException(err)) }
                    )
                }

                // Route 5: Native Raster Image Converter
                inputExt in listOf("png", "jpg", "jpeg", "webp", "bmp") &&
                    targetExt in listOf("png", "jpg", "jpeg", "webp", "bmp") -> {
                    convertImageNatively(context, inputUri, targetExt, originalFileName, qualityPreset, onProgress,
                        { path -> resultDeferred.complete(path) },
                        { err -> resultDeferred.completeExceptionally(IOException(err)) }
                    )
                }

                // Route 6: Multimedia (Video & Audio) via FFmpeg Engine
                else -> {
                    convertMediaWithFFmpeg(context, inputUri, inputExt, targetExt, originalFileName, qualityPreset, onProgress,
                        { path -> resultDeferred.complete(path) },
                        { err -> resultDeferred.completeExceptionally(IOException(err)) }
                    )
                }
            }

            resultDeferred.await()
        } finally {
            if (activeJob == coroutineContext[Job]) {
                activeJob = null
            }
            withContext(NonCancellable) {
                currentStatus.value = ConvertState.Idle
            }
        }
    }

    fun convert(
        context: Context,
        inputUri: Uri,
        outputExtension: String,
        originalFileName: String,
        qualityPreset: Int, // 0: Tinggi, 1: Sedang, 2: Rendah, 3: Instan (Lossless)
        onProgress: (String, Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val targetExt = outputExtension.trim().removePrefix(".").lowercase()

        // Cancel any pending conversion job before starting a new one
        activeJob?.cancel()

        activeJob = engineScope.launch {
            try {
                val finalPath = executeConversion(
                    context = context,
                    inputUri = inputUri,
                    outputExtension = targetExt,
                    originalFileName = originalFileName,
                    qualityPreset = qualityPreset,
                    onProgress = onProgress
                )
                withContext(Dispatchers.Main) {
                    currentStatus.value = ConvertState.Success
                    onSuccess(finalPath)
                }
            } catch (e: CancellationException) {
                withContext(Dispatchers.Main) {
                    currentStatus.value = ConvertState.Idle
                    onError(AppStrings.get().engineCancelled)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Conversion setup error", e)
                withContext(Dispatchers.Main) {
                    val errMsg = AppStrings.get().engineErrSetup(e.message ?: "")
                    currentStatus.value = ConvertState.Error(errMsg, inputUri, targetExt)
                    onError(errMsg)
                }
            }
        }
    }

    // --- 1. MEDIA CONVERTER (FFMPEG) ---
    private suspend fun convertMediaWithFFmpeg(
        context: Context,
        inputUri: Uri,
        inputExt: String,
        targetExt: String,
        originalFileName: String,
        qualityPreset: Int,
        onProgress: (String, Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val strings = AppStrings.get()
        val tempInput = OutputManager.createTempFile(context, "in_media", inputExt)
        val tempOutput = OutputManager.createTempFile(context, "out_media", targetExt)

        try {
            updateProgress(targetExt, strings.enginePreparingMedia, 5, onProgress)

            // Copy input stream to temp cache file
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempInput).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException(strings.engineErrReadInput)

            if (!tempInput.exists() || tempInput.length() <= 0) {
                throw IOException(strings.engineErrInputEmpty)
            }

            // Probe media information via FFprobe
            val mediaInfo = FFprobeKit.getMediaInformation(tempInput.absolutePath).mediaInformation
            val totalDurationStr = mediaInfo?.duration
            val totalDurationMs = (totalDurationStr?.toDoubleOrNull()?.times(1000))?.toLong() ?: 0L

            val originalBitrateBps = if (totalDurationMs > 0) {
                ((tempInput.length() * 8.0) / (totalDurationMs / 1000.0)).toLong()
            } else {
                4000000L
            }
            val videoBitrateBps = (originalBitrateBps * 0.9).toLong().coerceIn(400000L, 20000000L)

            // Video rotation check
            val videoRotation = getVideoRotation(mediaInfo)

            // Lossless / Stream Copy validation
            val isLosslessSupported = ConversionCapabilityManager.isLosslessCompatible(inputExt, targetExt)
            // Crucial: If video has rotation metadata and target is MKV or WebM, stream copy causes unrotated orientation on Android
            val isLosslessCapable = isLosslessSupported && !(videoRotation != 0 && targetExt in listOf("mkv", "webm"))

            val effectivePreset = if (qualityPreset == 3 && !isLosslessCapable) {
                0 // Fallback to High if format doesn't support stream copying
            } else {
                qualityPreset
            }

            val qualityArgs = buildFFmpegQualityArgs(inputExt, targetExt, effectivePreset, videoBitrateBps, videoRotation)
            val command = "-y -i \"${tempInput.absolutePath}\" $qualityArgs \"${tempOutput.absolutePath}\""

            val ffmpegSession = CompletableDeferred<ReturnCode>()

            val session = FFmpegKit.executeAsync(
                command,
                { s -> ffmpegSession.complete(s.returnCode) },
                { log -> Log.d(TAG, "[FFmpeg] ${log.message}") },
                { stats ->
                    if (!ffmpegSession.isCompleted) {
                        if (totalDurationMs > 0) {
                            val percentage = ((stats.time.toFloat() / totalDurationMs) * 95).toInt().coerceIn(5, 95)
                            currentStatus.value = ConvertState.Converting(targetExt, percentage)
                            onProgress(strings.engineProcessedPercent(percentage), percentage)
                        } else {
                            val seconds = (stats.time / 1000).toLong()
                            currentStatus.value = ConvertState.Converting(targetExt, 50)
                            onProgress(strings.engineProcessedSeconds(seconds), 50)
                        }
                    }
                }
            )

            val returnCode = ffmpegSession.await()

            if (ReturnCode.isSuccess(returnCode)) {
                updateProgress(targetExt, strings.engineFinalizingValidating, 98, onProgress)

                // Validate output file
                when (val valResult = ConversionValidator.validateFile(tempOutput, targetExt)) {
                    is ConversionValidator.ValidationResult.Invalid -> {
                        OutputManager.cleanTempFiles(tempInput, tempOutput)
                        withContext(Dispatchers.Main) {
                            val errMsg = strings.engineErrInvalidOutput(valResult.reason)
                            currentStatus.value = ConvertState.Error(errMsg, inputUri, targetExt)
                            onError(errMsg)
                        }
                    }
                    is ConversionValidator.ValidationResult.Valid -> {
                        val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, targetExt)
                        OutputManager.cleanTempFiles(tempInput)
                        withContext(Dispatchers.Main) {
                            if (finalPath != null) {
                                currentStatus.value = ConvertState.Success
                                onProgress(strings.engineFinished100, 100)
                                onSuccess(finalPath)
                            } else {
                                currentStatus.value = ConvertState.Error(strings.engineErrSaveOutput, inputUri, targetExt)
                                onError(strings.engineErrSaveOutput)
                            }
                        }
                    }
                }
            } else if (ReturnCode.isCancel(returnCode)) {
                OutputManager.cleanTempFiles(tempInput, tempOutput)
                withContext(Dispatchers.Main) {
                    currentStatus.value = ConvertState.Idle
                    onError(strings.engineCancelled)
                }
            } else {
                val failLogs = session.allLogsAsString ?: ""
                OutputManager.cleanTempFiles(tempInput, tempOutput)
                val errorMessage = when {
                    qualityPreset == 3 ->
                        strings.engineErrStreamCopyIncompatible
                    failLogs.contains("does not support", ignoreCase = true) ->
                        strings.engineErrFormatNotSupported
                    else ->
                        strings.engineErrFFmpeg(returnCode.value)
                }
                withContext(Dispatchers.Main) {
                    currentStatus.value = ConvertState.Error(errorMessage, inputUri, targetExt)
                    onError(errorMessage)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, tempOutput)
            throw e
        }
    }

    private fun getVideoRotation(mediaInfo: com.arthenica.ffmpegkit.MediaInformation?): Int {
        if (mediaInfo == null) return 0
        val videoStream = mediaInfo.streams?.firstOrNull { it.type.equals("video", ignoreCase = true) } ?: return 0

        // 1. Check stream tags ("rotate")
        val tags = videoStream.tags
        if (tags != null) {
            val tagRotate = tags.optString("rotate", "").toIntOrNull()
                ?: if (tags.has("rotate")) tags.optInt("rotate", 0) else null
            if (tagRotate != null && tagRotate != 0) {
                return ((tagRotate % 360) + 360) % 360
            }
        }

        // 2. Check side_data_list in stream properties
        try {
            val allProps = videoStream.allProperties
            val sideDataList = allProps?.optJSONArray("side_data_list")
            if (sideDataList != null) {
                for (i in 0 until sideDataList.length()) {
                    val sideData = sideDataList.optJSONObject(i)
                    if (sideData != null && sideData.has("rotation")) {
                        val rot = sideData.optInt("rotation", 0)
                        if (rot != 0) {
                            return ((rot % 360) + 360) % 360
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membaca rotasi video dari side_data_list", e)
        }

        return 0
    }

    private fun buildFFmpegQualityArgs(
        inputExt: String,
        targetExt: String,
        preset: Int,
        videoBitrateBps: Long,
        videoRotation: Int = 0
    ): String {
        // Stream Copy (Lossless Remuxing)
        if (preset == 3) {
            return when (targetExt) {
                "mp4", "mov" -> "-c copy -sn -dn -movflags +faststart"
                "mkv" -> "-c copy -sn -dn"
                "m4a", "aac" -> "-vn -sn -dn -c:a copy"
                else -> "-c copy"
            }
        }

        // Target: Animated GIF
        if (targetExt == "gif") {
            val fps = when (preset) {
                0 -> 15
                1 -> 12
                else -> 10
            }
            val maxDim = when (preset) {
                0 -> 480
                1 -> 360
                else -> 240
            }
            val rotateArg = if (videoRotation != 0) "-metadata:s:v rotate=0 " else ""
            val filterGraph = "fps=$fps,scale='if(gt(iw,ih),$maxDim,-2)':'if(gt(iw,ih),-2,$maxDim)':flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse"
            return "-an -sn -dn $rotateArg-vf \"$filterGraph\""
        }

        // Target: WebM
        if (targetExt == "webm") {
            val rotateArg = if (videoRotation != 0) "-metadata:s:v rotate=0 " else ""
            return when (preset) {
                0 -> "-c:v libvpx-vp9 -b:v $videoBitrateBps -deadline realtime -cpu-used 4 -c:a libopus -b:a 192k $rotateArg"
                1 -> {
                    val halfBv = (videoBitrateBps * 0.6).toLong()
                    "-c:v libvpx-vp9 -b:v $halfBv -deadline realtime -cpu-used 4 -c:a libopus -b:a 128k $rotateArg"
                }
                2 -> {
                    val lowBv = (videoBitrateBps * 0.35).toLong()
                    val scaleFilter = "scale='if(gt(iw,ih),854,-2)':'if(gt(iw,ih),-2,854)'"
                    "-vf \"$scaleFilter\" -c:v libvpx-vp9 -b:v $lowBv -deadline realtime -cpu-used 4 -c:a libopus -b:a 96k $rotateArg"
                }
                else -> "-c:v libvpx-vp9 -b:v $videoBitrateBps -deadline realtime -cpu-used 4 -c:a libopus -b:a 128k $rotateArg"
            }
        }

        // Target: Standard Video Containers (MP4, MKV, MOV, AVI)
        if (targetExt in listOf("mp4", "mkv", "mov", "avi")) {
            val rotateArg = if (videoRotation != 0) "-metadata:s:v rotate=0 " else ""
            val fastStartArg = if (targetExt in listOf("mp4", "mov")) "-movflags +faststart " else ""
            return when (preset) {
                0 -> "$fastStartArg-b:v $videoBitrateBps -b:a 256k $rotateArg"
                1 -> {
                    val targetBv = (videoBitrateBps * 0.6).toLong()
                    "$fastStartArg-b:v $targetBv -b:a 160k $rotateArg"
                }
                2 -> {
                    val targetBv = (videoBitrateBps * 0.35).toLong()
                    val scaleFilter = "scale='if(gt(iw,ih),854,-2)':'if(gt(iw,ih),-2,854)'"
                    "$fastStartArg-vf \"$scaleFilter\" -b:v $targetBv -b:a 96k $rotateArg"
                }
                else -> "$fastStartArg-b:v $videoBitrateBps -b:a 256k $rotateArg"
            }
        }

        // Target: Audio Formats
        val audioCommon = "-vn -sn -dn"
        return when (targetExt) {
            "wav" -> "$audioCommon -c:a pcm_s16le"
            "flac" -> "$audioCommon -c:a flac"
            "mp3" -> {
                val b = when (preset) {
                    0 -> "320k"
                    1 -> "192k"
                    2 -> "128k"
                    else -> "256k"
                }
                "$audioCommon -c:a libmp3lame -b:a $b"
            }
            "aac", "m4a" -> {
                val b = when (preset) {
                    0 -> "256k"
                    1 -> "192k"
                    2 -> "128k"
                    else -> "192k"
                }
                val faststart = if (targetExt == "m4a") " -movflags +faststart" else ""
                "$audioCommon -c:a aac -b:a $b$faststart"
            }
            "opus" -> {
                val b = when (preset) {
                    0 -> "160k"
                    1 -> "128k"
                    2 -> "96k"
                    else -> "128k"
                }
                "$audioCommon -c:a libopus -b:a $b"
            }
            "ogg" -> {
                val b = when (preset) {
                    0 -> "256k"
                    1 -> "160k"
                    2 -> "112k"
                    else -> "160k"
                }
                "$audioCommon -c:a libvorbis -b:a $b"
            }
            else -> audioCommon
        }
    }

    // --- 2. PDF TO IMAGES ---
    private suspend fun convertPdfToImages(
        context: Context,
        inputUri: Uri,
        targetExtension: String,
        originalFileName: String,
        qualityPreset: Int,
        onProgress: (String, Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val strings = AppStrings.get()
        val tempPdf = OutputManager.createTempFile(context, "in_pdf", "pdf")
        val createdTempFiles = mutableListOf<File>()

        try {
            updateProgress(targetExtension, strings.engineReadingPdf, 10, onProgress)

            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempPdf).use { output -> input.copyTo(output) }
            } ?: throw IOException(strings.engineErrPdfRead)

            val pfd = ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            if (pageCount == 0) {
                renderer.close()
                pfd.close()
                throw IllegalStateException(strings.engineErrPdfNoPages)
            }

            val scale = when (qualityPreset) {
                0, 3 -> 2.0f // High / Lossless (144+ DPI)
                1    -> 1.5f // Medium
                2    -> 1.0f // Low (72 DPI)
                else -> 1.5f
            }

            val format = if (targetExtension == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val qualityVal = when (qualityPreset) {
                0, 3 -> 100
                1    -> 85
                2    -> 65
                else -> 85
            }

            val baseName = originalFileName.substringBeforeLast('.', "doc")
            var lastSavedPath: String? = null

            for (i in 0 until pageCount) {
                currentCoroutineContext().ensureActive()

                val percent = (((i + 1).toFloat() / pageCount) * 85).toInt() + 10
                updateProgress(targetExtension, strings.engineRenderingPage(i + 1, pageCount), percent, onProgress)

                val page = renderer.openPage(i)
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE) // PDF pages need opaque white background

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageSuffix = if (pageCount > 1) "_hal_${i + 1}" else ""
                val pageFileName = "$baseName$pageSuffix.$targetExtension"
                val tempPageFile = OutputManager.createTempFile(context, "pdf_page_$i", targetExtension)
                createdTempFiles.add(tempPageFile)

                FileOutputStream(tempPageFile).use { fos ->
                    bitmap.compress(format, qualityVal, fos)
                }
                bitmap.recycle()

                val savedPath = OutputManager.finalizeOutput(context, tempPageFile, pageFileName, targetExtension)
                if (savedPath != null) lastSavedPath = savedPath
            }

            renderer.close()
            pfd.close()
            OutputManager.cleanTempFiles(tempPdf, *createdTempFiles.toTypedArray())

            withContext(Dispatchers.Main) {
                if (lastSavedPath != null) {
                    currentStatus.value = ConvertState.Success
                    val msg = if (pageCount > 1) strings.enginePagesExtractedSuccess(pageCount) else lastSavedPath
                    onSuccess(msg)
                } else {
                    currentStatus.value = ConvertState.Error(strings.engineErrSavePdfImage, inputUri, targetExtension)
                    onError(strings.engineErrSavePdfImage)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempPdf, *createdTempFiles.toTypedArray())
            throw e
        }
    }

    // --- 3. PDF COMPRESSOR ---
    private suspend fun compressPdf(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        qualityPreset: Int,
        onProgress: (String, Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val strings = AppStrings.get()
        val tempInput = OutputManager.createTempFile(context, "comp_in", "pdf")
        val tempOutput = OutputManager.createTempFile(context, "comp_out", "pdf")

        try {
            updateProgress("pdf", strings.engineAnalyzingPdf, 10, onProgress)

            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempInput).use { output -> input.copyTo(output) }
            } ?: throw IOException(strings.engineErrPdfRead)

            val pfd = ParcelFileDescriptor.open(tempInput, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            if (pageCount == 0) {
                renderer.close()
                pfd.close()
                throw IllegalStateException(strings.engineErrPdfEmpty)
            }

            val (scale, jpegQuality) = when (qualityPreset) {
                0 -> Pair(1.0f, 85)
                1 -> Pair(0.8f, 65)
                2 -> Pair(0.6f, 45)
                3 -> Pair(1.0f, 95)
                else -> Pair(0.8f, 65)
            }

            val pdfDocument = PdfDocument()

            for (i in 0 until pageCount) {
                currentCoroutineContext().ensureActive()

                val page = renderer.openPage(i)
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
                val compressedBytes = baos.toByteArray()
                val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                bitmap.recycle()

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                docPage.canvas.drawBitmap(compressedBitmap, 0f, 0f, null)
                pdfDocument.finishPage(docPage)
                compressedBitmap.recycle()

                val percent = (((i + 1).toFloat() / pageCount) * 80).toInt() + 10
                updateProgress("pdf", strings.engineCompressingPage(i + 1, pageCount), percent, onProgress)
            }

            renderer.close()
            pfd.close()

            FileOutputStream(tempOutput).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, "pdf")
            OutputManager.cleanTempFiles(tempInput, tempOutput)

            withContext(Dispatchers.Main) {
                if (finalPath != null) {
                    currentStatus.value = ConvertState.Success
                    onSuccess(finalPath)
                } else {
                    currentStatus.value = ConvertState.Error(strings.engineErrSavePdfCompress, inputUri, "pdf")
                    onError(strings.engineErrSavePdfCompress)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, tempOutput)
            throw e
        }
    }

    // --- 4. IMAGE TO PDF ---
    private suspend fun convertImageToPdf(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        onProgress: (String, Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val strings = AppStrings.get()
        val tempOutput = OutputManager.createTempFile(context, "img_pdf", "pdf")
        try {
            updateProgress("pdf", strings.engineLoadingImage, 30, onProgress)

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, inputUri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, inputUri)
            }

            updateProgress("pdf", strings.engineBuildingPdfPages, 60, onProgress)

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
            bitmap.recycle()

            FileOutputStream(tempOutput).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, "pdf")
            OutputManager.cleanTempFiles(tempOutput)

            withContext(Dispatchers.Main) {
                if (finalPath != null) {
                    currentStatus.value = ConvertState.Success
                    onSuccess(finalPath)
                } else {
                    currentStatus.value = ConvertState.Error(strings.engineErrImageToPdf, inputUri, "pdf")
                    onError(strings.engineErrImageToPdf)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempOutput)
            throw e
        }
    }

    suspend fun mergeImagesToPdf(
        context: Context,
        inputUris: List<Uri>,
        originalFileName: String,
        qualityPreset: Int = 0,
        onProgress: (String, Int) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val strings = AppStrings.get()
        val tempOutput = OutputManager.createTempFile(context, "img_merge_pdf", "pdf")
        val pdfDocument = PdfDocument()

        try {
            val total = inputUris.size
            for ((index, uri) in inputUris.withIndex()) {
                currentCoroutineContext().ensureActive()
                val percent = ((index.toFloat() / total) * 85).toInt() + 5
                updateProgress("pdf", strings.engineBuildingPdfPage(index + 1, total), percent, onProgress)

                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            updateProgress("pdf", strings.engineSavingPdfDoc, 95, onProgress)

            FileOutputStream(tempOutput).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, "pdf")
            OutputManager.cleanTempFiles(tempOutput)

            if (finalPath != null) {
                withContext(Dispatchers.Main) {
                    currentStatus.value = ConvertState.Success
                }
                finalPath
            } else {
                throw IOException(strings.engineErrSaveMergedPdf)
            }
        } catch (e: Exception) {
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
            OutputManager.cleanTempFiles(tempOutput)
            withContext(Dispatchers.Main) {
                val errMsg = strings.engineErrMergePdf(e.message ?: "")
                currentStatus.value = ConvertState.Error(errMsg, inputUris.firstOrNull(), "pdf")
            }
            throw e
        }
    }

    // --- 5. NATIVE IMAGE CONVERTER ---
    private suspend fun convertImageNatively(
        context: Context,
        inputUri: Uri,
        extension: String,
        originalFileName: String,
        qualityPreset: Int,
        onProgress: (String, Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val strings = AppStrings.get()
        val targetExt = extension.trim().removePrefix(".").lowercase()
        val tempOutput = OutputManager.createTempFile(context, "img_out", targetExt)

        try {
            updateProgress(targetExt, strings.engineProcessingImage, 40, onProgress)

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, inputUri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, inputUri)
            }

            updateProgress(targetExt, strings.engineCompressingImage, 70, onProgress)

            if (targetExt == "bmp") {
                BmpWriter.writeBitmapAsBmp(bitmap, tempOutput)
                bitmap.recycle()
            } else {
                val isJpg = targetExt == "jpg" || targetExt == "jpeg"
                val finalBitmap = if (isJpg && bitmap.hasAlpha()) {
                    // Composite alpha over solid white background to prevent black distortion
                    val opaqueBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(opaqueBitmap)
                    canvas.drawColor(Color.WHITE)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    bitmap.recycle()
                    opaqueBitmap
                } else {
                    bitmap
                }

                val format = when (targetExt) {
                    "png" -> Bitmap.CompressFormat.PNG
                    "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                    }
                    else -> Bitmap.CompressFormat.JPEG
                }

                val qualitySetting = when (qualityPreset) {
                    0, 3 -> 100 // High / Lossless
                    1    -> 85  // Medium
                    2    -> 65  // Low
                    else -> 90
                }

                FileOutputStream(tempOutput).use { fos ->
                    finalBitmap.compress(format, qualitySetting, fos)
                }
                finalBitmap.recycle()
            }

            val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, targetExt)
            OutputManager.cleanTempFiles(tempOutput)

            withContext(Dispatchers.Main) {
                if (finalPath != null) {
                    currentStatus.value = ConvertState.Success
                    onSuccess(finalPath)
                } else {
                    currentStatus.value = ConvertState.Error(strings.engineErrSaveImage, inputUri, targetExt)
                    onError(strings.engineErrSaveImage)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempOutput)
            throw e
        }
    }

    // --- 6. SVG CONVERTER ---
    private suspend fun convertSvgToImage(
        context: Context,
        inputUri: Uri,
        targetExtension: String,
        originalFileName: String,
        qualityPreset: Int,
        onProgress: (String, Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        var tempSvgOut: File? = null
        try {
            tempSvgOut = SvgConverter.convertSvg(
                context = context,
                inputUri = inputUri,
                targetExtension = targetExtension,
                qualityPreset = qualityPreset,
                onProgress = { step, percent ->
                    updateProgress(targetExtension, step, percent, onProgress)
                }
            )

            val finalPath = OutputManager.finalizeOutput(context, tempSvgOut, originalFileName, targetExtension)
            OutputManager.cleanTempFiles(tempSvgOut)

            withContext(Dispatchers.Main) {
                if (finalPath != null) {
                    currentStatus.value = ConvertState.Success
                    onSuccess(finalPath)
                } else {
                    val errMsg = AppStrings.get().engineErrSaveSvg
                    currentStatus.value = ConvertState.Error(errMsg, inputUri, targetExtension)
                    onError(errMsg)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempSvgOut)
            throw e
        }
    }

    // --- 7. EPUB TO TXT ---
    private suspend fun convertEpubToTxt(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        onProgress: (String, Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val strings = AppStrings.get()
        val tempEpub = OutputManager.createTempFile(context, "in_epub", "epub")
        val tempTxt = OutputManager.createTempFile(context, "out_txt", "txt")

        try {
            updateProgress("txt", strings.engineReadingEpub, 10, onProgress)

            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempEpub).use { output -> input.copyTo(output) }
            } ?: throw IOException(strings.engineErrReadEpub)

            val extractedText = EpubParser.parseEpubToText(tempEpub) { step, percent ->
                updateProgress("txt", step, percent, onProgress)
            }

            updateProgress("txt", strings.engineSavingEpubTxt, 95, onProgress)
            tempTxt.writeText(extractedText, Charsets.UTF_8)

            val finalPath = OutputManager.finalizeOutput(context, tempTxt, originalFileName, "txt")
            OutputManager.cleanTempFiles(tempEpub, tempTxt)

            withContext(Dispatchers.Main) {
                if (finalPath != null) {
                    currentStatus.value = ConvertState.Success
                    onSuccess(finalPath)
                } else {
                    currentStatus.value = ConvertState.Error(strings.engineErrSaveTxt, inputUri, "txt")
                    onError(strings.engineErrSaveTxt)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempEpub, tempTxt)
            throw e
        }
    }

    // --- 8. TOOL OPERATIONS (Batch 1: kompres target, potong, potong otomatis) ---

    suspend fun executeToolOperation(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        operation: ToolOperation,
        targetSizeBytes: Long = 0L,
        trimStartSec: Double = 0.0,
        trimEndSec: Double = 0.0,
        splitSeconds: Int = 0,
        pdfPageRange: String = "",
        cropRatio: String = "",
        resizeHeight: Int = 0,
        extraInputUris: List<Uri> = emptyList(),
        extraFileNames: List<String> = emptyList(),
        onProgress: (String, Int) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        activeJob = coroutineContext[Job]
        try {
            when (operation) {
                ToolOperation.COMPRESS_VIDEO -> compressVideoToTargetSize(
                    context, inputUri, originalFileName, targetSizeBytes, onProgress
                )
                ToolOperation.COMPRESS_IMAGE -> compressImageToTargetSize(
                    context, inputUri, originalFileName, targetSizeBytes, onProgress
                )
                ToolOperation.TRIM_VIDEO -> trimVideo(
                    context, inputUri, originalFileName, trimStartSec, trimEndSec, onProgress
                )
                ToolOperation.AUTO_SPLIT -> splitVideoAuto(
                    context, inputUri, originalFileName, splitSeconds, onProgress
                )
                ToolOperation.MERGE_VIDEO -> mergeVideos(
                    context, inputUri, originalFileName, extraInputUris, extraFileNames, onProgress
                )
                ToolOperation.SPLIT_PDF -> splitPdf(
                    context, inputUri, originalFileName, pdfPageRange, onProgress
                )
                ToolOperation.MERGE_PDF -> mergePdfs(
                    context, inputUri, originalFileName, extraInputUris, onProgress
                )
                ToolOperation.HARDSUB -> burnSubtitles(
                    context, inputUri, originalFileName, extraInputUris, extraFileNames, onProgress
                )
                ToolOperation.CROP_VIDEO -> cropVideo(
                    context, inputUri, originalFileName, cropRatio, onProgress
                )
                ToolOperation.RESIZE_VIDEO -> resizeVideo(
                    context, inputUri, originalFileName, resizeHeight, onProgress
                )
                ToolOperation.MUTE_VIDEO -> muteVideo(
                    context, inputUri, originalFileName, onProgress
                )
                ToolOperation.REPLACE_AUDIO -> replaceVideoAudio(
                    context, inputUri, originalFileName, extraInputUris, extraFileNames, onProgress
                )
                ToolOperation.HEIC_TO_JPG -> convertHeicToJpg(
                    context, inputUri, originalFileName, onProgress
                )
                ToolOperation.FILE_INFO -> throw IllegalArgumentException(
                    "Operasi FILE_INFO hanya untuk tampilan info, bukan antrean"
                )
                ToolOperation.NORMALIZE_VOLUME -> normalizeVolume(
                    context, inputUri, originalFileName, onProgress
                )
                ToolOperation.CONVERT -> throw IllegalArgumentException(
                    "Operasi CONVERT harus lewat executeConversion"
                )
            }
        } finally {
            if (activeJob == coroutineContext[Job]) {
                activeJob = null
            }
            withContext(NonCancellable) {
                currentStatus.value = ConvertState.Idle
            }
        }
    }

    // Salin berkas dari Uri ke berkas temp cache, sama seperti pola route konversi lain
    private fun copyInputToTempFile(
        context: Context,
        inputUri: Uri,
        prefix: String,
        inputExt: String
    ): File {
        val strings = AppStrings.get()
        val tempInput = OutputManager.createTempFile(context, prefix, inputExt)
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            FileOutputStream(tempInput).use { output -> input.copyTo(output) }
        } ?: throw IOException(strings.engineErrReadInput)
        if (!tempInput.exists() || tempInput.length() <= 0) {
            throw IOException(strings.engineErrInputEmpty)
        }
        return tempInput
    }

    // Durasi media dalam detik lewat FFprobeKit, 0.0 bila tidak terbaca
    private fun probeDurationSeconds(mediaFile: File): Double {
        val mediaInfo = FFprobeKit.getMediaInformation(mediaFile.absolutePath).mediaInformation
        return mediaInfo?.duration?.toDoubleOrNull() ?: 0.0
    }

    // Jalankan satu perintah FFmpeg dan tunggu hasilnya, dengan progres berbasis durasi
    private suspend fun runFfmpegCommand(
        command: String,
        totalDurationMs: Long,
        statusFormat: String,
        strings: Strings,
        onProgress: (String, Int) -> Unit,
        reportStats: Boolean = true
    ): ReturnCode {
        val ffmpegSession = CompletableDeferred<ReturnCode>()
        FFmpegKit.executeAsync(
            command,
            { s -> ffmpegSession.complete(s.returnCode) },
            { log -> Log.d(TAG, "[FFmpeg] ${log.message}") },
            { stats ->
                if (reportStats && !ffmpegSession.isCompleted) {
                    if (totalDurationMs > 0) {
                        val percentage = ((stats.time.toFloat() / totalDurationMs) * 95).toInt().coerceIn(5, 95)
                        currentStatus.value = ConvertState.Converting(statusFormat, percentage)
                        onProgress(strings.engineProcessedPercent(percentage), percentage)
                    } else {
                        val seconds = (stats.time / 1000).toLong()
                        currentStatus.value = ConvertState.Converting(statusFormat, 50)
                        onProgress(strings.engineProcessedSeconds(seconds), 50)
                    }
                }
            }
        )
        return ffmpegSession.await()
    }

    // --- 8a. KOMPRES VIDEO KE TARGET UKURAN ---
    // Bitrate video dihitung dari target: (targetByte * 8 / durasi) * 0.93 - audio 128k.
    // Bila hasilnya di bawah 400 kbps, video diskalakan ke lebar maks 854 agar tetap layak.
    private suspend fun compressVideoToTargetSize(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        targetSizeBytes: Long,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        val tempInput = copyInputToTempFile(context, inputUri, "cmpv_in", inputExt)
        val tempOutput = OutputManager.createTempFile(context, "cmpv_out", "mp4")

        try {
            updateProgress("mp4", strings.engineCompressingVideoTarget, 5, onProgress)

            val durationSec = probeDurationSeconds(tempInput)
            if (durationSec <= 0.0) {
                throw IOException(strings.engineErrNoDuration)
            }

            val audioBitrateBps = 128_000L
            val rawVideoBitrate = ((targetSizeBytes * 8.0 / durationSec) * 0.93 - audioBitrateBps).toLong()
            // Lantai keamanan agar perintah tetap valid untuk target yang sangat kecil
            val videoBitrateBps = rawVideoBitrate.coerceAtLeast(64_000L)
            val scaleArg = if (rawVideoBitrate < 400_000L) {
                " -vf \"scale='min(854,iw)':-2\""
            } else {
                ""
            }

            val command = "-y -i \"${tempInput.absolutePath}\"$scaleArg" +
                " -c:v libx264 -b:v $videoBitrateBps -c:a aac -b:a 128k" +
                " -movflags +faststart \"${tempOutput.absolutePath}\""

            val returnCode = runFfmpegCommand(
                command,
                (durationSec * 1000).toLong(),
                "mp4",
                strings,
                onProgress
            )

            if (ReturnCode.isCancel(returnCode)) {
                throw CancellationException(strings.engineCancelled)
            }
            if (!ReturnCode.isSuccess(returnCode)) {
                throw IOException(strings.engineErrFFmpeg(returnCode.value))
            }

            updateProgress("mp4", strings.engineFinalizingValidating, 98, onProgress)
            when (val valResult = ConversionValidator.validateFile(tempOutput, "mp4")) {
                is ConversionValidator.ValidationResult.Invalid -> {
                    throw IOException(strings.engineErrInvalidOutput(valResult.reason))
                }
                is ConversionValidator.ValidationResult.Valid -> {
                    val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, "mp4")
                        ?: throw IOException(strings.engineErrSaveOutput)
                    OutputManager.cleanTempFiles(tempInput)
                    withContext(Dispatchers.Main) {
                        currentStatus.value = ConvertState.Success
                        onProgress(strings.engineFinished100, 100)
                    }
                    return listOf(finalPath)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, tempOutput)
            throw e
        }
    }

    // --- 8b. KOMPRES GAMBAR KE TARGET UKURAN (JPG) ---
    // Pencarian biner nilai -q:v (2 = kualitas terbaik, 31 = terendah), maks 6 percobaan.
    // Dipilih kualitas terbaik yang ukurannya <= target; bila tidak ada yang muat,
    // dipakai hasil paling kecil pada kualitas terendah yang tercapai.
    private suspend fun compressImageToTargetSize(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        targetSizeBytes: Long,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "jpg" }
        val tempInput = copyInputToTempFile(context, inputUri, "cmpi_in", inputExt)
        val attemptFiles = mutableListOf<File>()

        try {
            updateProgress("jpg", strings.engineCompressingImage, 5, onProgress)

            var lo = 2
            var hi = 31
            var attempt = 0
            var bestFile: File? = null
            var bestQ = Int.MAX_VALUE
            var smallestFile: File? = null

            while (attempt < 6 && lo <= hi) {
                currentCoroutineContext().ensureActive()
                val mid = (lo + hi) / 2
                attempt++
                val attemptPercent = (attempt * 14).coerceAtMost(88)
                updateProgress("jpg", strings.engineCompressingImageAttempt(attempt), attemptPercent, onProgress)

                val tryFile = OutputManager.createTempFile(context, "cmpi_try", "jpg")
                attemptFiles.add(tryFile)

                // Paksa dimensi genap agar encoder MJPEG tidak gagal pada gambar ganjil
                val command = "-y -i \"${tempInput.absolutePath}\"" +
                    " -vf \"scale='trunc(iw/2)*2':'trunc(ih/2)*2'\"" +
                    " -q:v $mid \"${tryFile.absolutePath}\""
                val returnCode = runFfmpegCommand(command, 0L, "jpg", strings, onProgress, reportStats = false)

                if (ReturnCode.isCancel(returnCode)) {
                    throw CancellationException(strings.engineCancelled)
                }
                if (!ReturnCode.isSuccess(returnCode)) {
                    throw IOException(strings.engineErrFFmpeg(returnCode.value))
                }

                val size = tryFile.length()
                if (size > 0) {
                    if (smallestFile == null || size < smallestFile!!.length()) {
                        smallestFile = tryFile
                    }
                    if (size <= targetSizeBytes) {
                        if (mid < bestQ) {
                            bestQ = mid
                            bestFile = tryFile
                        }
                        hi = mid - 1 // muat: coba kualitas yang lebih baik lagi
                    } else {
                        lo = mid + 1 // terlalu besar: turunkan kualitas
                    }
                } else {
                    lo = mid + 1
                }
            }

            val chosen = bestFile ?: smallestFile
                ?: throw IOException(strings.engineErrSaveImage)

            updateProgress("jpg", strings.engineFinalizingValidating, 96, onProgress)
            when (val valResult = ConversionValidator.validateFile(chosen, "jpg")) {
                is ConversionValidator.ValidationResult.Invalid -> {
                    throw IOException(strings.engineErrInvalidOutput(valResult.reason))
                }
                is ConversionValidator.ValidationResult.Valid -> {
                    val finalPath = OutputManager.finalizeOutput(context, chosen, originalFileName, "jpg")
                        ?: throw IOException(strings.engineErrSaveOutput)
                    OutputManager.cleanTempFiles(tempInput, *attemptFiles.toTypedArray())
                    withContext(Dispatchers.Main) {
                        currentStatus.value = ConvertState.Success
                        onProgress(strings.engineFinished100, 100)
                    }
                    return listOf(finalPath)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, *attemptFiles.toTypedArray())
            throw e
        }
    }

    // --- 8c. POTONG VIDEO (TRIM) ---
    // Re-encode potongan dari detik mulai sampai detik selesai agar hasil akurat per frame.
    private suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        startSec: Double,
        endSec: Double,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        // Wadah keluaran dipertahankan untuk mp4/mov/mkv, selain itu mp4
        val outExt = if (inputExt in listOf("mp4", "mov", "mkv")) inputExt else "mp4"
        val tempInput = copyInputToTempFile(context, inputUri, "trim_in", inputExt)
        val tempOutput = OutputManager.createTempFile(context, "trim_out", outExt)

        try {
            updateProgress(outExt, strings.engineTrimmingVideo, 5, onProgress)

            val startArg = String.format(java.util.Locale.US, "%.3f", startSec)
            val endArg = String.format(java.util.Locale.US, "%.3f", endSec)
            val fastStartArg = if (outExt in listOf("mp4", "mov")) " -movflags +faststart" else ""
            val command = "-y -i \"${tempInput.absolutePath}\"" +
                " -ss $startArg -to $endArg" +
                " -c:v libx264 -c:a aac$fastStartArg \"${tempOutput.absolutePath}\""

            val segmentDurationMs = ((endSec - startSec) * 1000).toLong().coerceAtLeast(0L)
            val returnCode = runFfmpegCommand(command, segmentDurationMs, outExt, strings, onProgress)

            if (ReturnCode.isCancel(returnCode)) {
                throw CancellationException(strings.engineCancelled)
            }
            if (!ReturnCode.isSuccess(returnCode)) {
                throw IOException(strings.engineErrFFmpeg(returnCode.value))
            }

            updateProgress(outExt, strings.engineFinalizingValidating, 98, onProgress)
            when (val valResult = ConversionValidator.validateFile(tempOutput, outExt)) {
                is ConversionValidator.ValidationResult.Invalid -> {
                    throw IOException(strings.engineErrInvalidOutput(valResult.reason))
                }
                is ConversionValidator.ValidationResult.Valid -> {
                    val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, outExt)
                        ?: throw IOException(strings.engineErrSaveOutput)
                    OutputManager.cleanTempFiles(tempInput)
                    withContext(Dispatchers.Main) {
                        currentStatus.value = ConvertState.Success
                        onProgress(strings.engineFinished100, 100)
                    }
                    return listOf(finalPath)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, tempOutput)
            throw e
        }
    }

    // --- 8d. POTONG OTOMATIS (SEGMENT MUXER) ---
    // Stream copy per segmen: cepat, potongan jatuh di keyframe terdekat.
    // Semua bagian difinalisasi ke Downloads/Ubahkan dan dikembalikan sebagai daftar path.
    private suspend fun splitVideoAuto(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        segmentSeconds: Int,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        val tempInput = copyInputToTempFile(context, inputUri, "split_in", inputExt)
        val splitDir = File(context.cacheDir, "split_${java.util.UUID.randomUUID().toString().take(8)}")

        try {
            updateProgress(inputExt, strings.engineSplittingVideo, 5, onProgress)
            if (!splitDir.exists()) splitDir.mkdirs()

            val baseName = originalFileName.substringBeforeLast('.', "file")
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .ifBlank { "file" }
            val patternFile = File(splitDir, "$baseName - Bagian %02d.$inputExt")

            val command = "-y -i \"${tempInput.absolutePath}\"" +
                " -f segment -segment_time $segmentSeconds -c copy -reset_timestamps 1" +
                " \"${patternFile.absolutePath}\""

            val durationSec = probeDurationSeconds(tempInput)
            val returnCode = runFfmpegCommand(
                command,
                (durationSec * 1000).toLong(),
                inputExt,
                strings,
                onProgress
            )

            if (ReturnCode.isCancel(returnCode)) {
                throw CancellationException(strings.engineCancelled)
            }
            if (!ReturnCode.isSuccess(returnCode)) {
                throw IOException(strings.engineErrFFmpeg(returnCode.value))
            }

            // Urutkan berdasarkan nomor bagian (bukan leksikografis, agar Bagian 100 sesudah 99)
            val partNumberRegex = Regex("Bagian (\\d+)")
            val partFiles = (splitDir.listFiles()?.filter { it.isFile && it.length() > 0 } ?: emptyList())
                .sortedBy { file ->
                    partNumberRegex.find(file.name)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
                }
            if (partFiles.isEmpty()) {
                throw IOException(strings.engineErrSplitNoParts)
            }

            updateProgress(inputExt, strings.engineFinalizingValidating, 96, onProgress)
            val finalPaths = mutableListOf<String>()
            for (part in partFiles) {
                currentCoroutineContext().ensureActive()
                val saved = OutputManager.finalizeOutput(context, part, part.name, inputExt)
                if (saved != null) finalPaths.add(saved)
            }
            if (finalPaths.isEmpty()) {
                throw IOException(strings.engineErrSaveOutput)
            }

            OutputManager.cleanTempFiles(tempInput)
            splitDir.deleteRecursively()
            withContext(Dispatchers.Main) {
                currentStatus.value = ConvertState.Success
                onProgress(strings.engineFinished100, 100)
            }
            return finalPaths
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput)
            splitDir.deleteRecursively()
            throw e
        }
    }

    // --- 9a. GABUNG VIDEO (CONCAT DEMUXER, STREAM COPY) ---
    // Semua masukan disalin ke temp, daftar concat ditulis, lalu digabung tanpa encode ulang.
    // Stream copy menuntut codec/resolusi masukan sama; bila beda, FFmpeg bisa gagal
    // dan job ditandai gagal oleh pemanggil.
    private suspend fun mergeVideos(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        extraInputUris: List<Uri>,
        extraFileNames: List<String>,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        val outExt = if (inputExt in listOf("mp4", "mov", "mkv")) inputExt else "mp4"
        val allUris = listOf(inputUri) + extraInputUris
        val allNames = listOf(originalFileName) + extraFileNames
        val tempInputs = mutableListOf<File>()
        var listFile: File? = null
        val tempOutput = OutputManager.createTempFile(context, "mergev_out", outExt)

        try {
            updateProgress(outExt, strings.engineMergingVideos, 5, onProgress)

            var totalDurationMs = 0L
            for ((index, uri) in allUris.withIndex()) {
                currentCoroutineContext().ensureActive()
                val name = allNames.getOrNull(index) ?: ""
                val ext = name.substringAfterLast('.', "").trim().lowercase()
                    .ifEmpty { inputExt }
                val tempInput = copyInputToTempFile(context, uri, "mergev_in_$index", ext)
                tempInputs.add(tempInput)
                totalDurationMs += (probeDurationSeconds(tempInput) * 1000).toLong()
            }

            // Berkas daftar concat: satu baris "file '<path>'" per masukan, urut sesuai pilihan
            val listContent = buildString {
                for (tempInput in tempInputs) {
                    val escapedPath = tempInput.absolutePath.replace("'", "'\\''")
                    append("file '").append(escapedPath).append("'\n")
                }
            }
            listFile = OutputManager.createTempFile(context, "mergev_list", "txt")
            listFile.writeText(listContent)

            val command = "-y -f concat -safe 0 -i \"${listFile.absolutePath}\"" +
                " -c copy \"${tempOutput.absolutePath}\""
            val returnCode = runFfmpegCommand(command, totalDurationMs, outExt, strings, onProgress)

            if (ReturnCode.isCancel(returnCode)) {
                throw CancellationException(strings.engineCancelled)
            }
            if (!ReturnCode.isSuccess(returnCode)) {
                throw IOException(strings.engineErrFFmpeg(returnCode.value))
            }

            updateProgress(outExt, strings.engineFinalizingValidating, 98, onProgress)
            when (val valResult = ConversionValidator.validateFile(tempOutput, outExt)) {
                is ConversionValidator.ValidationResult.Invalid -> {
                    throw IOException(strings.engineErrInvalidOutput(valResult.reason))
                }
                is ConversionValidator.ValidationResult.Valid -> {
                    val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, outExt)
                        ?: throw IOException(strings.engineErrSaveOutput)
                    OutputManager.cleanTempFiles(*tempInputs.toTypedArray(), listFile)
                    withContext(Dispatchers.Main) {
                        currentStatus.value = ConvertState.Success
                        onProgress(strings.engineFinished100, 100)
                    }
                    return listOf(finalPath)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(*tempInputs.toTypedArray(), listFile, tempOutput)
            throw e
        }
    }

    // Parse rentang halaman "1-3", "1,3,5", atau campuran, berbasis 1.
    // String kosong berarti semua halaman. Null bila ada token yang tidak valid.
    private fun parsePdfPageRange(spec: String, pageCount: Int): List<Int>? {
        val trimmed = spec.trim()
        if (trimmed.isEmpty() || trimmed.equals("all", ignoreCase = true)) {
            return (1..pageCount).toList()
        }
        val result = mutableListOf<Int>()
        for (token in trimmed.split(',')) {
            val part = token.trim()
            if (part.isEmpty()) return null
            if (part.contains('-')) {
                val bounds = part.split('-')
                if (bounds.size != 2) return null
                val from = bounds[0].trim().toIntOrNull() ?: return null
                val to = bounds[1].trim().toIntOrNull() ?: return null
                if (from < 1 || to > pageCount || from > to) return null
                for (page in from..to) result.add(page)
            } else {
                val page = part.toIntOrNull() ?: return null
                if (page < 1 || page > pageCount) return null
                result.add(page)
            }
        }
        return result.distinct()
    }

    // --- 9b. PISAH PDF (SATU PDF PER HALAMAN, BERBASIS BITMAP 200 DPI) ---
    // Tanpa pustaka PDF tambahan: tiap halaman dirender PdfRenderer pada 200 DPI
    // lalu ditulis sebagai halaman PdfDocument. Hasilnya gambar (teks tidak lagi
    // bisa diseleksi), sama seperti jalur kompres PDF yang sudah ada.
    private suspend fun splitPdf(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        pageRange: String,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val tempInput = copyInputToTempFile(context, inputUri, "splitpdf_in", "pdf")
        val createdTempFiles = mutableListOf<File>()

        try {
            updateProgress("pdf", strings.engineSplittingPdf, 5, onProgress)

            val pfd = ParcelFileDescriptor.open(tempInput, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount == 0) {
                renderer.close()
                pfd.close()
                throw IllegalStateException(strings.engineErrPdfNoPages)
            }

            val pages = parsePdfPageRange(pageRange, pageCount)
                ?: throw IOException(strings.engineErrPdfRangeInvalid)

            // Ukuran titik PdfRenderer berbasis 72 DPI, skala ke 200 DPI
            val scale = 200f / 72f
            val baseName = originalFileName.substringBeforeLast('.', "doc")
            val finalPaths = mutableListOf<String>()

            for ((index, pageNumber) in pages.withIndex()) {
                currentCoroutineContext().ensureActive()
                val percent = (((index + 1).toFloat() / pages.size) * 88).toInt() + 6
                updateProgress(
                    "pdf",
                    strings.engineSplittingPdfPage(index + 1, pages.size),
                    percent,
                    onProgress
                )

                val page = renderer.openPage(pageNumber - 1)
                val width = (page.width * scale).toInt().coerceAtLeast(1)
                val height = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(docPage)
                bitmap.recycle()

                val tempPageFile = OutputManager.createTempFile(context, "splitpdf_page", "pdf")
                createdTempFiles.add(tempPageFile)
                FileOutputStream(tempPageFile).use { fos -> pdfDocument.writeTo(fos) }
                pdfDocument.close()

                val savedPath = OutputManager.finalizeOutput(
                    context, tempPageFile, "${baseName}_hal_$pageNumber.pdf", "pdf"
                )
                if (savedPath != null) finalPaths.add(savedPath)
            }

            renderer.close()
            pfd.close()

            if (finalPaths.isEmpty()) {
                throw IOException(strings.engineErrSaveOutput)
            }

            OutputManager.cleanTempFiles(tempInput, *createdTempFiles.toTypedArray())
            withContext(Dispatchers.Main) {
                currentStatus.value = ConvertState.Success
                onProgress(strings.engineFinished100, 100)
            }
            return finalPaths
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, *createdTempFiles.toTypedArray())
            throw e
        }
    }

    // --- 9c. GABUNG PDF (BEBERAPA PDF JADI SATU, BERBASIS BITMAP 200 DPI) ---
    // Cara yang sama seperti pisah PDF: semua halaman dirender lalu ditulis ulang
    // ke satu PdfDocument, mengikuti urutan berkas yang dipilih pengguna.
    private suspend fun mergePdfs(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        extraInputUris: List<Uri>,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val allUris = listOf(inputUri) + extraInputUris
        val tempInputs = mutableListOf<File>()
        val tempOutput = OutputManager.createTempFile(context, "mergepdf_out", "pdf")

        try {
            updateProgress("pdf", strings.engineMergingPdfs, 5, onProgress)

            for ((index, uri) in allUris.withIndex()) {
                currentCoroutineContext().ensureActive()
                tempInputs.add(copyInputToTempFile(context, uri, "mergepdf_in_$index", "pdf"))
            }

            // Hitung total halaman dulu agar progres akurat
            var totalPages = 0
            for (tempInput in tempInputs) {
                val pfd = ParcelFileDescriptor.open(tempInput, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                totalPages += renderer.pageCount
                renderer.close()
                pfd.close()
            }
            if (totalPages == 0) {
                throw IllegalStateException(strings.engineErrPdfNoPages)
            }

            val scale = 200f / 72f
            val pdfDocument = PdfDocument()
            var outputPage = 0
            try {
                for (tempInput in tempInputs) {
                    val pfd = ParcelFileDescriptor.open(tempInput, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    for (i in 0 until renderer.pageCount) {
                        currentCoroutineContext().ensureActive()
                        outputPage++
                        val percent = ((outputPage.toFloat() / totalPages) * 88).toInt() + 6
                        updateProgress(
                            "pdf",
                            strings.engineMergingPdfPage(outputPage, totalPages),
                            percent,
                            onProgress
                        )

                        val page = renderer.openPage(i)
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        page.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(width, height, outputPage).create()
                        val docPage = pdfDocument.startPage(pageInfo)
                        docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(docPage)
                        bitmap.recycle()
                    }
                    renderer.close()
                    pfd.close()
                }

                FileOutputStream(tempOutput).use { fos -> pdfDocument.writeTo(fos) }
            } finally {
                pdfDocument.close()
            }

            val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, "pdf")
                ?: throw IOException(strings.engineErrSaveOutput)
            OutputManager.cleanTempFiles(*tempInputs.toTypedArray())
            withContext(Dispatchers.Main) {
                currentStatus.value = ConvertState.Success
                onProgress(strings.engineFinished100, 100)
            }
            return listOf(finalPath)
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(*tempInputs.toTypedArray(), tempOutput)
            throw e
        }
    }

    // Escape path untuk filter subtitles: karakter khusus opsi filter harus diloloskan
    private fun escapeSubtitlesFilterPath(path: String): String {
        return path
            .replace("\\", "\\\\")
            .replace(":", "\\:")
            .replace("'", "\\'")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace(",", "\\,")
            .replace(";", "\\;")
    }

    // --- 9d. BAKAR SUBTITLE (HARDSUB) ---
    // Filter subtitles (libass) menggambar teks .srt langsung ke frame video,
    // video di-encode ulang libx264, audio disalin apa adanya.
    private suspend fun burnSubtitles(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        extraInputUris: List<Uri>,
        extraFileNames: List<String>,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val subtitleUri = extraInputUris.firstOrNull()
            ?: throw IOException(strings.engineErrSubtitleMissing)
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        val outExt = if (inputExt in listOf("mp4", "mov", "mkv")) inputExt else "mp4"
        val subtitleName = extraFileNames.firstOrNull() ?: ""
        val subtitleExt = subtitleName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "srt" }

        val tempInput = copyInputToTempFile(context, inputUri, "hardsub_in", inputExt)
        var tempSubtitle: File? = null
        val tempOutput = OutputManager.createTempFile(context, "hardsub_out", outExt)

        try {
            updateProgress(outExt, strings.engineBurningSubtitles, 5, onProgress)
            tempSubtitle = copyInputToTempFile(context, subtitleUri, "hardsub_srt", subtitleExt)

            val durationMs = (probeDurationSeconds(tempInput) * 1000).toLong()
            val escapedPath = escapeSubtitlesFilterPath(tempSubtitle.absolutePath)
            val command = "-y -i \"${tempInput.absolutePath}\"" +
                " -vf \"subtitles='$escapedPath'\"" +
                " -c:v libx264 -c:a copy \"${tempOutput.absolutePath}\""
            val returnCode = runFfmpegCommand(command, durationMs, outExt, strings, onProgress)

            if (ReturnCode.isCancel(returnCode)) {
                throw CancellationException(strings.engineCancelled)
            }
            if (!ReturnCode.isSuccess(returnCode)) {
                throw IOException(strings.engineErrFFmpeg(returnCode.value))
            }

            updateProgress(outExt, strings.engineFinalizingValidating, 98, onProgress)
            when (val valResult = ConversionValidator.validateFile(tempOutput, outExt)) {
                is ConversionValidator.ValidationResult.Invalid -> {
                    throw IOException(strings.engineErrInvalidOutput(valResult.reason))
                }
                is ConversionValidator.ValidationResult.Valid -> {
                    val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, outExt)
                        ?: throw IOException(strings.engineErrSaveOutput)
                    OutputManager.cleanTempFiles(tempInput, tempSubtitle)
                    withContext(Dispatchers.Main) {
                        currentStatus.value = ConvertState.Success
                        onProgress(strings.engineFinished100, 100)
                    }
                    return listOf(finalPath)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, tempSubtitle, tempOutput)
            throw e
        }
    }

    // --- 9e. CROP RASIO VIDEO ---
    // Filter crop terpusat: lebar/tinggi dibatasi min() terhadap rasio tujuan dan
    // dibulatkan ke genap (trunc ke kelipatan 2) agar libx264 tidak gagal.
    private suspend fun cropVideo(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        cropRatio: String,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        val outExt = if (inputExt in listOf("mp4", "mov", "mkv")) inputExt else "mp4"
        val tempInput = copyInputToTempFile(context, inputUri, "crop_in", inputExt)
        val tempOutput = OutputManager.createTempFile(context, "crop_out", outExt)

        try {
            updateProgress(outExt, strings.engineCroppingVideo, 5, onProgress)

            val (widthExpr, heightExpr) = when (cropRatio) {
                "9:16" -> "trunc(min(iw,ih*9/16)/2)*2" to "trunc(min(ih,iw*16/9)/2)*2"
                "1:1" -> "trunc(min(iw,ih)/2)*2" to "trunc(min(iw,ih)/2)*2"
                else -> "trunc(min(iw,ih*16/9)/2)*2" to "trunc(min(ih,iw*9/16)/2)*2"
            }
            val cropFilter = "crop='$widthExpr':'$heightExpr':'(iw-ow)/2':'(ih-oh)/2'"
            val fastStartArg = if (outExt in listOf("mp4", "mov")) " -movflags +faststart" else ""
            val command = "-y -i \"${tempInput.absolutePath}\"" +
                " -vf \"$cropFilter\" -c:v libx264 -c:a aac$fastStartArg" +
                " \"${tempOutput.absolutePath}\""

            val durationMs = (probeDurationSeconds(tempInput) * 1000).toLong()
            val returnCode = runFfmpegCommand(command, durationMs, outExt, strings, onProgress)

            if (ReturnCode.isCancel(returnCode)) {
                throw CancellationException(strings.engineCancelled)
            }
            if (!ReturnCode.isSuccess(returnCode)) {
                throw IOException(strings.engineErrFFmpeg(returnCode.value))
            }

            updateProgress(outExt, strings.engineFinalizingValidating, 98, onProgress)
            when (val valResult = ConversionValidator.validateFile(tempOutput, outExt)) {
                is ConversionValidator.ValidationResult.Invalid -> {
                    throw IOException(strings.engineErrInvalidOutput(valResult.reason))
                }
                is ConversionValidator.ValidationResult.Valid -> {
                    val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, outExt)
                        ?: throw IOException(strings.engineErrSaveOutput)
                    OutputManager.cleanTempFiles(tempInput)
                    withContext(Dispatchers.Main) {
                        currentStatus.value = ConvertState.Success
                        onProgress(strings.engineFinished100, 100)
                    }
                    return listOf(finalPath)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, tempOutput)
            throw e
        }
    }

    // --- 9f. UBAH RESOLUSI VIDEO ---
    // scale=-2:'min(ih,H)': tinggi hanya diturunkan ke preset; bila sumber lebih
    // kecil dari preset, min() mempertahankan ukuran aslinya.
    private suspend fun resizeVideo(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        targetHeight: Int,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        val outExt = if (inputExt in listOf("mp4", "mov", "mkv")) inputExt else "mp4"
        val height = if (targetHeight in listOf(1080, 720, 480, 360)) targetHeight else 720
        val tempInput = copyInputToTempFile(context, inputUri, "resize_in", inputExt)
        val tempOutput = OutputManager.createTempFile(context, "resize_out", outExt)

        try {
            updateProgress(outExt, strings.engineResizingVideo, 5, onProgress)

            val fastStartArg = if (outExt in listOf("mp4", "mov")) " -movflags +faststart" else ""
            val command = "-y -i \"${tempInput.absolutePath}\"" +
                " -vf \"scale=-2:'min(ih,$height)'\" -c:v libx264 -c:a aac$fastStartArg" +
                " \"${tempOutput.absolutePath}\""

            val durationMs = (probeDurationSeconds(tempInput) * 1000).toLong()
            val returnCode = runFfmpegCommand(command, durationMs, outExt, strings, onProgress)

            if (ReturnCode.isCancel(returnCode)) {
                throw CancellationException(strings.engineCancelled)
            }
            if (!ReturnCode.isSuccess(returnCode)) {
                throw IOException(strings.engineErrFFmpeg(returnCode.value))
            }

            updateProgress(outExt, strings.engineFinalizingValidating, 98, onProgress)
            when (val valResult = ConversionValidator.validateFile(tempOutput, outExt)) {
                is ConversionValidator.ValidationResult.Invalid -> {
                    throw IOException(strings.engineErrInvalidOutput(valResult.reason))
                }
                is ConversionValidator.ValidationResult.Valid -> {
                    val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, outExt)
                        ?: throw IOException(strings.engineErrSaveOutput)
                    OutputManager.cleanTempFiles(tempInput)
                    withContext(Dispatchers.Main) {
                        currentStatus.value = ConvertState.Success
                        onProgress(strings.engineFinished100, 100)
                    }
                    return listOf(finalPath)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, tempOutput)
            throw e
        }
    }

    // --- 9g. BISUKAN VIDEO ---
    // Video disalin tanpa encode ulang, jalur audio dibuang dengan -an.
    private suspend fun muteVideo(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        val outExt = if (inputExt in listOf("mp4", "mov", "mkv")) inputExt else "mp4"
        val tempInput = copyInputToTempFile(context, inputUri, "mute_in", inputExt)
        val tempOutput = OutputManager.createTempFile(context, "mute_out", outExt)

        try {
            updateProgress(outExt, strings.engineMutingVideo, 5, onProgress)

            val command = "-y -i \"${tempInput.absolutePath}\"" +
                " -c:v copy -an \"${tempOutput.absolutePath}\""
            val durationMs = (probeDurationSeconds(tempInput) * 1000).toLong()
            val returnCode = runFfmpegCommand(command, durationMs, outExt, strings, onProgress)

            if (ReturnCode.isCancel(returnCode)) {
                throw CancellationException(strings.engineCancelled)
            }
            if (!ReturnCode.isSuccess(returnCode)) {
                throw IOException(strings.engineErrFFmpeg(returnCode.value))
            }

            updateProgress(outExt, strings.engineFinalizingValidating, 98, onProgress)
            when (val valResult = ConversionValidator.validateFile(tempOutput, outExt)) {
                is ConversionValidator.ValidationResult.Invalid -> {
                    throw IOException(strings.engineErrInvalidOutput(valResult.reason))
                }
                is ConversionValidator.ValidationResult.Valid -> {
                    val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, outExt)
                        ?: throw IOException(strings.engineErrSaveOutput)
                    OutputManager.cleanTempFiles(tempInput)
                    withContext(Dispatchers.Main) {
                        currentStatus.value = ConvertState.Success
                        onProgress(strings.engineFinished100, 100)
                    }
                    return listOf(finalPath)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, tempOutput)
            throw e
        }
    }

    // --- 9h. GANTI AUDIO VIDEO ---
    // Video dari masukan pertama disalin, audio diambil dari berkas kedua,
    // hasil dipotong mengikuti yang terpendek (-shortest).
    private suspend fun replaceVideoAudio(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        extraInputUris: List<Uri>,
        extraFileNames: List<String>,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val audioUri = extraInputUris.firstOrNull()
            ?: throw IOException(strings.engineErrAudioMissing)
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        val outExt = if (inputExt in listOf("mp4", "mov", "mkv")) inputExt else "mp4"
        val audioName = extraFileNames.firstOrNull() ?: ""
        val audioExt = audioName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp3" }

        val tempInput = copyInputToTempFile(context, inputUri, "repaud_in", inputExt)
        var tempAudio: File? = null
        val tempOutput = OutputManager.createTempFile(context, "repaud_out", outExt)

        try {
            updateProgress(outExt, strings.engineReplacingAudio, 5, onProgress)
            tempAudio = copyInputToTempFile(context, audioUri, "repaud_audio", audioExt)

            val command = "-y -i \"${tempInput.absolutePath}\" -i \"${tempAudio.absolutePath}\"" +
                " -map 0:v:0 -map 1:a:0 -c:v copy -c:a aac -shortest" +
                " \"${tempOutput.absolutePath}\""
            val durationMs = (probeDurationSeconds(tempInput) * 1000).toLong()
            val returnCode = runFfmpegCommand(command, durationMs, outExt, strings, onProgress)

            if (ReturnCode.isCancel(returnCode)) {
                throw CancellationException(strings.engineCancelled)
            }
            if (!ReturnCode.isSuccess(returnCode)) {
                throw IOException(strings.engineErrFFmpeg(returnCode.value))
            }

            updateProgress(outExt, strings.engineFinalizingValidating, 98, onProgress)
            when (val valResult = ConversionValidator.validateFile(tempOutput, outExt)) {
                is ConversionValidator.ValidationResult.Invalid -> {
                    throw IOException(strings.engineErrInvalidOutput(valResult.reason))
                }
                is ConversionValidator.ValidationResult.Valid -> {
                    val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, outExt)
                        ?: throw IOException(strings.engineErrSaveOutput)
                    OutputManager.cleanTempFiles(tempInput, tempAudio)
                    withContext(Dispatchers.Main) {
                        currentStatus.value = ConvertState.Success
                        onProgress(strings.engineFinished100, 100)
                    }
                    return listOf(finalPath)
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput, tempAudio, tempOutput)
            throw e
        }
    }

    // --- 9i. HEIC KE JPG ---
    // ImageDecoder (API 28+) mendekode HEIC/HEIF secara native, hasil dikompres
    // JPEG kualitas 92. Di bawah API 28 tidak ada decoder HEIF bawaan yang bisa
    // diandalkan, jadi job gagal dengan pesan yang jelas.
    private suspend fun convertHeicToJpg(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            throw IOException(strings.engineErrHeicUnsupported)
        }
        val tempOutput = OutputManager.createTempFile(context, "heic_out", "jpg")

        try {
            updateProgress("jpg", strings.engineConvertingHeic, 30, onProgress)

            val bitmap = try {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, inputUri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (e: Exception) {
                throw IOException(strings.engineErrHeicDecode(e.message ?: ""))
            }

            updateProgress("jpg", strings.engineCompressingImage, 70, onProgress)

            val finalBitmap = if (bitmap.hasAlpha()) {
                val opaqueBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(opaqueBitmap)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                bitmap.recycle()
                opaqueBitmap
            } else {
                bitmap
            }

            FileOutputStream(tempOutput).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            finalBitmap.recycle()

            val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, "jpg")
                ?: throw IOException(strings.engineErrSaveOutput)
            OutputManager.cleanTempFiles(tempOutput)
            withContext(Dispatchers.Main) {
                currentStatus.value = ConvertState.Success
                onProgress(strings.engineFinished100, 100)
            }
            return listOf(finalPath)
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempOutput)
            throw e
        }
    }

    // --- 9j. NORMALISASI VOLUME (LOUDNORM SATU PASS) ---
    // Video: gambar disalin, audio difilter loudnorm I=-16:TP=-1.5:LRA=11 lalu
    // di-encode AAC. Audio saja: hanya jalur audio yang diproses ke format asal.
    private suspend fun normalizeVolume(
        context: Context,
        inputUri: Uri,
        originalFileName: String,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        val inputExt = originalFileName.substringAfterLast('.', "").trim().lowercase().ifEmpty { "mp4" }
        val tempInput = copyInputToTempFile(context, inputUri, "loudnorm_in", inputExt)

        try {
            val mediaInfo = FFprobeKit.getMediaInformation(tempInput.absolutePath).mediaInformation
            val hasVideo = mediaInfo?.streams?.any { it.type.equals("video", ignoreCase = true) } == true
            val durationMs = (mediaInfo?.duration?.toDoubleOrNull()?.times(1000))?.toLong() ?: 0L

            val loudnormFilter = "loudnorm=I=-16:TP=-1.5:LRA=11"
            val outExt: String
            val command: String
            if (hasVideo) {
                outExt = if (inputExt in listOf("mp4", "mov", "mkv")) inputExt else "mp4"
                val tempOutput = OutputManager.createTempFile(context, "loudnorm_out", outExt)
                try {
                    updateProgress(outExt, strings.engineNormalizingVolume, 5, onProgress)
                    command = "-y -i \"${tempInput.absolutePath}\"" +
                        " -c:v copy -af \"$loudnormFilter\" -c:a aac \"${tempOutput.absolutePath}\""
                    val returnCode = runFfmpegCommand(command, durationMs, outExt, strings, onProgress)
                    return finishLoudnormJob(context, tempInput, tempOutput, originalFileName, outExt, returnCode, onProgress)
                } catch (e: Exception) {
                    OutputManager.cleanTempFiles(tempOutput)
                    throw e
                }
            } else {
                outExt = if (inputExt in listOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")) inputExt else "mp3"
                val encoder = when (outExt) {
                    "mp3" -> "libmp3lame"
                    "wav" -> "pcm_s16le"
                    "flac" -> "flac"
                    "ogg" -> "libvorbis"
                    "opus" -> "libopus"
                    else -> "aac"
                }
                val tempOutput = OutputManager.createTempFile(context, "loudnorm_out", outExt)
                try {
                    updateProgress(outExt, strings.engineNormalizingVolume, 5, onProgress)
                    command = "-y -i \"${tempInput.absolutePath}\"" +
                        " -vn -af \"$loudnormFilter\" -c:a $encoder \"${tempOutput.absolutePath}\""
                    val returnCode = runFfmpegCommand(command, durationMs, outExt, strings, onProgress)
                    return finishLoudnormJob(context, tempInput, tempOutput, originalFileName, outExt, returnCode, onProgress)
                } catch (e: Exception) {
                    OutputManager.cleanTempFiles(tempOutput)
                    throw e
                }
            }
        } catch (e: Exception) {
            OutputManager.cleanTempFiles(tempInput)
            throw e
        }
    }

    // Penutup bersama jalur loudnorm video dan audio: validasi lalu finalisasi
    private suspend fun finishLoudnormJob(
        context: Context,
        tempInput: File,
        tempOutput: File,
        originalFileName: String,
        outExt: String,
        returnCode: ReturnCode,
        onProgress: (String, Int) -> Unit
    ): List<String> {
        val strings = AppStrings.get()
        if (ReturnCode.isCancel(returnCode)) {
            throw CancellationException(strings.engineCancelled)
        }
        if (!ReturnCode.isSuccess(returnCode)) {
            throw IOException(strings.engineErrFFmpeg(returnCode.value))
        }

        updateProgress(outExt, strings.engineFinalizingValidating, 98, onProgress)
        when (val valResult = ConversionValidator.validateFile(tempOutput, outExt)) {
            is ConversionValidator.ValidationResult.Invalid -> {
                throw IOException(strings.engineErrInvalidOutput(valResult.reason))
            }
            is ConversionValidator.ValidationResult.Valid -> {
                val finalPath = OutputManager.finalizeOutput(context, tempOutput, originalFileName, outExt)
                    ?: throw IOException(strings.engineErrSaveOutput)
                OutputManager.cleanTempFiles(tempInput)
                withContext(Dispatchers.Main) {
                    currentStatus.value = ConvertState.Success
                    onProgress(strings.engineFinished100, 100)
                }
                return listOf(finalPath)
            }
        }
    }

    private fun updateProgress(
        format: String,
        message: String,
        percent: Int,
        onProgress: (String, Int) -> Unit
    ) {
        currentStatus.value = ConvertState.Converting(format, percent)
        onProgress(message, percent)
    }

    fun cancelCurrentConversion() {
        activeJob?.cancel()
        activeJob = null
        FFmpegKit.cancel()
        currentStatus.value = ConvertState.Idle
    }
}
