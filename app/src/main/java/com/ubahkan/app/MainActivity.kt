package com.ubahkan.app

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.OutputManager
import com.ubahkan.app.core.SelectedFileItem
import com.ubahkan.app.core.Strings
import com.ubahkan.app.ui.components.UbahkanHomeTopBar
import com.ubahkan.app.ui.screens.BottomSheetConverter
import com.ubahkan.app.ui.screens.HomeScreen
import com.ubahkan.app.ui.screens.ProgressScreen
import com.ubahkan.app.ui.screens.SettingsScreen
import com.ubahkan.app.ui.screens.SplashScreen
import com.ubahkan.app.ui.screens.ToolsScreen
import com.ubahkan.app.ui.screens.WelcomeScreen
import com.ubahkan.app.ui.theme.UbahkanTheme
import java.io.File
import android.os.PowerManager
import android.provider.Settings

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

enum class UbahkanTab {
    Home, Tools, Settings
}

class MainActivity : ComponentActivity() {

    // Notification permission launcher (PBI 7.5)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, AppStrings.get().notifPermissionRequiredToast, Toast.LENGTH_LONG).show()
        }
    }
    

    // File Picker for Standalone Launcher - Supports single or multiple files (PBI 8.3 & PBI 7.1)
    private var filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedFilesForConversion = uris.map { uri ->
                val name = getFileNameFromUri(this, uri)
                val mime = contentResolver.getType(uri) ?: getExplicitMimeType(name.substringAfterLast('.', ""))
                val size = getFileSizeFromUri(this, uri)
                SelectedFileItem(uri, name, mime, size)
            }
            showBottomSheet = true
        }
    }

    private var selectedFilesForConversion by mutableStateOf<List<SelectedFileItem>>(emptyList())
    private var showBottomSheet by mutableStateOf(false)

    companion object {
        var isAppInForeground = false
    }

    private var isStandaloneMode by mutableStateOf(true)

    // Splash hanya tampil sekali per proses, saat start dingin mode standalone
    private var splashConsumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppStrings.init(this)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val isInitialStandalone = intent.action == Intent.ACTION_MAIN
        val startWithSplash = isInitialStandalone && !splashConsumed

        if (!isInitialStandalone && intent.action == Intent.ACTION_SEND) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }

            if (uri != null) {
                val name = getFileNameFromUri(this, uri)
                val mime = intent.type ?: contentResolver.getType(uri) ?: "*/*"
                val size = getFileSizeFromUri(this, uri)
                selectedFilesForConversion = listOf(SelectedFileItem(uri, name, mime, size))
                isStandaloneMode = false
                showBottomSheet = true
            } else {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!text.isNullOrBlank()) {
                    Toast.makeText(this, AppStrings.get().webUrlNotSupportedToast, Toast.LENGTH_LONG).show()
                }
                isStandaloneMode = true
            }
        } else if (!isInitialStandalone && intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val uris: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }

            val validUris = if (!uris.isNullOrEmpty()) {
                uris.filterNotNull()
            } else {
                val clipData = intent.clipData
                if (clipData != null) {
                    (0 until clipData.itemCount).mapNotNull { clipData.getItemAt(it).uri }
                } else {
                    emptyList()
                }
            }

            if (validUris.isNotEmpty()) {
                selectedFilesForConversion = validUris.map { uri ->
                    val name = getFileNameFromUri(this, uri)
                    val mime = contentResolver.getType(uri) ?: "*/*"
                    val size = getFileSizeFromUri(this, uri)
                    SelectedFileItem(uri, name, mime, size)
                }
                isStandaloneMode = false
                showBottomSheet = true
            } else {
                Toast.makeText(this, AppStrings.get().noFilesFromShareToast, Toast.LENGTH_SHORT).show()
                isStandaloneMode = true
            }
        } else {
            isStandaloneMode = true
        }

        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val prefs = remember { UbahkanPrefs(context) }

            // Reactive Theme & Language State
            var currentThemeOption by remember { mutableStateOf(prefs.themeOption) }
            var currentLanguage by remember { mutableStateOf(prefs.appLanguage) }
            val strings = AppStrings.get(currentLanguage)

            //batterry setting
            var isBatteryUnrestricted by remember { mutableStateOf(checkBatteryOptimizationDisabled(context)) }

            // Dual-State Navigation: Standalone vs Share Mode
            var currentTab by remember { mutableStateOf(UbahkanTab.Home) }
            var isOnboarding by remember { mutableStateOf(!prefs.onboardingDone) }
            var showProgressScreen by remember { mutableStateOf(false) }
            var showSplash by remember { mutableStateOf(startWithSplash) }

            // Double-back-to-exit (PBI: Back Navigation)
            var backPressedOnce by remember { mutableStateOf(false) }

            // BackHandler: tab non-Home → kembali ke Home; Home → double back exit
            BackHandler(enabled = isStandaloneMode) {
                if (currentTab != UbahkanTab.Home) {
                    currentTab = UbahkanTab.Home
                    backPressedOnce = false
                } else {
                    if (backPressedOnce) {
                        (context as? ComponentActivity)?.finish()
                    } else {
                        backPressedOnce = true
                        Toast.makeText(context, strings.pressBackAgainToExit, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Reset flag double-back setelah 2 detik
            LaunchedEffect(backPressedOnce) {
                if (backPressedOnce) {
                    kotlinx.coroutines.delay(2000)
                    backPressedOnce = false
                }
            }

            // Converted files state
            var ubahkanFiles by remember { mutableStateOf(getUbahkanFiles()) }
            var isRefreshing by remember { mutableStateOf(false) }
            var hasNotificationPermission by remember { mutableStateOf(checkNotifPermission(context)) }

            val refreshFiles = {
                isRefreshing = true
                ubahkanFiles = getUbahkanFiles()
                isRefreshing = false
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            isAppInForeground = true
                            hasNotificationPermission = checkNotifPermission(context)
                            isBatteryUnrestricted = checkBatteryOptimizationDisabled(context)
                            refreshFiles()
                        }
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            isAppInForeground = false
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    isAppInForeground = false
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            UbahkanTheme(themeOption = currentThemeOption, language = currentLanguage) {
                val backgroundColor = if (isStandaloneMode) MaterialTheme.colorScheme.background else Color.Transparent

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor
                ) {
                    if (isStandaloneMode) {
                        if (showSplash) {
                            SplashScreen(
                                onFinished = {
                                    showSplash = false
                                    splashConsumed = true
                                }
                            )
                        } else if (isOnboarding) {
                            WelcomeScreen(
                                onGetStarted = {
                                    prefs.onboardingDone = true
                                    isOnboarding = false
                                }
                            )
                        } else if (showProgressScreen) {
                            ProgressScreen(
                                onMinimize = { showProgressScreen = false },
                                onFinish = {
                                    showProgressScreen = false
                                    refreshFiles()
                                }, onViewResult = {
                                    showProgressScreen = false
                                    refreshFiles()
                                    // Ambil file yang baru saja selesai dikonversi
                                    val latestFile = getUbahkanFiles().firstOrNull()
                                    if (latestFile != null) {
                                        openFileDirectly(context, latestFile, strings)
                                    } else {
                                        Toast.makeText(context, strings.fileNotFoundToast, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            Scaffold(
                            topBar = {
                                // Top bar hanya di Home — Tools dan Settings punya header sendiri
                                if (currentTab == UbahkanTab.Home) {
                                    UbahkanHomeTopBar(
                                        onSettingsClick = { currentTab = UbahkanTab.Settings }
                                    )
                                }
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == UbahkanTab.Home,
                                        onClick = { currentTab = UbahkanTab.Home },
                                        icon = {
                                            Icon(
                                                if (currentTab == UbahkanTab.Home) Icons.Default.Home
                                                else Icons.Outlined.Home,
                                                contentDescription = strings.tabHome
                                            )
                                        },
                                        label = {
                                            Text(
                                                strings.tabHome,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (currentTab == UbahkanTab.Home) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == UbahkanTab.Tools,
                                        onClick = { currentTab = UbahkanTab.Tools },
                                        icon = {
                                            Icon(
                                                if (currentTab == UbahkanTab.Tools) Icons.Default.Build
                                                else Icons.Outlined.Build,
                                                contentDescription = strings.tabTools
                                            )
                                        },
                                        label = {
                                            Text(
                                                strings.tabTools,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (currentTab == UbahkanTab.Tools) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == UbahkanTab.Settings,
                                        onClick = { currentTab = UbahkanTab.Settings },
                                        icon = {
                                            Icon(
                                                if (currentTab == UbahkanTab.Settings) Icons.Default.Settings
                                                else Icons.Outlined.Settings,
                                                contentDescription = strings.tabSettings
                                            )
                                        },
                                        label = {
                                            Text(
                                                strings.tabSettings,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (currentTab == UbahkanTab.Settings) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (currentTab) {
                                    UbahkanTab.Home -> {
                                        HomeScreen(
                                            ubahkanFiles = ubahkanFiles,
                                            isRefreshing = isRefreshing,
                                            hasNotificationPermission = hasNotificationPermission,
                                            onRefresh = { refreshFiles() },
                                            onPickFileClick = { filePickerLauncher.launch("*/*") },
                                            onRequestPermission = { askNotificationPermission() },
                                            onFileClick = { file -> openFileDirectly(context, file, strings) },
                                            onShareFile = { file -> shareFileDirectly(context, file, strings) },
                                            onDeleteFile = { file ->
                                                file.delete()
                                                refreshFiles()
                                            },
                                            onRetryConversion = { retryUri, retryFormat ->
                                                val serviceIntent = Intent(context, UbahkanService::class.java).apply {
                                                    putExtra("INPUT_URI", retryUri)
                                                    putExtra("TARGET_FORMAT", retryFormat)
                                                    putExtra("QUALITY_PRESET", prefs.qualityPreset)
                                                }
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    context.startForegroundService(serviceIntent)
                                                } else {
                                                    context.startService(serviceIntent)
                                                }
                                            },
                                            onBannerClick = { showProgressScreen = true }
                                        )

                                        // BottomSheet Picker Trigger
                                        if (showBottomSheet && selectedFilesForConversion.isNotEmpty()) {
                                            BottomSheetConverter(
                                                selectedFiles = selectedFilesForConversion,
                                                defaultQuality = prefs.qualityPreset,
                                                onDismiss = {
                                                    showBottomSheet = false
                                                    selectedFilesForConversion = emptyList()
                                                },
                                                onConvertStarted = {
                                                    showBottomSheet = false
                                                    selectedFilesForConversion = emptyList()
                                                    showProgressScreen = true
                                                }
                                            )
                                        }
                                    }

                                    UbahkanTab.Tools -> {
                                        ToolsScreen(
                                            onPickFileForTool = { filePickerLauncher.launch("*/*") },
                                            onBackClick = { currentTab = UbahkanTab.Home }
                                        )
                                    }

                                    UbahkanTab.Settings -> {
                                        SettingsScreen(
                                            currentTheme = currentThemeOption,
                                            onThemeChange = { newTheme ->
                                                currentThemeOption = newTheme
                                                prefs.themeOption = newTheme
                                            },
                                            onBackClick = { currentTab = UbahkanTab.Home },
                                            currentLanguage = currentLanguage,
                                            onLanguageChange = { newLang ->
                                                currentLanguage = newLang
                                                prefs.appLanguage = newLang
                                                AppStrings.setLanguage(newLang)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                        // --- LAYAR SHARE OVERLAY (Transparan) ---
                        if (showBottomSheet && selectedFilesForConversion.isNotEmpty()) {
                            LaunchedEffect(Unit) { askNotificationPermission() }

                            BottomSheetConverter(
                                selectedFiles = selectedFilesForConversion,
                                defaultQuality = prefs.qualityPreset,
                                onDismiss = {
                                    showBottomSheet = false
                                    selectedFilesForConversion = emptyList()
                                    finish() // Tutup overlay transparan
                                },
                                onConvertStarted = {
                                    showBottomSheet = false
                                    selectedFilesForConversion = emptyList()
                                    Toast.makeText(this@MainActivity, strings.conversionStartedBackgroundToast, Toast.LENGTH_SHORT).show()
                                    // PBI 8.2.3: Perilaku Tutup Otomatis
                                    if (prefs.closeAfterShare) {
                                        finish()
                                    } else {
                                        isStandaloneMode = true
                                        currentTab = UbahkanTab.Home
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

// --- UTILITY METHODS FOR FILE ACCESS & SHARING ---
fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name: String? = null
    try {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1) name = c.getString(nameIdx)
            }
        }
    } catch (_: Exception) {}
    return name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "File"
}

fun getFileSizeFromUri(context: Context, uri: Uri): Long {
    var size = 0L
    try {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIdx != -1) size = c.getLong(sizeIdx)
            }
        }
    } catch (_: Exception) {}
    return size
}

fun checkNotifPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true
}

fun getUbahkanFiles(): List<File> {
    val ubahkanDir = OutputManager.getUbahkanDirectory()
    return if (ubahkanDir.exists() && ubahkanDir.isDirectory) {
        ubahkanDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    } else {
        emptyList()
    }
}

fun getExplicitMimeType(extension: String): String {
    val ext = extension.lowercase()
    return when (ext) {
        "opus" -> "audio/opus"
        "ogg", "oga" -> "audio/ogg"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "aac" -> "audio/aac"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "svg" -> "image/svg+xml"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "epub" -> "application/epub+zip"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }
}

fun openFileDirectly(context: Context, file: File, strings: Strings = AppStrings.get()) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val mimeType = getExplicitMimeType(file.extension)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } 
        val chooserIntent = Intent.createChooser(intent, strings.openWithTitle(file.extension.uppercase()))
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, strings.noAppToOpenToast(file.extension.uppercase()), Toast.LENGTH_SHORT).show()
    }
}

fun shareFileDirectly(context: Context, file: File, strings: Strings = AppStrings.get()) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val mimeType = getExplicitMimeType(file.extension)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, strings.shareViaTitle))
    } catch (e: Exception) {
        Toast.makeText(context, strings.shareFailedToast, Toast.LENGTH_SHORT).show()
    }
}

fun checkBatteryOptimizationDisabled(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
}

fun openAppBatterySettings(context: Context, strings: Strings = AppStrings.get()) {
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(context, strings.batterySettingsGuideToast, Toast.LENGTH_LONG).show()
        }
    }
}