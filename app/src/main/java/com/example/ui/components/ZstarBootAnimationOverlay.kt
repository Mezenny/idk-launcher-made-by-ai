package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ZstarBootAnimationOverlay(
    reduceMotion: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var skipped by remember { mutableStateOf(false) }

    BackHandler {
        skipped = true
        onFinished()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Multi-stage MIUI / HyperOS-inspired animation timeline
    val glowProgress = remember { Animatable(0f) }
    val ringSweep = remember { Animatable(0f) }
    val prismScale = remember { Animatable(0.45f) }
    val prismAlpha = remember { Animatable(0f) }
    val wordmarkProgress = remember { Animatable(0f) }
    val shimmerSweep = remember { Animatable(-0.5f) }
    val bootBarProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            prismScale.snapTo(1f)
            prismAlpha.animateTo(1f, tween(220))
            wordmarkProgress.animateTo(1f, tween(220))
            bootBarProgress.animateTo(1f, tween(280))
            delay(180)
            exitAlpha.animateTo(0f, tween(200))
            onFinished()
            return@LaunchedEffect
        }

        // Phase 1: Ignition, halo ring sweep & faceted star spring-in
        coroutineScope {
            launch {
                glowProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            }
            launch {
                ringSweep.animateTo(1f, tween(820, easing = FastOutSlowInEasing))
            }
            launch {
                prismAlpha.animateTo(1f, tween(420))
            }
            launch {
                prismScale.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            // Phase 2: Wordmark compression reveal + light shimmer
            launch {
                delay(380)
                wordmarkProgress.animateTo(1f, tween(760, easing = FastOutSlowInEasing))
            }
            launch {
                delay(620)
                shimmerSweep.animateTo(1.5f, tween(850, easing = LinearEasing))
            }
            // Phase 3: Workspace boot progress beam
            launch {
                delay(250)
                bootBarProgress.animateTo(1f, tween(1650, easing = FastOutSlowInEasing))
            }
        }

        if (!skipped) {
            delay(220)
            // Phase 4: Seamless zoom-dissolve into live Home Screen
            coroutineScope {
                launch { exitScale.animateTo(1.07f, tween(380, easing = FastOutSlowInEasing)) }
                launch { exitAlpha.animateTo(0f, tween(360, easing = FastOutSlowInEasing)) }
            }
            onFinished()
        }
    }

    val statusLabel = when {
        bootBarProgress.value < 0.45f -> "INITIALIZING ZSTAR ENGINE"
        bootBarProgress.value < 0.88f -> "OPTIMIZING ICON CACHE & GESTURES"
        else -> "WORKSPACE READY"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exitAlpha.value
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
            .background(Color(0xFF05070E))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (bootBarProgress.value > 0.25f && !skipped) {
                    skipped = true
                    onFinished()
                }
            }
            .testTag("zstar_boot_animation_overlay")
    ) {
        // Atmospheric Canvas: radial nebula bloom, sweeping orbital ring & faceted Zstar emblem
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.43f)
            val maxGlowRadius = size.minDimension * 0.58f * (0.4f + 0.6f * glowProgress.value)

            // Deep ambient bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.26f * glowProgress.value),
                        secondaryColor.copy(alpha = 0.08f * glowProgress.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxGlowRadius.coerceAtLeast(1f)
                ),
                radius = maxGlowRadius,
                center = center
            )

            // Sweeping orbital arc
            val ringRadius = 92.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.9f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = 360f * ringSweep.value,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = androidx.compose.ui.geometry.Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Outer subtle hairline halo
            if (glowProgress.value > 0.2f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f * glowProgress.value),
                    radius = ringRadius + 16.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Faceted 4-point Zstar geometric prism
            val arm = 54.dp.toPx() * prismScale.value
            val waist = 13.dp.toPx() * prismScale.value

            val starPath = Path().apply {
                moveTo(center.x, center.y - arm)
                lineTo(center.x + waist, center.y - waist)
                lineTo(center.x + arm, center.y)
                lineTo(center.x + waist, center.y + waist)
                lineTo(center.x, center.y + arm)
                lineTo(center.x - waist, center.y + waist)
                lineTo(center.x - arm, center.y)
                lineTo(center.x - waist, center.y - waist)
                close()
            }

            drawPath(
                path = starPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = prismAlpha.value),
                        Color.White.copy(alpha = 0.95f * prismAlpha.value),
                        secondaryColor.copy(alpha = prismAlpha.value)
                    ),
                    start = Offset(center.x - arm, center.y - arm),
                    end = Offset(center.x + arm, center.y + arm)
                )
            )

            // Diagonal specular gleam passing across emblem
            val gleamX = center.x + (shimmerSweep.value - 0.5f) * arm * 2.6f
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent)
                ),
                start = Offset(gleamX - 24.dp.toPx(), center.y - arm * 0.6f),
                end = Offset(gleamX + 24.dp.toPx(), center.y + arm * 0.6f),
                strokeWidth = 3.dp.toPx()
            )
        }

        // Centered Wordmark below the star emblem
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 195.dp)
                .graphicsLayer {
                    alpha = wordmarkProgress.value
                    translationY = (1f - wordmarkProgress.value) * 28f
                }
        ) {
            val dynamicTracking = (14f - 9.5f * wordmarkProgress.value).sp
            Text(
                text = "ZSTAR",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = dynamicTracking,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "HYPER LAUNCHER",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 3.5.sp,
                color = primaryColor.copy(alpha = 0.9f)
            )
        }

        // Bottom MIUI-inspired progress beam + status + tap hint
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 44.dp, start = 40.dp, end = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bootBarProgress.value.coerceIn(0.02f, 1f))
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(primaryColor, secondaryColor))
                        )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = statusLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}
