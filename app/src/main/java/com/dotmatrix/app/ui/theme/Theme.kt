package com.dotmatrix.app.ui.theme

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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dotmatrix.app.viewmodel.FontFamilyOption
import com.dotmatrix.app.viewmodel.FontSizeOption
import com.dotmatrix.app.viewmodel.ThemeMode

// ── Color schemes ─────────────────────────────────────────────────────────────

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
    onPrimary            = OnGoogleBlue,
    primaryContainer     = GoogleBlueContainerDark,
    onPrimaryContainer   = GoogleBlueDark,
    secondary            = GoogleBlueDark,
    onSecondary          = OnGoogleBlue,
    secondaryContainer   = GoogleBlueContainerDark,
    onSecondaryContainer = GoogleBlueDark,
    background           = BackgroundDark,
    onBackground         = TextPrimaryDark,
    surface              = SurfaceDark,
    onSurface            = TextPrimaryDark,
    surfaceVariant       = SurfaceVariantDark,
    onSurfaceVariant     = TextSecondaryDark,
    outline              = SurfaceVariantDark,
    error                = DangerRed,
    onError              = OnGoogleBlue
)

private val PitchDarkColorScheme = darkColorScheme(
    primary              = GoogleBluePitchDark,
    onPrimary            = OnGoogleBlue,
    primaryContainer     = GoogleBlueContainerDark,
    onPrimaryContainer   = GoogleBluePitchDark,
    secondary            = GoogleBluePitchDark,
    onSecondary          = OnGoogleBlue,
    secondaryContainer   = GoogleBlueContainerDark,
    onSecondaryContainer = GoogleBluePitchDark,
    background           = BackgroundPitchDark,
    onBackground         = TextPrimaryPitchDark,
    surface              = SurfacePitchDark,
    onSurface            = TextPrimaryPitchDark,
    surfaceVariant       = SurfaceVariantPitchDark,
    onSurfaceVariant     = TextSecondaryPitchDark,
    outline              = SurfaceVariantPitchDark,
    error                = DangerRed,
    onError              = OnGoogleBlue
)

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun DotMatrixAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontSize: FontSizeOption = FontSizeOption.MEDIUM,
    fontFamily: FontFamilyOption = FontFamilyOption.DEFAULT,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT      -> false
        ThemeMode.DARK       -> true
        ThemeMode.PITCH_DARK -> true
        ThemeMode.SYSTEM     -> systemInDark
    }

    val colorScheme = when (themeMode) {
        ThemeMode.PITCH_DARK -> PitchDarkColorScheme
        ThemeMode.DARK       -> DarkColorScheme
        ThemeMode.LIGHT      -> LightColorScheme
        ThemeMode.SYSTEM     -> {
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (systemInDark) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                systemInDark -> DarkColorScheme
                else         -> LightColorScheme
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    val typography = remember(fontSize, fontFamily) { buildTypography(fontSize, fontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        content     = content
    )
}

// ── Extension ─────────────────────────────────────────────────────────────────
// Keep backward compat — old call sites that pass no args still compile.
@Composable
fun DotMatrixAppTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = DotMatrixAppTheme(
    themeMode   = ThemeMode.SYSTEM,
    fontSize    = FontSizeOption.MEDIUM,
    fontFamily  = FontFamilyOption.DEFAULT,
    dynamicColor = dynamicColor,
    content      = content
)
