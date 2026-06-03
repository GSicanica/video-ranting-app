plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.saints.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
