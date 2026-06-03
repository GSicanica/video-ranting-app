plugins {
    id("youtube.android.library")
}

android {
    namespace = "com.youtube.rating.watchhistory.data"
}

dependencies {
    api(project(":core:core-data"))
    implementation(project(":shared"))
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.serialization.json)
}
