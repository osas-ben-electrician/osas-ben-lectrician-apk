package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val OsasNavyColorScheme = darkColorScheme(
    primary = SafetyYellow,
    onPrimary = NavyDeepest,
    primaryContainer = NavySurfaceElevated,
    onPrimaryContainer = WhiteText,
    secondary = EmergencyRed,
    onSecondary = WhiteText,
    secondaryContainer = EmergencyRedContainer,
    onSecondaryContainer = WhiteText,
    tertiary = InfoSky,
    onTertiary = NavyDeepest,
    background = NavyBackground,
    onBackground = WhiteText,
    surface = NavySurface,
    onSurface = WhiteText,
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = SlateTextSecondary,
    outline = NavyBorder,
    outlineVariant = NavySurfaceElevated,
    error = EmergencyRedBright,
    onError = WhiteText,
    errorContainer = EmergencyRedContainer,
    onErrorContainer = WhiteText
)

val OsasShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OsasBenTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OsasNavyColorScheme,
        typography = OsasTypography,
        shapes = OsasShapes,
        content = content
    )
}
