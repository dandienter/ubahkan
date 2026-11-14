# Ubahkan - aturan ProGuard/R8
# Dipakai buildType debug (CI assembleDebug) sejak 1.4.0 agar APK ramping.

# FFmpegKit (native + JNI): jangan dikecilkan atau diganti namanya
-keep class dev.ffmpegkit.** { *; }
-keep class com.arthenica.** { *; }
-dontwarn dev.ffmpegkit.**
-dontwarn com.arthenica.**

# Metode native (JNI) tidak boleh diganti namanya
-keepclasseswithmembernames class * {
    native <methods>;
}

# Compose: keep bawaan untuk runtime dan compiler-generated code
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.compose.**

# OpenCV (Pindai Dokumen): kelas JNI dan callback native tidak boleh diganti nama
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# ML Kit OCR (Pindai Dokumen): model bundled dan API teks
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit_vision_text.**
-dontwarn com.google.android.gms.internal.mlkit_common.**
