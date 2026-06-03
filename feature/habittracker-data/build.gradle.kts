plugins {
    id("youtube.android.library")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.youtube.rating.habittracker.data"

    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = layout.projectDirectory.dir("schemas").asFile.path
            }
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    api(project(":feature:habittracker-domain"))
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    kapt(libs.room.compiler)
    kapt(libs.kotlin.stdlib.kapt.runtime)
}
