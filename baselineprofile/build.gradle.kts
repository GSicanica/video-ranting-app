plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.youtube.rating.baselineprofile"
    compileSdk = providers.gradleProperty("android.compileSdk").orNull?.toInt() ?: 36
    targetProjectPath = ":androidApp"

    defaultConfig {
        minSdk = providers.gradleProperty("android.minSdk").orNull?.toInt() ?: 24
        targetSdk = providers.gradleProperty("android.targetSdk").orNull?.toInt() ?: 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Required by our benchmarks (see StartupBenchmarks.kt) to know which app to launch/profile.
        testInstrumentationRunnerArguments["targetAppId"] = "com.youtube.rating.android"
        // Prefer baseline profile generation by default. Startup macrobenchmarks can be enabled explicitly.
        testInstrumentationRunnerArguments["androidx.benchmark.enabledRules"] = "baselineprofile"
        testInstrumentationRunnerArguments["androidx.benchmark.skipOnEmulator"] = "true"
        testInstrumentationRunnerArguments["androidx.benchmark.targetPackageName"] = "com.youtube.rating.android"
    }

    // Keep this module focused on benchmarks/profile generation only.
    buildTypes {
        create("benchmark") {
            isDebuggable = true
            // Baseline Profile plugin expects a benchmark build type.
            matchingFallbacks += listOf("release")
        }
    }

    // Macrobenchmarks use Android Test Orchestrator style execution.
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    // Keep JVM targets consistent with the app module to avoid Gradle JVM target validation failures.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Alias task to avoid ambiguity when invoking from CLI
tasks.register("connectedBenchmark") {
    group = "verification"
    description = "Run baseline profile connected benchmarks"
    dependsOn("connectedBenchmarkBenchmarkAndroidTest")
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.uiautomator)
    implementation(libs.test.ext.junit)
    implementation(libs.test.runner)
    implementation(libs.test.rules)
    implementation(libs.test.core)

    androidTestUtil(libs.test.orchestrator)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
