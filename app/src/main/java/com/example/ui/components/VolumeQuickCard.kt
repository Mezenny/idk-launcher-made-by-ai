package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.VolumeStateUi
import com.example.ui.theme.LocalZstarColors
import kotlin.math.roundToInt

@Composable
fun VolumeQuickCard(
    volumeState: VolumeStateUi,
    onMediaVolumeChange: (Int) -> Unit,
    onRingVolumeChange: (Int) -> Unit,
    onNotificationVolumeChange: (Int) -> Unit,
    onAlarmVolumeChange: (Int) -> Unit,
    onToggleMediaMute: () -> Unit,
    onOpenVolumePanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zstarColors = LocalZstarColors.current
    var expanded by remember { mutableStateOf(false) }

    val volumeColor = if (volumeState.isMediaMuted) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(zstarColors.glassSurface)
            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(20.dp))
            .padding(14.dp)
            .testTag("volume_quick_card")
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(volumeColor.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = if (volumeState.isMediaMuted) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = "Media Volume",
                        tint = volumeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SOUND & VOLUME",
                        style = MaterialTheme.typography.labelSmall,
                        color = volumeColor
                    )
                    Text(
                        text = if (volumeState.isMediaMuted) "Media Muted" else "Media ${volumeState.mediaPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (volumeState.isMediaMuted) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clickable(onClick = onToggleMediaMute)
                        .testTag("btn_toggle_mute")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (volumeState.isMediaMuted) {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            },
                            contentDescription = if (volumeState.isMediaMuted) "Unmute" else "Mute",
                            tint = if (volumeState.isMediaMuted) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (volumeState.isMediaMuted) "Muted" else "Mute",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    shape = CircleShape,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clickable(onClick = onOpenVolumePanel)
                        .testTag("btn_system_volume_panel")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "System Volume Panel",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Panel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_expand_volume_streams")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Hide extra volume streams" else "Show all volume streams",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        VolumeStreamSliderRow(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            label = "Media",
            value = volumeState.mediaVolume,
            max = volumeState.mediaMax,
            onValueCommit = onMediaVolumeChange,
            testTag = "slider_media_volume"
        )

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                VolumeStreamSliderRow(
                    icon = Icons.Rounded.RingVolume,
                    label = "Ring",
                    value = volumeState.ringVolume,
                    max = volumeState.ringMax,
                    onValueCommit = onRingVolumeChange,
                    testTag = "slider_ring_volume"
                )
                VolumeStreamSliderRow(
                    icon = Icons.Rounded.NotificationsActive,
                    label = "Notifications",
                    value = volumeState.notificationVolume,
                    max = volumeState.notificationMax,
                    onValueCommit = onNotificationVolumeChange,
                    testTag = "slider_notification_volume"
                )
                VolumeStreamSliderRow(
                    icon = Icons.Rounded.Alarm,
                    label = "Alarm",
                    value = volumeState.alarmVolume,
                    max = volumeState.alarmMax,
                    onValueCommit = onAlarmVolumeChange,
                    testTag = "slider_alarm_volume"
                )
            }
        }
    }
}

@Composable
private fun VolumeStreamSliderRow(
    icon: ImageVector,
    label: String,
    value: Int,
    max: Int,
    onValueCommit: (Int) -> Unit,
    testTag: String
) {
    val safeMax = max.coerceAtLeast(1)
    var sliderPos by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(78.dp)
        )
        Slider(
            value = sliderPos,
            onValueChange = { next ->
                sliderPos = next
                val rounded = next.roundToInt().coerceIn(0, safeMax)
                if (rounded != value) {
                    onValueCommit(rounded)
                }
            },
            valueRange = 0f..safeMax.toFloat(),
            steps = (safeMax - 1).coerceAtLeast(0),
            modifier = Modifier
                .weight(1f)
                .testTag(testTag)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${sliderPos.roundToInt()}/$safeMax",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
