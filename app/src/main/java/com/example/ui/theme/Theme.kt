package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AgriGreenPrimaryDark,
    onPrimary = AgriGreenOnPrimaryDark,
    primaryContainer = AgriGreenPrimaryContainerDark,
    onPrimaryContainer = AgriGreenOnPrimaryContainerDark,
    secondary = AgriWheatSecondaryDark,
    onSecondary = AgriWheatOnSecondaryDark,
    secondaryContainer = AgriWheatSecondaryContainerDark,
    onSecondaryContainer = AgriWheatOnSecondaryContainerDark,
    tertiary = AgriSoilTertiaryDark,
    onTertiary = AgriSoilOnTertiaryDark,
    tertiaryContainer = AgriSoilTertiaryContainerDark,
    onTertiaryContainer = AgriSoilOnTertiaryContainerDark,
    background = AgriBackgroundDark,
    surface = AgriSurfaceDark,
    surfaceVariant = AgriSurfaceVariantDark,
    onBackground = AgriOnSurfaceDark,
    onSurface = AgriOnSurfaceDark,
    onSurfaceVariant = AgriOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = AgriGreenPrimary,
    onPrimary = AgriGreenOnPrimary,
    primaryContainer = AgriGreenPrimaryContainer,
    onPrimaryContainer = AgriGreenOnPrimaryContainer,
    secondary = AgriWheatSecondary,
    onSecondary = AgriWheatOnSecondary,
    secondaryContainer = AgriWheatSecondaryContainer,
    onSecondaryContainer = AgriWheatOnSecondaryContainer,
    tertiary = AgriSoilTertiary,
    onTertiary = AgriSoilOnTertiary,
    tertiaryContainer = AgriSoilTertiaryContainer,
    onTertiaryContainer = AgriSoilOnTertiaryContainer,
    background = AgriBackgroundLight,
    surface = AgriSurfaceLight,
    surfaceVariant = AgriSurfaceVariantLight,
    onBackground = AgriOnSurfaceLight,
    onSurface = AgriOnSurfaceLight,
    onSurfaceVariant = AgriOnSurfaceVariantLight
)

@Composable
fun ZariaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted farming palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
