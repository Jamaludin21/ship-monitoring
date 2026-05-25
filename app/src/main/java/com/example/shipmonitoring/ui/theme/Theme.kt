package com.example.shipmonitoring.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrightColorScheme = lightColorScheme(
    primary = SeaBlue,
    onPrimary = Color.White,
    primaryContainer = SeaBlueLight,
    onPrimaryContainer = SeaBlueDark,
    secondary = Aqua,
    onSecondary = Color.White,
    secondaryContainer = AquaLight,
    onSecondaryContainer = SeaBlueDark,
    tertiary = Coral,
    onTertiary = Color.White,
    background = Sky,
    onBackground = OnLight,
    surface = Color.White,
    onSurface = OnLight,
    surfaceVariant = Sand,
    onSurfaceVariant = OnLight,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun ShipMonitoringTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BrightColorScheme,
        typography = Typography,
        content = content
    )
}
