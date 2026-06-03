import org.gradle.api.Project

object AndroidConfig {
    private const val DEFAULT_COMPILE_SDK = 36
    private const val DEFAULT_MIN_SDK = 24
    private const val DEFAULT_TARGET_SDK = 35

    fun compileSdk(project: Project): Int =
        project.findProperty("android.compileSdk")?.toString()?.toIntOrNull()
            ?: DEFAULT_COMPILE_SDK

    fun minSdk(project: Project): Int =
        project.findProperty("android.minSdk")?.toString()?.toIntOrNull()
            ?: DEFAULT_MIN_SDK

    fun targetSdk(project: Project): Int =
        project.findProperty("android.targetSdk")?.toString()?.toIntOrNull()
            ?: DEFAULT_TARGET_SDK
}
