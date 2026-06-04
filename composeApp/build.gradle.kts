import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.jvm.tasks.Jar
import java.util.Properties

plugins {
    id("youtube.kmp.library")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Incremental migration step:
// - `androidApp` becomes a thin launcher (manifest + buildTypes/signing).
// - All existing Android Kotlin sources + res/assets are compiled/packaged from this module.
//
// This keeps the app running while we gradually refactor toward a true KMP `composeApp` like in `Multiplatform-App`.

val compileSdkInt = providers.gradleProperty("android.compileSdk").orNull?.toInt() ?: 36
val targetSdkInt = providers.gradleProperty("android.targetSdk").orNull?.toInt() ?: 35
val minSdkInt = providers.gradleProperty("android.minSdk").orNull?.toInt() ?: 24
val versionCodeInt = providers.gradleProperty("app.versionCode").orNull?.toInt() ?: 67
val versionNameStr = providers.gradleProperty("app.versionName").orNull ?: "5.0"

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val configuredBaseUrl: String? =
    localProperties.getProperty("BASE_URL") ?: providers.gradleProperty("app.baseUrl").orNull

val bibleSourceDir = rootProject.file("backend-php/output1")
val bibleAssetsOutDir = layout.buildDirectory.dir("generated/bibleAssets/output1")

val syncBibleAssets by tasks.registering(Sync::class) {
    onlyIf { bibleSourceDir.exists() }
    from(bibleSourceDir)
    into(bibleAssetsOutDir)
    include("**/*.md")
}

android {
    // Keep the original namespace so existing Kotlin code (package `com.youtube.rating.android.*`)
    // continues to resolve `R` and `BuildConfig` without mass refactors.
    namespace = "com.youtube.rating.android"
    compileSdk = compileSdkInt

    defaultConfig {
        minSdk = minSdkInt

        // Keep BuildConfig shape identical to what `androidApp` used to provide.
        buildConfigField("String", "VERSION_NAME", "\"$versionNameStr\"")
        buildConfigField("int", "VERSION_CODE", versionCodeInt.toString())
        buildConfigField("String", "APPLICATION_ID", "\"com.youtube.rating.android\"")
        // Default to empty so builds do not accidentally ship a committed DSN.
        // CI/local properties may opt into direct Sentry client reporting.
        buildConfigField("String", "SENTRY_DSN", "\"\"")
        buildConfigField("int", "MIN_SDK", minSdkInt.toString())
        buildConfigField("int", "TARGET_SDK", targetSdkInt.toString())
        buildConfigField("int", "COMPILE_SDK", compileSdkInt.toString())
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("String", "DEBUG_UNLOCK_PASSWORD", "\"\"")
            buildConfigField(
                "String",
                "BASE_URL",
                "\"${configuredBaseUrl ?: "https://tmbv-hms.com/backend-fastapi"}\""
            )
            buildConfigField("boolean", "USE_LOCAL_SERVER", "false")
            buildConfigField(
                "String",
                "SENTRY_DSN",
                "\"${localProperties.getProperty("SENTRY_DSN") ?: ""}\""
            )
        }
        release {
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            buildConfigField("String", "DEBUG_UNLOCK_PASSWORD", "\"\"")
            buildConfigField("String", "BASE_URL", "\"${configuredBaseUrl ?: "https://tmbv-hms.com/backend-fastapi"}\"")
            buildConfigField("boolean", "USE_LOCAL_SERVER", "false")
            buildConfigField(
                "String",
                "SENTRY_DSN",
                "\"${localProperties.getProperty("SENTRY_DSN") ?: ""}\""
            )
        }
    }

    sourceSets {
        // Bible markdown files live in backend-php/output1 and are synced into build/generated/bibleAssets/output1
        // so they are packaged as Android assets/output1/**.
        getByName("main").apply {
            assets.srcDirs(
                "src/androidMain/assets",
                layout.buildDirectory.dir("generated/bibleAssets")
            )
        }
    }

    // Ensure generated Bible assets are available when building (no-op if backend-php/output1 missing).
    tasks.matching { it.name.endsWith("PreBuild") }.configureEach { dependsOn(syncBibleAssets) }

    buildFeatures {
        compose = true
        // KmpLibraryConventionPlugin disables BuildConfig by default; this module still uses BuildConfig.
        buildConfig = true
    }
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    freeCompilerArgs.addAll(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-Xjvm-default=all-compatibility",
                        "-Xskip-metadata-version-check",
                        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                        "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                // Multiplatform-ready dependencies (safe to keep in commonMain even while Android-only).
                implementation(libs.koin.core)
                implementation(libs.serialization.json)
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.no.arg)
            }
        }
        val androidMain by getting {
            kotlin.exclude("**/feature/running/**")
            dependencies {
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.graphics)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.material3)
                implementation(libs.compose.material)
                implementation(libs.compose.icons.extended)

                implementation(libs.activity.compose)
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.lifecycle.viewmodel.ktx)
                implementation(libs.lifecycle.runtime.compose)
                implementation(libs.navigation.compose)

                implementation(libs.lifecycle.process)
                implementation(libs.core.ktx)
                implementation(libs.datastore.preferences)

                implementation(libs.coil.compose)
                implementation(libs.coroutines.android)
                implementation(libs.work.runtime.ktx)

                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)

                implementation(libs.security.crypto)

                implementation(libs.media3.exoplayer)
                implementation(libs.media3.ui)
                implementation(libs.media3.exoplayer.dash)
                implementation(libs.media3.exoplayer.hls)
                implementation(libs.media3.session)

                implementation(libs.youtube.player)
                implementation(libs.sentry.android)
                implementation(libs.okhttp)

                implementation(libs.compose.markdown)

                implementation(libs.paging.runtime)
                implementation(libs.paging.runtime.ktx)
                implementation(libs.paging.compose)

                implementation(libs.animation.core)

                implementation(libs.profileinstaller)

                implementation(libs.play.services.maps)
                implementation(libs.maps.compose)
                implementation(libs.kuiver.android)

                implementation(libs.bundles.compose.screen)
                implementation(project(":core:core-data"))
                implementation(project(":core:core-presentation"))
                implementation(project(":core:core-design-system"))
                implementation(project(":feature:habittracker-data"))
                implementation(project(":feature:habittracker-presentation"))
                implementation(project(":feature:running-data"))
                implementation(project(":feature:running-presentation"))
                implementation(project(":feature:home-data"))
                implementation(project(":feature:home-presentation"))
                implementation(project(":feature:rating-data"))
                implementation(project(":feature:favorites-data"))
                implementation(project(":feature:favorites-presentation"))
                implementation(project(":feature:watchhistory-data"))
                implementation(project(":feature:watchhistory-domain"))
                implementation(project(":feature:watchhistory-presentation"))
                implementation(project(":feature:offlinevideos-data"))
                implementation(project(":feature:offlinevideos-presentation"))
                implementation(project(":feature:gallery-data"))
                implementation(project(":feature:gallery-presentation"))
                implementation(project(":feature:ratedvideos-data"))
                implementation(project(":feature:ratedvideos-presentation"))
                implementation(project(":feature:bible-data"))
                implementation(project(":feature:bible-presentation"))
                implementation(project(":feature:gospel-data"))
                implementation(project(":feature:gospel-presentation"))
                implementation(project(":feature:prayer-data"))
                implementation(project(":feature:prayer-presentation"))
                implementation(project(":feature:notes-data"))
                implementation(project(":feature:notes-presentation"))
                implementation(project(":feature:calls-data"))
                implementation(project(":feature:calls-presentation"))
                implementation(project(":feature:analytics-data"))
                implementation(project(":feature:analytics-presentation"))
                implementation(project(":feature:settings-data"))
                implementation(project(":feature:settings-presentation"))
                implementation(project(":feature:kuiver-data"))
                implementation(project(":feature:kuiver-presentation"))
                implementation(project(":feature:fasting-data"))
                implementation(project(":feature:fasting-presentation"))
                implementation(project(":feature:rosary-data"))
                implementation(project(":feature:rosary-presentation"))
                implementation(project(":feature:saints-data"))
                implementation(project(":feature:saints-presentation"))
                implementation(project(":feature:spiritualtraining-data"))
                implementation(project(":feature:spiritualtraining-presentation"))
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(project(":feature:running-domain"))
                implementation(project(":feature:running-data"))
                implementation(project(":feature:running-presentation"))
            }
        }
    }
}

val syncComposeAppXCFrameworkToXcode by tasks.registering(Sync::class) {
    dependsOn(tasks.matching { it.name.startsWith("assemble") && it.name.endsWith("XCFramework") })
    from(layout.buildDirectory.dir("XCFrameworks/release/composeApp.xcframework"))
    into(rootProject.file("iosApp/composeApp.xcframework"))
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    // LeakCanary - automatic memory leak detection (debug only)
    debugImplementation(libs.leakcanary)

    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.coroutines.test)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit4)
}

tasks.withType<Jar>().configureEach {
    when (name) {
        "bundleLibCompileToJarDebug",
        "bundleLibRuntimeToJarDebug",
        "createFullJarDebug" -> from(layout.buildDirectory.dir("tmp/kotlin-classes/debug"))
        "bundleLibCompileToJarRelease",
        "bundleLibRuntimeToJarRelease",
        "createFullJarRelease" -> from(layout.buildDirectory.dir("tmp/kotlin-classes/release"))
    }
}
