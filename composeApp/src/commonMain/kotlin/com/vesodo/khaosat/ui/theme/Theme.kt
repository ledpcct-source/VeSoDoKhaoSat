package com.vesodo.khaosat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

val PrimaryBlue = Color(0xFF0284C7)
val SuccessGreen = Color(0xFF059669)
val DangerRed = Color(0xFFDC2626)
val WarningOrange = Color(0xFFD97706)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = Slate700,
    onSecondary = Color.White,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    onSurface = Slate900
)

@Composable
fun VeSoDoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
