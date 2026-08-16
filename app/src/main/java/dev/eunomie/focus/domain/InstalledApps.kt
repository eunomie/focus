package dev.eunomie.focus.domain

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process

data class AppEntry(val packageName: String, val label: String)

/**
 * The launchable apps on the device.
 *
 * `LauncherApps` needs no permission — `QUERY_ALL_PACKAGES` is not required for this and
 * is deliberately not declared.
 */
class InstalledApps(private val context: Context) {

    private val launcherApps get() = context.getSystemService(LauncherApps::class.java)

    fun all(): List<AppEntry> =
        launcherApps.getActivityList(null, Process.myUserHandle())
            .map { AppEntry(it.applicationInfo.packageName, it.label.toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

    fun labelsFor(packageNames: List<String>): List<AppEntry> {
        val byPackage = all().associateBy { it.packageName }
        return packageNames.mapNotNull { byPackage[it] }
    }

    fun launchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
