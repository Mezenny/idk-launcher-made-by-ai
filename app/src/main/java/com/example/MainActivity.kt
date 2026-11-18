package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.system.AppDownloadHelper
import com.example.data.system.ConnectivityRepository
import com.example.data.system.DefaultLauncherHelper
import com.example.data.system.VolumeRepository
import com.example.model.WidgetProviderItem
import com.example.ui.components.AppContextMenuSheet
import com.example.ui.components.WallpaperBackground
import com.example.ui.components.ZstarBootAnimationOverlay
import com.example.ui.screens.AppDrawerOverlay
import com.example.ui.screens.FolderModalDialog
import com.example.ui.screens.GetAppsOverlay
import com.example.ui.screens.LauncherHomeScreen
import com.example.ui.screens.LauncherSettingsScreen
import com.example.ui.screens.UniversalSearchOverlay
import com.example.ui.screens.WidgetPickerSheet
import com.example.ui.theme.ZstarLauncherTheme
import com.example.viewmodel.LauncherActionEvent
import com.example.viewmodel.LauncherOverlay
import com.example.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private var pendingConfigureWidgetId: Int = -1
    private var pendingConfigureProvider: WidgetProviderItem? = null

    companion object {
        private const val REQUEST_CONFIGURE_WIDGET = 4091
    }

    override fun onStart() {
        super.onStart()
        viewModel.widgetHostManager.startListening()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDefaultLauncherStatus()
    }

    override fun onStop() {
        super.onStop()
        viewModel.widgetHostManager.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.closeOverlay()
            viewModel.setEditMode(false)
            viewModel.refreshDefaultLauncherStatus()
        }
    }

    @Deprecated("Required by AppWidgetHost.startAppWidgetConfigureActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            val appWidgetId = pendingConfigureWidgetId
            val provider = pendingConfigureProvider
            pendingConfigureWidgetId = -1
            pendingConfigureProvider = null
            if (appWidgetId > 0 && provider != null) {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.finalizeThirdPartyWidget(appWidgetId, provider)
                } else {
                    viewModel.cancelWidgetAllocation(appWidgetId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val allApps by viewModel.allApps.collectAsStateWithLifecycle()
            val drawerApps by viewModel.drawerFilteredApps.collectAsStateWithLifecycle()
            val drawerCategory by viewModel.drawerCategory.collectAsStateWithLifecycle()
            val drawerQuery by viewModel.drawerQuery.collectAsStateWithLifecycle()
            val homeItems by viewModel.homeItems.collectAsStateWithLifecycle()
            val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
            val overlay by viewModel.overlay.collectAsStateWithLifecycle()
            val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
            val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()
            val currentDefaultName by viewModel.currentDefaultLauncherName.collectAsStateWithLifecycle()
            val originalLauncherName by viewModel.originalLauncherName.collectAsStateWithLifecycle()
            val showDefaultOnboarding by viewModel.showDefaultHomeOnboarding.collectAsStateWithLifecycle()
            val searchState by viewModel.searchState.collectAsStateWithLifecycle()
            val indexedFolders by viewModel.indexedFolders.collectAsStateWithLifecycle()
            val externalIconPacks by viewModel.externalIconPacks.collectAsStateWithLifecycle()
            val widgetProviders by viewModel.widgetProviders.collectAsStateWithLifecycle()
            val metrics by viewModel.performanceMetrics.collectAsStateWithLifecycle()
            val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
            val volumeState by viewModel.volumeState.collectAsStateWithLifecycle()
            val isPlayStoreInstalled by viewModel.isPlayStoreInstalled.collectAsStateWithLifecycle()
            val menuApp by viewModel.selectedAppForMenu.collectAsStateWithLifecycle()
            val menuHomeItem by viewModel.selectedHomeItemForMenu.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            var showStartingScreen by rememberSaveable { mutableStateOf(savedInstanceState == null) }
            var forceReplayBoot by remember { mutableStateOf(false) }

            var pendingBindWidgetId by remember { mutableStateOf(-1) }
            var pendingBindProvider by remember { mutableStateOf<WidgetProviderItem?>(null) }

            val defaultHomeRoleLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.refreshDefaultLauncherStatus()
            }

            val requestDefaultHome: () -> Unit = {
                viewModel.dismissDefaultHomeOnboarding()
                try {
                    val intent = DefaultLauncherHelper.createRequestDefaultHomeIntent(this@MainActivity)
                    defaultHomeRoleLauncher.launch(intent)
                } catch (_: Exception) {
                    DefaultLauncherHelper.openHomeSettings(this@MainActivity)
                }
            }

            val bindWidgetLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val widgetId = pendingBindWidgetId
                val provider = pendingBindProvider
                pendingBindWidgetId = -1
                pendingBindProvider = null
                if (widgetId > 0 && provider != null) {
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.onWidgetBound(widgetId, provider)
                    } else {
                        viewModel.cancelWidgetAllocation(widgetId)
                    }
                }
            }

            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    viewModel.indexSafFolder(uri)
                }
            }

            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    viewModel.indexPickedFile(uri)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is LauncherActionEvent.ExpandNotifications -> expandStatusBar(quickSettings = false)
                        is LauncherActionEvent.ExpandQuickSettings -> expandStatusBar(quickSettings = true)
                        is LauncherActionEvent.OpenInternetPanel -> {
                            if (!ConnectivityRepository.openInternetPanel(this@MainActivity)) {
                                snackbarHostState.showSnackbar("Unable to open Internet panel")
                            }
                        }
                        is LauncherActionEvent.OpenWifiControls -> {
                            if (!ConnectivityRepository.openWifiControls(this@MainActivity)) {
                                snackbarHostState.showSnackbar("Unable to open Wi-Fi settings")
                            }
                        }
                        is LauncherActionEvent.OpenMobileDataControls -> {
                            if (!ConnectivityRepository.openMobileDataControls(this@MainActivity)) {
                                snackbarHostState.showSnackbar("Unable to open Mobile Data settings")
                            }
                        }
                        is LauncherActionEvent.OpenVolumePanel -> {
                            if (!VolumeRepository.openSystemVolumePanel(this@MainActivity)) {
                                snackbarHostState.showSnackbar("Unable to open system Volume panel")
                            }
                        }
                        is LauncherActionEvent.OpenAppStoreHome -> {
                            if (!AppDownloadHelper.openAppStoreHome(this@MainActivity)) {
                                snackbarHostState.showSnackbar("No app store or browser found")
                            }
                        }
                        is LauncherActionEvent.OpenAppStoreSearch -> {
                            if (!AppDownloadHelper.openAppStoreSearch(this@MainActivity, event.query)) {
                                snackbarHostState.showSnackbar("No app store or browser found")
                            }
                        }
                        is LauncherActionEvent.OpenAppStoreDetails -> {
                            if (!AppDownloadHelper.openAppStoreDetails(this@MainActivity, event.packageName)) {
                                snackbarHostState.showSnackbar("No app store or browser found")
                            }
                        }
                        is LauncherActionEvent.OpenHomeSettings -> {
                            if (!DefaultLauncherHelper.openHomeSettings(this@MainActivity)) {
                                snackbarHostState.showSnackbar("Unable to open Default Home settings")
                            }
                        }
                        is LauncherActionEvent.LaunchOriginalLauncher -> {
                            if (!DefaultLauncherHelper.launchOriginalLauncherOnce(this@MainActivity)) {
                                snackbarHostState.showSnackbar("No other Android launcher found")
                            }
                        }
                        is LauncherActionEvent.OpenAndroidSettings -> {
                            if (!DefaultLauncherHelper.openAndroidSystemSettings(this@MainActivity)) {
                                snackbarHostState.showSnackbar("Unable to open Android Settings")
                            }
                        }
                        is LauncherActionEvent.SuggestHomeWifi -> {
                            val msg = ConnectivityRepository.suggestHomeWifiNetwork(
                                this@MainActivity,
                                event.ssid,
                                event.password
                            )
                            snackbarHostState.showSnackbar(msg)
                        }
                        is LauncherActionEvent.ToastMessage -> snackbarHostState.showSnackbar(event.message)
                        is LauncherActionEvent.RequestBindWidget -> {
                            pendingBindWidgetId = event.appWidgetId
                            pendingBindProvider = event.provider
                            val intent = viewModel.widgetHostManager.createBindIntent(
                                event.appWidgetId,
                                event.provider.providerInfo
                            )
                            bindWidgetLauncher.launch(intent)
                        }
                        is LauncherActionEvent.RequestConfigureWidget -> {
                            pendingConfigureWidgetId = event.appWidgetId
                            pendingConfigureProvider = event.provider
                            try {
                                viewModel.widgetHostManager.appWidgetHost
                                    .startAppWidgetConfigureActivityForResult(
                                        this@MainActivity,
                                        event.appWidgetId,
                                        0,
                                        REQUEST_CONFIGURE_WIDGET,
                                        null
                                    )
                            } catch (_: Exception) {
                                pendingConfigureWidgetId = -1
                                pendingConfigureProvider = null
                                viewModel.finalizeThirdPartyWidget(event.appWidgetId, event.provider)
                            }
                        }
                    }
                }
            }

            // Keep home active when back is pressed on Home screen; exit edit mode if active
            BackHandler(enabled = overlay == LauncherOverlay.None) {
                if (isEditMode) {
                    viewModel.setEditMode(false)
                }
            }

            ZstarLauncherTheme(
                themePreset = settings.themePreset,
                dockGlassOpacity = settings.dockGlassOpacity
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val overlayDim = if (overlay != LauncherOverlay.None) 0.35f else 0f

                    WallpaperBackground(
                        preset = settings.wallpaperPreset,
                        customWallpaperUri = settings.customWallpaperUri,
                        dimAlpha = settings.wallpaperDim,
                        blurEnabled = settings.wallpaperBlurEnabled,
                        extraOverlayDim = overlayDim
                    )

                    LauncherHomeScreen(
                        settings = settings,
                        homeItems = homeItems,
                        totalPages = totalPages,
                        metrics = metrics,
                        iconCache = viewModel.iconCache,
                        widgetHostManager = viewModel.widgetHostManager,
                        resolveWidgetProvider = viewModel::resolveWidgetProviderInfo,
                        isEditMode = isEditMode,
                        isDefaultLauncher = isDefaultLauncher,
                        isPlayStoreInstalled = isPlayStoreInstalled,
                        onSetEditMode = viewModel::setEditMode,
                        onRequestDefaultHome = requestDefaultHome,
                        onTriggerGesture = viewModel::triggerGesture,
                        onLaunchApp = viewModel::launchApp,
                        onOpenFolder = { folder -> viewModel.openOverlay(LauncherOverlay.FolderOpen(folder)) },
                        onShowAppMenu = { app, homeItem -> viewModel.showAppContextMenu(app, homeItem) },
                        onRemoveHomeItem = viewModel::removeHomeItem,
                        onResizeWidget = viewModel::resizeWidgetItem,
                        onAddPage = viewModel::addNewHomePage,
                        onOpenOverlay = viewModel::openOverlay,
                        onToggleZenMode = viewModel::toggleZenMode
                    )

                    // App Drawer Overlay
                    AnimatedVisibility(
                        visible = overlay is LauncherOverlay.AppDrawer,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { it / 2 } + fadeIn(),
                        exit = slideOutVertically { it / 2 } + fadeOut()
                    ) {
                        AppDrawerOverlay(
                            apps = drawerApps,
                            selectedCategory = drawerCategory,
                            query = drawerQuery,
                            isPlayStoreInstalled = isPlayStoreInstalled,
                            settings = settings,
                            iconCache = viewModel.iconCache,
                            onCategoryChange = viewModel::setDrawerCategory,
                            onQueryChange = viewModel::setDrawerQuery,
                            onLaunchApp = viewModel::launchApp,
                            onLongPressApp = { app -> viewModel.showAppContextMenu(app, null) },
                            onOpenGetApps = { viewModel.openOverlay(LauncherOverlay.GetApps) },
                            onSearchAppStore = viewModel::searchAppStore,
                            onOpenSettings = { viewModel.openOverlay(LauncherOverlay.Settings()) },
                            onClose = viewModel::closeOverlay
                        )
                    }

                    // Universal Search Overlay (Apps + Files + Math + Web + Volume + Internet)
                    AnimatedVisibility(
                        visible = overlay is LauncherOverlay.UniversalSearch,
                        enter = fadeIn() + scaleIn(initialScale = 0.96f),
                        exit = fadeOut() + scaleOut(targetScale = 0.96f)
                    ) {
                        UniversalSearchOverlay(
                            searchState = searchState,
                            networkStatus = networkStatus,
                            volumeState = volumeState,
                            indexedFoldersCount = indexedFolders.size,
                            settings = settings,
                            iconCache = viewModel.iconCache,
                            onQueryChange = viewModel::setSearchQuery,
                            onLaunchApp = viewModel::launchApp,
                            onLongPressApp = { app -> viewModel.showAppContextMenu(app, null) },
                            onOpenFile = viewModel::openFile,
                            onPickFolderToIndex = { folderPickerLauncher.launch(null) },
                            onPickSingleFileToIndex = { filePickerLauncher.launch(arrayOf("*/*")) },
                            onOpenInternetPanel = viewModel::openInternetPanel,
                            onOpenWifiControls = viewModel::openWifiControls,
                            onOpenMobileDataControls = viewModel::openMobileDataControls,
                            onConnectHomeWifi = viewModel::connectHomeWifi,
                            onMediaVolumeChange = viewModel::setMediaVolume,
                            onRingVolumeChange = viewModel::setRingVolume,
                            onNotificationVolumeChange = viewModel::setNotificationVolume,
                            onAlarmVolumeChange = viewModel::setAlarmVolume,
                            onToggleMediaMute = viewModel::toggleMediaMute,
                            onOpenVolumePanel = viewModel::openSystemVolumePanel,
                            onSearchAppStore = viewModel::searchAppStore,
                            onClose = viewModel::closeOverlay
                        )
                    }

                    // Get Apps Overlay (Download Apps via Play Store / Market)
                    AnimatedVisibility(
                        visible = overlay is LauncherOverlay.GetApps,
                        enter = slideInVertically { it / 2 } + fadeIn(),
                        exit = slideOutVertically { it / 2 } + fadeOut()
                    ) {
                        GetAppsOverlay(
                            installedApps = allApps,
                            isPlayStoreInstalled = isPlayStoreInstalled,
                            onOpenStoreHome = viewModel::openAppStoreHome,
                            onSearchStore = viewModel::searchAppStore,
                            onDownloadPackage = viewModel::downloadAppFromStore,
                            onLaunchInstalledApp = viewModel::launchApp,
                            onPinStoreToHome = { viewModel.pinStoreToHome() },
                            onClose = viewModel::closeOverlay
                        )
                    }

                    // Widget Studio Overlay
                    AnimatedVisibility(
                        visible = overlay is LauncherOverlay.WidgetPicker,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        WidgetPickerSheet(
                            providers = widgetProviders,
                            onAddZstarWidget = { type -> viewModel.addZstarSmartWidget(type) },
                            onSelectThirdPartyWidget = { provider ->
                                viewModel.requestAddThirdPartyWidget(provider)
                            },
                            onClose = viewModel::closeOverlay
                        )
                    }

                    // Settings & Customization Studio Overlay
                    val settingsOverlay = overlay as? LauncherOverlay.Settings
                    AnimatedVisibility(
                        visible = settingsOverlay != null,
                        enter = slideInVertically { it / 3 } + fadeIn(),
                        exit = slideOutVertically { it / 3 } + fadeOut()
                    ) {
                        if (settingsOverlay != null) {
                            LauncherSettingsScreen(
                                initialTab = settingsOverlay.initialTab,
                                isDefaultLauncher = isDefaultLauncher,
                                originalLauncherName = originalLauncherName,
                                settings = settings,
                                allApps = allApps,
                                externalIconPacks = externalIconPacks,
                                indexedFolders = indexedFolders,
                                metrics = metrics,
                                networkStatus = networkStatus,
                                volumeState = volumeState,
                                iconCache = viewModel.iconCache,
                                onRequestDefaultHome = requestDefaultHome,
                                onOpenHomeSettings = viewModel::openSystemDefaultHomeSettings,
                                onLaunchOriginalLauncher = viewModel::launchOriginalLauncherOnce,
                                onOpenAndroidSettings = viewModel::openAndroidSystemSettings,
                                onOpenInternetPanel = viewModel::openInternetPanel,
                                onOpenWifiControls = viewModel::openWifiControls,
                                onOpenMobileDataControls = viewModel::openMobileDataControls,
                                onConnectHomeWifi = viewModel::connectHomeWifi,
                                onMediaVolumeChange = viewModel::setMediaVolume,
                                onRingVolumeChange = viewModel::setRingVolume,
                                onNotificationVolumeChange = viewModel::setNotificationVolume,
                                onAlarmVolumeChange = viewModel::setAlarmVolume,
                                onToggleMediaMute = viewModel::toggleMediaMute,
                                onOpenVolumePanel = viewModel::openSystemVolumePanel,
                                onSelectTheme = viewModel::setThemePreset,
                                onSelectWallpaper = viewModel::setWallpaperPreset,
                                onWallpaperDimChange = viewModel::setWallpaperDim,
                                onWallpaperBlurChange = viewModel::setWallpaperBlur,
                                onDockGlassChange = viewModel::setDockGlassOpacity,
                                onSelectIconPack = viewModel::setIconPackStyle,
                                onSelectIconShape = viewModel::setIconShape,
                                onGridColumnsChange = viewModel::setGridColumns,
                                onDockSlotsChange = viewModel::setDockSlots,
                                onShowLabelsChange = viewModel::setShowAppLabels,
                                onIconScaleChange = viewModel::setIconScale,
                                onGestureBindingChange = viewModel::setGestureBinding,
                                onResetGestures = viewModel::resetGestures,
                                onTestGesture = viewModel::triggerGesture,
                                onAddSafFolder = { folderPickerLauncher.launch(null) },
                                onRescanFolder = viewModel::rescanFolder,
                                onRemoveFolder = viewModel::removeFolder,
                                onReduceMotionChange = viewModel::setReduceMotion,
                                onPreloadIconsChange = viewModel::setPreloadIcons,
                                onShowHudChange = viewModel::setShowPerformanceHud,
                                onShowBootAnimationChange = viewModel::setShowBootAnimation,
                                onReplayBootAnimation = {
                                    viewModel.closeOverlay()
                                    forceReplayBoot = true
                                    showStartingScreen = true
                                },
                                onClearIconCache = viewModel::clearIconCache,
                                onClose = viewModel::closeOverlay
                            )
                        }
                    }

                    // Folder Modal Overlay
                    val folderOverlay = overlay as? LauncherOverlay.FolderOpen
                    val liveFolder = folderOverlay?.let { target ->
                        homeItems.firstOrNull { it.id == target.folderItem.id } ?: target.folderItem
                    }
                    AnimatedVisibility(
                        visible = liveFolder != null,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f)
                    ) {
                        if (liveFolder != null) {
                            FolderModalDialog(
                                folder = liveFolder,
                                settings = settings,
                                iconCache = viewModel.iconCache,
                                onLaunchApp = viewModel::launchApp,
                                onRemoveAppFromFolder = { key ->
                                    viewModel.removeAppFromFolder(liveFolder, key)
                                },
                                onRenameFolder = { title ->
                                    viewModel.renameFolder(liveFolder, title)
                                },
                                onClose = viewModel::closeOverlay
                            )
                        }
                    }

                    // Long-Press App Context Menu Sheet
                    menuApp?.let { app ->
                        AppContextMenuSheet(
                            app = app,
                            homeItem = menuHomeItem,
                            iconCache = viewModel.iconCache,
                            iconPackStyle = settings.iconPackStyle,
                            externalPackPkg = settings.externalIconPackPackage,
                            iconShape = settings.iconShape,
                            onDismiss = viewModel::dismissAppContextMenu,
                            onLaunch = { viewModel.launchApp(app) },
                            onPinToHome = { viewModel.pinAppToHome(app) },
                            onPinToDock = { viewModel.pinAppToDock(app) },
                            onAddToFolder = { viewModel.addAppToFolderOrCreate(app) },
                            onRemoveFromHome = menuHomeItem?.let { item ->
                                { viewModel.removeHomeItem(item) }
                            },
                            onRename = { newLabel -> viewModel.renameApp(app, newLabel) },
                            onAppInfo = { viewModel.openAppDetails(app) },
                            onUninstall = { viewModel.requestUninstallApp(app) }
                        )
                    }

                    val isBootOverlayVisible = showStartingScreen && (settings.showBootAnimation || forceReplayBoot)

                    // First-launch prompt to replace system launcher (shown after starting screen finishes)
                    if (!isBootOverlayVisible && !isDefaultLauncher && showDefaultOnboarding && overlay == LauncherOverlay.None) {
                        ReplaceSystemLauncherDialog(
                            currentLauncherName = currentDefaultName,
                            onSetDefaultHome = requestDefaultHome,
                            onOpenSystemHomeSettings = {
                                viewModel.dismissDefaultHomeOnboarding()
                                DefaultLauncherHelper.openHomeSettings(this@MainActivity)
                            },
                            onDismiss = viewModel::dismissDefaultHomeOnboarding
                        )
                    }

                    // MIUI-style starting / boot animation overlay
                    if (isBootOverlayVisible) {
                        ZstarBootAnimationOverlay(
                            reduceMotion = settings.reduceMotion && !forceReplayBoot,
                            onFinished = {
                                showStartingScreen = false
                                forceReplayBoot = false
                            }
                        )
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun expandStatusBar(quickSettings: Boolean) {
        try {
            val statusBarService = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val methodName = if (quickSettings) "expandSettingsPanel" else "expandNotificationsPanel"
            val method = statusBarManager.getMethod(methodName)
            method.invoke(statusBarService)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Notification expansion restricted by system policy",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
private fun ReplaceSystemLauncherDialog(
    currentLauncherName: String?,
    onSetDefaultHome: () -> Unit,
    onOpenSystemHomeSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = "Set Zstar as Default Home",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Replace System Launcher",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Android currently routes the Home button to ${currentLauncherName ?: "the system launcher"}. Select Zstar Launcher as your Default Home app so it replaces your phone's home screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSetDefaultHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_set_default_home_button")
                ) {
                    Text("Set Zstar as Default Home")
                }
                OutlinedButton(
                    onClick = onOpenSystemHomeSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_open_home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "System Home Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Default Home Settings")
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Later")
                    }
                }
            }
        }
    )
}
