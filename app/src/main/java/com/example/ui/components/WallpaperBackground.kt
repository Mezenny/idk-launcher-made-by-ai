package com.example.ui.components

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.WallpaperPreset

@Composable
fun WallpaperBackground(
    preset: WallpaperPreset,
    customWallpaperUri: String?,
    dimAlpha: Float,
    blurEnabled: Boolean,
    parallaxFraction: Float = 0f,
    extraOverlayDim: Float = 0f,
    modifier: Modifier = Modifier
) {
    val blurModifier = if (blurEnabled && extraOverlayDim > 0.1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(18.dp)
    } else {
        Modifier
    }

    Box(modifier = modifier.fillMaxSize()) {
        val imageModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 1.08f
                scaleY = 1.08f
                translationX = -parallaxFraction * 36f
            }
            .then(blurModifier)

        if (preset == WallpaperPreset.CUSTOM_PHOTO && !customWallpaperUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(customWallpaperUri),
                contentDescription = "Custom wallpaper",
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        } else {
            val resId = when (preset) {
                WallpaperPreset.MONOLITH -> R.drawable.img_wallpaper_monolith
                WallpaperPreset.AURORA -> R.drawable.img_wallpaper_aurora
                else -> R.drawable.img_wallpaper_nebula
            }
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Launcher wallpaper",
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        }

        // Top-to-bottom subtle legibility gradient + user dim
        val totalDim = (dimAlpha + extraOverlayDim).coerceIn(0f, 0.88f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = (totalDim * 0.9f).coerceAtMost(0.85f)),
                            Color.Black.copy(alpha = totalDim * 0.65f),
                            Color.Black.copy(alpha = (totalDim + 0.12f).coerceAtMost(0.9f))
                        )
                    )
                )
        )
    }
}
