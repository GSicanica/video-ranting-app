plugins {
    id("youtube.android.library")
    id("youtube.android.compose")
}

android {
    namespace = "com.youtube.rating.core.designsystem"
}

dependencies {
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.coil.compose)
}
