plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.bible.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
