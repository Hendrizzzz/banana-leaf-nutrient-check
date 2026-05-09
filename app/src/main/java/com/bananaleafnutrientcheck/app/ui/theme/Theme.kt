package com.bananaleafnutrientcheck.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFACF4A4),
    onPrimaryContainer = Color(0xFF002203),
    secondary = Color(0xFF5F6368),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE0E6),
    onSecondaryContainer = Color(0xFF181C20),
    tertiary = Color(0xFF6E4B00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEAC),
    onTertiaryContainer = Color(0xFF281900),
    background = Color(0xFFF7FBF1),
    onBackground = Color(0xFF191D17),
    surface = Color.White,
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFE0E4DA),
    onSurfaceVariant = Color(0xFF41493E),
    outline = Color(0xFF717A6D),
    outlineVariant = Color(0xFFC0C9BB),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
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
    surfaceVariant = Color(0xFF41493E),
    onSurfaceVariant = Color(0xFFC4C8BE),
    outline = Color(0xFF8B9486),
    outlineVariant = Color(0xFF41493E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun BananaLeafNutrientTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
