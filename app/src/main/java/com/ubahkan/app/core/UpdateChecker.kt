package com.ubahkan.app.core

import android.content.Context
import com.ubahkan.app.BuildConfig
import com.ubahkan.app.UbahkanPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/*
 * Copyright (C) 2026 Rizki Fauzi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// ============================================================
// PEMERIKSA PEMBARUAN
// Satu-satunya fitur yang memakai internet: menanyakan rilis
// terbaru ke GitHub dan membandingkan nomor versinya dengan
// versi terpasang. Berkas pengguna tidak pernah dikirim.
// Kegagalan jaringan selalu diam, tanpa dialog atau pesan galat.
// ============================================================

sealed class UpdateStatus {
    object Unknown : UpdateStatus()
    object Checking : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Available(val version: String, val downloadUrl: String) : UpdateStatus()
}

object UpdateChecker {

    private const val LATEST_RELEASE_ENDPOINT =
        "https://api.github.com/repos/dandienter/ubahkan/releases/latest"
    private const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L
    private const val NETWORK_TIMEOUT_MS = 8000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Unknown)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    @Volatile
    private var restored = false

    /** Pulihkan status Available dari penyimpanan lokal (sekali per proses). */
    fun init(context: Context) {
        if (restored) return
        restored = true
        val prefs = UbahkanPrefs(context.applicationContext)
        val tag = prefs.updateAvailableTag
        val url = prefs.updateDownloadUrl
        if (!tag.isNullOrBlank() && !url.isNullOrBlank() && isNewerVersion(tag, BuildConfig.VERSION_NAME)) {
            _status.value = UpdateStatus.Available(tag, url)
        }
    }

    /** Cek otomatis saat aplikasi mulai: hanya bila cek terakhir sudah lewat 12 jam. */
    fun checkOnStart(context: Context) {
        init(context)
        val prefs = UbahkanPrefs(context.applicationContext)
        val last = prefs.lastUpdateCheck
        val stale = last <= 0L || System.currentTimeMillis() - last > CHECK_INTERVAL_MS
        if (stale) checkNow(context)
    }

    /** Cek paksa sekarang, tanpa memandang jeda 12 jam. */
    fun checkNow(context: Context) {
        if (_status.value is UpdateStatus.Checking) return
        val previous = _status.value
        _status.value = UpdateStatus.Checking
        val appContext = context.applicationContext
        scope.launch {
            val result = fetchLatestRelease()
            val prefs = UbahkanPrefs(appContext)
            prefs.lastUpdateCheck = System.currentTimeMillis()
            if (result == null) {
                // Jaringan gagal: diam, kembalikan status sebelum memeriksa.
                _status.value = previous
                return@launch
            }
            val (latestVersion, downloadUrl) = result
            if (isNewerVersion(latestVersion, BuildConfig.VERSION_NAME)) {
                prefs.updateAvailableTag = latestVersion
                prefs.updateDownloadUrl = downloadUrl
                _status.value = UpdateStatus.Available(latestVersion, downloadUrl)
            } else {
                prefs.updateAvailableTag = null
                prefs.updateDownloadUrl = null
                _status.value = UpdateStatus.UpToDate
            }
        }
    }

    /** Hasil: pasangan (versi tanpa awalan "v", URL unduh atau halaman rilis). */
    private fun fetchLatestRelease(): Pair<String, String>? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(LATEST_RELEASE_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").trim()
            if (tag.isBlank()) return null
            val version = tag.removePrefix("v").removePrefix("V")
            val htmlUrl = json.optString("html_url")
            var downloadUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val assetUrl = asset.optString("browser_download_url")
                    if (assetUrl.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = assetUrl
                        break
                    }
                }
            }
            val finalUrl = downloadUrl ?: htmlUrl
            if (finalUrl.isBlank()) null else Pair(version, finalUrl)
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** Bandingkan semver numerik per komponen, abaikan akhiran non-numerik. */
    internal fun isNewerVersion(candidate: String, current: String): Boolean {
        val candidateParts = parseVersion(candidate)
        val currentParts = parseVersion(current)
        val size = maxOf(candidateParts.size, currentParts.size)
        for (i in 0 until size) {
            val c = candidateParts.getOrElse(i) { 0 }
            val u = currentParts.getOrElse(i) { 0 }
            if (c != u) return c > u
        }
        return false
    }

    private fun parseVersion(version: String): List<Int> {
        return version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split('.', '-', '+')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    }
}
