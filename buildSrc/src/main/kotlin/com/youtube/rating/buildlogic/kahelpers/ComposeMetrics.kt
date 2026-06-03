package com.youtube.rating.buildlogic.kahelpers

import org.gradle.api.Project
import java.io.File

val Project.composeMetricsDir: String
    get() = layout.buildDirectory.asFile.get().absolutePath + "/compose_metrics"

val Project.composeReportsDir: String
    get() = layout.buildDirectory.asFile.get().absolutePath + "/compose_reports"

/**
 * Builds Compose compiler metrics/reports arguments based on Gradle properties:
 * - `enableComposeCompilerMetrics=true`
 * - `enableComposeCompilerReports=true`
 */
fun Project.buildComposeMetricsParameters(): List<String> {
    val metricParameters = mutableListOf<String>()

    val enableMetrics = providers.gradleProperty("enableComposeCompilerMetrics").orNull == "true"
    if (enableMetrics) {
        val metricsFolder = File(composeMetricsDir)
        metricParameters += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${metricsFolder.absolutePath}"
        )
    }

    val enableReports = providers.gradleProperty("enableComposeCompilerReports").orNull == "true"
    if (enableReports) {
        val reportsFolder = File(composeReportsDir)
        metricParameters += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${reportsFolder.absolutePath}"
        )
    }

    return metricParameters
}

