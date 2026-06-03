import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class ProductionArchitectureCheckTask : DefaultTask() {

    @get:Internal
    abstract val projectRoot: DirectoryProperty

    @get:Input
    abstract val requiredPaths: ListProperty<String>

    @TaskAction
    fun checkArchitecture() {
        val root = projectRoot.get().asFile

        val missing = requiredPaths.get()
            .map { root.resolve(it) }
            .filterNot { it.exists() }

        check(missing.isEmpty()) {
            "Missing expected production architecture paths:\n" + missing.joinToString(separator = "\n") { " - ${it.relativeTo(root)}" }
        }

        val legacyCoreModule = root.resolve("core/legacy")
        check(!legacyCoreModule.exists()) {
            "core:legacy has been removed. Move any new shared code into core-domain, core-data, core-presentation, or core-design-system."
        }

        val settingsFile = root.resolve("settings.gradle.kts")
        check(!settingsFile.readText().contains("include(\":core:legacy\")")) {
            "settings.gradle.kts must not include :core:legacy."
        }

        val androidAppMain = root.resolve("androidApp/src/main")
        val androidAppMainSources = androidAppMain
            .takeIf { it.exists() }
            ?.walkTopDown()
            ?.filter { it.isFile && it.extension in setOf("kt", "java") }
            ?.filterNot { it.invariantSeparatorsPath.contains("/src/launcher/") }
            ?.toList()
            .orEmpty()

        check(androidAppMainSources.isEmpty()) {
            "androidApp must stay a thin launcher. Move app logic to composeApp or feature modules:\n" +
                androidAppMainSources.joinToString(separator = "\n") { " - ${it.relativeTo(root)}" }
        }

        val productionKotlinRoots = listOf(
            root.resolve("composeApp/src/androidMain/kotlin"),
            root.resolve("core"),
            root.resolve("feature")
        )
        val kotlinSources = productionKotlinRoots
            .asSequence()
            .filter { it.exists() }
            .flatMap { sourceRoot ->
                sourceRoot.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
            }
            .filterNot { it.invariantSeparatorsPath.contains("/build/") }
            .distinctBy { it.absolutePath }
            .toList()

        val domainToDataImports = root
            .resolve("composeApp/src/androidMain/kotlin/com/youtube/rating/android/domain")
            .takeIf { it.exists() }
            ?.walkTopDown()
            ?.filter { it.isFile && it.extension == "kt" }
            ?.flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (line.trim().startsWith("import com.youtube.rating.android.data.")) {
                        "${file.relativeTo(root)}:${index + 1}: ${line.trim()}"
                    } else {
                        null
                    }
                }
            }
            ?.toList()
            .orEmpty()

        check(domainToDataImports.isEmpty()) {
            "Domain layer must not import data layer symbols directly. Found:\n" +
                domainToDataImports.joinToString(separator = "\n") { " - $it" }
        }

        val extractedDomainToDataImports = kotlinSources
            .filter { file ->
                file.invariantSeparatorsPath.contains("/src/") &&
                    (
                        file.invariantSeparatorsPath.contains("/core-domain/") ||
                            file.invariantSeparatorsPath.contains("-domain/src/")
                    )
            }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("import ") && (trimmed.contains(".data.") || trimmed.contains(".network."))) {
                        "${file.relativeTo(root)}:${index + 1}: $trimmed"
                    } else {
                        null
                    }
                }
            }

        check(extractedDomainToDataImports.isEmpty()) {
            "Extracted domain modules must not import data/network symbols directly. Found:\n" +
                extractedDomainToDataImports.joinToString(separator = "\n") { " - $it" }
        }

        val emptyFeatureDataModules = root.resolve("feature")
            .takeIf { it.exists() }
            ?.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.endsWith("-data") }
            .filter { featureModule ->
                featureModule.resolve("src")
                    .takeIf { it.exists() }
                    ?.walkTopDown()
                    ?.none { source -> source.isFile && source.extension == "kt" }
                    ?: true
            }

        val emptyFeatureDomainModules = root.resolve("feature")
            .takeIf { it.exists() }
            ?.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.endsWith("-domain") }
            .filter { featureModule ->
                featureModule.resolve("src")
                    .takeIf { it.exists() }
                    ?.walkTopDown()
                    ?.none { source -> source.isFile && source.extension == "kt" }
                    ?: true
            }

        val featureModulesDependingOnLegacy = root.resolve("feature")
            .takeIf { it.exists() }
            ?.walkTopDown()
            ?.filter { it.isFile && it.name == "build.gradle.kts" }
            ?.filter { buildFile ->
                val text = buildFile.readText()
                text.contains("project(\":core:legacy\")") || text.contains("projects.core.legacy")
            }
            ?.map { it.parentFile.relativeTo(root).invariantSeparatorsPath }
            ?.sorted()
            ?.toList()
            .orEmpty()

        val featurePresentationDependencies = root.resolve("feature")
            .takeIf { it.exists() }
            ?.walkTopDown()
            ?.filter { it.isFile && it.name == "build.gradle.kts" }
            ?.filter { buildFile -> buildFile.parentFile.name.endsWith("-presentation") }
            ?.flatMap { buildFile ->
                val currentModule = buildFile.parentFile.name
                buildFile.readLines().mapIndexedNotNull { index, line ->
                    val target = line.extractFeaturePresentationProject()
                    if (target != null && target != currentModule) {
                        "${buildFile.relativeTo(root)}:${index + 1}: ${line.trim()}"
                    } else {
                        null
                    }
                }
            }
            ?.sorted()
            ?.toList()
            .orEmpty()

        check(featurePresentationDependencies.isEmpty()) {
            "Feature presentation modules must not depend on other feature presentation modules. Use app-level wiring, callbacks, or domain contracts:\n" +
                featurePresentationDependencies.joinToString(separator = "\n") { " - $it" }
        }

        val featurePresentationDataDependencies = root.resolve("feature")
            .takeIf { it.exists() }
            ?.walkTopDown()
            ?.filter { it.isFile && it.name == "build.gradle.kts" }
            ?.filter { buildFile -> buildFile.parentFile.name.endsWith("-presentation") }
            ?.flatMap { buildFile ->
                buildFile.readLines().mapIndexedNotNull { index, line ->
                    val target = line.extractFeatureDataProject()
                    if (target != null) {
                        "${buildFile.relativeTo(root)}:${index + 1}: ${line.trim()}"
                    } else {
                        null
                    }
                }
            }
            ?.sorted()
            ?.toList()
            .orEmpty()

        val coreDataBuildFile = root.resolve("core/core-data/build.gradle.kts")
        val coreDataUiDependencies = if (coreDataBuildFile.exists()) {
            val uiDependencyMarkers = listOf(
                "core-design-system",
                "activity.compose",
                "lifecycle.viewmodel.compose",
                "lifecycle.runtime.compose",
                "navigation.compose",
                "coil.compose",
                "media3.ui",
                "compose.markdown",
                "paging.compose",
                "maps.compose",
                "youtube.player"
            )
            coreDataBuildFile.readLines().mapIndexedNotNull { index, line ->
                val trimmed = line.trim()
                if (uiDependencyMarkers.any { marker -> trimmed.contains(marker) }) {
                    "${coreDataBuildFile.relativeTo(root)}:${index + 1}: $trimmed"
                } else {
                    null
                }
            }
        } else {
            emptyList()
        }

        val featureDataModulesDependingOnLegacy = featureModulesDependingOnLegacy
            .filter { it.substringAfterLast('/').endsWith("-data") }

        val featureDomainModulesDependingOnLegacy = featureModulesDependingOnLegacy
            .filter { it.substringAfterLast('/').endsWith("-domain") }

        val featurePresentationModulesDependingOnLegacy = featureModulesDependingOnLegacy
            .filter { it.substringAfterLast('/').endsWith("-presentation") }

        val coreModuleKotlinSourceCounts = listOf(
            "core/core-domain",
            "core/core-data",
            "core/core-presentation",
            "core/core-design-system"
        ).associateWith { path ->
            root.resolve(path)
                .resolve("src")
                .takeIf { it.exists() }
                ?.walkTopDown()
                ?.count { it.isFile && it.extension == "kt" }
                ?: 0
        }

        val legacySingletonGetInstancePattern = Regex(
            """com\.youtube\.rating\.[A-Za-z0-9_\.]+\.getInstance\(|\b[A-Z][A-Za-z0-9_]*(Manager|Provider|Repository|Holder)\.getInstance\("""
        )

        val migrationSignals = listOf(
            "Dispatchers.IO" to kotlinSources.count { it.readText().contains("Dispatchers.IO") },
            "KoinJavaComponent.get" to kotlinSources.count { it.readText().contains("KoinJavaComponent.get") },
            "legacy getInstance(" to kotlinSources.count {
                legacySingletonGetInstancePattern.containsMatchIn(it.readText())
            },
            "presentation -> feature data build deps" to featurePresentationDataDependencies.size,
            "core-data UI dependency declarations" to coreDataUiDependencies.size
        )

        logger.lifecycle("Production architecture check passed.")
        logger.lifecycle("Current migration signals (reported, not failed):")
        migrationSignals.forEach { (name, count) ->
            logger.lifecycle(" - $name: $count file(s)")
        }

        logger.lifecycle("Domain->Data import violations: ${domainToDataImports.size}")
        logger.lifecycle("Extracted domain->data/network import violations: ${extractedDomainToDataImports.size}")
        logger.lifecycle("Empty feature data modules: ${emptyFeatureDataModules.size}")
        logger.lifecycle("Empty feature domain modules: ${emptyFeatureDomainModules.size}")
        logger.lifecycle("Feature presentation dependency violations: ${featurePresentationDependencies.size}")
        logger.lifecycle("Feature presentation -> data dependency migration debt: ${featurePresentationDataDependencies.size}")
        featurePresentationDataDependencies.forEach { logger.lifecycle(" - $it") }
        logger.lifecycle("core-data UI dependency migration debt: ${coreDataUiDependencies.size}")
        coreDataUiDependencies.forEach { logger.lifecycle(" - $it") }
        logger.lifecycle("Core module Kotlin source counts:")
        coreModuleKotlinSourceCounts.forEach { (module, count) ->
            logger.lifecycle(" - $module: $count file(s)")
        }
        logger.lifecycle("Feature modules still depending on core:legacy: ${featureModulesDependingOnLegacy.size}")
        logger.lifecycle(" - data: ${featureDataModulesDependingOnLegacy.size} ${featureDataModulesDependingOnLegacy.joinToString(prefix = "[", postfix = "]")}")
        logger.lifecycle(" - domain: ${featureDomainModulesDependingOnLegacy.size} ${featureDomainModulesDependingOnLegacy.joinToString(prefix = "[", postfix = "]")}")
        logger.lifecycle(" - presentation: ${featurePresentationModulesDependingOnLegacy.size} ${featurePresentationModulesDependingOnLegacy.joinToString(prefix = "[", postfix = "]")}")
    }

    private fun String.extractFeaturePresentationProject(): String? {
        val quotedProject = Regex("""project\(":feature:([^"]+-presentation)"\)""")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
        if (quotedProject != null) return quotedProject

        return Regex("""projects\.feature\.([A-Za-z0-9]+)Presentation""")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            ?.lowercase()
            ?.let { "$it-presentation" }
    }

    private fun String.extractFeatureDataProject(): String? {
        val quotedProject = Regex("""project\(":feature:([^"]+-data)"\)""")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
        if (quotedProject != null) return quotedProject

        return Regex("""projects\.feature\.([A-Za-z0-9]+)Data""")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            ?.lowercase()
            ?.let { "$it-data" }
    }
}
