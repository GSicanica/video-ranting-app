plugins {
    id("youtube.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.youtube.rating.core.data"

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    api(project(":core:core-domain"))
    api(project(":shared"))

    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.coil)
    implementation(libs.coroutines.android)
    implementation(libs.work.runtime.ktx)
    implementation(libs.koin.android)
    implementation(libs.security.crypto)
    implementation(libs.sentry.android)
    implementation(libs.okhttp)
    implementation(libs.paging.runtime)
    implementation(libs.paging.runtime.ktx)
    implementation(libs.animation.core)
    implementation(libs.play.services.maps)
    implementation(libs.kuiver.android)
    implementation(libs.serialization.json)
    implementation(libs.multiplatform.settings)
    implementation(libs.multiplatform.settings.no.arg)
}
