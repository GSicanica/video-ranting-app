plugins {
    id("youtube.android.feature")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.youtube.rating.habittracker.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
    implementation(libs.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.serialization.json)
}
