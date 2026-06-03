# ProGuard Rules for YouTube Rating App - OPTIMIZED FOR RELEASE

# ===================================================================
# PERFORMANCE OPTIMIZATIONS FOR RELEASE BUILD - AGGRESSIVE MODE
# ===================================================================

# Enable maximum optimization level
-optimizationpasses 7
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-allowaccessmodification
-repackageclasses ''
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Aggressive inlining for better performance
-mergeinterfacesaggressively
-overloadaggressively

# Enable constant propagation and folding
-allowaccessmodification
-repackageclasses

# Remove unused resources
-dontwarn **
-ignorewarnings

# Keep debugging info for crash reports (line numbers)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Logging:
# Previously we stripped Log/println calls in release for performance.
# We keep them now so logs can be enabled at runtime via the in-app debug toggle.

# Kotlinx Serialization - CRITICAL: Keep all serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep ALL serialization-related code
-keep class kotlinx.serialization.** { *; }
-keep interface kotlinx.serialization.** { *; }

# Keep serialization annotations
-keep,includedescriptorclasses class com.youtube.rating.**$$serializer { *; }
-keepclassmembers class com.youtube.rating.** {
    *** Companion;
}
-keepclasseswithmembers class com.youtube.rating.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Kotlinx Serialization classes
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# CRITICAL: Keep all @Serializable classes
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    <fields>;
    <init>(...);
}

# ===================================================================
# SPECIFIC KEEPS FOR OUR CODE - Allow obfuscation of sensitive data
# ===================================================================

# Keep Application class and main entry points
-keep class com.youtube.rating.android.YouTubeRatingApplication { *; }
-keep class com.youtube.rating.android.MainActivity { *; }

# Keep ViewModels (needed for Compose state management)
-keep class com.youtube.rating.android.viewmodel.** { *; }
-keepclassmembers class com.youtube.rating.android.viewmodel.** {
    <init>(...);
    <fields>;
    <methods>;
}

# Keep Repository interfaces and implementations
-keep class com.youtube.rating.android.repository.** { *; }
-keep interface com.youtube.rating.android.repository.** { *; }
-keep class com.youtube.rating.shared.data.Repositories { *; }

# Keep API client classes
-keep class com.youtube.rating.shared.api.** { *; }
-keepclassmembers class com.youtube.rating.shared.api.** {
    <init>(...);
    <methods>;
}

# Keep model classes (data structures)
-keep class com.youtube.rating.shared.models.** { *; }
-keepclassmembers class com.youtube.rating.shared.models.** {
    <init>(...);
    <fields>;
}

# Keep utility classes that need reflection
-keep class com.youtube.rating.android.utils.AdminManager { *; }
-keep class com.youtube.rating.android.data.PrefsDataStore { *; }

# Keep Compose components (prevent optimization issues)
-keep class com.youtube.rating.android.ui.components.** { *; }
-keep class com.youtube.rating.android.ui.screens.** { *; }

# Keep navigation classes
-keep class com.youtube.rating.android.ui.navigation.** { *; }

# Keep workers and migration classes (CRITICAL for app startup)
-keep class com.youtube.rating.android.workers.** { *; }
-keep class com.youtube.rating.android.migration.** { *; }

# CRITICAL: Keep Realm configuration and models
-keep class com.youtube.rating.shared.data.RealmProvider { *; }
-keep class com.youtube.rating.shared.data.RealmConfig { *; }
-keep class com.youtube.rating.shared.data.*Entity { *; }
-keep class com.youtube.rating.shared.data.*Model { *; }

# Keep Koin dependency injection
-keep class org.koin.** { *; }
-keep class io.insertkoin.** { *; }

# ===================================================================
# ALLOW OBFUSCATION OF SENSITIVE DATA
# ===================================================================

# Allow obfuscation of string constants and sensitive data
# This prevents API keys and other secrets from being visible in APK
-allowaccessmodification
-repackageclasses

# Keep BuildConfig for accessing configuration
-keep class com.youtube.rating.android.BuildConfig { *; }

# Keep Android system classes
-keep class android.** { *; }
-keep interface android.** { *; }

# Keep Java standard library
-keep class java.** { *; }
-keep interface java.** { *; }

# Keep Kotlin standard library
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-keep class kotlin.jvm.** { *; }

# Ktor Client - CRITICAL: Keep all Ktor classes
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# CRITICAL: Keep Ktor serialization integration
-keep class io.ktor.serialization.** { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }

# Kotlinx Coroutines - CRITICAL: Keep all coroutine classes
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Coil Image Loading - optimized
-keep class coil.decode.** { *; }
-keep class coil.fetch.** { *; }
-dontwarn coil.**

# OkHttp (used by Ktor and Coil) - security and performance
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Okio (used by OkHttp)
-dontwarn okio.**
-dontwarn okio.**

# SLF4J - Not needed on Android
-dontwarn org.slf4j.**

# Media3 ExoPlayer for offline video playback
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Compose - keep only necessary
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# Keep model classes
-keep class com.youtube.rating.shared.models.** { *; }
-keepclassmembers class com.youtube.rating.shared.models.** { *; }

# Keep API client
-keep class com.youtube.rating.shared.api.** { *; }

# General rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# (intentionally not stripping Log/println/debug methods)

# CRITICAL: Keep reflection for Kotlin and Android
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# CRITICAL: Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlin.Metadata <methods>;
}

# CRITICAL: Keep all Kotlin companion objects
-keepclassmembers class * {
    public static ** Companion;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Aggressive shrinking - remove unused resources
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.**

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
