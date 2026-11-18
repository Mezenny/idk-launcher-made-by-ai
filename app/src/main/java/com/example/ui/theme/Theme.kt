package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.ThemePreset

@Immutable
data class ZstarExtendedColors(
    val glassSurface: Color,
    val glassBorder: Color,
    val dockSurface: Color,
    val homeLabelColor: Color,
    val accentGlow: Color,
    val widgetCardSurface: Color,
    val isDarkPreset: Boolean
)

val LocalZstarColors = staticCompositionLocalOf {
    ZstarExtendedColors(
        glassSurface = Color(0xB811192E),
        glassBorder = Color(0x38FFFFFF),
        dockSurface = Color(0xB80E1629),
        homeLabelColor = Color(0xFFF8FAFC),
        accentGlow = StarlightPrimary,
        widgetCardSurface = Color(0xA60F172A),
        isDarkPreset = true
    )
}

@Composable
fun ZstarLauncherTheme(
    themePreset: ThemePreset = ThemePreset.STARLIGHT_OBSIDIAN,
    dockGlassOpacity: Float = 0.72f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val colorScheme: ColorScheme = when (themePreset) {
        ThemePreset.STARLIGHT_OBSIDIAN -> darkColorScheme(
            primary = StarlightPrimary,
            onPrimary = StarlightOnPrimary,
            secondary = StarlightSecondary,
            onSecondary = Color(0xFF281900),
            tertiary = Color(0xFF60A5FA),
            background = StarlightSurface,
            onBackground = StarlightText,
            surface = StarlightSurface,
            onSurface = StarlightText,
            surfaceVariant = StarlightSurfaceVariant,
            onSurfaceVariant = StarlightMuted,
            outline = Color(0xFF2B3B5E)
        )
        ThemePreset.FROSTED_GLASS -> darkColorScheme(
            primary = FrostedPrimary,
            onPrimary = FrostedOnPrimary,
            secondary = FrostedSecondary,
            onSecondary = Color(0xFF1F123A),
            tertiary = Color(0xFF99F6E4),
            background = FrostedSurface,
            onBackground = Color(0xFFF1F5F9),
            surface = FrostedSurface,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = FrostedSurfaceVariant,
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = Color(0xFF475569)
        )
        ThemePreset.NEON_CYBER -> darkColorScheme(
            primary = NeonPrimary,
            onPrimary = NeonOnPrimary,
            secondary = NeonSecondary,
            onSecondary = Color(0xFF33061C),
            tertiary = NeonTertiary,
            background = NeonSurface,
            onBackground = Color(0xFFFDFEFE),
            surface = NeonSurface,
            onSurface = Color(0xFFFDFEFE),
            surfaceVariant = NeonSurfaceVariant,
            onSurfaceVariant = Color(0xFFA1A1AA),
            outline = Color(0xFF2B2B3C)
        )
        ThemePreset.SOLAR_WARM -> lightColorScheme(
            primary = SandPrimary,
            onPrimary = SandOnPrimary,
            secondary = SandSecondary,
            onSecondary = Color.White,
            tertiary = Color(0xFFB45309),
            background = SandSurface,
            onBackground = SandText,
            surface = SandSurface,
            onSurface = SandText,
            surfaceVariant = SandSurfaceVariant,
            onSurfaceVariant = SandMuted,
            outline = Color(0xFFD6CEC0)
        )
        ThemePreset.DYNAMIC_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                darkColorScheme(
                    primary = StarlightPrimary,
                    secondary = StarlightSecondary,
                    surface = StarlightSurface,
                    surfaceVariant = StarlightSurfaceVariant
                )
            }
        }
    }

    val isDark = when (themePreset) {
        ThemePreset.SOLAR_WARM -> false
        ThemePreset.DYNAMIC_YOU -> systemDark
        else -> true
    }

    val extended = when (themePreset) {
        ThemePreset.STARLIGHT_OBSIDIAN -> ZstarExtendedColors(
            glassSurface = Color(0xFF0E1629).copy(alpha = 0.78f),
            glassBorder = Color.White.copy(alpha = 0.16f),
            dockSurface = Color(0xFF0B1222).copy(alpha = dockGlassOpacity),
            homeLabelColor = Color(0xFFF8FAFC),
            accentGlow = StarlightPrimary,
            widgetCardSurface = Color(0xFF0F182D).copy(alpha = 0.74f),
            isDarkPreset = true
        )
        ThemePreset.FROSTED_GLASS -> ZstarExtendedColors(
            glassSurface = Color(0xFF1E293B).copy(alpha = 0.62f),
            glassBorder = Color.White.copy(alpha = 0.28f),
            dockSurface = Color(0xFF1E293B).copy(alpha = dockGlassOpacity * 0.88f),
            homeLabelColor = Color.White,
            accentGlow = FrostedPrimary,
            widgetCardSurface = Color(0xFF1E293B).copy(alpha = 0.56f),
            isDarkPreset = true
        )
        ThemePreset.NEON_CYBER -> ZstarExtendedColors(
            glassSurface = Color(0xFF090910).copy(alpha = 0.86f),
            glassBorder = NeonPrimary.copy(alpha = 0.38f),
            dockSurface = Color(0xFF07070C).copy(alpha = dockGlassOpacity),
            homeLabelColor = Color(0xFFFDFEFE),
            accentGlow = NeonSecondary,
            widgetCardSurface = Color(0xFF0A0A12).copy(alpha = 0.82f),
            isDarkPreset = true
        )
        ThemePreset.SOLAR_WARM -> ZstarExtendedColors(
            glassSurface = Color(0xFFF7F4EC).copy(alpha = 0.88f),
            glassBorder = Color(0xFF1C1917).copy(alpha = 0.12f),
            dockSurface = Color(0xFFF5F0E4).copy(alpha = dockGlassOpacity),
            homeLabelColor = Color.White,
            accentGlow = SandSecondary,
            widgetCardSurface = Color(0xFFFBF8F1).copy(alpha = 0.84f),
            isDarkPreset = false
        )
        ThemePreset.DYNAMIC_YOU -> ZstarExtendedColors(
            glassSurface = colorScheme.surfaceVariant.copy(alpha = 0.78f),
            glassBorder = colorScheme.outline.copy(alpha = 0.35f),
            dockSurface = colorScheme.surface.copy(alpha = dockGlassOpacity),
            homeLabelColor = Color.White,
            accentGlow = colorScheme.primary,
            widgetCardSurface = colorScheme.surfaceVariant.copy(alpha = 0.72f),
            isDarkPreset = isDark
        )
    }

    CompositionLocalProvider(LocalZstarColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ZstarTypography,
            content = content
        )
    }
}
