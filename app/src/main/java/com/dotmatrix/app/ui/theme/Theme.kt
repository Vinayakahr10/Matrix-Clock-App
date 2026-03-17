package com.dotmatrix.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary              = GoogleBlue,
    onPrimary            = OnGoogleBlue,
    primaryContainer     = GoogleBlueContainer,
    onPrimaryContainer   = GoogleBlueHover,
    secondary            = GoogleBlueHover,
    onSecondary          = OnGoogleBlue,
    secondaryContainer   = GoogleBlueContainer,
    onSecondaryContainer = GoogleBlue,
    background           = BackgroundLight,
    onBackground         = TextPrimary,
    surface              = SurfaceLight,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceVariantLight,
    onSurfaceVariant     = TextSecondary,
    outline              = OutlineLight,
    error                = DangerRed,
    onError              = OnGoogleBlue
)

private val DarkColorScheme = darkColorScheme(
    primary              = GoogleBlueDark,
    onPrimary            = BackgroundDark,
    primaryContainer     = GoogleBlueContainerDark,
    onPrimaryContainer   = GoogleBlueDark,
    secondary            = GoogleBlueDark,
    onSecondary          = BackgroundDark,
    background           = BackgroundDark,
    onBackground         = TextPrimaryDark,
    surface              = SurfaceDark,
    onSurface            = TextPrimaryDark,
    surfaceVariant       = SurfaceVariantDark,
    onSurfaceVariant     = TextSecondaryDark,
    error                = DangerRed,
    onError              = OnGoogleBlue
)

@Composable
fun DotMatrixAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
