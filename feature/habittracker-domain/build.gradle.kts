plugins {
    id("youtube.android.library")
}

android {
    namespace = "com.youtube.rating.habittracker.domain"
}

dependencies {
    implementation(libs.coroutines.core)
}
