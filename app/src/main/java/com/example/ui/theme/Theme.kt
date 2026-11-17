package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SakuUsahaTheme(
    brandColor: Color = BrandGreenPrimary,
    themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = brandColor,
            onPrimary = Color.White,
            primaryContainer = brandColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = brandColor,
            onSecondary = Color.White,
            background = DarkBackground,
            onBackground = DarkTextPrimary,
            surface = DarkCard,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkTextMuted,
            outline = Color(0xFF2E3E3B)
        )
    } else {
        lightColorScheme(
            primary = brandColor,
            onPrimary = Color.White,
            primaryContainer = brandColor.copy(alpha = 0.12f),
            onPrimaryContainer = brandColor,
            secondary = brandColor,
            onSecondary = Color.White,
            background = BrandSurfaceCream,
            onBackground = TextInk,
            surface = Color.White,
            onSurface = TextInk,
            surfaceVariant = BrandSurfaceCreamDarker,
            onSurfaceVariant = TextMuted,
            outline = BorderLight
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
