plugins {
    id("youtube.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.youtube.rating.running.domain"
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)
}
