package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


private val DarkColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = Color(0xFF381E72),
    onPrimaryContainer = ImmersivePrimary,
    secondary = ImmersiveOnSurfaceVariant,
    onSecondary = ImmersiveBackground,
    background = ImmersiveBackground,
    onBackground = ImmersiveOnBackground,
    surface = ImmersiveSurface,
    onSurface = ImmersiveOnBackground,
    surfaceVariant = ImmersiveSurface,
    onSurfaceVariant = ImmersiveOnSurfaceVariant,
    outline = ImmersiveBorder
)

private val LightColorScheme = DarkColorScheme // Keep dark mode default to look immersive in all states

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for the immersive aesthetic
  dynamicColor: Boolean = false, // Disable wallpaper overriding to preserve styled brand integrity
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
