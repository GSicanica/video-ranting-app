package com.youtube.rating.core.root

import android.os.Build
import java.io.File

fun isRooted(): Boolean {
    val buildTags = Build.TAGS
    if (buildTags != null && buildTags.contains("test-keys")) return true

    // /system/app/Superuser.apk is a legacy location, but still a useful signal.
    runCatching {
        if (File("/system/app/Superuser.apk").exists()) return true
    }

    return canExecuteCommand(command = "/system/xbin/which su") ||
        canExecuteCommand(command = "/system/bin/which su") ||
        canExecuteCommand(command = "which su")
}

private fun canExecuteCommand(command: String): Boolean =
    try {
        Runtime.getRuntime().exec(command)
        true
    } catch (_: Exception) {
        false
    }

fun isRootAvailable(): Boolean {
    val path = System.getenv("PATH") ?: return false
    for (pathDir in path.split(":")) {
        if (pathDir.isNotEmpty() && File(pathDir, "su").exists()) return true
    }
    return false
}
