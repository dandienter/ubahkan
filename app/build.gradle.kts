import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.ubahkan.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ubahkan.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "1.5.0"

        // Tekan ukuran APK: hanya arm64-v8a. Dukungan 32-bit
        // (armeabi-v7a) dan x86/x86_64 dilepas sejak 1.4.0.
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    packaging {
        jniLibs {
            // .so disimpan terkompresi di APK agar unduhan lebih kecil
            useLegacyPackaging = true
            // OpenCV dan FFmpegKit sama-sama membawa libc++_shared.so;
            // tanpa pickFirst build gagal di mergeReleaseNativeLibs
            // (terbukti di run Actions #21 untuk modul Pindai Dokumen).
            pickFirsts += "lib/arm64-v8a/libc++_shared.so"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            // Hanya diisi bila local.properties menyediakan keystore rilis.
            // Di CI berkas itu tidak ada, dan file("") membuat konfigurasi gagal.
            val storePath = localProps.getProperty("RELEASE_STORE_FILE")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            // Rilis resmi sejak 1.4.0: ditandatangani keystore tetap lewat
            // local.properties di CI, minify + shrink resources aktif sama
            // seperti debug agar artefak rilis ikut ramping.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // CI membangun assembleDebug: minify + shrink resources aktif
            // di sini agar artefak debug ikut ramping. Aturan keep aman
            // untuk FFmpegKit dan Compose ada di proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // fork komunitas yang aktif di-maintain tahun ini (full variant: includes libvpx, libvorbis, opus, lame)
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7")
    // Tambahkan baris di bawah ini untuk menambal crash "NoClassDefFoundError"
    implementation("com.arthenica:smart-exception-java:0.2.1")
    implementation("androidx.compose.material:material:1.5.4") // Dibutuhkan untuk pullrefresh
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    // SVG Renderer (PBI Sprint 3 - Apache 2.0)
    implementation("com.caverock:androidsvg-aar:1.4")

    // Pindai Dokumen (1.5.0): kamera CameraX untuk tangkap + analisis tepi
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    // Pindai Dokumen: OpenCV untuk deteksi tepi, warp perspektif, dan filter
    implementation("org.opencv:opencv:4.10.0")
    // Pindai Dokumen: OCR ML Kit bundled (model di dalam APK, bekerja offline)
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
}