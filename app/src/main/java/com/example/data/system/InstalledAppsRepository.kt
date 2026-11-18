package com.example.data.system

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.ExternalIconPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class InstalledAppsRepository(private val context: Context) {

    data class AppScanResult(
        val apps: List<AppInfo>,
        val scanDurationMs: Long
    )

    suspend fun loadInstalledApps(): AppScanResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val selfPackage = context.packageName
        val apps = resolved
            .asSequence()
            .filter { it.activityInfo != null && it.activityInfo.packageName != selfPackage }
            .map { info ->
                val activityInfo = info.activityInfo
                val appInfo = activityInfo.applicationInfo
                val pkg = activityInfo.packageName
                val act = activityInfo.name
                val label = info.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                AppInfo(
                    packageName = pkg,
                    activityName = act,
                    label = label,
                    componentKey = "$pkg/$act",
                    isSystemApp = isSystem,
                    category = resolveCategory(appInfo, pkg, label)
                )
            }
            .distinctBy { it.componentKey }
            .sortedBy { it.label.lowercase() }
            .toList()

        val duration = (System.currentTimeMillis() - start).coerceAtLeast(1L)
        AppScanResult(apps = apps, scanDurationMs = duration)
    }

    suspend fun discoverExternalIconPacks(): List<ExternalIconPack> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val actions = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME")
        val packs = linkedMapOf<String, ExternalIconPack>()

        for (action in actions) {
            val intent = Intent(action)
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

            for (info in resolved) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (!packs.containsKey(pkg)) {
                    val label = info.loadLabel(pm)?.toString() ?: pkg
                    packs[pkg] = ExternalIconPack(packageName = pkg, label = label)
                }
            }
        }
        packs.values.sortedBy { it.label.lowercase() }
    }

    fun observePackageChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    fun launchApp(app: AppInfo): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun requestUninstall(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun resolveCategory(
        appInfo: ApplicationInfo,
        pkg: String,
        label: String
    ): AppCategory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.COMMUNICATION
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE,
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.MEDIA
                ApplicationInfo.CATEGORY_PRODUCTIVITY,
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_MAPS -> return AppCategory.TOOLS
            }
        }
        val lower = "$pkg $label".lowercase()
        return when {
            listOf("dialer", "phone", "message", "sms", "mail", "chat", "meet", "contacts", "social")
                .any { lower.contains(it) } -> AppCategory.COMMUNICATION
            listOf("camera", "gallery", "photos", "music", "video", "youtube", "spotify", "media", "play")
                .any { lower.contains(it) } -> AppCategory.MEDIA
            listOf("calendar", "docs", "drive", "notes", "keep", "tasks", "clock", "office", "pdf")
                .any { lower.contains(it) } -> AppCategory.PRODUCTIVITY
            else -> AppCategory.TOOLS
        }
    }
}
