plugins {
    id("youtube.android.library")
    id("youtube.android.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.youtube.rating.core.presentation"
}

dependencies {
    implementation(project(":core:core-domain"))
    implementation(project(":shared"))
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.youtube.player)
    implementation(libs.serialization.json)
}
