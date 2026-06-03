plugins {
    base
}

description = "Desktop app scaffold module wired into the main Gradle build."

tasks.register("desktopAppInfo") {
    group = "help"
    description = "Explains the current desktop scaffold state."
    doLast {
        logger.lifecycle("desktopApp is wired into the root build as a scaffold module. No runtime desktop target is implemented yet.")
    }
}
