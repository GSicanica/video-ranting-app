plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.offlinevideos.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
