package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.HomeItemType
import com.example.model.WidgetProviderItem
import com.example.ui.theme.LocalZstarColors

@Composable
fun WidgetPickerSheet(
    providers: List<WidgetProviderItem>,
    onAddZstarWidget: (HomeItemType) -> Unit,
    onSelectThirdPartyWidget: (WidgetProviderItem) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val zstarColors = LocalZstarColors.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredProviders = remember(providers, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) providers
        else providers.filter {
            it.label.lowercase().contains(q) || it.appName.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .testTag("widget_picker_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = "Widget Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${providers.size} third-party widgets ready to host",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(zstarColors.glassSurface)
                    .border(1.dp, zstarColors.glassBorder, CircleShape)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close Widget Picker")
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Third-Party (${providers.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Zstar Smart") }
            )
        }

        if (selectedTab == 0) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = { Text("Filter installed app widgets…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Filter widgets") },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = zstarColors.glassSurface,
                    unfocusedContainerColor = zstarColors.glassSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (filteredProviders.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(zstarColors.glassSurface)
                                .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "No matching third-party widgets found on this device.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try the Zstar Smart tab for built-in Clock, Telemetry, and Command widgets.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredProviders, key = { it.componentName }) { provider ->
                        ThirdPartyWidgetProviderRow(
                            provider = provider,
                            onAdd = { onSelectThirdPartyWidget(provider) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    ZstarBuiltInWidgetOption(
                        icon = Icons.Rounded.AccessTime,
                        title = "Zstar Clock & Telemetry",
                        subtitle = "Digital Space Grotesk clock, battery level, cache stats, and quick search pill (4×2)",
                        onAdd = { onAddZstarWidget(HomeItemType.ZSTAR_CLOCK_WIDGET) }
                    )
                }
                item {
                    ZstarBuiltInWidgetOption(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Zstar Quick Command",
                        subtitle = "Instant toggles for Zen Mode, Widget Studio, and Theme Engine (4×1)",
                        onAdd = { onAddZstarWidget(HomeItemType.ZSTAR_FOCUS_WIDGET) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThirdPartyWidgetProviderRow(
    provider: WidgetProviderItem,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    val zstarColors = LocalZstarColors.current

    val previewBitmap = remember(provider.componentName) {
        runCatching {
            val drawable = provider.providerInfo.loadPreviewImage(context, 0)
                ?: provider.providerInfo.loadIcon(context, 0)
            drawable?.toBitmap(width = 96, height = 96)?.asImageBitmap()
        }.getOrNull()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(zstarColors.glassSurface)
            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onAdd)
            .padding(14.dp)
            .testTag("widget_provider_${provider.packageName}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = provider.label,
                    modifier = Modifier.size(44.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Widgets,
                    contentDescription = provider.label,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${provider.appName} · ${provider.spanCols}×${provider.spanRows} cells",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add ${provider.label}",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun ZstarBuiltInWidgetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onAdd: () -> Unit
) {
    val zstarColors = LocalZstarColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(zstarColors.glassSurface)
            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(22.dp))
            .clickable(onClick = onAdd)
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add $title",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
