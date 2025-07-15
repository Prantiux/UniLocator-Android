package com.pramanshav.unilocator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkPrimaryAccent.copy(alpha = 0.2f),
    onPrimaryContainer = DarkTextColor,
    secondary = DarkPrimaryAccent.copy(alpha = 0.7f),
    onSecondary = DarkBackground,
    secondaryContainer = DarkBorderDivider,
    onSecondaryContainer = DarkTextColor,
    tertiary = DarkPrimaryAccent.copy(alpha = 0.5f),
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextColor,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkBorderDivider,
    onSurfaceVariant = DarkTextColor.copy(alpha = 0.8f),
    outline = DarkBorderDivider,
    outlineVariant = DarkBorderDivider.copy(alpha = 0.5f),
    error = Error,
    onError = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryAccent,
    onPrimary = LightBackground,
    primaryContainer = LightSecondaryAccent.copy(alpha = 0.3f),
    onPrimaryContainer = LightTextColor,
    secondary = LightSecondaryAccent,
    onSecondary = LightBackground,
    secondaryContainer = LightBorderDivider,
    onSecondaryContainer = LightTextColor,
    tertiary = LightPrimaryAccent.copy(alpha = 0.7f),
    onTertiary = LightBackground,
    background = LightBackground,
    onBackground = LightTextColor,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightBorderDivider,
    onSurfaceVariant = LightTextColor.copy(alpha = 0.8f),
    outline = LightBorderDivider,
    outlineVariant = LightBorderDivider.copy(alpha = 0.5f),
    error = Error,
    onError = LightBackground
)

@Composable
fun UniLocatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = UniLocatorTypography,
        content = content
    )
}
