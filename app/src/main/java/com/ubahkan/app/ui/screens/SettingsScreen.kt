package com.ubahkan.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubahkan.app.BuildConfig
import com.ubahkan.app.UbahkanPrefs
import com.ubahkan.app.core.AppStrings
import com.ubahkan.app.core.EnglishStrings
import com.ubahkan.app.core.IndonesianStrings
import com.ubahkan.app.core.UpdateChecker
import com.ubahkan.app.core.UpdateStatus
import com.ubahkan.app.ui.components.UbahkanScreenHeader
import com.ubahkan.app.ui.theme.UbahkanBlue

// ============================================================
// SETTINGS SCREEN (UI 1.4.0 - bahasa modern ui-v6)
// Grup kartu putih melayang radius 22dp di latar abu lembut:
// Tampilan (tema ubin + bahasa), Konversi (kualitas pil +
// saklar biru), Lainnya (baterai, lisensi, tentang).
// ============================================================

@Composable
fun SettingsScreen(
    currentTheme: Int,
    onThemeChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: String? = null,
    onLanguageChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { UbahkanPrefs(context) }
    val strings = AppStrings.current

    var selectedLanguage by remember(currentLanguage) {
        mutableStateOf(currentLanguage ?: prefs.appLanguage)
    }
    val updateStatus by UpdateChecker.status.collectAsState()

    var keepName by remember { mutableStateOf(prefs.keepOriginalName) }
    var closeShare by remember { mutableStateOf(prefs.closeAfterShare) }
    var qualityPreset by remember { mutableStateOf(prefs.qualityPreset) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isBatteryOk by remember {
        mutableStateOf(com.ubahkan.app.checkBatteryOptimizationDisabled(context))
    }
    var showBatteryDialogInSettings by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isBatteryOk = com.ubahkan.app.checkBatteryOptimizationDisabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun applyLanguage(code: String) {
        selectedLanguage = code
        prefs.appLanguage = code
        AppStrings.setLanguage(code)
        onLanguageChange?.invoke(code)
        val msg = if (code == "en") {
            EnglishStrings.languageSetToast
        } else {
            IndonesianStrings.languageSetToast
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header - tanpa back button (bottom nav sudah ada)
        item {
            UbahkanScreenHeader(
                title = strings.settingsTitle,
                subtitle = strings.settingsSubtitle
            )
        }

        // ============ GRUP TAMPILAN ============
        item { SettingsGroupLabel(text = strings.settingsGroupAppearance) }
        item {
            SettingsGroupCard {
                // Tema Aplikasi: header tile + tiga ubin pilihan
                SettingsTileHeader(
                    icon = Icons.Outlined.WbSunny,
                    title = strings.themeSectionTitle,
                    desc = strings.themeDefaultNote
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(strings.themeLight, 1, Icons.Outlined.WbSunny),
                        Triple(strings.themeDark, 2, Icons.Outlined.DarkMode),
                        Triple(strings.themeSystem, 0, Icons.Outlined.DesktopWindows)
                    ).forEach { (title, idx, icon) ->
                        ThemeTile(
                            label = title,
                            icon = icon,
                            selected = currentTheme == idx,
                            onClick = { onThemeChange(idx) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SettingsGroupDivider()

                // Bahasa: baris tile + nilai + chevron, membuka dialog pilihan
                SettingsNavRow(
                    icon = Icons.Outlined.Language,
                    title = strings.languageSectionTitle,
                    value = if (selectedLanguage == "en") strings.langEnglish else strings.langIndonesian,
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        // ============ GRUP KONVERSI ============
        item { SettingsGroupLabel(text = strings.settingsGroupConversion) }
        item {
            SettingsGroupCard {
                // Kualitas Default: segmented pil modern
                SettingsTileHeader(
                    icon = Icons.Outlined.Tune,
                    title = strings.qualitySectionTitle,
                    desc = strings.qualitySectionDesc
                )
                Spacer(modifier = Modifier.height(12.dp))
                QualitySegmented(
                    selected = qualityPreset,
                    onSelect = { idx ->
                        qualityPreset = idx
                        prefs.qualityPreset = idx
                    }
                )
                val desc = when (qualityPreset) {
                    0 -> strings.qualityDescHigh
                    1 -> strings.qualityDescMedium
                    2 -> strings.qualityDescLow
                    3 -> strings.qualityDescInstant
                    else -> ""
                }
                if (desc.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                SettingsGroupDivider()

                SettingsSwitchRow(
                    title = strings.keepNameTitle,
                    desc = strings.keepNameDesc,
                    checked = keepName,
                    onCheckedChange = {
                        keepName = it
                        prefs.keepOriginalName = it
                    }
                )

                SettingsGroupDivider(indent = false)

                SettingsSwitchRow(
                    title = strings.closeShareTitle,
                    desc = strings.closeShareDesc,
                    checked = closeShare,
                    onCheckedChange = {
                        closeShare = it
                        prefs.closeAfterShare = it
                    }
                )
            }
        }

        // ============ GRUP LAINNYA ============
        item { SettingsGroupLabel(text = strings.settingsGroupOther) }
        item {
            SettingsGroupCard {
                // Baterai latar belakang
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBatteryDialogInSettings = true }
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsTileIcon(icon = Icons.Outlined.BatterySaver)
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.batteryOptTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBatteryOk) strings.batteryOptStatusOk else strings.batteryOptStatusRestricted,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isBatteryOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                SettingsGroupDivider(indent = false)

                // Lisensi sumber terbuka
                SettingsNavRow(
                    icon = Icons.Outlined.Description,
                    title = strings.licensesSectionTitle,
                    desc = strings.licensesSectionSubtitle,
                    onClick = { showLicensesDialog = true }
                )

                SettingsGroupDivider(indent = false)

                // Periksa Pembaruan: ketuk untuk cek manual; bila ada versi
                // baru, ketuk yang sama membuka halaman unduhnya.
                SettingsNavRow(
                    icon = Icons.Outlined.SystemUpdate,
                    title = strings.updateCheckTitle,
                    desc = when (val s = updateStatus) {
                        is UpdateStatus.Available -> strings.updateAvailableTitle(s.version)
                        UpdateStatus.Checking -> strings.updateStatusChecking
                        UpdateStatus.UpToDate -> strings.updateStatusUpToDate
                        UpdateStatus.Unknown -> strings.updateStatusNever
                    },
                    onClick = {
                        val s = updateStatus
                        if (s is UpdateStatus.Available) {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(s.downloadUrl))
                                )
                            }
                        } else {
                            UpdateChecker.checkNow(context)
                        }
                    }
                )

                SettingsGroupDivider(indent = false)

                // Tentang Ubahkan: keterangan penulis bisa diketuk, versi di kanan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsTileIcon(icon = Icons.Outlined.Info)
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.aboutSectionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.aboutAuthor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.instagram.com/dandisubhani_")
                                        )
                                    )
                                }
                            }
                        )
                    }
                    Text(
                        text = "v" + BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = strings.languageSectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    listOf(
                        Pair(strings.langIndonesian, "id"),
                        Pair(strings.langEnglish, "en")
                    ).forEach { (label, code) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    applyLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLanguage == code,
                                onClick = {
                                    applyLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(
                        text = strings.btnClose,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    if (showLicensesDialog) {
        OpenSourceLicensesDialog(onDismiss = { showLicensesDialog = false })
    }

    if (showBatteryDialogInSettings) {
        BatteryOptimizationGuideDialog(
            onDismiss = { showBatteryDialogInSettings = false },
            onOpenSettings = {
                showBatteryDialogInSettings = false
                com.ubahkan.app.openAppBatterySettings(context)
            }
        )
    }
}

// ============================================================
// KOMPONEN GRUP MODERN
// ============================================================

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsTileIcon(icon: ImageVector) {
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
}

@Composable
private fun SettingsTileHeader(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsTileIcon(icon = icon)
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsGroupDivider(indent: Boolean = true) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = if (indent) 45.dp else 0.dp, top = 2.dp, bottom = 2.dp)
    )
}

@Composable
private fun ThemeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) ink else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) ink else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) ink else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QualitySegmented(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val strings = AppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(
            Pair(strings.qualityLow, 2),
            Pair(strings.qualityMedium, 1),
            Pair(strings.qualityHigh, 0),
            Pair(strings.qualityInstant, 3)
        ).forEach { (title, idx) ->
            val isSelected = selected == idx
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(idx) }
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = UbahkanBlue,
                checkedThumbColor = Color.White,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    desc: String? = null,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsTileIcon(icon = icon)
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (desc != null) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(15.dp)
        )
    }
}

// ============================================================
// OPEN SOURCE LICENSES DIALOG
// ============================================================

@Composable
private fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    val strings = AppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = strings.licensesSectionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.licensesDialogIntro,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LicenseItemCard(
                    name = "FFmpegKit Full (v8.1.7)",
                    license = "GNU General Public License v3.0 (GPL-3.0) / LGPL-3.0",
                    copyright = "Copyright (c) Arlen Ceylan & FFmpeg Developers"
                )

                LicenseItemCard(
                    name = "AndroidSVG (v1.4)",
                    license = "Apache License, Version 2.0",
                    copyright = "Copyright 2013–2019 Paul LeBeau, Cave Rock Software Ltd."
                )

                LicenseItemCard(
                    name = "Kotlin, Coroutines & Android Jetpack (Compose)",
                    license = "Apache License, Version 2.0",
                    copyright = "Copyright JetBrains s.r.o. & The Android Open Source Project"
                )

                LicenseItemCard(
                    name = "Android Native SDK (PdfRenderer, PdfDocument, java.util.zip)",
                    license = "Apache License, Version 2.0",
                    copyright = "Copyright The Android Open Source Project"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.btnClose,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
private fun LicenseItemCard(
    name: String,
    license: String,
    copyright: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = license,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = copyright,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
