package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.system.AppIconCache
import com.example.model.AppInfo
import com.example.model.HomeItemUi
import com.example.model.LauncherSettingsState
import com.example.ui.components.StyledAppIconTile
import com.example.ui.theme.LocalZstarColors

@Composable
fun FolderModalDialog(
    folder: HomeItemUi,
    settings: LauncherSettingsState,
    iconCache: AppIconCache,
    onLaunchApp: (AppInfo) -> Unit,
    onRemoveAppFromFolder: (String) -> Unit,
    onRenameFolder: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val zstarColors = LocalZstarColors.current
    var showRename by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = onClose)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(zstarColors.glassSurface)
                .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(28.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showRename = true }
                ) {
                    Text(
                        text = folder.folderTitle ?: "Folder",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showRename = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Rename folder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close folder")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (folder.folderApps.isEmpty()) {
                Text(
                    text = "Long-press any app to add it to this folder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(folder.folderApps, key = { it.componentKey }) { app ->
                        StyledAppIconTile(
                            app = app,
                            iconCache = iconCache,
                            iconPackStyle = settings.iconPackStyle,
                            externalPackPkg = settings.externalIconPackPackage,
                            iconShape = settings.iconShape,
                            iconSize = 54.dp,
                            showLabel = true,
                            isEditMode = true,
                            onRemoveInEditMode = { onRemoveAppFromFolder(app.componentKey) },
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onRemoveAppFromFolder(app.componentKey) }
                        )
                    }
                }
            }
        }
    }

    if (showRename) {
        var name by remember { mutableStateOf(folder.folderTitle ?: "Folder") }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameFolder(name)
                        showRename = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }
}
