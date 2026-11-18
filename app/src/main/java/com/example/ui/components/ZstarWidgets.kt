package com.example.ui.components

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.AlarmClock
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.system.WidgetHostManager
import com.example.model.HomeItemUi
import com.example.model.PerformanceMetrics
import com.example.ui.theme.LocalZstarColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ZstarClockTelemetryWidget(
    metrics: PerformanceMetrics,
    isEditMode: Boolean,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val zstarColors = LocalZstarColors.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    val timeText = remember(nowMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
    }
    val dateText = remember(nowMillis) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
    }
    val batteryLevel = remember(nowMillis) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: 100
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        zstarColors.widgetCardSurface,
                        zstarColors.widgetCardSurface.copy(alpha = 0.55f)
                    )
                )
            )
            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(26.dp))
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("zstar_clock_widget")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 46.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateText.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEditMode) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove Clock Widget",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            shape = CircleShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BatteryChargingFull,
                                    contentDescription = "Battery",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$batteryLevel%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier.clickable(onClick = onOpenSettings)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = "Cache hit rate",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${metrics.appCount} apps · ${metrics.cacheHitRatioPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Universal Search pill inside the hero widget
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(onClick = onOpenSearch)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("widget_search_pill")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Universal Search",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Search apps, indexed files, or calculate…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ZstarFocusWidget(
    zenMode: Boolean,
    isEditMode: Boolean,
    onToggleZen: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenThemes: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zstarColors = LocalZstarColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(zstarColors.widgetCardSurface)
            .border(1.dp, zstarColors.glassBorder, RoundedCornerShape(22.dp))
            .padding(14.dp)
            .testTag("zstar_focus_widget")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ZSTAR QUICK COMMAND",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (isEditMode) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove Focus Widget",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickCommandChip(
                icon = Icons.Rounded.AutoAwesome,
                label = if (zenMode) "Zen On" else "Zen Mode",
                active = zenMode,
                onClick = onToggleZen,
                modifier = Modifier.weight(1f)
            )
            QuickCommandChip(
                icon = Icons.Rounded.Widgets,
                label = "Widgets",
                active = false,
                onClick = onOpenWidgets,
                modifier = Modifier.weight(1f)
            )
            QuickCommandChip(
                icon = Icons.Rounded.Palette,
                label = "Themes",
                active = false,
                onClick = onOpenThemes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickCommandChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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

@Composable
fun ThirdPartyWidgetHostCard(
    item: HomeItemUi,
    providerInfo: AppWidgetProviderInfo?,
    widgetHostManager: WidgetHostManager,
    isEditMode: Boolean,
    onCycleHeight: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zstarColors = LocalZstarColors.current
    val heightDp = (item.spanRows.coerceIn(1, 4) * 84).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(zstarColors.widgetCardSurface)
            .border(
                width = if (isEditMode) 1.5.dp else 1.dp,
                color = if (isEditMode) MaterialTheme.colorScheme.primary else zstarColors.glassBorder,
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("third_party_widget_${item.appWidgetId}")
    ) {
        if (isEditMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Widget (${item.spanCols}×${item.spanRows})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable(onClick = onCycleHeight)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Height,
                                contentDescription = "Resize widget height",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Resize",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable(onClick = onRemove)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove Widget",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .padding(4.dp)
        ) {
            if (providerInfo != null && item.appWidgetId > 0) {
                AndroidView(
                    factory = { ctx ->
                        val hostView: AppWidgetHostView? =
                            widgetHostManager.createHostView(item.appWidgetId, providerInfo)
                        hostView?.apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        } ?: android.widget.TextView(ctx).apply {
                            text = "Widget unavailable"
                            setTextColor(android.graphics.Color.WHITE)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Widget provider uninstalled or pending",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
