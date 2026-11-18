package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.GestureAction
import com.example.model.GestureBinding
import com.example.model.GestureTrigger
import com.example.model.IconPackStyle
import com.example.model.IconShapeOption
import com.example.model.LauncherSettingsState
import com.example.model.ThemePreset
import com.example.model.WallpaperPreset
import com.example.model.defaultGestureMappings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zstarDataStore: DataStore<Preferences> by preferencesDataStore(name = "zstar_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val ICON_PACK_STYLE = stringPreferencesKey("icon_pack_style")
        val EXTERNAL_ICON_PACK = stringPreferencesKey("external_icon_pack")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val WALLPAPER_PRESET = stringPreferencesKey("wallpaper_preset")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
        val WALLPAPER_BLUR = booleanPreferencesKey("wallpaper_blur")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val DOCK_SLOTS = intPreferencesKey("dock_slots")
        val SHOW_APP_LABELS = booleanPreferencesKey("show_app_labels")
        val DOCK_GLASS_OPACITY = floatPreferencesKey("dock_glass_opacity")
        val ICON_SCALE = floatPreferencesKey("icon_scale")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val PRELOAD_ICONS = booleanPreferencesKey("preload_icons")
        val SHOW_PERF_HUD = booleanPreferencesKey("show_perf_hud")
        val ZEN_MODE = booleanPreferencesKey("zen_mode")
        val SHOW_BOOT_ANIMATION = booleanPreferencesKey("show_boot_animation")

        fun gestureActionKey(trigger: GestureTrigger) = stringPreferencesKey("gesture_action_${trigger.id}")
        fun gestureAppKey(trigger: GestureTrigger) = stringPreferencesKey("gesture_app_${trigger.id}")
        fun gestureAppLabelKey(trigger: GestureTrigger) = stringPreferencesKey("gesture_label_${trigger.id}")
    }

    val settingsFlow: Flow<LauncherSettingsState> = context.zstarDataStore.data.map { prefs ->
        val defaults = defaultGestureMappings()
        val mappedGestures = GestureTrigger.entries.associateWith { trigger ->
            val actionId = prefs[Keys.gestureActionKey(trigger)]
            val action = GestureAction.entries.find { it.id == actionId }
                ?: defaults[trigger]?.action
                ?: GestureAction.OPEN_DRAWER
            GestureBinding(
                action = action,
                targetComponentKey = prefs[Keys.gestureAppKey(trigger)],
                targetAppLabel = prefs[Keys.gestureAppLabelKey(trigger)]
            )
        }

        LauncherSettingsState(
            themePreset = ThemePreset.entries.find { it.id == prefs[Keys.THEME_PRESET] }
                ?: ThemePreset.STARLIGHT_OBSIDIAN,
            iconPackStyle = IconPackStyle.entries.find { it.id == prefs[Keys.ICON_PACK_STYLE] }
                ?: IconPackStyle.STARLIGHT_GLASS,
            externalIconPackPackage = prefs[Keys.EXTERNAL_ICON_PACK],
            iconShape = IconShapeOption.entries.find { it.id == prefs[Keys.ICON_SHAPE] }
                ?: IconShapeOption.SQUIRCLE,
            wallpaperPreset = WallpaperPreset.entries.find { it.id == prefs[Keys.WALLPAPER_PRESET] }
                ?: WallpaperPreset.NEBULA,
            customWallpaperUri = prefs[Keys.CUSTOM_WALLPAPER_URI],
            wallpaperDim = prefs[Keys.WALLPAPER_DIM] ?: 0.18f,
            wallpaperBlurEnabled = prefs[Keys.WALLPAPER_BLUR] ?: true,
            gridColumns = prefs[Keys.GRID_COLUMNS] ?: 4,
            dockSlots = prefs[Keys.DOCK_SLOTS] ?: 4,
            showAppLabels = prefs[Keys.SHOW_APP_LABELS] ?: true,
            dockGlassOpacity = prefs[Keys.DOCK_GLASS_OPACITY] ?: 0.72f,
            iconScale = prefs[Keys.ICON_SCALE] ?: 1.0f,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            preloadIcons = prefs[Keys.PRELOAD_ICONS] ?: true,
            showPerformanceHud = prefs[Keys.SHOW_PERF_HUD] ?: false,
            zenMode = prefs[Keys.ZEN_MODE] ?: false,
            showBootAnimation = prefs[Keys.SHOW_BOOT_ANIMATION] ?: true,
            gestureMappings = mappedGestures
        )
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        context.zstarDataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }

    suspend fun setIconPackStyle(style: IconPackStyle, externalPkg: String? = null) {
        context.zstarDataStore.edit {
            it[Keys.ICON_PACK_STYLE] = style.id
            if (externalPkg != null) {
                it[Keys.EXTERNAL_ICON_PACK] = externalPkg
            } else if (style != IconPackStyle.EXTERNAL) {
                it.remove(Keys.EXTERNAL_ICON_PACK)
            }
        }
    }

    suspend fun setIconShape(shape: IconShapeOption) {
        context.zstarDataStore.edit { it[Keys.ICON_SHAPE] = shape.id }
    }

    suspend fun setWallpaperPreset(preset: WallpaperPreset, customUri: String? = null) {
        context.zstarDataStore.edit {
            it[Keys.WALLPAPER_PRESET] = preset.id
            if (customUri != null) {
                it[Keys.CUSTOM_WALLPAPER_URI] = customUri
            }
        }
    }

    suspend fun setWallpaperDim(dim: Float) {
        context.zstarDataStore.edit { it[Keys.WALLPAPER_DIM] = dim.coerceIn(0f, 0.75f) }
    }

    suspend fun setWallpaperBlur(enabled: Boolean) {
        context.zstarDataStore.edit { it[Keys.WALLPAPER_BLUR] = enabled }
    }

    suspend fun setGridColumns(columns: Int) {
        context.zstarDataStore.edit { it[Keys.GRID_COLUMNS] = columns.coerceIn(4, 5) }
    }

    suspend fun setDockSlots(slots: Int) {
        context.zstarDataStore.edit { it[Keys.DOCK_SLOTS] = slots.coerceIn(4, 5) }
    }

    suspend fun setShowAppLabels(show: Boolean) {
        context.zstarDataStore.edit { it[Keys.SHOW_APP_LABELS] = show }
    }

    suspend fun setDockGlassOpacity(opacity: Float) {
        context.zstarDataStore.edit { it[Keys.DOCK_GLASS_OPACITY] = opacity.coerceIn(0.15f, 0.95f) }
    }

    suspend fun setIconScale(scale: Float) {
        context.zstarDataStore.edit { it[Keys.ICON_SCALE] = scale.coerceIn(0.8f, 1.15f) }
    }

    suspend fun setReduceMotion(reduce: Boolean) {
        context.zstarDataStore.edit { it[Keys.REDUCE_MOTION] = reduce }
    }

    suspend fun setPreloadIcons(preload: Boolean) {
        context.zstarDataStore.edit { it[Keys.PRELOAD_ICONS] = preload }
    }

    suspend fun setShowPerformanceHud(show: Boolean) {
        context.zstarDataStore.edit { it[Keys.SHOW_PERF_HUD] = show }
    }

    suspend fun setZenMode(zen: Boolean) {
        context.zstarDataStore.edit { it[Keys.ZEN_MODE] = zen }
    }

    suspend fun setShowBootAnimation(enabled: Boolean) {
        context.zstarDataStore.edit { it[Keys.SHOW_BOOT_ANIMATION] = enabled }
    }

    suspend fun setGestureBinding(trigger: GestureTrigger, binding: GestureBinding) {
        context.zstarDataStore.edit { prefs ->
            prefs[Keys.gestureActionKey(trigger)] = binding.action.id
            if (binding.targetComponentKey != null) {
                prefs[Keys.gestureAppKey(trigger)] = binding.targetComponentKey
            } else {
                prefs.remove(Keys.gestureAppKey(trigger))
            }
            if (binding.targetAppLabel != null) {
                prefs[Keys.gestureAppLabelKey(trigger)] = binding.targetAppLabel
            } else {
                prefs.remove(Keys.gestureAppLabelKey(trigger))
            }
        }
    }

    suspend fun resetGesturesToDefaults() {
        context.zstarDataStore.edit { prefs ->
            defaultGestureMappings().forEach { (trigger, binding) ->
                prefs[Keys.gestureActionKey(trigger)] = binding.action.id
                prefs.remove(Keys.gestureAppKey(trigger))
                prefs.remove(Keys.gestureAppLabelKey(trigger))
            }
        }
    }
}
