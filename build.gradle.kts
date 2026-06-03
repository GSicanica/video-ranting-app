plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false

    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.kotzilla) apply false
    alias(libs.plugins.sentry.android.gradle) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    group = "com.youtube.rating"
    version = "1.0.0"

    // Keep Kotlin artifacts aligned where KMP metadata/native outputs are sensitive.
    // Do not pin Android runtime classpaths, otherwise libraries like Kuiver can miss
    // newer stdlib coroutine internals at runtime (e.g. SpillingKt).
    configurations.configureEach {
        if (!name.startsWith("kapt", ignoreCase = true)) {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") {
                    if (requested.name.startsWith("kotlin-stdlib") || requested.name == "kotlin-reflect") {
                        useVersion(libs.versions.kotlin.get())
                    }
                }
            }
        }
    }
}

tasks.register<ProductionArchitectureCheckTask>("productionArchitectureCheck") {
    group = "verification"
    description = "Validates production architecture guardrails without changing runtime behavior."
    projectRoot.set(layout.projectDirectory)
    requiredPaths.set(
        listOf(
            "androidApp/src/main/AndroidManifest.xml",
            "core/core-domain/src/main/kotlin",
            "core/core-data/src/main/kotlin",
            "core/core-presentation/src/main/kotlin",
            "core/core-design-system/src/main/kotlin",
            "composeApp/src/androidMain/kotlin/com/youtube/rating/android/di/AppModule.kt",
            "composeApp/src/androidMain/kotlin/com/youtube/rating/android/navigation",
            "core/core-presentation/src/main/kotlin/com/youtube/rating/android/mvi",
            "core/core-data/src/main/kotlin/com/youtube/rating/android/data",
            "core/core-data/src/main/kotlin/com/youtube/rating/android/domain",
            "feature",
            "shared/src/commonMain",
            "build-logic/src/main/kotlin"
        )
    )
}

tasks.register("iosAppAssembleXCFrameworks") {
    group = "ios"
    description = "Builds the canonical shared and composeApp XCFrameworks used by iosApp."
    onlyIf { providers.gradleProperty("enableIosBuild").map(String::toBoolean).getOrElse(false) }
    dependsOn(
        ":shared:assembleXCFramework",
        ":iosComposeApp:assembleComposeAppXCFramework",
    )
}

tasks.register("iosAppSyncXCFrameworks") {
    group = "ios"
    description = "Syncs the canonical shared and composeApp XCFrameworks into iosApp."
    onlyIf { providers.gradleProperty("enableIosBuild").map(String::toBoolean).getOrElse(false) }
    dependsOn(
        ":shared:syncSharedXCFrameworkToXcode",
        ":iosComposeApp:syncComposeAppXCFrameworkToXcode",
    )
}
