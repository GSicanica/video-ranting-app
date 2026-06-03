plugins {
    id("youtube.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.youtube.rating.running.data"
}

dependencies {
    api(project(":feature:running-domain"))
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.serialization.json)
    implementation(libs.datastore.preferences)
}
