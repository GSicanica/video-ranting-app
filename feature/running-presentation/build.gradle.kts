plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.running.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
    implementation(libs.activity.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.maps.compose)
    implementation(libs.serialization.json)
}
