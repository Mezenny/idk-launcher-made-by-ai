package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.system.AppIconCache
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.LauncherSettingsState
import com.example.ui.components.StyledAppIconTile
import com.example.ui.theme.LocalZstarColors
import kotlinx.coroutines.launch

@Composable
fun AppDrawerOverlay(
    apps: List<AppInfo>,
    selectedCategory: AppCategory,
    query: String,
    settings: LauncherSettingsState,
    iconCache: AppIconCache,
    onCategoryChange: (AppCategory) -> Unit,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit,
    onOpenGetApps: () -> Unit,
    onSearchAppStore: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val zstarColors = LocalZstarColors.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val alphabetLetters by remember(apps) {
        derivedStateOf {
            apps.mapNotNull { app ->
                app.label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' }
            }.distinct()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .padding(top = 8.dp)
            .testTag("app_drawer_overlay")
    ) {
        // Header + Search Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Filter ${apps.size} apps…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search drawer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear filter")
                        }
                    }
                },
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = zstarColors.glassSurface,
                    unfocusedContainerColor = zstarColors.glassSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = zstarColors.glassBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("drawer_search_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onOpenGetApps,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape)
                    .testTag("drawer_get_apps_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shop,
                    contentDescription = "Get Apps",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(zstarColors.glassSurface)
                    .border(1.dp, zstarColors.glassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Launcher settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(zstarColors.glassSurface)
                    .border(1.dp, zstarColors.glassBorder, CircleShape)
                    .testTag("close_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close app drawer",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppCategory.entries, key = { it.name }) { category ->
                val selected = category == selectedCategory
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else zstarColors.glassSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else zstarColors.glassBorder,
                            CircleShape
                        )
                        .clickable { onCategoryChange(category) }
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // App Grid + Quick A-Z Jump Rail
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (apps.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "No matching apps installed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (query.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onSearchAppStore(query) },
                            modifier = Modifier.testTag("drawer_download_query_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Download from Store",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download \"${query.trim()}\" on Play Store")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(settings.gridColumns),
                    contentPadding = PaddingValues(start = 12.dp, end = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    items(apps, key = { it.componentKey }) { app ->
                        StyledAppIconTile(
                            app = app,
                            iconCache = iconCache,
                            iconPackStyle = settings.iconPackStyle,
                            externalPackPkg = settings.externalIconPackPackage,
                            iconShape = settings.iconShape,
                            iconSize = (52f * settings.iconScale).dp,
                            showLabel = true,
                            reduceMotion = settings.reduceMotion,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onLongPressApp(app) }
                        )
                    }
                }

                // A-Z Fast Index Rail
                if (alphabetLetters.size > 3 && selectedCategory != AppCategory.FREQUENT) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 6.dp, top = 8.dp, bottom = 16.dp)
                    ) {
                        alphabetLetters.forEach { ch ->
                            Text(
                                text = ch.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        val idx = apps.indexOfFirst {
                                            it.label.firstOrNull()?.uppercaseChar() == ch
                                        }
                                        if (idx >= 0) {
                                            scope.launch { gridState.animateScrollToItem(idx) }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
