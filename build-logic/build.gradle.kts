plugins {
    `kotlin-dsl`
}

group = "com.youtube.rating.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "youtube.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "youtube.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "youtube.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "youtube.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidFeatureCoil") {
            id = "youtube.android.feature.coil"
            implementationClass = "AndroidFeatureCoilConventionPlugin"
        }
        register("kmpLibrary") {
            id = "youtube.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
    }
}
