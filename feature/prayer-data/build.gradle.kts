plugins {
    id("youtube.android.library")
}

android {
    namespace = "com.youtube.rating.prayer.data"
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.koin.android)
}
