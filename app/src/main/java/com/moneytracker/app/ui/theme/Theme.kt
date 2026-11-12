package com.moneytracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    onPrimary = Color.White,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TextPrimaryDark,
    secondary = IncomeGreen,
    onSecondary = Color.Black,
    error = ExpenseRed,
    onError = Color.White,
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNeon,
    onPrimary = Color.White,
    primaryContainer = SurfaceElevatedLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = IncomeGreen,
    onSecondary = Color.White,
    error = ExpenseRed,
    onError = Color.White,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight
)

@Composable
fun MoneyTrackerTheme(
    darkTheme: Boolean = true, // Default to sleek fintech dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
