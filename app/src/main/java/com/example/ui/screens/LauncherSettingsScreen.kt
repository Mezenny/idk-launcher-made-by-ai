package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.system.AppIconCache
import com.example.model.AppInfo
import com.example.model.ExternalIconPack
import com.example.model.GestureBinding
import com.example.model.GestureTrigger
import com.example.model.IconPackStyle
import com.example.model.IconShapeOption
import com.example.model.IndexedFolderSummary
import com.example.model.LauncherSettingsState
import com.example.model.NetworkStatusUi
import com.example.model.PerformanceMetrics
import com.example.model.ThemePreset
import com.example.model.VolumeStateUi
import com.example.model.WallpaperPreset
import com.example.ui.components.StyledAppIconGraphic
import com.example.ui.theme.LocalZstarColors

@Composable
fun LauncherSettingsScreen(
    initialTab: Int,
    isDefaultLauncher: Boolean,
    originalLauncherName: String?,
    settings: LauncherSettingsState,
    allApps: List<AppInfo>,
    externalIconPacks: List<ExternalIconPack>,
    indexedFolders: List<IndexedFolderSummary>,
    metrics: PerformanceMetrics,
    networkStatus: NetworkStatusUi,
    volumeState: VolumeStateUi,
    iconCache: AppIconCache,
    onRequestDefaultHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onLaunchOriginalLauncher: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onOpenInternetPanel: () -> Unit,
    onOpenWifiControls: () -> Unit,
    onOpenMobileDataControls: () -> Unit,
    onConnectHomeWifi: (String, String) -> Unit,
    onMediaVolumeChange: (Int) -> Unit,
    onRingVolumeChange: (Int) -> Unit,
    onNotificationVolumeChange: (Int) -> Unit,
    onAlarmVolumeChange: (Int) -> Unit,
    onToggleMediaMute: () -> Unit,
    onOpenVolumePanel: () -> Unit,
    onSelectTheme: (ThemePreset) -> Unit,
    onSelectWallpaper: (WallpaperPreset, String?) -> Unit,
    onWallpaperDimChange: (Float) -> Unit,
    onWallpaperBlurChange: (Boolean) -> Unit,
    onDockGlassChange: (Float) -> Unit,
    onSelectIconPack: (IconPackStyle, String?) -> Unit,
    onSelectIconShape: (IconShapeOption) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onDockSlotsChange: (Int) -> Unit,
    onShowLabelsChange: (Boolean) -> Unit,
    onIconScaleChange: (Float) -> Unit,
    onGestureBindingChange: (GestureTrigger, GestureBinding) -> Unit,
    onResetGestures: () -> Unit,
    onTestGesture: (GestureTrigger) -> Unit,
    onAddSafFolder: () -> Unit,
    onRescanFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onPreloadIconsChange: (Boolean) -> Unit,
    onShowHudChange: (Boolean) -> Unit,
    onShowBootAnimationChange: (Boolean) -> Unit,
    onReplayBootAnimation: () -> Unit,
    onClearIconCache: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectWallpaper(WallpaperPreset.CUSTOM_PHOTO, uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .testTag("launcher_settings_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            IconButton(onClick = onClose, modifier = Modifier.testTag("settings_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to Home"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Zstar Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Themes, icon packs, gestures & performance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onOpenAndroidSettings,
                modifier = Modifier.testTag("header_android_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Android Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = if (isDefaultLauncher) onOpenHomeSettings else onRequestDefaultHome,
                colors = if (isDefaultLauncher) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("set_default_launcher_button")
            ) {
                Icon(
                    imageVector = if (isDefaultLauncher) Icons.Rounded.SwapHoriz else Icons.Rounded.Home,
                    contentDescription = if (isDefaultLauncher) "Switch Home Launcher" else "Set Default Home",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDefaultLauncher) "Switch Home" else "Set Default",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        val tabs = listOf("Themes", "Icons & Grid", "Gestures", "Engine & Files")
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) },
                    modifier = Modifier.testTag("settings_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> ThemesAndWallpaperTab(
                settings = settings,
                onSelectTheme = onSelectTheme,
                onSelectWallpaper = onSelectWallpaper,
                onPickPhotoWallpaper = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onWallpaperDimChange = onWallpaperDimChange,
                onWallpaperBlurChange = onWallpaperBlurChange,
                onDockGlassChange = onDockGlassChange
            )
            1 -> IconsAndGridTab(
                settings = settings,
                previewApps = allApps.take(4),
                externalIconPacks = externalIconPacks,
                iconCache = iconCache,
                onSelectIconPack = onSelectIconPack,
                onSelectIconShape = onSelectIconShape,
                onGridColumnsChange = onGridColumnsChange,
                onDockSlotsChange = onDockSlotsChange,
                onShowLabelsChange = onShowLabelsChange,
                onIconScaleChange = onIconScaleChange
            )
            2 -> GesturesSettingsTab(
                settings = settings,
                allApps = allApps,
                onGestureBindingChange = onGestureBindingChange,
                onResetGestures = onResetGestures,
                onTestGesture = onTestGesture
            )
            3 -> PerformanceAndFilesTab(
                isDefaultLauncher = isDefaultLauncher,
                originalLauncherName = originalLauncherName,
                settings = settings,
                metrics = metrics,
                networkStatus = networkStatus,
                volumeState = volumeState,
                indexedFolders = indexedFolders,
                onOpenHomeSettings = onOpenHomeSettings,
                onLaunchOriginalLauncher = onLaunchOriginalLauncher,
                onOpenAndroidSettings = onOpenAndroidSettings,
                onOpenInternetPanel = onOpenInternetPanel,
                onOpenWifiControls = onOpenWifiControls,
                onOpenMobileDataControls = onOpenMobileDataControls,
                onConnectHomeWifi = onConnectHomeWifi,
                onMediaVolumeChange = onMediaVolumeChange,
                onRingVolumeChange = onRingVolumeChange,
                onNotificationVolumeChange = onNotificationVolumeChange,
                onAlarmVolumeChange = onAlarmVolumeChange,
                onToggleMediaMute = onToggleMediaMute,
                onOpenVolumePanel = onOpenVolumePanel,
                onReduceMotionChange = onReduceMotionChange,
                onPreloadIconsChange = onPreloadIconsChange,
                onShowHudChange = onShowHudChange,
                onShowBootAnimationChange = onShowBootAnimationChange,
                onReplayBootAnimation = onReplayBootAnimation,
                onClearIconCache = onClearIconCache,
                onAddSafFolder = onAddSafFolder,
                onRescanFolder = onRescanFolder,
                onRemoveFolder = onRemoveFolder
            )
        }
    }
}

@Composable
private fun ThemesAndWallpaperTab(
    settings: LauncherSettingsState,
    onSelectTheme: (ThemePreset) -> Unit,
    onSelectWallpaper: (WallpaperPreset, String?) -> Unit,
    onPickPhotoWallpaper: () -> Unit,
    onWallpaperDimChange: (Float) -> Unit,
    onWallpaperBlurChange: (Boolean) -> Unit,
    onDockGlassChange: (Float) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "THEME PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemePreset.entries.forEach { preset ->
                    val active = preset == settings.themePreset
                    SettingsOptionCard(
                        title = preset.title,
                        subtitle = preset.subtitle,
                        selected = active,
                        onClick = { onSelectTheme(preset) }
                    )
                }
            }
        }

        item {
            Text(
                text = "WALLPAPER STUDIO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val builtIns = listOf(
                    WallpaperPreset.NEBULA to R.drawable.img_wallpaper_nebula,
                    WallpaperPreset.MONOLITH to R.drawable.img_wallpaper_monolith,
                    WallpaperPreset.AURORA to R.drawable.img_wallpaper_aurora
                )
                items(builtIns, key = { it.first.id }) { (preset, drawableRes) ->
                    val selected = settings.wallpaperPreset == preset
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 152.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onSelectWallpaper(preset, null) }
                    ) {
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = preset.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                item {
                    val isCustom = settings.wallpaperPreset == WallpaperPreset.CUSTOM_PHOTO
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .size(width = 96.dp, height = 152.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isCustom) 2.5.dp else 1.dp,
                                color = if (isCustom) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable(onClick = onPickPhotoWallpaper)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = "Choose photo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Photo Picker",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            SettingsSliderCard(
                title = "Wallpaper Dim Scrim",
                valueText = "${(settings.wallpaperDim * 100).toInt()}%",
                value = settings.wallpaperDim,
                range = 0f..0.7f,
                onValueChange = onWallpaperDimChange
            )
        }

        item {
            SettingsSliderCard(
                title = "Dock Glass Opacity",
                valueText = "${(settings.dockGlassOpacity * 100).toInt()}%",
                value = settings.dockGlassOpacity,
                range = 0.2f..0.95f,
                onValueChange = onDockGlassChange
            )
        }

        item {
            SettingsSwitchCard(
                title = "Overlay Depth Blur",
                subtitle = "Blur wallpaper behind App Drawer and Search (Android 12+)",
                checked = settings.wallpaperBlurEnabled,
                onCheckedChange = onWallpaperBlurChange
            )
        }
    }
}

@Composable
private fun IconsAndGridTab(
    settings: LauncherSettingsState,
    previewApps: List<AppInfo>,
    externalIconPacks: List<ExternalIconPack>,
    iconCache: AppIconCache,
    onSelectIconPack: (IconPackStyle, String?) -> Unit,
    onSelectIconShape: (IconShapeOption) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onDockSlotsChange: (Int) -> Unit,
    onShowLabelsChange: (Boolean) -> Unit,
    onIconScaleChange: (Float) -> Unit
) {
    val zstarColors = LocalZstarColors.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (previewApps.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(zstarColors.glassSurface)
                        .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "LIVE ICON PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        previewApps.forEach { app ->
                            StyledAppIconGraphic(
                                app = app,
                                iconCache = iconCache,
                                iconPackStyle = settings.iconPackStyle,
                                externalPackPkg = settings.externalIconPackPackage,
                                iconShape = settings.iconShape,
                                size = (54f * settings.iconScale).dp
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "BUILT-IN ICON PACK STYLES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconPackStyle.entries.filter { it != IconPackStyle.EXTERNAL }.forEach { style ->
                    SettingsOptionCard(
                        title = style.title,
                        subtitle = style.description,
                        selected = settings.iconPackStyle == style,
                        onClick = { onSelectIconPack(style, null) }
                    )
                }
            }
        }

        item {
            Text(
                text = "INSTALLED EXTERNAL ICON PACKS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (externalIconPacks.isEmpty()) {
                Text(
                    text = "No third-party ADW/Nova icon packs detected. Built-in Zstar packs are active.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    externalIconPacks.forEach { pack ->
                        val active = settings.iconPackStyle == IconPackStyle.EXTERNAL &&
                            settings.externalIconPackPackage == pack.packageName
                        SettingsOptionCard(
                            title = pack.label,
                            subtitle = pack.packageName,
                            selected = active,
                            onClick = { onSelectIconPack(IconPackStyle.EXTERNAL, pack.packageName) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "ICON SHAPE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconShapeOption.entries.forEach { shape ->
                    val selected = settings.iconShape == shape
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectIconShape(shape) }
                    ) {
                        Text(
                            text = shape.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            SettingsSliderCard(
                title = "Icon Scale",
                valueText = "${(settings.iconScale * 100).toInt()}%",
                value = settings.iconScale,
                range = 0.85f..1.15f,
                onValueChange = onIconScaleChange
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HOME GRID COLUMNS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 5).forEach { cols ->
                            val selected = settings.gridColumns == cols
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onGridColumnsChange(cols) }
                            ) {
                                Text(
                                    text = "$cols cols",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DOCK SLOTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 5).forEach { slots ->
                            val selected = settings.dockSlots == slots
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDockSlotsChange(slots) }
                            ) {
                                Text(
                                    text = "$slots icons",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSwitchCard(
                title = "Show Home Screen Labels",
                subtitle = "Display app titles underneath home grid & folder icons",
                checked = settings.showAppLabels,
                onCheckedChange = onShowLabelsChange
            )
        }
    }
}

@Composable
fun SettingsOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SettingsSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSliderCard(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}
