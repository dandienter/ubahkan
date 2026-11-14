package com.ubahkan.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// UBAHKAN DESIGN SYSTEM  -  COLOR TOKENS v2.0 (MINIMALIS)
// Palet mockup: latar putih tulang, tinta hitam hangat,
// abu hangat sebagai sekunder, satu aksen biru.
// Ungu/indigo/pink dari identitas lama sudah dibuang total.
// ============================================================

// --- Aksen tunggal: biru ---
val UbahkanBlue           = Color(0xFF1565C0)   // Aksen utama, CTA biru, ikon kategori
val UbahkanBlueDark       = Color(0xFF0D47A1)   // Varian gelap / pressed / teks di container pucat
val UbahkanBlueLight      = Color(0xFF7FB2F0)   // Biru terang untuk mode gelap
val UbahkanBlueContainer  = Color(0xFFEAF1F9)   // Biru sangat pucat: tile ikon, chip terpilih lembut
val UbahkanBlueContainerDark = Color(0xFF1D2F45) // Container biru untuk mode gelap

// --- Tinta & netral hangat ---
val UbahkanInk            = Color(0xFF141414)   // Teks utama terang + tombol primer hitam
val UbahkanGray           = Color(0xFF55554E)   // Teks chip / netral menengah
val UbahkanSegmentTrack   = Color(0xFFF1F1EC)   // Rel segmented control (terang)

// --- Primary & Brand (nama token dipertahankan, nilai baru) ---
val UbahkanPrimary        = UbahkanBlue
val UbahkanPrimaryDark    = UbahkanBlueDark
val UbahkanPrimaryLight   = UbahkanBlueLight
val UbahkanPrimaryContainer = UbahkanBlueContainer
val UbahkanPrimaryContainerDark = UbahkanBlueContainerDark

// --- Secondary & Accent ---
val UbahkanSecondary      = UbahkanGray       // Netral hangat, bukan lagi ungu muda
val UbahkanAccentPink     = UbahkanBlue       // Nama lama dipertahankan; pink sudah dibuang

// --- Instant Bolt ---
val UbahkanInstantBolt    = Color(0xFFF5B51B)   // Ikon petir Instant  -  SELALU KUNING

// --- Semantic ---
val UbahkanSuccess        = Color(0xFF249B69)   // Berhasil
val UbahkanWarning        = Color(0xFFD99A27)   // Peringatan
val UbahkanError          = Color(0xFFD94B55)   // Error / cancel

// --- Light Theme Surfaces ---
val UbahkanLightBackground      = Color(0xFFFCFCFA) // Putih tulang
val UbahkanLightSurface         = Color(0xFFFFFFFF)
val UbahkanLightSurfaceVariant  = Color(0xFFF4F4F0) // Tile ikon / permukaan redup
val UbahkanLightTextPrimary     = Color(0xFF141414)
val UbahkanLightTextSecondary   = Color(0xFF83837B) // Abu hangat
val UbahkanLightMuted           = Color(0xFFA3A39B) // Label redup
val UbahkanLightOutline         = Color(0xFFE7E7E0) // Garis kartu
val UbahkanLightDivider         = Color(0xFFEFEFEA) // Garis pemisah baris

// --- Dark Theme Surfaces ---
val UbahkanDarkBackground       = Color(0xFF131311)
val UbahkanDarkSurface          = Color(0xFF1C1C1A)
val UbahkanDarkSurfaceVariant   = Color(0xFF262624)
val UbahkanDarkTextPrimary      = Color(0xFFF2F2ED)
val UbahkanDarkTextSecondary    = Color(0xFFA8A8A0)
val UbahkanDarkMuted            = Color(0xFF7C7C74)
val UbahkanDarkOutline          = Color(0xFF33332F)

// --- Error containers ---
val UbahkanErrorContainerLight  = Color(0xFFFDE8EA)
val UbahkanErrorContainerDark   = Color(0xFF3D1518)

// --- Legacy aliases (nama lama, nilai palet baru) ---
val UbahkanIndigo     = UbahkanBlue
val UbahkanPink       = UbahkanBlue
val UbahkanViolet     = Color(0xFF4C9AFF)
val UbahkanIndigoLight = UbahkanBlueLight
val UbahkanIndigoDark  = UbahkanBlueDark
val UbahkanVioletLight = Color(0xFF4C9AFF)
val UbahkanPinkLight   = Color(0xFFBFD9F7)
val UbahkanLightBorder = UbahkanLightOutline
val UbahkanDarkBorder  = UbahkanDarkOutline
val UbahkanLightOnSurface = UbahkanLightTextPrimary
val UbahkanLightOnSurfaceVariant = UbahkanLightTextSecondary
val UbahkanDarkOnSurface = UbahkanDarkTextPrimary
val UbahkanDarkOnSurfaceVariant = UbahkanDarkTextSecondary
val UbahkanLightBg = UbahkanLightBackground
val UbahkanDarkBg = UbahkanDarkBackground
val UbahkanDarkSurfaceCard = UbahkanDarkSurface
val UbahkanLightSurfaceCard = UbahkanLightSurface
