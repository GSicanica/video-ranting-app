import java.util.Properties
import java.io.FileInputStream

plugins {
    id("youtube.android.application")
    id("youtube.android.compose")
}

kotlin {
    compilerOptions {
        // Launcher depends on :composeApp, which can pull newer stdlib metadata transitively.
        // Keep compilation stable until Kotlin plugin/toolchain is fully aligned.
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val compileSdkInt = providers.gradleProperty("android.compileSdk").orNull?.toInt() ?: 36
val targetSdkInt = providers.gradleProperty("android.targetSdk").orNull?.toInt() ?: 35
val minSdkInt = providers.gradleProperty("android.minSdk").orNull?.toInt() ?: 24
val versionCodeInt = providers.gradleProperty("app.versionCode").orNull?.toInt() ?: 67
val versionNameStr = providers.gradleProperty("app.versionName").orNull ?: "5.0"
val releaseKeystorePropertiesFile = rootProject.file("androidApp/src/key.properties")
val releaseKeystoreProperties = Properties()
if (releaseKeystorePropertiesFile.exists()) {
    releaseKeystoreProperties.load(FileInputStream(releaseKeystorePropertiesFile))
}
val requiredReleaseKeystoreKeys = listOf("keyAlias", "keyPassword", "storeFile", "storePassword")
val missingReleaseKeystoreKeys = requiredReleaseKeystoreKeys.filter {
    releaseKeystoreProperties.getProperty(it).isNullOrBlank()
}
val releaseSigningError = when {
    !releaseKeystorePropertiesFile.exists() ->
        "Release signing requires androidApp/src/key.properties."
    missingReleaseKeystoreKeys.isNotEmpty() ->
        "Release signing key.properties is missing: ${missingReleaseKeystoreKeys.joinToString(", ")}."
    else -> null
}
val releaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("Release")
}
if (releaseSigningError != null && releaseTaskRequested) {
    throw GradleException(
        "$releaseSigningError Required keys: ${requiredReleaseKeystoreKeys.joinToString(", ")}."
    )
}

android {
    // Thin launcher module: all Kotlin sources + res/assets are compiled/packaged from `:composeApp`.
    namespace = "com.youtube.rating.androidApp"
    compileSdk = compileSdkInt

    defaultConfig {
        applicationId = "com.youtube.rating.android"
        // Some Compose artifacts in this project require 23+.
        // minSdk=24 => Android 7.0+.
        minSdk = minSdkInt
        targetSdk = targetSdkInt
        versionCode = versionCodeInt
        versionName = versionNameStr

        // Vector drawable support
        vectorDrawables.useSupportLibrary = true

        // Use AndroidX Test instrumentation runner for instrumentation tests
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Keep builds from accidentally shipping a committed DSN.
        // Configure Sentry explicitly from local/CI properties when direct client reporting is needed.
        manifestPlaceholders["sentryDsn"] = ""

        // Defaults (can be overridden per build type)
        manifestPlaceholders["sentryTracesSampleRate"] = "0.1"
        manifestPlaceholders["sentryEnvironment"] = "release"
        // Profiling defaults (can be overridden per build type)
        manifestPlaceholders["sentryProfilingSessionSampleRate"] = "0.0"
        manifestPlaceholders["sentryProfilingLifecycle"] = "trace"
        manifestPlaceholders["sentryProfilingStartOnAppStart"] = "true"
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        manifestPlaceholders["googleMapsApiKey"] = localProperties.getProperty("GOOGLE_MAPS_API_KEY") ?: ""

        // NDK filters - support all architectures for AAB/Play Store
        // Play Store will handle per-device delivery automatically
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

    }

    // APK splits - ONLY for debug/testing with direct APK install
    // DISABLED for release builds (use AAB for Play Store instead)
    splits {
        abi {
            // Enable splits only for debug builds
            // For release AAB, Play Store handles architecture splits automatically
            isEnable = false  // Changed from true - AAB handles splits automatically
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false  // No need for universal when using AAB
        }
    }

    signingConfigs {
        create("release") {
            if (releaseSigningError == null) {
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
                storeFile = file(releaseKeystoreProperties.getProperty("storeFile"))
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildFeatures { buildConfig = true } // no sources in :androidApp (see :composeApp)

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.md",
                "META-INF/*.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
            pickFirsts += listOf(
                "META-INF/io.netty.versions.properties"
            )
        }
        jniLibs {
            useLegacyPackaging = false
            // Avoid bundling optional NEON-only armeabi-v7a natives to improve compatibility
            // on older 32-bit devices without NEON support.
            excludes += setOf("lib/armeabi-v7a/*_neon.so")
        }
        // Exclude baseline profiles from APK to avoid installation errors
        dex {
            useLegacyPackaging = false
        }
    }

    buildTypes {
        debug {
            // Enable basic optimizations for better performance in debug builds
            isMinifyEnabled = false
            isShrinkResources = false

            // Allow debug + release to be installed side-by-side
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            // Set app label for debug
            manifestPlaceholders["appLabel"] = "@string/app_name_debug"
            manifestPlaceholders["sentryEnvironment"] = "debug"
            // Avoid hitting Sentry quotas while developing (429 spam). Opt-in via local.properties if needed.
            manifestPlaceholders["sentryTracesSampleRate"] = "0.0"
            manifestPlaceholders["sentryProfilingSessionSampleRate"] = "0.0"
            manifestPlaceholders["sentryProfilingLifecycle"] = "trace"
            manifestPlaceholders["sentryProfilingStartOnAppStart"] = "true"
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            manifestPlaceholders["sentryDsn"] = localProperties.getProperty("SENTRY_DSN") ?: ""

            // Optimize DEX compilation for faster startup
            packaging {
                dex {
                    useLegacyPackaging = false
                }
            }
        }
        release {
            // Enable FULL optimizations for maximum performance
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = signingConfigs.getByName("release")

            // Set app label for release
            manifestPlaceholders["appLabel"] = "@string/app_name"
            manifestPlaceholders["sentryEnvironment"] = "release"
            manifestPlaceholders["sentryTracesSampleRate"] = "0.1"
            // Enable profiling in release only if you explicitly want it. Keep low by default.
            manifestPlaceholders["sentryProfilingSessionSampleRate"] = "0.1"
            manifestPlaceholders["sentryProfilingLifecycle"] = "trace"
            manifestPlaceholders["sentryProfilingStartOnAppStart"] = "true"
            manifestPlaceholders["sentryDsn"] = localProperties.getProperty("SENTRY_DSN") ?: ""
            // Release should stay HTTPS/WSS-only. If you ever need cleartext for a specific dev host,
            // use the debug-only network security config instead of enabling global cleartext here.
            manifestPlaceholders["usesCleartextTraffic"] = "false"

            proguardFiles(
                // Use OPTIMIZED ProGuard config for better performance
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // CRITICAL: Keep source file names and line numbers for crash reports
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            // R8 full mode matching fallbacks for optimized builds
            matchingFallbacks += listOf("release")

            // Optimize DEX for faster startup
            packaging {
                resources {
                    excludes += listOf(
                        "META-INF/*.kotlin_module",
                        "META-INF/com/android/build/gradle/*"
                    )
                }
                jniLibs {
                    useLegacyPackaging = false
                }
                dex {
                    useLegacyPackaging = false
                }
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // Keep only selected languages to reduce app size.
        localeFilters += listOf("en", "hr", "de")
        // Expose per-app language picker on Android 13+ from packaged resources.
        generateLocaleConfig = true
    }

    dependenciesInfo {
        // Avoid embedding dependency metadata blobs in release artifacts.
        includeInApk = false
        includeInBundle = false
    }

    // Keep lint strict on errors so release issues are surfaced in CI.
    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    // Bundle configuration for 16KB page size support and ABI splits
    bundle {
        abi {
            enableSplit = true
        }
    }

    sourceSets {
        getByName("main") {
            // Launcher module is intentionally thin; compile only launcher MainActivity from this module.
            java.setSrcDirs(listOf("src/launcher/kotlin"))
        }
    }

}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
