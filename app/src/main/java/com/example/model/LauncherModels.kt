package com.example.model

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.runtime.Immutable

@Immutable
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val componentKey: String,
    val isSystemApp: Boolean,
    val category: AppCategory,
    val launchCount: Int = 0
)

enum class AppCategory(val title: String) {
    ALL("All"),
    FREQUENT("Frequent"),
    COMMUNICATION("Social"),
    MEDIA("Media"),
    PRODUCTIVITY("Work"),
    TOOLS("Tools")
}

enum class ThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val isDark: Boolean
) {
    STARLIGHT_OBSIDIAN(
        id = "starlight_obsidian",
        title = "Starlight Obsidian",
        subtitle = "Deep cosmic slate with electric cyan & solar gold",
        isDark = true
    ),
    FROSTED_GLASS(
        id = "frosted_glass",
        title = "Frosted Glass",
        subtitle = "Translucent ice surfaces & crisp sapphire highlights",
        isDark = true
    ),
    NEON_CYBER(
        id = "neon_cyber",
        title = "Neon Cyber",
        subtitle = "Pure OLED black with vivid magenta & mint lime",
        isDark = true
    ),
    SOLAR_WARM(
        id = "solar_warm",
        title = "Editorial Sand",
        subtitle = "Warm minimalist linen & terracotta accents",
        isDark = false
    ),
    DYNAMIC_YOU(
        id = "dynamic_you",
        title = "Material You",
        subtitle = "Adapts dynamically to your system wallpaper palette",
        isDark = true
    )
}

enum class IconPackStyle(
    val id: String,
    val title: String,
    val description: String
) {
    SYSTEM("system", "System Original", "Native adaptive & legacy app icons"),
    STARLIGHT_GLASS("starlight_glass", "Starlight Glass", "Frosted glass frames with subtle radial glow"),
    MONOCHROME_TINT("monochrome_tint", "Zstar Monochrome", "Unified duotone icons tinted to theme accent"),
    NEON_OUTLINE("neon_outline", "Neon Rim", "Obsidian core framed by luminous theme edges"),
    MINIMAL_PIXEL("minimal_pixel", "Minimal Flat", "Clean uniform matte backdrop & crisp scaling"),
    EXTERNAL("external", "External Pack", "Apply an installed third-party icon pack")
}

enum class IconShapeOption(
    val id: String,
    val title: String,
    val cornerPercent: Int
) {
    SQUIRCLE("squircle", "Squircle", 32),
    CIRCLE("circle", "Circle", 50),
    ROUNDED("rounded", "Rounded", 22),
    SHARP("sharp", "Crisp Tile", 12)
}

enum class WallpaperPreset(
    val id: String,
    val title: String
) {
    NEBULA("nebula", "Cosmic Glass"),
    MONOLITH("monolith", "Basalt Rim"),
    AURORA("aurora", "Midnight Aurora"),
    CUSTOM_PHOTO("custom_photo", "Custom Photo")
}

enum class GestureTrigger(
    val id: String,
    val title: String,
    val description: String
) {
    SWIPE_UP("swipe_up", "Swipe Up", "Slide upward anywhere on the home workspace"),
    SWIPE_DOWN("swipe_down", "Swipe Down", "Slide downward anywhere on the home workspace"),
    DOUBLE_TAP("double_tap", "Double Tap", "Tap twice quickly on empty workspace"),
    TWO_FINGER_TAP("two_finger_tap", "Two-Finger Tap", "Tap workspace with two fingers simultaneously"),
    DOCK_SWIPE_LEFT("dock_swipe_left", "Dock Swipe Left", "Swipe left across the bottom dock bar"),
    DOCK_SWIPE_RIGHT("dock_swipe_right", "Dock Swipe Right", "Swipe right across the bottom dock bar")
}

enum class GestureAction(
    val id: String,
    val title: String,
    val subtitle: String
) {
    OPEN_DRAWER("open_drawer", "Open App Drawer", "Reveal all installed apps"),
    OPEN_SEARCH("open_search", "Universal Search", "Search apps, files, and quick math"),
    EXPAND_NOTIFICATIONS("expand_notifications", "Notification Shade", "Pull down system notifications"),
    EXPAND_QUICK_SETTINGS("expand_quick_settings", "Quick Settings", "Open system quick settings panel"),
    OPEN_WIDGET_PICKER("open_widget_picker", "Add Widgets", "Open Zstar & third-party widget picker"),
    OPEN_SETTINGS("open_settings", "Zstar Settings", "Open theme, icon pack, and gesture studio"),
    TOGGLE_ZEN_MODE("toggle_zen_mode", "Toggle Zen Mode", "Hide home grid & labels for pure focus"),
    OPEN_INTERNET_CONTROLS("open_internet_controls", "Internet & Wi-Fi Panel", "Open quick Wi-Fi & Mobile Data controls"),
    OPEN_VOLUME_CONTROLS("open_volume_controls", "Volume & Sound Panel", "Open system volume sliders"),
    OPEN_APP_STORE("open_app_store", "Get Apps Store", "Browse and download new apps"),
    LAUNCH_APP("launch_app", "Launch Specific App", "Open a chosen app instantly")
}

@Immutable
data class GestureBinding(
    val action: GestureAction,
    val targetComponentKey: String? = null,
    val targetAppLabel: String? = null
)

@Immutable
data class ExternalIconPack(
    val packageName: String,
    val label: String
)

@Immutable
data class WidgetProviderItem(
    val componentName: String,
    val packageName: String,
    val label: String,
    val appName: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val spanCols: Int,
    val spanRows: Int,
    val providerInfo: AppWidgetProviderInfo
)

@Immutable
data class IndexedFileItem(
    val uriString: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val folderName: String
)

@Immutable
data class IndexedFolderSummary(
    val treeUriString: String,
    val displayName: String,
    val addedAt: Long,
    val fileCount: Int
)

enum class HomeItemType {
    APP,
    DOCK_APP,
    THIRD_PARTY_WIDGET,
    ZSTAR_CLOCK_WIDGET,
    ZSTAR_FOCUS_WIDGET,
    FOLDER
}

@Immutable
data class HomeItemUi(
    val id: Long,
    val type: HomeItemType,
    val pageIndex: Int,
    val orderIndex: Int,
    val componentKey: String? = null,
    val appInfo: AppInfo? = null,
    val customLabel: String? = null,
    val appWidgetId: Int = -1,
    val widgetProviderComponent: String? = null,
    val spanCols: Int = 1,
    val spanRows: Int = 1,
    val folderTitle: String? = null,
    val folderApps: List<AppInfo> = emptyList()
)

@Immutable
data class LauncherSettingsState(
    val themePreset: ThemePreset = ThemePreset.STARLIGHT_OBSIDIAN,
    val iconPackStyle: IconPackStyle = IconPackStyle.STARLIGHT_GLASS,
    val externalIconPackPackage: String? = null,
    val iconShape: IconShapeOption = IconShapeOption.SQUIRCLE,
    val wallpaperPreset: WallpaperPreset = WallpaperPreset.NEBULA,
    val customWallpaperUri: String? = null,
    val wallpaperDim: Float = 0.18f,
    val wallpaperBlurEnabled: Boolean = true,
    val gridColumns: Int = 4,
    val dockSlots: Int = 4,
    val showAppLabels: Boolean = true,
    val dockGlassOpacity: Float = 0.72f,
    val iconScale: Float = 1.0f,
    val reduceMotion: Boolean = false,
    val preloadIcons: Boolean = true,
    val showPerformanceHud: Boolean = false,
    val zenMode: Boolean = false,
    val showBootAnimation: Boolean = true,
    val gestureMappings: Map<GestureTrigger, GestureBinding> = defaultGestureMappings()
)

fun defaultGestureMappings(): Map<GestureTrigger, GestureBinding> = mapOf(
    GestureTrigger.SWIPE_UP to GestureBinding(GestureAction.OPEN_DRAWER),
    GestureTrigger.SWIPE_DOWN to GestureBinding(GestureAction.OPEN_SEARCH),
    GestureTrigger.DOUBLE_TAP to GestureBinding(GestureAction.TOGGLE_ZEN_MODE),
    GestureTrigger.TWO_FINGER_TAP to GestureBinding(GestureAction.OPEN_SETTINGS),
    GestureTrigger.DOCK_SWIPE_LEFT to GestureBinding(GestureAction.OPEN_WIDGET_PICKER),
    GestureTrigger.DOCK_SWIPE_RIGHT to GestureBinding(GestureAction.EXPAND_NOTIFICATIONS)
)

@Immutable
data class PerformanceMetrics(
    val appCount: Int = 0,
    val iconCacheEntries: Int = 0,
    val iconCacheHits: Long = 0,
    val iconCacheMisses: Long = 0,
    val lastAppScanMs: Long = 0,
    val indexedFilesCount: Int = 0
) {
    val cacheHitRatioPercent: Int
        get() {
            val total = iconCacheHits + iconCacheMisses
            return if (total == 0L) 100 else ((iconCacheHits * 100) / total).toInt()
        }
}

enum class NetworkTransport(val label: String) {
    WIFI("Wi-Fi"),
    CELLULAR("Mobile Data"),
    ETHERNET("Ethernet"),
    VPN("VPN"),
    NONE("Offline")
}

@Immutable
data class NetworkStatusUi(
    val isConnected: Boolean = false,
    val isValidated: Boolean = false,
    val transport: NetworkTransport = NetworkTransport.NONE,
    val isMetered: Boolean = false
) {
    val summaryText: String
        get() = when {
            !isConnected -> "Offline · No Internet"
            isValidated -> "${transport.label} · Online"
            else -> "${transport.label} · Checking"
        }
}

@Immutable
data class VolumeStateUi(
    val mediaVolume: Int = 8,
    val mediaMax: Int = 15,
    val ringVolume: Int = 5,
    val ringMax: Int = 7,
    val notificationVolume: Int = 5,
    val notificationMax: Int = 7,
    val alarmVolume: Int = 5,
    val alarmMax: Int = 7,
    val ringerMode: Int = 2,
    val isMediaMuted: Boolean = false
) {
    val mediaPercent: Int
        get() = if (mediaMax <= 0) 0 else ((mediaVolume * 100f) / mediaMax).toInt().coerceIn(0, 100)
}
