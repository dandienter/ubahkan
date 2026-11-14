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
