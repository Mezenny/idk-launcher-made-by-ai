package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.system.AppIconCache
import com.example.data.system.WidgetHostManager
import com.example.model.AppInfo
import com.example.model.GestureTrigger
import com.example.model.HomeItemType
import com.example.model.HomeItemUi
import com.example.model.LauncherSettingsState
import com.example.model.PerformanceMetrics
import com.example.ui.components.FolderIconTile
import com.example.ui.components.StoreShortcutIconTile
import com.example.ui.components.StyledAppIconTile
import com.example.ui.components.ThirdPartyWidgetHostCard
import com.example.ui.components.ZstarClockTelemetryWidget
import com.example.ui.components.ZstarFocusWidget
import com.example.ui.theme.LocalZstarColors
import com.example.viewmodel.LauncherOverlay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherHomeScreen(
    settings: LauncherSettingsState,
    homeItems: List<HomeItemUi>,
    totalPages: Int,
    metrics: PerformanceMetrics,
    iconCache: AppIconCache,
    widgetHostManager: WidgetHostManager,
    resolveWidgetProvider: (String?) -> android.appwidget.AppWidgetProviderInfo?,
    isEditMode: Boolean,
    isDefaultLauncher: Boolean,
    isPlayStoreInstalled: Boolean,
    onSetEditMode: (Boolean) -> Unit,
    onRequestDefaultHome: () -> Unit,
    onTriggerGesture: (GestureTrigger) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenFolder: (HomeItemUi) -> Unit,
    onShowAppMenu: (AppInfo, HomeItemUi) -> Unit,
    onRemoveHomeItem: (HomeItemUi) -> Unit,
    onResizeWidget: (HomeItemUi, Int, Int) -> Unit,
    onAddPage: () -> Int,
    onOpenOverlay: (LauncherOverlay) -> Unit,
    onToggleZenMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zstarColors = LocalZstarColors.current
    val pagerState = rememberPagerState(pageCount = { totalPages.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()

    val dockItems = remember(homeItems, settings.dockSlots) {
        homeItems.filter { it.type == HomeItemType.DOCK_APP }
            .sortedBy { it.orderIndex }
            .take(settings.dockSlots)
    }

    var verticalDragTotal by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Two-finger tap detection without interfering with children
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.size == 2 && event.changes.all { it.pressed && !it.previousPressed }) {
                            onTriggerGesture(GestureTrigger.TWO_FINGER_TAP)
                        }
                    }
                }
            }
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onDoubleTap = { onTriggerGesture(GestureTrigger.DOUBLE_TAP) },
                    onLongPress = { onSetEditMode(!isEditMode) }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDragTotal = 0f },
                    onVerticalDrag = { _, dragAmount -> verticalDragTotal += dragAmount },
                    onDragEnd = {
                        if (verticalDragTotal < -90f) {
                            onTriggerGesture(GestureTrigger.SWIPE_UP)
                        } else if (verticalDragTotal > 90f) {
                            onTriggerGesture(GestureTrigger.SWIPE_DOWN)
                        }
                        verticalDragTotal = 0f
                    }
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
        ) {
            // Top Edit Mode Banner or Performance HUD
            AnimatedVisibility(
                visible = isEditMode,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                EditModeToolbar(
                    onAddWidget = { onOpenOverlay(LauncherOverlay.WidgetPicker) },
                    onOpenThemes = { onOpenOverlay(LauncherOverlay.Settings(0)) },
                    onAddPage = {
                        val newPageIdx = onAddPage()
                        scope.launch { pagerState.animateScrollToPage(newPageIdx) }
                    },
                    onDone = { onSetEditMode(false) }
                )
            }

            if (!isEditMode && !isDefaultLauncher) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), CircleShape)
                        .clickable(onClick = onRequestDefaultHome)
                        .testTag("home_set_default_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Not Default Home · Tap to Replace System Launcher",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            if (!isEditMode && settings.showPerformanceHud) {
                Surface(
                    color = zstarColors.glassSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .border(1.dp, zstarColors.glassBorder, CircleShape)
                        .clickable { onOpenOverlay(LauncherOverlay.Settings(3)) }
                ) {
                    Text(
                        text = "ZSTAR HUD · ${metrics.appCount} APPS · CACHE ${metrics.cacheHitRatioPercent}% (${metrics.iconCacheEntries}) · ${metrics.lastAppScanMs}ms",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            if (settings.zenMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(zstarColors.glassSurface)
                            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(24.dp))
                            .clickable(onClick = onToggleZenMode)
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Zen Mode Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Zen Mode Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Double-tap workspace or tap here to restore icons",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Multi-Page Home Workspace
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                ) { page ->
                    val pageItems = remember(homeItems, page) {
                        homeItems.filter { it.pageIndex == page && it.type != HomeItemType.DOCK_APP }
                            .sortedBy { it.orderIndex }
                    }
                    HomeWorkspacePage(
                        pageItems = pageItems,
                        settings = settings,
                        metrics = metrics,
                        iconCache = iconCache,
                        widgetHostManager = widgetHostManager,
                        resolveWidgetProvider = resolveWidgetProvider,
                        isEditMode = isEditMode,
                        isPlayStoreInstalled = isPlayStoreInstalled,
                        onSetEditMode = onSetEditMode,
                        onLaunchApp = onLaunchApp,
                        onOpenFolder = onOpenFolder,
                        onShowAppMenu = onShowAppMenu,
                        onRemoveHomeItem = onRemoveHomeItem,
                        onResizeWidget = onResizeWidget,
                        onOpenSearch = { onOpenOverlay(LauncherOverlay.UniversalSearch) },
                        onOpenGetApps = { onOpenOverlay(LauncherOverlay.GetApps) },
                        onOpenSettings = { onOpenOverlay(LauncherOverlay.Settings()) },
                        onOpenWidgetPicker = { onOpenOverlay(LauncherOverlay.WidgetPicker) },
                        onToggleZenMode = onToggleZenMode
                    )
                }
            }

            // Page Indicators & Quick Action Strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                if (totalPages > 1) {
                    repeat(totalPages) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }

            // Bottom Dock + Quick Control Bar
            HomeDockBar(
                dockItems = dockItems,
                settings = settings,
                iconCache = iconCache,
                isEditMode = isEditMode,
                onLaunchApp = onLaunchApp,
                onShowAppMenu = onShowAppMenu,
                onRemoveHomeItem = onRemoveHomeItem,
                onOpenDrawer = { onOpenOverlay(LauncherOverlay.AppDrawer) },
                onOpenSearch = { onOpenOverlay(LauncherOverlay.UniversalSearch) },
                onOpenGetApps = { onOpenOverlay(LauncherOverlay.GetApps) },
                onOpenSettings = { onOpenOverlay(LauncherOverlay.Settings()) },
                onDockSwipeLeft = { onTriggerGesture(GestureTrigger.DOCK_SWIPE_LEFT) },
                onDockSwipeRight = { onTriggerGesture(GestureTrigger.DOCK_SWIPE_RIGHT) }
            )
        }
    }
}

@Composable
private fun HomeWorkspacePage(
    pageItems: List<HomeItemUi>,
    settings: LauncherSettingsState,
    metrics: PerformanceMetrics,
    iconCache: AppIconCache,
    widgetHostManager: WidgetHostManager,
    resolveWidgetProvider: (String?) -> android.appwidget.AppWidgetProviderInfo?,
    isEditMode: Boolean,
    isPlayStoreInstalled: Boolean,
    onSetEditMode: (Boolean) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenFolder: (HomeItemUi) -> Unit,
    onShowAppMenu: (AppInfo, HomeItemUi) -> Unit,
    onRemoveHomeItem: (HomeItemUi) -> Unit,
    onResizeWidget: (HomeItemUi, Int, Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenGetApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onToggleZenMode: () -> Unit
) {
    val widgets = remember(pageItems) {
        pageItems.filter {
            it.type == HomeItemType.ZSTAR_CLOCK_WIDGET ||
                it.type == HomeItemType.ZSTAR_FOCUS_WIDGET ||
                it.type == HomeItemType.THIRD_PARTY_WIDGET
        }
    }
    val gridItems = remember(pageItems) {
        pageItems.filter {
            it.type == HomeItemType.APP ||
                it.type == HomeItemType.FOLDER ||
                it.type == HomeItemType.ZSTAR_STORE_SHORTCUT
        }
    }

    val scrollState = rememberScrollState()
    val iconSize = (54f * settings.iconScale).dp

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        widgets.forEach { widgetItem ->
            when (widgetItem.type) {
                HomeItemType.ZSTAR_CLOCK_WIDGET -> {
                    ZstarClockTelemetryWidget(
                        metrics = metrics,
                        isEditMode = isEditMode,
                        onOpenSearch = onOpenSearch,
                        onOpenSettings = onOpenSettings,
                        onRemove = { onRemoveHomeItem(widgetItem) }
                    )
                }
                HomeItemType.ZSTAR_FOCUS_WIDGET -> {
                    ZstarFocusWidget(
                        zenMode = settings.zenMode,
                        isEditMode = isEditMode,
                        onToggleZen = onToggleZenMode,
                        onOpenWidgets = onOpenWidgetPicker,
                        onOpenThemes = onOpenSettings,
                        onRemove = { onRemoveHomeItem(widgetItem) }
                    )
                }
                HomeItemType.THIRD_PARTY_WIDGET -> {
                    ThirdPartyWidgetHostCard(
                        item = widgetItem,
                        providerInfo = resolveWidgetProvider(widgetItem.widgetProviderComponent),
                        widgetHostManager = widgetHostManager,
                        isEditMode = isEditMode,
                        onCycleHeight = {
                            val nextRows = (widgetItem.spanRows % 4) + 1
                            onResizeWidget(widgetItem, widgetItem.spanCols, nextRows)
                        },
                        onRemove = { onRemoveHomeItem(widgetItem) }
                    )
                }
                else -> Unit
            }
        }

        if (gridItems.isNotEmpty()) {
            val rows = remember(gridItems, settings.gridColumns) {
                gridItems.chunked(settings.gridColumns)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                if (item.type == HomeItemType.FOLDER) {
                                    FolderIconTile(
                                        title = item.folderTitle ?: "Folder",
                                        apps = item.folderApps,
                                        iconCache = iconCache,
                                        iconPackStyle = settings.iconPackStyle,
                                        externalPackPkg = settings.externalIconPackPackage,
                                        iconShape = settings.iconShape,
                                        iconSize = iconSize,
                                        showLabel = settings.showAppLabels,
                                        isEditMode = isEditMode,
                                        onRemoveInEditMode = { onRemoveHomeItem(item) },
                                        onClick = { onOpenFolder(item) },
                                        onLongClick = { onOpenFolder(item) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (item.type == HomeItemType.ZSTAR_STORE_SHORTCUT) {
                                    StoreShortcutIconTile(
                                        label = if (isPlayStoreInstalled) "Play Store" else "App Store",
                                        iconPackStyle = settings.iconPackStyle,
                                        iconShape = settings.iconShape,
                                        iconSize = iconSize,
                                        showLabel = settings.showAppLabels,
                                        reduceMotion = settings.reduceMotion,
                                        isEditMode = isEditMode,
                                        onRemoveInEditMode = { onRemoveHomeItem(item) },
                                        onClick = onOpenGetApps,
                                        onLongClick = { onSetEditMode(!isEditMode) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (item.appInfo != null) {
                                    StyledAppIconTile(
                                        app = item.appInfo,
                                        iconCache = iconCache,
                                        iconPackStyle = settings.iconPackStyle,
                                        externalPackPkg = settings.externalIconPackPackage,
                                        iconShape = settings.iconShape,
                                        iconSize = iconSize,
                                        showLabel = settings.showAppLabels,
                                        reduceMotion = settings.reduceMotion,
                                        isEditMode = isEditMode,
                                        onRemoveInEditMode = { onRemoveHomeItem(item) },
                                        onClick = { onLaunchApp(item.appInfo) },
                                        onLongClick = { onShowAppMenu(item.appInfo, item) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        repeat(settings.gridColumns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else if (widgets.isEmpty()) {
            // Empty page prompt
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(LocalZstarColors.current.glassSurface)
                        .border(1.dp, LocalZstarColors.current.glassBorder, RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Blank Workspace Page",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add third-party widgets or pin apps from the App Drawer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.clickable(onClick = onOpenWidgetPicker)
                    ) {
                        Text(
                            text = "Add Widget",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun HomeDockBar(
    dockItems: List<HomeItemUi>,
    settings: LauncherSettingsState,
    iconCache: AppIconCache,
    isEditMode: Boolean,
    onLaunchApp: (AppInfo) -> Unit,
    onShowAppMenu: (AppInfo, HomeItemUi) -> Unit,
    onRemoveHomeItem: (HomeItemUi) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenGetApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onDockSwipeLeft: () -> Unit,
    onDockSwipeRight: () -> Unit
) {
    val zstarColors = LocalZstarColors.current
    var horizontalDragTotal by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .padding(horizontal = 14.dp)
            .padding(bottom = 12.dp)
    ) {
        // Subtle Action Strip above dock (Drawer / Search / Get Apps / Zstar)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Surface(
                color = zstarColors.glassSurface,
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, zstarColors.glassBorder, CircleShape)
                    .clickable(onClick = onOpenDrawer)
                    .testTag("open_drawer_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = "All Apps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                color = zstarColors.glassSurface,
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, zstarColors.glassBorder, CircleShape)
                    .clickable(onClick = onOpenSearch)
                    .testTag("open_search_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search Apps and Files",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                color = zstarColors.glassSurface,
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, zstarColors.glassBorder, CircleShape)
                    .clickable(onClick = onOpenGetApps)
                    .testTag("open_get_apps_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shop,
                        contentDescription = "Get Apps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Get Apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                color = zstarColors.glassSurface,
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, zstarColors.glassBorder, CircleShape)
                    .clickable(onClick = onOpenSettings)
                    .testTag("open_settings_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Zstar Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Zstar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Frosted Glass Dock
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(zstarColors.dockSurface)
                .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(28.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { horizontalDragTotal = 0f },
                        onHorizontalDrag = { _, dragAmount -> horizontalDragTotal += dragAmount },
                        onDragEnd = {
                            if (horizontalDragTotal < -80f) onDockSwipeLeft()
                            else if (horizontalDragTotal > 80f) onDockSwipeRight()
                            horizontalDragTotal = 0f
                        }
                    )
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("home_dock")
        ) {
            dockItems.forEach { item ->
                val app = item.appInfo
                if (app != null) {
                    StyledAppIconTile(
                        app = app,
                        iconCache = iconCache,
                        iconPackStyle = settings.iconPackStyle,
                        externalPackPkg = settings.externalIconPackPackage,
                        iconShape = settings.iconShape,
                        iconSize = (50f * settings.iconScale).dp,
                        showLabel = false,
                        reduceMotion = settings.reduceMotion,
                        isEditMode = isEditMode,
                        onRemoveInEditMode = { onRemoveHomeItem(item) },
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onShowAppMenu(app, item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (dockItems.isEmpty()) {
                Text(
                    text = "Long-press any app to pin to Dock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun EditModeToolbar(
    onAddWidget: () -> Unit,
    onOpenThemes: () -> Unit,
    onAddPage: () -> Unit,
    onDone: () -> Unit
) {
    val zstarColors = LocalZstarColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(zstarColors.glassSurface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("edit_mode_toolbar")
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EditActionPill(icon = Icons.Rounded.Widgets, label = "Widget", onClick = onAddWidget)
            EditActionPill(icon = Icons.Rounded.Palette, label = "Theme", onClick = onOpenThemes)
            EditActionPill(icon = Icons.Rounded.Add, label = "Page", onClick = onAddPage)
        }
        IconButton(
            onClick = onDone,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Done editing",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EditActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
