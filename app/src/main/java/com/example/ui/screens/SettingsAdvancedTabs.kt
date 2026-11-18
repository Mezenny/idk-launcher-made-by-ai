package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppInfo
import com.example.model.GestureAction
import com.example.model.GestureBinding
import com.example.model.GestureTrigger
import com.example.model.IndexedFolderSummary
import com.example.model.LauncherSettingsState
import com.example.model.NetworkStatusUi
import com.example.model.PerformanceMetrics
import com.example.model.VolumeStateUi
import com.example.ui.components.ConnectivityQuickCard
import com.example.ui.components.VolumeQuickCard

@Composable
fun GesturesSettingsTab(
    settings: LauncherSettingsState,
    allApps: List<AppInfo>,
    onGestureBindingChange: (GestureTrigger, GestureBinding) -> Unit,
    onResetGestures: () -> Unit,
    onTestGesture: (GestureTrigger) -> Unit
) {
    var editingTrigger by remember { mutableStateOf<GestureTrigger?>(null) }
    var pickingAppForTrigger by remember { mutableStateOf<GestureTrigger?>(null) }
    var lastDetectedLabel by remember { mutableStateOf("Swipe or tap inside the pad to test") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // Interactive Live Gesture Test Pad
            var dragX by remember { mutableFloatStateOf(0f) }
            var dragY by remember { mutableFloatStateOf(0f) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        RoundedCornerShape(22.dp)
                    )
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.size == 2 && event.changes.all { it.pressed && !it.previousPressed }) {
                                    lastDetectedLabel = "Detected: Two-Finger Tap"
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                lastDetectedLabel = "Detected: Double Tap"
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                dragX = 0f
                                dragY = 0f
                            },
                            onDrag = { _, offset ->
                                dragX += offset.x
                                dragY += offset.y
                            },
                            onDragEnd = {
                                val trigger = when {
                                    kotlin.math.abs(dragY) >= kotlin.math.abs(dragX) && dragY < -60f -> GestureTrigger.SWIPE_UP
                                    kotlin.math.abs(dragY) >= kotlin.math.abs(dragX) && dragY > 60f -> GestureTrigger.SWIPE_DOWN
                                    dragX < -60f -> GestureTrigger.DOCK_SWIPE_LEFT
                                    dragX > 60f -> GestureTrigger.DOCK_SWIPE_RIGHT
                                    else -> null
                                }
                                if (trigger != null) {
                                    val bound = settings.gestureMappings[trigger]?.action?.title ?: "Unbound"
                                    lastDetectedLabel = "${trigger.title} → $bound"
                                }
                            }
                        )
                    }
                    .padding(16.dp)
                    .testTag("gesture_test_pad")
            ) {
                Icon(
                    imageVector = Icons.Rounded.TouchApp,
                    contentDescription = "Gesture test pad",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "LIVE GESTURE PAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = lastDetectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GESTURE MAPPINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onResetGestures) {
                    Icon(
                        imageVector = Icons.Rounded.Restore,
                        contentDescription = "Reset gestures",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }

        items(GestureTrigger.entries, key = { it.id }) { trigger ->
            val binding = settings.gestureMappings[trigger]
            val summary = if (binding?.action == GestureAction.LAUNCH_APP && !binding.targetAppLabel.isNullOrBlank()) {
                "Launch ${binding.targetAppLabel}"
            } else {
                binding?.action?.title ?: "None"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .clickable { editingTrigger = trigger }
                    .padding(14.dp)
                    .testTag("gesture_row_${trigger.id}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Gesture,
                        contentDescription = trigger.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trigger.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = trigger.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    shape = CircleShape,
                    modifier = Modifier.clickable { onTestGesture(trigger) }
                ) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    editingTrigger?.let { trigger ->
        AlertDialog(
            onDismissRequest = { editingTrigger = null },
            title = { Text("Action for ${trigger.title}") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GestureAction.entries, key = { it.id }) { action ->
                        val currentAction = settings.gestureMappings[trigger]?.action
                        SettingsOptionCard(
                            title = action.title,
                            subtitle = action.subtitle,
                            selected = currentAction == action,
                            onClick = {
                                if (action == GestureAction.LAUNCH_APP) {
                                    editingTrigger = null
                                    pickingAppForTrigger = trigger
                                } else {
                                    onGestureBindingChange(trigger, GestureBinding(action))
                                    editingTrigger = null
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingTrigger = null }) { Text("Close") }
            }
        )
    }

    pickingAppForTrigger?.let { trigger ->
        AlertDialog(
            onDismissRequest = { pickingAppForTrigger = null },
            title = { Text("Pick App for ${trigger.title}") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(allApps, key = { it.componentKey }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onGestureBindingChange(
                                        trigger,
                                        GestureBinding(
                                            action = GestureAction.LAUNCH_APP,
                                            targetComponentKey = app.componentKey,
                                            targetAppLabel = app.label
                                        )
                                    )
                                    pickingAppForTrigger = null
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingAppForTrigger = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PerformanceAndFilesTab(
    isDefaultLauncher: Boolean,
    originalLauncherName: String?,
    settings: LauncherSettingsState,
    metrics: PerformanceMetrics,
    networkStatus: NetworkStatusUi,
    volumeState: VolumeStateUi,
    indexedFolders: List<IndexedFolderSummary>,
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
    onReduceMotionChange: (Boolean) -> Unit,
    onPreloadIconsChange: (Boolean) -> Unit,
    onShowHudChange: (Boolean) -> Unit,
    onShowBootAnimationChange: (Boolean) -> Unit,
    onReplayBootAnimation: () -> Unit,
    onClearIconCache: () -> Unit,
    onAddSafFolder: () -> Unit,
    onRescanFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ConnectivityQuickCard(
                networkStatus = networkStatus,
                onOpenInternetPanel = onOpenInternetPanel,
                onOpenWifiControls = onOpenWifiControls,
                onOpenMobileDataControls = onOpenMobileDataControls,
                onConnectHomeWifi = onConnectHomeWifi
            )
        }

        item {
            VolumeQuickCard(
                volumeState = volumeState,
                onMediaVolumeChange = onMediaVolumeChange,
                onRingVolumeChange = onRingVolumeChange,
                onNotificationVolumeChange = onNotificationVolumeChange,
                onAlarmVolumeChange = onAlarmVolumeChange,
                onToggleMediaMute = onToggleMediaMute,
                onOpenVolumePanel = onOpenVolumePanel
            )
        }

        item {
            val fallbackName = originalLauncherName ?: "Original Android Launcher"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(16.dp)
                    .testTag("switch_launcher_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = "System Launcher",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYSTEM LAUNCHER & ANDROID SETTINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isDefaultLauncher) {
                        "Zstar Launcher is your active Default Home. You can switch back to $fallbackName anytime."
                    } else {
                        "$fallbackName is currently your system Default Home."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenHomeSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_switch_back_original_launcher")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Switch Default Home App",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDefaultLauncher) {
                            "Switch Back to Original Android Launcher"
                        } else {
                            "Choose Default Home Launcher"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onLaunchOriginalLauncher,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_launch_original_once")
                    ) {
                        Text(
                            text = "Open Original Home",
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenAndroidSettings,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_android_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Android Settings",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Android Settings",
                            maxLines = 1
                        )
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Performance Telemetry",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENGINE TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricBlock(label = "Installed Apps", value = "${metrics.appCount}")
                    MetricBlock(label = "Cache Hit Rate", value = "${metrics.cacheHitRatioPercent}%")
                    MetricBlock(label = "Cached Icons", value = "${metrics.iconCacheEntries}")
                    MetricBlock(label = "Scan Time", value = "${metrics.lastAppScanMs}ms")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onClearIconCache,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cached,
                        contentDescription = "Rebuild Icon Cache",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rebuild & Pre-warm Icon Cache")
                }
            }
        }

        item {
            SettingsSwitchCard(
                title = "Preload App Icons on Startup",
                subtitle = "Warm LruCache in background for zero-jank scrolling",
                checked = settings.preloadIcons,
                onCheckedChange = onPreloadIconsChange
            )
        }

        item {
            SettingsSwitchCard(
                title = "Show Live Performance HUD",
                subtitle = "Display live cache hit ratio & app scan latency pill on Home",
                checked = settings.showPerformanceHud,
                onCheckedChange = onShowHudChange
            )
        }

        item {
            SettingsSwitchCard(
                title = "Reduce Motion",
                subtitle = "Minimize spring icon bounce and transition scale",
                checked = settings.reduceMotion,
                onCheckedChange = onReduceMotionChange
            )
        }

        item {
            SettingsSwitchCard(
                title = "MIUI-Style Starting Screen",
                subtitle = "Play Zstar boot emblem & wordmark animation on cold start / restart",
                checked = settings.showBootAnimation,
                onCheckedChange = onShowBootAnimationChange
            )
        }

        item {
            OutlinedButton(
                onClick = onReplayBootAnimation,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("replay_boot_animation_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Preview Starting Screen",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview Starting Screen Animation")
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "FILE SEARCH INDEX (${metrics.indexedFilesCount} FILES)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Uses privacy-safe Storage Access Framework",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onAddSafFolder, modifier = Modifier.testTag("settings_add_folder_button")) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = "Add folder",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Folder")
                }
            }
        }

        if (indexedFolders.isEmpty()) {
            item {
                Text(
                    text = "No folders indexed yet. Add Downloads or Documents so Universal Search finds your files instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(indexedFolders, key = { it.treeUriString }) { folder ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${folder.fileCount} indexed files",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onRescanFolder(folder.treeUriString) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Rescan ${folder.displayName}")
                    }
                    IconButton(onClick = { onRemoveFolder(folder.treeUriString) }) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove ${folder.displayName}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
