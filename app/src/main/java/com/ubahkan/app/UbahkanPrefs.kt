package com.ubahkan.app

import android.content.Context

// --- PENGELOLA PREFERENSI & PENGATURAN (PBI 8.2) ---
class UbahkanPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("ubahkan_settings", Context.MODE_PRIVATE)

    var themeOption: Int // 0: System, 1: Light, 2: Dark
        // Bawaan instalasi baru: Terang (1). Pilihan lain tetap tersimpan seperti biasa.
        get() = prefs.getInt("theme", 1)
        set(value) = prefs.edit().putInt("theme", value).apply()

    var toolsViewMode: Int // 0: List, 1: Grid (tab Alat)
        get() = prefs.getInt("tools_view", 0)
        set(value) = prefs.edit().putInt("tools_view", value).apply()

    var savedBytesTotal: Long // Akumulasi ruang yang dihemat dari konversi yang tercatat ukurannya
        get() = prefs.getLong("saved_bytes_total", 0L)
        set(value) = prefs.edit().putLong("saved_bytes_total", value).apply()

    var countedJobIds: Set<String> // ID pekerjaan yang hematnya sudah masuk savedBytesTotal
        get() = prefs.getStringSet("counted_job_ids", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("counted_job_ids", value).apply()

    var showNotifications: Boolean
        get() = prefs.getBoolean("notif", true)
        set(value) = prefs.edit().putBoolean("notif", value).apply()

    var keepOriginalName: Boolean
        get() = prefs.getBoolean("keep_name", true)
        set(value) = prefs.edit().putBoolean("keep_name", value).apply()

    var closeAfterShare: Boolean
        get() = prefs.getBoolean("close_share", true)
        set(value) = prefs.edit().putBoolean("close_share", value).apply()

    var qualityPreset: Int // 0: Tinggi, 1: Sedang, 2: Rendah, 3: Instan (Lossless)
        get() = prefs.getInt("quality", 0)
        set(value) = prefs.edit().putInt("quality", value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) = prefs.edit().putBoolean("onboarding_done", value).apply()
        
    var appLanguage: String // "id": Bahasa Indonesia, "en": English
        get() = prefs.getString("app_language", "id") ?: "id"
        set(value) = prefs.edit().putString("app_language", value).apply()
        
    var dismissBatteryWarning: Boolean
        get() = prefs.getBoolean("dismiss_battery_warning", false)
        set(value) = prefs.edit().putBoolean("dismiss_battery_warning", value).apply()
}
