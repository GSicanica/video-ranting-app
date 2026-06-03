plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.home.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
