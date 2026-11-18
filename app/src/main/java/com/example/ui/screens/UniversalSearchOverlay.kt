package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.system.AppIconCache
import com.example.data.system.FileSearchRepository
import com.example.model.AppInfo
import com.example.model.IndexedFileItem
import com.example.model.LauncherSettingsState
import com.example.model.NetworkStatusUi
import com.example.model.VolumeStateUi
import com.example.ui.components.ConnectivityQuickCard
import com.example.ui.components.StyledAppIconTile
import com.example.ui.components.VolumeQuickCard
import com.example.ui.theme.LocalZstarColors
import com.example.viewmodel.UniversalSearchState

@Composable
fun UniversalSearchOverlay(
    searchState: UniversalSearchState,
    networkStatus: NetworkStatusUi,
    volumeState: VolumeStateUi,
    indexedFoldersCount: Int,
    settings: LauncherSettingsState,
    iconCache: AppIconCache,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit,
    onOpenFile: (IndexedFileItem) -> Unit,
    onPickFolderToIndex: () -> Unit,
    onPickSingleFileToIndex: () -> Unit,
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
    onSearchAppStore: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val zstarColors = LocalZstarColors.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.90f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .padding(top = 10.dp)
            .testTag("universal_search_overlay")
    ) {
        // Search Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchState.query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Search apps, files, or type math…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Universal Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchState.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = zstarColors.glassSurface,
                    unfocusedContainerColor = zstarColors.glassSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = zstarColors.glassBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("universal_search_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(zstarColors.glassSurface)
                    .border(1.dp, zstarColors.glassBorder, CircleShape)
                    .testTag("close_search_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close search",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "connectivity_quick_bar") {
                ConnectivityQuickCard(
                    networkStatus = networkStatus,
                    onOpenInternetPanel = onOpenInternetPanel,
                    onOpenWifiControls = onOpenWifiControls,
                    onOpenMobileDataControls = onOpenMobileDataControls,
                    onConnectHomeWifi = onConnectHomeWifi
                )
            }

            item(key = "volume_quick_bar") {
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

            // Live Math Evaluator Result
            if (searchState.mathResult != null) {
                item(key = "math_result") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = "Calculator result",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "QUICK CALC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "= ${searchState.mathResult}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Matching Apps Section
            item(key = "apps_header") {
                Text(
                    text = if (searchState.query.isBlank()) "SUGGESTED APPS" else "APPS (${searchState.matchingApps.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item(key = "apps_row") {
                if (searchState.matchingApps.isEmpty()) {
                    Text(
                        text = "No apps match \"${searchState.query}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchState.matchingApps, key = { it.componentKey }) { app ->
                            StyledAppIconTile(
                                app = app,
                                iconCache = iconCache,
                                iconPackStyle = settings.iconPackStyle,
                                externalPackPkg = settings.externalIconPackPackage,
                                iconShape = settings.iconShape,
                                iconSize = 52.dp,
                                showLabel = true,
                                reduceMotion = settings.reduceMotion,
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) },
                                modifier = Modifier.width(78.dp)
                            )
                        }
                    }
                }
            }

            // Files Section Header + SAF Index Actions
            item(key = "files_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "DEVICE FILES (${searchState.matchingFiles.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = zstarColors.glassSurface,
                            shape = CircleShape,
                            modifier = Modifier
                                .border(1.dp, zstarColors.glassBorder, CircleShape)
                                .clickable(onClick = onPickFolderToIndex)
                                .testTag("index_folder_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CreateNewFolder,
                                    contentDescription = "Index Folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Index Folder",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            color = zstarColors.glassSurface,
                            shape = CircleShape,
                            modifier = Modifier
                                .border(1.dp, zstarColors.glassBorder, CircleShape)
                                .clickable(onClick = onPickSingleFileToIndex)
                                .testTag("pin_file_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NoteAdd,
                                    contentDescription = "Add File",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add File",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (searchState.matchingFiles.isEmpty()) {
                item(key = "files_empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(zstarColors.glassSurface)
                            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = if (indexedFoldersCount == 0) {
                                "Index a folder (like Downloads or Documents) to search device files instantly."
                            } else {
                                "No indexed files match \"${searchState.query}\"."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(searchState.matchingFiles, key = { it.uriString }) { file ->
                    FileSearchRow(
                        file = file,
                        onClick = { onOpenFile(file) }
                    )
                }
            }

            // Web & App Store fallback when user typed a query
            if (searchState.query.trim().isNotEmpty()) {
                item(key = "store_download_search") {
                    val q = searchState.query.trim()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { onSearchAppStore(q) }
                            .padding(16.dp)
                            .testTag("search_download_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shop,
                            contentDescription = "Download from App Store",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Download \"$q\" from Play Store",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                item(key = "web_search") {
                    val q = searchState.query.trim()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(zstarColors.glassSurface)
                            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(18.dp))
                            .clickable {
                                runCatching {
                                    val url = "https://www.google.com/search?q=${Uri.encode(q)}"
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Public,
                            contentDescription = "Web Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search web for \"$q\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileSearchRow(
    file: IndexedFileItem,
    onClick: () -> Unit
) {
    val zstarColors = LocalZstarColors.current
    val icon: ImageVector = when {
        file.mimeType.startsWith("image/") -> Icons.Rounded.Image
        file.mimeType.startsWith("video/") -> Icons.Rounded.VideoFile
        file.mimeType.startsWith("audio/") -> Icons.Rounded.AudioFile
        file.mimeType.contains("pdf") || file.mimeType.startsWith("text/") -> Icons.Rounded.Description
        else -> Icons.Rounded.InsertDriveFile
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(zstarColors.glassSurface)
            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = file.mimeType,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${file.folderName} · ${FileSearchRepository.formatFileSize(file.sizeBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
