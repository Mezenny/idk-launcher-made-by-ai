package com.example.data.system

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

data class HomeLauncherInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isZstar: Boolean
)

object DefaultLauncherHelper {

    fun isZstarDefaultHome(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun getCurrentDefaultLauncherName(context: Context): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        } ?: return null
        val pkg = resolveInfo.activityInfo?.packageName ?: return null
        if (pkg == "android" || pkg == "com.android.internal.app") return null
        return resolveInfo.loadLabel(context.packageManager)?.toString()
    }

    fun getInstalledHomeLaunchers(context: Context): List<HomeLauncherInfo> {
        val homeIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addCategory(Intent.CATEGORY_DEFAULT)
        val pm = context.packageManager
        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(homeIntent, 0)
        }
        return list.mapNotNull { info ->
            val act = info.activityInfo ?: return@mapNotNull null
            if (act.packageName == "android" || act.packageName == "com.android.internal.app") {
                return@mapNotNull null
            }
            HomeLauncherInfo(
                packageName = act.packageName,
                activityName = act.name,
                label = info.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
                    ?: act.packageName,
                isZstar = act.packageName == context.packageName
            )
        }.distinctBy { it.packageName }
    }

    fun getOriginalLauncherName(context: Context): String? {
        return getInstalledHomeLaunchers(context).firstOrNull { !it.isZstar }?.label
    }

    fun createRequestDefaultHomeIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun openHomeSettings(context: Context): Boolean {
        val candidates = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in candidates) {
            val ok = runCatching {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }

    fun launchOriginalLauncherOnce(context: Context): Boolean {
        val original = getInstalledHomeLaunchers(context).firstOrNull { !it.isZstar }
        if (original != null) {
            val launched = runCatching {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    component = ComponentName(original.packageName, original.activityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (launched) return true
        }
        return openHomeSettings(context)
    }

    fun openAndroidSystemSettings(context: Context): Boolean {
        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }
}
