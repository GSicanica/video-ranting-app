import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("youtube.kmp.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.realm)
}

kotlin {
    applyDefaultHierarchyTemplate()
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
    val enableIosTargets = providers.gradleProperty("enableIosBuild").map(String::toBoolean).getOrElse(false)
    val xcf = if (enableIosTargets) XCFramework("shared") else null

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    freeCompilerArgs.addAll(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-Xjvm-default=all"
                    )
                }
            }
        }
    }

    if (enableIosTargets) {
        iosArm64()
        iosSimulatorArm64()

        targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
            binaries.framework {
                baseName = "shared"
                xcf?.add(this)
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.koin.core)

                // Ktor Client with all needed features
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
            }
        }

        // Realm-based persistence (Android/iOS/Desktop only). JS target uses network-only features.
        val realmMain by creating {
            dependsOn(commonMain)
            dependencies {
                // Realm Kotlin for local database - export to consumers (androidApp uses Realm types)
                api(libs.realm.library.base)
            }
        }

        val androidMain by getting {
            dependsOn(realmMain)
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.coroutines.android)
            }
        }

        if (enableIosTargets) {
            val iosMain by getting {
                dependencies {
                    implementation(libs.ktor.client.darwin)
                }
            }
        }
    }
}

android {
    namespace = "com.youtube.rating.shared"
}

val syncSharedXCFrameworkToXcode by tasks.registering(Sync::class) {
    onlyIf { providers.gradleProperty("enableIosBuild").map(String::toBoolean).getOrElse(false) }
    dependsOn(tasks.matching { it.name.startsWith("assemble") && it.name.endsWith("XCFramework") })
    from(layout.buildDirectory.dir("XCFrameworks/release/shared.xcframework"))
    into(rootProject.file("iosApp/shared.xcframework"))
}
