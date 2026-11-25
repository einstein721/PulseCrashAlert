package com.strathmore.predictivedispatch.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class DispatchColors(
    val background: Color,
    val surfaceCard: Color,
    val chipIdle: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val signalCrimson: Color,
    val emeraldLock: Color,
    val amberAlert: Color
)

val DarkDispatchColors = DispatchColors(
    background = Color(0xFF111318),     // Deep Obsidian
    surfaceCard = Color(0xFF1C2028),    // Slate Charcoal
    chipIdle = Color(0xFF111318),
    border = Color(0xFF2D3340),
    textPrimary = Color(0xFFF5F7FA),    // Crisp Off-White
    textSecondary = Color(0xFF9AA4B2),
    signalCrimson = Color(0xFFE53935),
    emeraldLock = Color(0xFF00C853),
    amberAlert = Color(0xFFFFB300)
)

val LightDispatchColors = DispatchColors(
    background = Color(0xFFF4F6F9),     // Cool Alabaster
    surfaceCard = Color(0xFFFFFFFF),    // Pure White Elevated Sheet
    chipIdle = Color(0xFFEEF1F6),       // Soft Mist Gray
    border = Color(0xFFD5DCE6),         // Crisp Hairline Border
    textPrimary = Color(0xFF111827),    // Deep Ink Charcoal
    textSecondary = Color(0xFF4B5563),  // Muted Slate
    signalCrimson = Color(0xFFD32F2F),  // High-Contrast Daylight Crimson
    emeraldLock = Color(0xFF00A844),
    amberAlert = Color(0xFFED8900)
)

@Composable
fun rememberDispatchColors(
    darkTheme: Boolean = isSystemInDarkTheme()
): DispatchColors = if (darkTheme) DarkDispatchColors else LightDispatchColors
