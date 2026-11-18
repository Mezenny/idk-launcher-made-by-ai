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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.system.AppDownloadHelper
import com.example.data.system.FeaturedStoreApp
import com.example.model.AppInfo
import com.example.ui.theme.LocalZstarColors

@Composable
fun GetAppsOverlay(
    installedApps: List<AppInfo>,
    onOpenStoreHome: () -> Unit,
    onSearchStore: (String) -> Unit,
    onDownloadPackage: (String) -> Unit,
    onLaunchInstalledApp: (AppInfo) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val zstarColors = LocalZstarColors.current
    var storeQuery by remember { mutableStateOf("") }
    val installedByPackage = remember(installedApps) {
        installedApps.associateBy { it.packageName }
    }

    val filteredCatalog = remember(storeQuery) {
        val q = storeQuery.trim().lowercase()
        if (q.isEmpty()) {
            AppDownloadHelper.featuredCatalog
        } else {
            AppDownloadHelper.featuredCatalog.filter {
                it.title.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.description.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .padding(top = 10.dp)
            .testTag("get_apps_overlay")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = onClose, modifier = Modifier.testTag("get_apps_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to Home"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Get Apps",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Search & download new apps directly to Zstar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onOpenStoreHome,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("btn_open_play_store")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shop,
                    contentDescription = "Open Play Store",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play Store", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Store Search Input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = storeQuery,
                onValueChange = { storeQuery = it },
                singleLine = true,
                placeholder = { Text("Search any app or game to download…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search App Store",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (storeQuery.isNotEmpty()) {
                        IconButton(onClick = { storeQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (storeQuery.isNotBlank()) onSearchStore(storeQuery)
                    }
                ),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = zstarColors.glassSurface,
                    unfocusedContainerColor = zstarColors.glassSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = zstarColors.glassBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("get_apps_search_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSearchStore(storeQuery) },
                enabled = storeQuery.isNotBlank(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("get_apps_search_submit")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Download from Store",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (storeQuery.isNotBlank()) {
                item(key = "direct_store_search") {
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
                            .clickable { onSearchStore(storeQuery) }
                            .padding(16.dp)
                            .testTag("get_apps_direct_search_card")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shop,
                            contentDescription = "Search Play Store",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Download \"${storeQuery.trim()}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Search Google Play Store & install directly",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "store_info_banner") {
                Surface(
                    color = zstarColors.glassSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(18.dp))
                ) {
                    Text(
                        text = "Newly downloaded apps appear automatically in Zstar's App Drawer as soon as installation finishes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            item(key = "catalog_header") {
                Text(
                    text = "POPULAR APPS TO DOWNLOAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(filteredCatalog, key = { it.packageName }) { item ->
                val installed = installedByPackage[item.packageName]
                StoreCatalogRow(
                    app = item,
                    installedApp = installed,
                    onDownload = { onDownloadPackage(item.packageName) },
                    onLaunch = {
                        if (installed != null) onLaunchInstalledApp(installed)
                    }
                )
            }
        }
    }
}

@Composable
private fun StoreCatalogRow(
    app: FeaturedStoreApp,
    installedApp: AppInfo?,
    onDownload: () -> Unit,
    onLaunch: () -> Unit
) {
    val zstarColors = LocalZstarColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(zstarColors.glassSurface)
            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
        ) {
            Icon(
                imageVector = if (installedApp != null) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                contentDescription = app.title,
                tint = if (installedApp != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = CircleShape
                ) {
                    Text(
                        text = app.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = app.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (installedApp != null) {
            OutlinedButton(
                onClick = onLaunch,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Open", style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Button(
                onClick = onDownload,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("Get", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
