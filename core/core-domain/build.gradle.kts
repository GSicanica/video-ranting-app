plugins {
    id("youtube.android.library")
}

android {
    namespace = "com.youtube.rating.core.domain"
}

dependencies {
    implementation(project(":shared"))
}
