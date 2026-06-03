plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.moduleguard"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

intellij {
    // Use a stable IntelliJ version for building; compatibility is set below.
    version.set("2023.3")
    type.set("IC")
    plugins.set(emptyList())
}

// Avoid buildSearchableOptions requiring a running IDE instance
tasks.named("buildSearchableOptions").configure {
    enabled = false
}

tasks.patchPluginXml {
    sinceBuild.set("233")
    untilBuild.set("253.*")
}

kotlin {
    jvmToolchain(17)
}

// Ensure Kotlin serialization is available in runtime
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}
