plugins {
    id("youtube.android.library")
}

android {
    namespace = "com.youtube.rating.watchhistory.domain"
}

dependencies {
    implementation(libs.coroutines.core)
}
