plugins {
    base
}

description = "Web app scaffold module wired into the main Gradle build."

tasks.register("webAppInfo") {
    group = "help"
    description = "Explains the current web scaffold state."
    doLast {
        logger.lifecycle("webApp is wired into the root build as a scaffold module. No runtime web target is implemented yet.")
    }
}
