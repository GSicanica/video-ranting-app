package com.youtube.rating.core.packageutils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings

fun Context.isAppInstalled(packageName: String): Boolean =
    try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

fun Context.isAppEnabled(packageName: String): Boolean =
    try {
        packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (_: Exception) {
        false
    }

fun Context.whoInstalledMyApp(packageName: String): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { packageManager.getInstallSourceInfo(packageName).installingPackageName }.getOrNull()
    } else {
        @Suppress("DEPRECATION")
        packageManager.getInstallerPackageName(packageName)
    }

fun Context.showAppInfo(packageName: String) {
    try {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
    }
}

val buildIsMarshmallowAndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

val buildIsNougatAndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

val buildIsOreoAndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

val buildIsPieAndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

val buildIs10AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

val buildIs11AndUp: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

const val INSTALLER_GOOGLE_PLAY_VENDING = "com.android.vending"
const val INSTALLER_GOOGLE_PLAY_FEEDBACK = "com.google.android.feedback"

val Context.installerPackageName: String?
    get() = whoInstalledMyApp(packageName)

val Context.isFromGooglePlay: Boolean
    get() {
        val installer = installerPackageName
        return arrayOf(
            INSTALLER_GOOGLE_PLAY_FEEDBACK,
            INSTALLER_GOOGLE_PLAY_VENDING
        ).any { it == installer }
    }

fun PackageManager.isIntentSafe(intent: Intent): Boolean =
    queryIntentActivities(intent, 0).isNotEmpty()

fun isSystemPackage(pkgInfo: PackageInfo): Boolean =
    (pkgInfo.applicationInfo?.flags ?: -1) and ApplicationInfo.FLAG_SYSTEM != 0

fun Context.launchAnApp(packageName: String) {
    val launchApp = packageManager.getLaunchIntentForPackage(packageName) ?: return
    startActivity(launchApp)
}

fun PackageManager.getAppInfoFromPackageName(packageName: String): ResolveInfo? {
    val intent = Intent().apply {
        `package` = packageName
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    return resolveActivity(intent, 0)
}

fun PackageManager.getAvailableApplications(): MutableList<ResolveInfo> {
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    return queryIntentActivities(mainIntent, 0)
}

fun Context.openAppInfo(packageName: String) = showAppInfo(packageName)

