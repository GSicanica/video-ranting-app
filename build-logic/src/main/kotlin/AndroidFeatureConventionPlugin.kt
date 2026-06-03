import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("youtube.android.library")
            pluginManager.apply("youtube.android.compose")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            dependencies {
                add("implementation", project(":core:core-presentation"))
                add("implementation", project(":core:core-design-system"))
                featureDomainProject()?.let { domainProject ->
                    add("implementation", domainProject)
                }
                add("implementation", libs.findBundle("compose-screen").get())
                add("implementation", libs.findBundle("koin-compose").get())
                add("implementation", libs.findLibrary("coil-compose").get())
                add("implementation", libs.findLibrary("compose-markdown").get())
                add("implementation", libs.findLibrary("paging-runtime").get())
                add("implementation", libs.findLibrary("paging-runtime-ktx").get())
                add("implementation", libs.findLibrary("paging-compose").get())
                add("implementation", libs.findLibrary("serialization-json").get())
                add("implementation", libs.findLibrary("sentry-android").get())
                add("implementation", libs.findLibrary("youtube-player").get())
                add("implementation", libs.findBundle("media3").get())
            }
        }
    }

    private fun Project.featureDomainProject(): Project? {
        if (!path.startsWith(":feature:") || !path.endsWith("-presentation")) return null
        return findProject(path.removeSuffix("-presentation") + "-domain")
    }
}
