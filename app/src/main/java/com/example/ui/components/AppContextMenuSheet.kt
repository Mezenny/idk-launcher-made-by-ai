package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.system.AppIconCache
import com.example.model.AppInfo
import com.example.model.HomeItemUi
import com.example.model.IconPackStyle
import com.example.model.IconShapeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContextMenuSheet(
    app: AppInfo,
    homeItem: HomeItemUi?,
    iconCache: AppIconCache,
    iconPackStyle: IconPackStyle,
    externalPackPkg: String?,
    iconShape: IconShapeOption,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onPinToHome: () -> Unit,
    onPinToDock: () -> Unit,
    onAddToFolder: () -> Unit,
    onRemoveFromHome: (() -> Unit)?,
    onRename: (String) -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                StyledAppIconGraphic(
                    app = app,
                    iconCache = iconCache,
                    iconPackStyle = iconPackStyle,
                    externalPackPkg = externalPackPkg,
                    iconShape = iconShape,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(6.dp))

            MenuActionRow(
                icon = Icons.Rounded.OpenInNew,
                label = "Launch App",
                onClick = {
                    onDismiss()
                    onLaunch()
                }
            )
            if (homeItem == null) {
                MenuActionRow(
                    icon = Icons.Rounded.AddToHomeScreen,
                    label = "Pin to Home Screen",
                    onClick = onPinToHome
                )
            } else if (onRemoveFromHome != null) {
                MenuActionRow(
                    icon = Icons.Rounded.RemoveCircleOutline,
                    label = "Remove from Home",
                    onClick = onRemoveFromHome
                )
            }
            MenuActionRow(
                icon = Icons.Rounded.Dock,
                label = "Pin to Dock",
                onClick = onPinToDock
            )
            MenuActionRow(
                icon = Icons.Rounded.CreateNewFolder,
                label = "Add to Workspace Folder",
                onClick = onAddToFolder
            )
            MenuActionRow(
                icon = Icons.Rounded.DriveFileRenameOutline,
                label = "Rename Label",
                onClick = { showRenameDialog = true }
            )
            MenuActionRow(
                icon = Icons.Rounded.Info,
                label = "App Info",
                onClick = onAppInfo
            )
            if (!app.isSystemApp) {
                MenuActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Uninstall",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onUninstall
                )
            }
        }
    }

    if (showRenameDialog) {
        var text by remember { mutableStateOf(app.label) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename App Label") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Custom Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        onRename(text)
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag("menu_action_$label")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
