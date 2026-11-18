package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.NetworkStatusUi
import com.example.model.NetworkTransport
import com.example.ui.theme.LocalZstarColors

@Composable
fun ConnectivityQuickCard(
    networkStatus: NetworkStatusUi,
    onOpenInternetPanel: () -> Unit,
    onOpenWifiControls: () -> Unit,
    onOpenMobileDataControls: () -> Unit,
    onConnectHomeWifi: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val zstarColors = LocalZstarColors.current
    var showHomeWifiDialog by remember { mutableStateOf(false) }

    val statusColor = when {
        !networkStatus.isConnected -> MaterialTheme.colorScheme.error
        networkStatus.isValidated -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val statusIcon: ImageVector = when {
        !networkStatus.isConnected -> Icons.Rounded.WifiOff
        networkStatus.transport == NetworkTransport.CELLULAR -> Icons.Rounded.CellTower
        networkStatus.transport == NetworkTransport.ETHERNET -> Icons.Rounded.SettingsEthernet
        else -> Icons.Rounded.Wifi
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(zstarColors.glassSurface)
            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(20.dp))
            .padding(14.dp)
            .testTag("connectivity_quick_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = networkStatus.summaryText,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "NETWORK & INTERNET",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                    Text(
                        text = networkStatus.summaryText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (networkStatus.isConnected && networkStatus.isMetered) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "Metered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ConnectivityActionChip(
                icon = Icons.Rounded.Language,
                label = "Internet",
                accent = MaterialTheme.colorScheme.primary,
                highlighted = true,
                onClick = onOpenInternetPanel,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_internet_panel")
            )
            ConnectivityActionChip(
                icon = Icons.Rounded.Wifi,
                label = "Wi-Fi",
                accent = MaterialTheme.colorScheme.primary,
                highlighted = networkStatus.transport == NetworkTransport.WIFI && networkStatus.isConnected,
                onClick = onOpenWifiControls,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_wifi_controls")
            )
            ConnectivityActionChip(
                icon = Icons.Rounded.CellTower,
                label = "Data",
                accent = MaterialTheme.colorScheme.secondary,
                highlighted = networkStatus.transport == NetworkTransport.CELLULAR && networkStatus.isConnected,
                onClick = onOpenMobileDataControls,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_mobile_data_controls")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showHomeWifiDialog = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("btn_home_wifi_ssid")
        ) {
            Icon(
                imageVector = Icons.Rounded.Router,
                contentDescription = "Connect Home Wi-Fi",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Connect Home Wi-Fi (Enter SSID & Password)",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    if (showHomeWifiDialog) {
        HomeWifiConnectDialog(
            onDismiss = { showHomeWifiDialog = false },
            onConnect = { ssid, password ->
                showHomeWifiDialog = false
                onConnectHomeWifi(ssid, password)
            },
            onOpenSystemWifiList = {
                showHomeWifiDialog = false
                onOpenWifiControls()
            }
        )
    }
}

@Composable
private fun HomeWifiConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (String, String) -> Unit,
    onOpenSystemWifiList: () -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = "Connect Home Wi-Fi",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Connect to Home Wi-Fi",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your home router name (SSID) and password to register it with Android, or browse nearby networks directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Wi-Fi Network Name (SSID)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_wifi_ssid_input")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional if open)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_wifi_password_input")
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onConnect(ssid, password) },
                    enabled = ssid.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_wifi_connect_button")
                ) {
                    Text("Save & Connect Wi-Fi")
                }
                OutlinedButton(
                    onClick = onOpenSystemWifiList,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_wifi_browse_button")
                ) {
                    Text("Browse Nearby Wi-Fi Networks")
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    )
}

@Composable
private fun ConnectivityActionChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (highlighted) accent.copy(alpha = 0.20f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            )
            .border(
                width = 1.dp,
                color = if (highlighted) accent.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlighted) accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
