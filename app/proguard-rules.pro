# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ===============================
# SECURITY & OBFUSCATION RULES
# ===============================

# Keep source file names and line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove all logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove debug information
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ===============================
# FIREBASE RULES
# ===============================

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }

# ===============================
# COMPOSE RULES
# ===============================

# Keep Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ===============================
# APP-SPECIFIC RULES
# ===============================

# Keep device registration classes (sensitive security code)
-keep class com.pramanshav.unilocator.utils.DeviceRegistrationManager { *; }
-keep class com.pramanshav.unilocator.utils.DeviceIdGenerator { *; }

# Keep data classes for Firebase serialization
-keep class com.pramanshav.unilocator.data.** { *; }

# Keep repository classes
-keep class com.pramanshav.unilocator.repository.** { *; }

# ===============================
# NETWORK SECURITY
# ===============================

# Keep Retrofit classes
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Keep model classes for JSON serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===============================
# SECURITY HARDENING
# ===============================

# Obfuscate sensitive method names (but keep for functionality)
-keepclassmembers class com.pramanshav.unilocator.utils.DeviceRegistrationManager {
    public <methods>;
}

# Remove test code from release builds
-assumenosideeffects class com.pramanshav.unilocator.** {
    *** test*(...);
    *** debug*(...);
}

# Optimize and shrink
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
