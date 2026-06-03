import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    applyDefaultHierarchyTemplate()
    val enableIosTargets = providers.gradleProperty("enableIosBuild").map(String::toBoolean).getOrElse(false)

    val iosArm64Target = iosArm64()
    val iosSimulatorArm64Target = iosSimulatorArm64()

    if (enableIosTargets) {
        val xcf = XCFramework("composeApp")

        iosArm64Target.apply {
            binaries {
                framework {
                    baseName = "composeApp"
                    isStatic = false
                    export(projects.shared)
                    xcf.add(this)
                }
            }
        }

        iosSimulatorArm64Target.apply {
            binaries {
                framework {
                    baseName = "composeApp"
                    isStatic = false
                    export(projects.shared)
                    xcf.add(this)
                }
            }
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.addAll(
                        "-opt-in=kotlin.RequiresOptIn",
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.shared)
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)

                // Compose
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
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

val syncComposeAppXCFrameworkToXcode by tasks.registering(Sync::class) {
    onlyIf { providers.gradleProperty("enableIosBuild").map(String::toBoolean).getOrElse(false) }
    dependsOn(tasks.matching { it.name.startsWith("assemble") && it.name.endsWith("XCFramework") })
    from(layout.buildDirectory.dir("XCFrameworks/release/composeApp.xcframework"))
    into(rootProject.file("iosApp/composeApp.xcframework"))
}
