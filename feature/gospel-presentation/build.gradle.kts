plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.gospel.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
