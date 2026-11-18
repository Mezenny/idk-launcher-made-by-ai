package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.system.AppIconCache
import com.example.model.AppInfo
import com.example.model.IconPackStyle
import com.example.model.IconShapeOption
import com.example.ui.theme.LocalZstarColors

fun iconShapeToComposeShape(option: IconShapeOption): Shape {
    return when (option) {
        IconShapeOption.CIRCLE -> CircleShape
        IconShapeOption.SQUIRCLE -> RoundedCornerShape(percent = 32)
        IconShapeOption.ROUNDED -> RoundedCornerShape(percent = 22)
        IconShapeOption.SHARP -> RoundedCornerShape(percent = 12)
    }
}

@Composable
fun rememberAppIconBitmap(
    app: AppInfo,
    iconCache: AppIconCache,
    iconPackStyle: IconPackStyle,
    externalPackPkg: String?
): ImageBitmap? {
    val activeExtPkg = if (iconPackStyle == IconPackStyle.EXTERNAL) externalPackPkg else null
    var bitmap by remember(app.componentKey, activeExtPkg) {
        mutableStateOf(iconCache.getCachedIcon(app.componentKey, activeExtPkg))
    }

    LaunchedEffect(app.componentKey, activeExtPkg) {
        if (bitmap == null) {
            bitmap = iconCache.loadIcon(app.packageName, app.activityName, activeExtPkg)
        }
    }
    return bitmap
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StyledAppIconTile(
    app: AppInfo,
    iconCache: AppIconCache,
    iconPackStyle: IconPackStyle,
    externalPackPkg: String?,
    iconShape: IconShapeOption,
    iconSize: Dp = 54.dp,
    showLabel: Boolean = true,
    reduceMotion: Boolean = false,
    isEditMode: Boolean = false,
    onRemoveInEditMode: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_press_scale"
    )
    val zstarColors = LocalZstarColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("app_tile_${app.packageName}")
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            StyledAppIconGraphic(
                app = app,
                iconCache = iconCache,
                iconPackStyle = iconPackStyle,
                externalPackPkg = externalPackPkg,
                iconShape = iconShape,
                size = iconSize
            )

            if (isEditMode && onRemoveInEditMode != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .combinedClickable(onClick = onRemoveInEditMode)
                        .testTag("remove_item_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove ${app.label}",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.72f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                ),
                color = zstarColors.homeLabelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StyledAppIconGraphic(
    app: AppInfo,
    iconCache: AppIconCache,
    iconPackStyle: IconPackStyle,
    externalPackPkg: String?,
    iconShape: IconShapeOption,
    size: Dp = 54.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberAppIconBitmap(app, iconCache, iconPackStyle, externalPackPkg)
    val shape = iconShapeToComposeShape(iconShape)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val monochromeFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.08f) })
    }

    val containerModifier = when (iconPackStyle) {
        IconPackStyle.SYSTEM, IconPackStyle.EXTERNAL -> {
            modifier
                .size(size)
                .clip(shape)
        }
        IconPackStyle.STARLIGHT_GLASS -> {
            modifier
                .size(size)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            primary.copy(alpha = 0.14f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.32f), shape)
                .padding(5.dp)
        }
        IconPackStyle.MONOCHROME_TINT -> {
            modifier
                .size(size)
                .clip(shape)
                .background(primary.copy(alpha = 0.20f))
                .border(1.2.dp, primary.copy(alpha = 0.55f), shape)
                .padding(6.dp)
        }
        IconPackStyle.NEON_OUTLINE -> {
            modifier
                .size(size)
                .clip(shape)
                .background(Color(0xFF070B14))
                .border(
                    width = 1.6.dp,
                    brush = Brush.linearGradient(listOf(primary, secondary)),
                    shape = shape
                )
                .padding(6.dp)
        }
        IconPackStyle.MINIMAL_PIXEL -> {
            modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
                .padding(5.dp)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = containerModifier
    ) {
        if (bitmap != null) {
            val innerShape = if (iconPackStyle == IconPackStyle.SYSTEM || iconPackStyle == IconPackStyle.EXTERNAL) {
                shape
            } else {
                RoundedCornerShape(percent = (iconShape.cornerPercent - 6).coerceAtLeast(8))
            }
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                colorFilter = if (iconPackStyle == IconPackStyle.MONOCHROME_TINT) monochromeFilter else null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(innerShape)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = app.label,
                tint = primary,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderIconTile(
    title: String,
    apps: List<AppInfo>,
    iconCache: AppIconCache,
    iconPackStyle: IconPackStyle,
    externalPackPkg: String?,
    iconShape: IconShapeOption,
    iconSize: Dp = 54.dp,
    showLabel: Boolean = true,
    isEditMode: Boolean = false,
    onRemoveInEditMode: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zstarColors = LocalZstarColors.current
    val shape = iconShapeToComposeShape(iconShape)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("folder_tile_$title")
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(shape)
                    .background(zstarColors.glassSurface)
                    .border(1.dp, zstarColors.glassBorder, shape)
                    .padding(6.dp)
            ) {
                val preview = apps.take(4)
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        preview.getOrNull(0)?.let { app ->
                            StyledAppIconGraphic(
                                app = app,
                                iconCache = iconCache,
                                iconPackStyle = IconPackStyle.SYSTEM,
                                externalPackPkg = externalPackPkg,
                                iconShape = IconShapeOption.ROUNDED,
                                size = 18.dp,
                                modifier = Modifier.weight(1f)
                            )
                        } ?: Spacer(modifier = Modifier.weight(1f))
                        preview.getOrNull(1)?.let { app ->
                            StyledAppIconGraphic(
                                app = app,
                                iconCache = iconCache,
                                iconPackStyle = IconPackStyle.SYSTEM,
                                externalPackPkg = externalPackPkg,
                                iconShape = IconShapeOption.ROUNDED,
                                size = 18.dp,
                                modifier = Modifier.weight(1f)
                            )
                        } ?: Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        preview.getOrNull(2)?.let { app ->
                            StyledAppIconGraphic(
                                app = app,
                                iconCache = iconCache,
                                iconPackStyle = IconPackStyle.SYSTEM,
                                externalPackPkg = externalPackPkg,
                                iconShape = IconShapeOption.ROUNDED,
                                size = 18.dp,
                                modifier = Modifier.weight(1f)
                            )
                        } ?: Spacer(modifier = Modifier.weight(1f))
                        preview.getOrNull(3)?.let { app ->
                            StyledAppIconGraphic(
                                app = app,
                                iconCache = iconCache,
                                iconPackStyle = IconPackStyle.SYSTEM,
                                externalPackPkg = externalPackPkg,
                                iconShape = IconShapeOption.ROUNDED,
                                size = 18.dp,
                                modifier = Modifier.weight(1f)
                            )
                        } ?: Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (isEditMode && onRemoveInEditMode != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .combinedClickable(onClick = onRemoveInEditMode)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove folder",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    shadow = Shadow(Color.Black.copy(alpha = 0.72f), Offset(0f, 1.5f), 4f)
                ),
                color = zstarColors.homeLabelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
