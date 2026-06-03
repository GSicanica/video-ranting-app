plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.notes.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
