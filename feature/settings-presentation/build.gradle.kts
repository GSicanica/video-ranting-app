plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.settings.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
