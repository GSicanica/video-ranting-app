plugins {
    id("youtube.android.feature")
}

android {
    namespace = "com.youtube.rating.ratedvideos.presentation"
}

dependencies {
    implementation(project(":core:core-data"))
}
