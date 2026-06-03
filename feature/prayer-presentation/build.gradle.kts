plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.prayer.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
