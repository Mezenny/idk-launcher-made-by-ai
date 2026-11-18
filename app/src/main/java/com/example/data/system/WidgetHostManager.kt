package com.example.data.system

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.WidgetProviderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetHostManager(private val context: Context) {

    companion object {
        const val APPWIDGET_HOST_ID = 1024
    }

    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(context.applicationContext, APPWIDGET_HOST_ID)

    fun startListening() {
        try {
            appWidgetHost.startListening()
        } catch (_: Exception) {
        }
    }

    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {
        }
    }

    fun allocateAppWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteAppWidgetId(appWidgetId: Int) {
        if (appWidgetId > 0) {
            try {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            } catch (_: Exception) {
            }
        }
    }

    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, info: AppWidgetProviderInfo): Boolean {
        return try {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
        } catch (_: Exception) {
            false
        }
    }

    fun createBindIntent(appWidgetId: Int, info: AppWidgetProviderInfo): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
        }
    }

    fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (_: Exception) {
            null
        }
    }

    fun createHostView(appWidgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView? {
        return try {
            appWidgetHost.createView(context, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getInstalledWidgetProviders(): List<WidgetProviderItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val providers = try {
            appWidgetManager.installedProviders
        } catch (_: Exception) {
            emptyList()
        }

        providers.mapNotNull { info ->
            try {
                val pkg = info.provider.packageName
                val label = info.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
                    ?: info.provider.shortClassName
                val appLabel = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    pkg
                }
                val spanCols = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
                    info.targetCellWidth.coerceIn(1, 4)
                } else {
                    ((info.minWidth + 30) / 70).coerceIn(1, 4)
                }
                val spanRows = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
                    info.targetCellHeight.coerceIn(1, 4)
                } else {
                    ((info.minHeight + 30) / 70).coerceIn(1, 4)
                }

                WidgetProviderItem(
                    componentName = info.provider.flattenToString(),
                    packageName = pkg,
                    label = label,
                    appName = appLabel,
                    minWidthDp = info.minWidth,
                    minHeightDp = info.minHeight,
                    spanCols = spanCols,
                    spanRows = spanRows,
                    providerInfo = info
                )
            } catch (_: Exception) {
                null
            }
        }.sortedWith(compareBy({ it.appName.lowercase() }, { it.label.lowercase() }))
    }
}
