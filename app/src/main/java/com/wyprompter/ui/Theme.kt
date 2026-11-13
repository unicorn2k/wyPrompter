package com.wyprompter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF2563EB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFDBEAFE)
private val OnPrimaryContainer = Color(0xFF1E3A8A)
private val Secondary = Color(0xFF0D7A6A)
private val Background = Color(0xFFF4F6FB)
private val Surface = Color(0xFFFFFFFF)
private val SurfaceVariant = Color(0xFFE7ECF5)
private val OnSurfaceVariant = Color(0xFF43535F)
private val Outline = Color(0xFFB7C0CD)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnPrimary,
    surface = Surface,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    background = Background,
    onBackground = Color(0xFF1C1B1F),
    outline = Outline
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}