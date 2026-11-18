package com.example.viewmodel

import android.app.Application
import android.appwidget.AppWidgetProviderInfo
import android.media.AudioManager
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppUsageEntity
import com.example.data.local.HomeItemEntity
import com.example.data.local.LauncherDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.system.AppIconCache
import com.example.data.system.ConnectivityRepository
import com.example.data.system.DefaultLauncherHelper
import com.example.data.system.FileSearchRepository
import com.example.data.system.InstalledAppsRepository
import com.example.data.system.VolumeRepository
import com.example.data.system.WidgetHostManager
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.ExternalIconPack
import com.example.model.GestureAction
import com.example.model.GestureBinding
import com.example.model.GestureTrigger
import com.example.model.HomeItemType
import com.example.model.HomeItemUi
import com.example.model.IconPackStyle
import com.example.model.IconShapeOption
import com.example.model.IndexedFileItem
import com.example.model.IndexedFolderSummary
import com.example.model.LauncherSettingsState
import com.example.model.NetworkStatusUi
import com.example.model.PerformanceMetrics
import com.example.model.ThemePreset
import com.example.model.VolumeStateUi
import com.example.model.WallpaperPreset
import com.example.model.WidgetProviderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LauncherOverlay {
    data object None : LauncherOverlay
    data object AppDrawer : LauncherOverlay
    data object UniversalSearch : LauncherOverlay
    data object WidgetPicker : LauncherOverlay
    data object GetApps : LauncherOverlay
    data class Settings(val initialTab: Int = 0) : LauncherOverlay
    data class FolderOpen(val folderItem: HomeItemUi) : LauncherOverlay
}

sealed interface LauncherActionEvent {
    data object ExpandNotifications : LauncherActionEvent
    data object ExpandQuickSettings : LauncherActionEvent
    data object OpenInternetPanel : LauncherActionEvent
    data object OpenWifiControls : LauncherActionEvent
    data object OpenMobileDataControls : LauncherActionEvent
    data object OpenVolumePanel : LauncherActionEvent
    data object OpenAppStoreHome : LauncherActionEvent
    data class OpenAppStoreSearch(val query: String) : LauncherActionEvent
    data class OpenAppStoreDetails(val packageName: String) : LauncherActionEvent
    data object OpenHomeSettings : LauncherActionEvent
    data object LaunchOriginalLauncher : LauncherActionEvent
    data object OpenAndroidSettings : LauncherActionEvent
    data class SuggestHomeWifi(val ssid: String, val password: String) : LauncherActionEvent
    data class ToastMessage(val message: String) : LauncherActionEvent
    data class RequestBindWidget(val appWidgetId: Int, val provider: WidgetProviderItem) : LauncherActionEvent
    data class RequestConfigureWidget(val appWidgetId: Int, val provider: WidgetProviderItem) : LauncherActionEvent
}

@Immutable
data class UniversalSearchState(
    val query: String = "",
    val mathResult: String? = null,
    val matchingApps: List<AppInfo> = emptyList(),
    val matchingFiles: List<IndexedFileItem> = emptyList()
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LauncherDatabase.getInstance(application)
    private val homeItemDao = database.homeItemDao()
    private val appUsageDao = database.appUsageDao()
    private val fileIndexDao = database.fileIndexDao()

    val settingsStore = SettingsDataStore(application)
    val iconCache = AppIconCache(application)
    private val appsRepo = InstalledAppsRepository(application)
    val widgetHostManager = WidgetHostManager(application)
    val fileRepo = FileSearchRepository(application, fileIndexDao)
    private val connectivityRepo = ConnectivityRepository(application)
    private val volumeRepo = VolumeRepository(application)

    val networkStatus: StateFlow<NetworkStatusUi> = connectivityRepo.observeNetworkStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetworkStatusUi()
        )

    private val _volumeState = MutableStateFlow(volumeRepo.readCurrentVolume())
    val volumeState: StateFlow<VolumeStateUi> = _volumeState.asStateFlow()

    private val _rawApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _lastAppScanMs = MutableStateFlow(0L)
    private val _externalIconPacks = MutableStateFlow<List<ExternalIconPack>>(emptyList())
    val externalIconPacks: StateFlow<List<ExternalIconPack>> = _externalIconPacks.asStateFlow()

    private val _widgetProviders = MutableStateFlow<List<WidgetProviderItem>>(emptyList())
    val widgetProviders: StateFlow<List<WidgetProviderItem>> = _widgetProviders.asStateFlow()

    private val _overlay = MutableStateFlow<LauncherOverlay>(LauncherOverlay.None)
    val overlay: StateFlow<LauncherOverlay> = _overlay.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _selectedAppForMenu = MutableStateFlow<AppInfo?>(null)
    val selectedAppForMenu: StateFlow<AppInfo?> = _selectedAppForMenu.asStateFlow()

    private val _selectedHomeItemForMenu = MutableStateFlow<HomeItemUi?>(null)
    val selectedHomeItemForMenu: StateFlow<HomeItemUi?> = _selectedHomeItemForMenu.asStateFlow()

    private val _drawerCategory = MutableStateFlow(AppCategory.ALL)
    val drawerCategory: StateFlow<AppCategory> = _drawerCategory.asStateFlow()

    private val _drawerQuery = MutableStateFlow("")
    val drawerQuery: StateFlow<String> = _drawerQuery.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _extraPagesCount = MutableStateFlow(0)

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _currentDefaultLauncherName = MutableStateFlow<String?>(null)
    val currentDefaultLauncherName: StateFlow<String?> = _currentDefaultLauncherName.asStateFlow()

    private val _originalLauncherName = MutableStateFlow<String?>(null)
    val originalLauncherName: StateFlow<String?> = _originalLauncherName.asStateFlow()

    private val _showDefaultHomeOnboarding = MutableStateFlow(false)
    val showDefaultHomeOnboarding: StateFlow<Boolean> = _showDefaultHomeOnboarding.asStateFlow()

    private var hasPromptedDefaultHomeThisSession = false

    private val _events = MutableSharedFlow<LauncherActionEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<LauncherActionEvent> = _events.asSharedFlow()

    val settings: StateFlow<LauncherSettingsState> = settingsStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LauncherSettingsState()
        )

    val indexedFolders: StateFlow<List<IndexedFolderSummary>> = fileRepo.indexedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApps: StateFlow<List<AppInfo>> = combine(
        _rawApps,
        appUsageDao.observeAllUsage()
    ) { raw, usageList ->
        val usageMap = usageList.associateBy { it.componentKey }
        raw.filterNot { usageMap[it.componentKey]?.isHidden == true }
            .map { app ->
                val usage = usageMap[app.componentKey]
                app.copy(
                    label = usage?.customLabel?.takeIf { it.isNotBlank() } ?: app.label,
                    launchCount = usage?.launchCount ?: 0
                )
            }
            .sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val drawerFilteredApps: StateFlow<List<AppInfo>> = combine(
        allApps,
        _drawerCategory,
        _drawerQuery
    ) { apps, category, query ->
        val q = query.trim().lowercase()
        val byCat = when (category) {
            AppCategory.ALL -> apps
            AppCategory.FREQUENT -> apps.sortedByDescending { it.launchCount }.take(16)
            else -> apps.filter { it.category == category }
        }
        if (q.isEmpty()) byCat else byCat.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeItems: StateFlow<List<HomeItemUi>> = combine(
        homeItemDao.observeHomeItems(),
        allApps
    ) { entities, apps ->
        val appMap = apps.associateBy { it.componentKey }
        entities.mapNotNull { entity ->
            val type = runCatching { HomeItemType.valueOf(entity.type) }.getOrNull()
                ?: return@mapNotNull null
            val resolvedApp = entity.componentKey?.let { appMap[it] }
            val folderApps = if (type == HomeItemType.FOLDER && !entity.folderComponentKeys.isNullOrBlank()) {
                entity.folderComponentKeys.split("|").mapNotNull { appMap[it] }
            } else {
                emptyList()
            }
            HomeItemUi(
                id = entity.id,
                type = type,
                pageIndex = entity.pageIndex,
                orderIndex = entity.orderIndex,
                componentKey = entity.componentKey,
                appInfo = resolvedApp,
                customLabel = entity.customLabel,
                appWidgetId = entity.appWidgetId,
                widgetProviderComponent = entity.widgetProviderComponent,
                spanCols = entity.spanCols,
                spanRows = entity.spanRows,
                folderTitle = entity.folderTitle,
                folderApps = folderApps
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalPages: StateFlow<Int> = combine(homeItems, _extraPagesCount) { items, extra ->
        val maxPage = items.filter { it.type != HomeItemType.DOCK_APP }
            .maxOfOrNull { it.pageIndex } ?: 0
        (maxOf(maxPage + 1, 1) + extra).coerceAtMost(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val searchState: StateFlow<UniversalSearchState> = combine(
        _searchQuery,
        allApps,
        fileRepo.indexedFiles
    ) { rawQuery, apps, files ->
        val q = rawQuery.trim()
        val math = FileSearchRepository.evaluateQuickMath(q)
        val matchedApps = if (q.isEmpty()) {
            apps.sortedByDescending { it.launchCount }.take(8)
        } else {
            val lower = q.lowercase()
            apps.filter {
                it.label.lowercase().contains(lower) || it.packageName.lowercase().contains(lower)
            }.sortedByDescending {
                (if (it.label.lowercase().startsWith(lower)) 100 else 0) + it.launchCount
            }.take(16)
        }
        val matchedFiles = if (q.isEmpty()) {
            files.take(12)
        } else {
            val lower = q.lowercase()
            files.filter {
                it.name.lowercase().contains(lower) ||
                    it.folderName.lowercase().contains(lower) ||
                    it.mimeType.lowercase().contains(lower)
            }.take(24)
        }
        UniversalSearchState(
            query = rawQuery,
            mathResult = math,
            matchingApps = matchedApps,
            matchingFiles = matchedFiles
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UniversalSearchState())

    val performanceMetrics: StateFlow<PerformanceMetrics> = combine(
        allApps,
        _lastAppScanMs,
        fileRepo.indexedFileCount
    ) { apps, scanMs, filesCount ->
        PerformanceMetrics(
            appCount = apps.size,
            iconCacheEntries = iconCache.getEntryCount(),
            iconCacheHits = iconCache.getHits(),
            iconCacheMisses = iconCache.getMisses(),
            lastAppScanMs = scanMs,
            indexedFilesCount = filesCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerformanceMetrics())

    init {
        refreshDefaultLauncherStatus()
        refreshInstalledApps(seedIfNeeded = true)
        refreshExternalIconPacks()
        refreshWidgetProviders()
        observePackageBroadcasts()
        observeVolumeChanges()
    }

    private fun observeVolumeChanges() {
        viewModelScope.launch {
            volumeRepo.observeVolumeState().collect { state ->
                _volumeState.value = state
            }
        }
    }

    fun refreshDefaultLauncherStatus() {
        val ctx = getApplication<Application>()
        val isDefault = DefaultLauncherHelper.isZstarDefaultHome(ctx)
        _isDefaultLauncher.value = isDefault
        _currentDefaultLauncherName.value = if (isDefault) null else DefaultLauncherHelper.getCurrentDefaultLauncherName(ctx)
        _originalLauncherName.value = DefaultLauncherHelper.getOriginalLauncherName(ctx)
        if (isDefault) {
            _showDefaultHomeOnboarding.value = false
        } else if (!hasPromptedDefaultHomeThisSession) {
            hasPromptedDefaultHomeThisSession = true
            _showDefaultHomeOnboarding.value = true
        }
    }

    fun dismissDefaultHomeOnboarding() {
        _showDefaultHomeOnboarding.value = false
    }

    @OptIn(FlowPreview::class)
    private fun observePackageBroadcasts() {
        viewModelScope.launch {
            appsRepo.observePackageChanges()
                .debounce(450)
                .collect {
                    refreshInstalledApps(seedIfNeeded = false)
                    refreshExternalIconPacks()
                    refreshWidgetProviders()
                }
        }
    }

    fun refreshInstalledApps(seedIfNeeded: Boolean = false) {
        viewModelScope.launch {
            val result = appsRepo.loadInstalledApps()
            _rawApps.value = result.apps
            _lastAppScanMs.value = result.scanDurationMs

            if (seedIfNeeded) {
                seedInitialHomeIfEmpty(result.apps)
            }

            if (settings.value.preloadIcons) {
                preloadTopIcons(result.apps)
            }
        }
    }

    private suspend fun preloadTopIcons(apps: List<AppInfo>) = withContext(Dispatchers.IO) {
        val extPkg = if (settings.value.iconPackStyle == IconPackStyle.EXTERNAL) {
            settings.value.externalIconPackPackage
        } else {
            null
        }
        apps.take(40).forEach { app ->
            iconCache.loadIcon(app.packageName, app.activityName, extPkg)
        }
    }

    private suspend fun seedInitialHomeIfEmpty(apps: List<AppInfo>) {
        if (homeItemDao.countItems() > 0) return

        val starterItems = mutableListOf<HomeItemEntity>()
        // Built-in Zstar Smart Clock & System Widget on Page 0
        starterItems.add(
            HomeItemEntity(
                type = HomeItemType.ZSTAR_CLOCK_WIDGET.name,
                pageIndex = 0,
                orderIndex = 0,
                spanCols = 4,
                spanRows = 2
            )
        )

        // Pick useful dock apps (Dialer/Phone, Messages, Browser, Camera, Settings)
        val priorityKeywords = listOf("dialer", "phone", "message", "chrome", "browser", "camera", "photos", "settings", "gallery", "clock")
        val dockApps = mutableListOf<AppInfo>()
        for (keyword in priorityKeywords) {
            if (dockApps.size >= 4) break
            val match = apps.firstOrNull { app ->
                (app.label.lowercase().contains(keyword) || app.packageName.lowercase().contains(keyword)) &&
                    dockApps.none { it.componentKey == app.componentKey }
            }
            if (match != null) dockApps.add(match)
        }
        for (app in apps) {
            if (dockApps.size >= 4) break
            if (dockApps.none { it.componentKey == app.componentKey }) dockApps.add(app)
        }

        dockApps.forEachIndexed { idx, app ->
            starterItems.add(
                HomeItemEntity(
                    type = HomeItemType.DOCK_APP.name,
                    pageIndex = -1,
                    orderIndex = idx,
                    componentKey = app.componentKey
                )
            )
        }

        // Fill 8 starter apps on Page 0 below the Clock widget
        val homeApps = apps.filter { a -> dockApps.none { it.componentKey == a.componentKey } }.take(8)
        homeApps.forEachIndexed { idx, app ->
            starterItems.add(
                HomeItemEntity(
                    type = HomeItemType.APP.name,
                    pageIndex = 0,
                    orderIndex = idx + 1,
                    componentKey = app.componentKey
                )
            )
        }

        homeItemDao.insertAll(starterItems)
    }

    fun refreshExternalIconPacks() {
        viewModelScope.launch {
            _externalIconPacks.value = appsRepo.discoverExternalIconPacks()
        }
    }

    fun refreshWidgetProviders() {
        viewModelScope.launch {
            _widgetProviders.value = widgetHostManager.getInstalledWidgetProviders()
        }
    }

    fun openOverlay(target: LauncherOverlay) {
        _isEditMode.value = false
        _overlay.value = target
    }

    fun closeOverlay() {
        _overlay.value = LauncherOverlay.None
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun showAppContextMenu(app: AppInfo, homeItem: HomeItemUi? = null) {
        _selectedAppForMenu.value = app
        _selectedHomeItemForMenu.value = homeItem
    }

    fun dismissAppContextMenu() {
        _selectedAppForMenu.value = null
        _selectedHomeItemForMenu.value = null
    }

    fun setDrawerCategory(category: AppCategory) {
        _drawerCategory.value = category
    }

    fun setDrawerQuery(query: String) {
        _drawerQuery.value = query
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(app: AppInfo) {
        val launched = appsRepo.launchApp(app)
        if (launched) {
            if (_overlay.value is LauncherOverlay.AppDrawer ||
                _overlay.value is LauncherOverlay.UniversalSearch ||
                _overlay.value is LauncherOverlay.FolderOpen
            ) {
                _overlay.value = LauncherOverlay.None
            }
            viewModelScope.launch {
                val existing = appUsageDao.getByComponentKey(app.componentKey)
                appUsageDao.upsert(
                    AppUsageEntity(
                        componentKey = app.componentKey,
                        launchCount = (existing?.launchCount ?: 0) + 1,
                        lastLaunchedAt = System.currentTimeMillis(),
                        customLabel = existing?.customLabel,
                        isHidden = existing?.isHidden ?: false
                    )
                )
            }
        } else {
            _events.tryEmit(LauncherActionEvent.ToastMessage("Unable to launch ${app.label}"))
        }
    }

    fun openAppDetails(app: AppInfo) {
        dismissAppContextMenu()
        appsRepo.openAppDetails(app.packageName)
    }

    fun requestUninstallApp(app: AppInfo) {
        dismissAppContextMenu()
        appsRepo.requestUninstall(app.packageName)
    }

    fun renameApp(app: AppInfo, newLabel: String) {
        viewModelScope.launch {
            val existing = appUsageDao.getByComponentKey(app.componentKey)
            appUsageDao.upsert(
                AppUsageEntity(
                    componentKey = app.componentKey,
                    launchCount = existing?.launchCount ?: 0,
                    lastLaunchedAt = existing?.lastLaunchedAt ?: 0L,
                    customLabel = newLabel.trim().takeIf { it.isNotEmpty() },
                    isHidden = existing?.isHidden ?: false
                )
            )
            dismissAppContextMenu()
        }
    }

    fun pinAppToHome(app: AppInfo, pageIndex: Int = 0) {
        viewModelScope.launch {
            val currentOnPage = homeItems.value.filter { it.pageIndex == pageIndex && it.type != HomeItemType.DOCK_APP }
            val nextOrder = (currentOnPage.maxOfOrNull { it.orderIndex } ?: 0) + 1
            homeItemDao.insertItem(
                HomeItemEntity(
                    type = HomeItemType.APP.name,
                    pageIndex = pageIndex,
                    orderIndex = nextOrder,
                    componentKey = app.componentKey
                )
            )
            dismissAppContextMenu()
            _events.tryEmit(LauncherActionEvent.ToastMessage("${app.label} pinned to Home"))
        }
    }

    fun pinAppToDock(app: AppInfo) {
        viewModelScope.launch {
            val maxSlots = settings.value.dockSlots
            val currentDock = homeItems.value.filter { it.type == HomeItemType.DOCK_APP }.sortedBy { it.orderIndex }
            if (currentDock.any { it.componentKey == app.componentKey }) {
                _events.tryEmit(LauncherActionEvent.ToastMessage("${app.label} is already in the Dock"))
                dismissAppContextMenu()
                return@launch
            }
            if (currentDock.size >= maxSlots) {
                currentDock.lastOrNull()?.let { homeItemDao.deleteById(it.id) }
            }
            homeItemDao.insertItem(
                HomeItemEntity(
                    type = HomeItemType.DOCK_APP.name,
                    pageIndex = -1,
                    orderIndex = currentDock.size,
                    componentKey = app.componentKey
                )
            )
            dismissAppContextMenu()
            _events.tryEmit(LauncherActionEvent.ToastMessage("${app.label} added to Dock"))
        }
    }

    fun addAppToFolderOrCreate(app: AppInfo, pageIndex: Int = 0) {
        viewModelScope.launch {
            val existingFolder = homeItems.value.firstOrNull { it.type == HomeItemType.FOLDER }
            if (existingFolder != null) {
                val keys = existingFolder.folderApps.map { it.componentKey }.toMutableList()
                if (!keys.contains(app.componentKey)) {
                    keys.add(app.componentKey)
                    homeItemDao.insertItem(
                        HomeItemEntity(
                            id = existingFolder.id,
                            type = HomeItemType.FOLDER.name,
                            pageIndex = existingFolder.pageIndex,
                            orderIndex = existingFolder.orderIndex,
                            folderTitle = existingFolder.folderTitle ?: "Workspace",
                            folderComponentKeys = keys.joinToString("|")
                        )
                    )
                }
                _events.tryEmit(LauncherActionEvent.ToastMessage("Added ${app.label} to ${existingFolder.folderTitle ?: "Workspace"}"))
            } else {
                val currentOnPage = homeItems.value.filter { it.pageIndex == pageIndex && it.type != HomeItemType.DOCK_APP }
                val nextOrder = (currentOnPage.maxOfOrNull { it.orderIndex } ?: 0) + 1
                homeItemDao.insertItem(
                    HomeItemEntity(
                        type = HomeItemType.FOLDER.name,
                        pageIndex = pageIndex,
                        orderIndex = nextOrder,
                        folderTitle = "Workspace",
                        folderComponentKeys = app.componentKey
                    )
                )
                _events.tryEmit(LauncherActionEvent.ToastMessage("Created 'Workspace' folder with ${app.label}"))
            }
            dismissAppContextMenu()
        }
    }

    fun renameFolder(folder: HomeItemUi, newTitle: String) {
        viewModelScope.launch {
            homeItemDao.insertItem(
                HomeItemEntity(
                    id = folder.id,
                    type = HomeItemType.FOLDER.name,
                    pageIndex = folder.pageIndex,
                    orderIndex = folder.orderIndex,
                    folderTitle = newTitle.trim().ifEmpty { "Folder" },
                    folderComponentKeys = folder.folderApps.joinToString("|") { it.componentKey }
                )
            )
        }
    }

    fun removeAppFromFolder(folder: HomeItemUi, componentKey: String) {
        viewModelScope.launch {
            val remaining = folder.folderApps.filterNot { it.componentKey == componentKey }
            if (remaining.isEmpty()) {
                homeItemDao.deleteById(folder.id)
                closeOverlay()
            } else {
                homeItemDao.insertItem(
                    HomeItemEntity(
                        id = folder.id,
                        type = HomeItemType.FOLDER.name,
                        pageIndex = folder.pageIndex,
                        orderIndex = folder.orderIndex,
                        folderTitle = folder.folderTitle,
                        folderComponentKeys = remaining.joinToString("|") { it.componentKey }
                    )
                )
            }
        }
    }

    fun removeHomeItem(item: HomeItemUi) {
        viewModelScope.launch {
            if (item.type == HomeItemType.THIRD_PARTY_WIDGET && item.appWidgetId > 0) {
                widgetHostManager.deleteAppWidgetId(item.appWidgetId)
            }
            homeItemDao.deleteById(item.id)
            dismissAppContextMenu()
        }
    }

    fun addNewHomePage(): Int {
        val next = totalPages.value
        _extraPagesCount.value += 1
        return next
    }

    fun addZstarSmartWidget(type: HomeItemType, pageIndex: Int = 0) {
        viewModelScope.launch {
            val currentOnPage = homeItems.value.filter { it.pageIndex == pageIndex && it.type != HomeItemType.DOCK_APP }
            val nextOrder = (currentOnPage.maxOfOrNull { it.orderIndex } ?: -1) + 1
            homeItemDao.insertItem(
                HomeItemEntity(
                    type = type.name,
                    pageIndex = pageIndex,
                    orderIndex = nextOrder,
                    spanCols = 4,
                    spanRows = if (type == HomeItemType.ZSTAR_CLOCK_WIDGET) 2 else 1
                )
            )
            closeOverlay()
            _events.tryEmit(LauncherActionEvent.ToastMessage("Added smart widget to Home"))
        }
    }

    fun requestAddThirdPartyWidget(provider: WidgetProviderItem, pageIndex: Int = 0) {
        val appWidgetId = widgetHostManager.allocateAppWidgetId()
        val bound = widgetHostManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.providerInfo)
        if (bound) {
            onWidgetBound(appWidgetId, provider, pageIndex)
        } else {
            _events.tryEmit(LauncherActionEvent.RequestBindWidget(appWidgetId, provider))
        }
    }

    fun onWidgetBound(appWidgetId: Int, provider: WidgetProviderItem, pageIndex: Int = 0) {
        if (provider.providerInfo.configure != null) {
            _events.tryEmit(LauncherActionEvent.RequestConfigureWidget(appWidgetId, provider))
        } else {
            finalizeThirdPartyWidget(appWidgetId, provider, pageIndex)
        }
    }

    fun finalizeThirdPartyWidget(appWidgetId: Int, provider: WidgetProviderItem, pageIndex: Int = 0) {
        viewModelScope.launch {
            val currentOnPage = homeItems.value.filter { it.pageIndex == pageIndex && it.type != HomeItemType.DOCK_APP }
            val nextOrder = (currentOnPage.maxOfOrNull { it.orderIndex } ?: -1) + 1
            homeItemDao.insertItem(
                HomeItemEntity(
                    type = HomeItemType.THIRD_PARTY_WIDGET.name,
                    pageIndex = pageIndex,
                    orderIndex = nextOrder,
                    appWidgetId = appWidgetId,
                    widgetProviderComponent = provider.componentName,
                    spanCols = provider.spanCols,
                    spanRows = provider.spanRows
                )
            )
            closeOverlay()
            _events.tryEmit(LauncherActionEvent.ToastMessage("${provider.label} widget added"))
        }
    }

    fun cancelWidgetAllocation(appWidgetId: Int) {
        widgetHostManager.deleteAppWidgetId(appWidgetId)
    }

    fun resizeWidgetItem(item: HomeItemUi, newSpanCols: Int, newSpanRows: Int) {
        viewModelScope.launch {
            homeItemDao.insertItem(
                HomeItemEntity(
                    id = item.id,
                    type = item.type.name,
                    pageIndex = item.pageIndex,
                    orderIndex = item.orderIndex,
                    componentKey = item.componentKey,
                    customLabel = item.customLabel,
                    appWidgetId = item.appWidgetId,
                    widgetProviderComponent = item.widgetProviderComponent,
                    spanCols = newSpanCols.coerceIn(1, 4),
                    spanRows = newSpanRows.coerceIn(1, 4),
                    folderTitle = item.folderTitle,
                    folderComponentKeys = item.folderApps.joinToString("|") { it.componentKey }
                )
            )
        }
    }

    fun resolveWidgetProviderInfo(componentName: String?): AppWidgetProviderInfo? {
        if (componentName.isNullOrBlank()) return null
        return _widgetProviders.value.firstOrNull { it.componentName == componentName }?.providerInfo
    }

    // Gesture execution
    fun triggerGesture(trigger: GestureTrigger) {
        val binding = settings.value.gestureMappings[trigger] ?: return
        when (binding.action) {
            GestureAction.OPEN_DRAWER -> openOverlay(LauncherOverlay.AppDrawer)
            GestureAction.OPEN_SEARCH -> openOverlay(LauncherOverlay.UniversalSearch)
            GestureAction.EXPAND_NOTIFICATIONS -> _events.tryEmit(LauncherActionEvent.ExpandNotifications)
            GestureAction.EXPAND_QUICK_SETTINGS -> _events.tryEmit(LauncherActionEvent.ExpandQuickSettings)
            GestureAction.OPEN_WIDGET_PICKER -> openOverlay(LauncherOverlay.WidgetPicker)
            GestureAction.OPEN_SETTINGS -> openOverlay(LauncherOverlay.Settings())
            GestureAction.TOGGLE_ZEN_MODE -> viewModelScope.launch {
                settingsStore.setZenMode(!settings.value.zenMode)
            }
            GestureAction.OPEN_INTERNET_CONTROLS -> _events.tryEmit(LauncherActionEvent.OpenInternetPanel)
            GestureAction.OPEN_VOLUME_CONTROLS -> _events.tryEmit(LauncherActionEvent.OpenVolumePanel)
            GestureAction.OPEN_APP_STORE -> openOverlay(LauncherOverlay.GetApps)
            GestureAction.LAUNCH_APP -> {
                val target = binding.targetComponentKey?.let { key ->
                    allApps.value.firstOrNull { it.componentKey == key }
                }
                if (target != null) {
                    launchApp(target)
                } else {
                    _events.tryEmit(LauncherActionEvent.ToastMessage("Assign an app to ${trigger.title} in Gesture Settings"))
                }
            }
        }
    }

    fun openInternetPanel() {
        _events.tryEmit(LauncherActionEvent.OpenInternetPanel)
    }

    fun openWifiControls() {
        _events.tryEmit(LauncherActionEvent.OpenWifiControls)
    }

    fun openMobileDataControls() {
        _events.tryEmit(LauncherActionEvent.OpenMobileDataControls)
    }

    fun connectHomeWifi(ssid: String, password: String) {
        _events.tryEmit(LauncherActionEvent.SuggestHomeWifi(ssid, password))
    }

    fun setMediaVolume(level: Int) {
        volumeRepo.setStreamVolume(AudioManager.STREAM_MUSIC, level)?.let {
            _volumeState.value = it
        }
    }

    fun setRingVolume(level: Int) {
        volumeRepo.setStreamVolume(AudioManager.STREAM_RING, level)?.let {
            _volumeState.value = it
        }
    }

    fun setNotificationVolume(level: Int) {
        volumeRepo.setStreamVolume(AudioManager.STREAM_NOTIFICATION, level)?.let {
            _volumeState.value = it
        }
    }

    fun setAlarmVolume(level: Int) {
        volumeRepo.setStreamVolume(AudioManager.STREAM_ALARM, level)?.let {
            _volumeState.value = it
        }
    }

    fun toggleMediaMute() {
        volumeRepo.toggleMediaMute()?.let {
            _volumeState.value = it
        }
    }

    fun openSystemVolumePanel() {
        _events.tryEmit(LauncherActionEvent.OpenVolumePanel)
    }

    fun openAppStoreHome() {
        _events.tryEmit(LauncherActionEvent.OpenAppStoreHome)
    }

    fun searchAppStore(query: String) {
        _events.tryEmit(LauncherActionEvent.OpenAppStoreSearch(query))
    }

    fun downloadAppFromStore(packageName: String) {
        _events.tryEmit(LauncherActionEvent.OpenAppStoreDetails(packageName))
    }

    fun openSystemDefaultHomeSettings() {
        _events.tryEmit(LauncherActionEvent.OpenHomeSettings)
    }

    fun launchOriginalLauncherOnce() {
        _events.tryEmit(LauncherActionEvent.LaunchOriginalLauncher)
    }

    fun openAndroidSystemSettings() {
        _events.tryEmit(LauncherActionEvent.OpenAndroidSettings)
    }

    // File indexing
    fun indexSafFolder(treeUri: Uri) {
        viewModelScope.launch {
            val count = fileRepo.indexSafFolder(treeUri)
            _events.tryEmit(LauncherActionEvent.ToastMessage("Indexed $count files for instant search"))
        }
    }

    fun indexPickedFile(fileUri: Uri) {
        viewModelScope.launch {
            fileRepo.indexSinglePickedFile(fileUri)
            _events.tryEmit(LauncherActionEvent.ToastMessage("Added file to Universal Search"))
        }
    }

    fun rescanFolder(treeUriString: String) {
        viewModelScope.launch {
            val count = fileRepo.rescanFolder(treeUriString)
            _events.tryEmit(LauncherActionEvent.ToastMessage("Refreshed $count files"))
        }
    }

    fun removeFolder(treeUriString: String) {
        viewModelScope.launch {
            fileRepo.removeIndexedFolder(treeUriString)
        }
    }

    fun openFile(file: IndexedFileItem) {
        if (!fileRepo.openFile(file)) {
            _events.tryEmit(LauncherActionEvent.ToastMessage("No app found to open ${file.name}"))
        }
    }

    // Settings updates
    fun setThemePreset(preset: ThemePreset) = viewModelScope.launch { settingsStore.setThemePreset(preset) }
    fun setIconPackStyle(style: IconPackStyle, externalPkg: String? = null) = viewModelScope.launch {
        settingsStore.setIconPackStyle(style, externalPkg)
        iconCache.clear()
        if (settings.value.preloadIcons) {
            preloadTopIcons(allApps.value)
        }
    }
    fun setIconShape(shape: IconShapeOption) = viewModelScope.launch { settingsStore.setIconShape(shape) }
    fun setWallpaperPreset(preset: WallpaperPreset, customUri: String? = null) = viewModelScope.launch {
        settingsStore.setWallpaperPreset(preset, customUri)
    }
    fun setWallpaperDim(dim: Float) = viewModelScope.launch { settingsStore.setWallpaperDim(dim) }
    fun setWallpaperBlur(enabled: Boolean) = viewModelScope.launch { settingsStore.setWallpaperBlur(enabled) }
    fun setGridColumns(cols: Int) = viewModelScope.launch { settingsStore.setGridColumns(cols) }
    fun setDockSlots(slots: Int) = viewModelScope.launch { settingsStore.setDockSlots(slots) }
    fun setShowAppLabels(show: Boolean) = viewModelScope.launch { settingsStore.setShowAppLabels(show) }
    fun setDockGlassOpacity(opacity: Float) = viewModelScope.launch { settingsStore.setDockGlassOpacity(opacity) }
    fun setIconScale(scale: Float) = viewModelScope.launch { settingsStore.setIconScale(scale) }
    fun setReduceMotion(reduce: Boolean) = viewModelScope.launch { settingsStore.setReduceMotion(reduce) }
    fun setPreloadIcons(preload: Boolean) = viewModelScope.launch { settingsStore.setPreloadIcons(preload) }
    fun setShowPerformanceHud(show: Boolean) = viewModelScope.launch { settingsStore.setShowPerformanceHud(show) }
    fun toggleZenMode() = viewModelScope.launch { settingsStore.setZenMode(!settings.value.zenMode) }
    fun setShowBootAnimation(enabled: Boolean) = viewModelScope.launch { settingsStore.setShowBootAnimation(enabled) }
    fun setGestureBinding(trigger: GestureTrigger, binding: GestureBinding) = viewModelScope.launch {
        settingsStore.setGestureBinding(trigger, binding)
    }
    fun resetGestures() = viewModelScope.launch {
        settingsStore.resetGesturesToDefaults()
        _events.tryEmit(LauncherActionEvent.ToastMessage("Gestures restored to defaults"))
    }
    fun clearIconCache() {
        iconCache.clear()
        _events.tryEmit(LauncherActionEvent.ToastMessage("Icon cache cleared"))
        if (settings.value.preloadIcons) {
            viewModelScope.launch { preloadTopIcons(allApps.value) }
        }
    }
}
