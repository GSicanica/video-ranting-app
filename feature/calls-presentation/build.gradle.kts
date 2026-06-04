plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.calls.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
