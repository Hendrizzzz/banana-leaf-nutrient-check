package com.bananaleafnutrientcheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    secondary = Color(0xFF735C00),
    onSecondary = Color.White,
    tertiary = Color(0xFF38656A),
    onTertiary = Color.White,
    background = Color(0xFFF7FBF1),
    onBackground = Color(0xFF191D17),
    surface = Color.White,
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFE0E4DA),
    onSurfaceVariant = Color(0xFF41493E),
    outlineVariant = Color(0xFFC0C9BB),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF96D8A3),
    onPrimary = Color(0xFF003916),
    secondary = Color(0xFFE2C46C),
    onSecondary = Color(0xFF3C2F00),
    tertiary = Color(0xFFA0D0D5),
    onTertiary = Color(0xFF00363B),
    background = Color(0xFF11140F),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFE2E3DD),
)

@Composable
fun BananaLeafNutrientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
