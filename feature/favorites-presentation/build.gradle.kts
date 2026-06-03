plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.favorites.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
