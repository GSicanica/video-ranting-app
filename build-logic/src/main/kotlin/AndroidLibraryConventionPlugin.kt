import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            extensions.configure<LibraryExtension> {
                compileSdk = AndroidConfig.compileSdk(project)

                defaultConfig {
                    minSdk = AndroidConfig.minSdk(project)
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                    isCoreLibraryDesugaringEnabled = true
                }

                buildFeatures {
                    buildConfig = true
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    freeCompilerArgs.add("-Xskip-metadata-version-check")
                }
            }

            dependencies {
                add("coreLibraryDesugaring", libs.findLibrary("desugar-jdk-libs").get())
                if (path.startsWith(":feature:") && path.endsWith("-domain")) {
                    add("implementation", project(":core:core-domain"))
                }
                if (path.startsWith(":feature:") && path.endsWith("-data")) {
                    add("implementation", project(":core:core-data"))
                    featureLayerProject("-data", "-domain")?.let { domainProject ->
                        add("implementation", domainProject)
                    }
                }
            }

            tasks.withType(Jar::class.java).configureEach {
                when (name) {
                    "bundleLibCompileToJarDebug",
                    "bundleLibRuntimeToJarDebug",
                    "createFullJarDebug" -> from(layout.buildDirectory.dir("tmp/kotlin-classes/debug"))
                    "bundleLibCompileToJarRelease",
                    "bundleLibRuntimeToJarRelease",
                    "createFullJarRelease" -> from(layout.buildDirectory.dir("tmp/kotlin-classes/release"))
                }
            }
        }
    }

    private fun Project.featureLayerProject(currentSuffix: String, targetSuffix: String): Project? {
        val targetPath = path.removeSuffix(currentSuffix) + targetSuffix
        return findProject(targetPath)
    }
}
