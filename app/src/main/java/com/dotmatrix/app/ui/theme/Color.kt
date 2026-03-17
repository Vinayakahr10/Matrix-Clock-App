package com.dotmatrix.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Google Material Design 3 Palette ──────────────────────────────────────────

// Google Blue primary
val GoogleBlue         = Color(0xFF1A73E8)
val GoogleBlueHover    = Color(0xFF1557B0)
val GoogleBlueContainer= Color(0xFFE8F0FE)
val OnGoogleBlue       = Color(0xFFFFFFFF)

// Light theme surfaces
val BackgroundLight    = Color(0xFFF8F9FA)   // Google gray background
val SurfaceLight       = Color(0xFFFFFFFF)   // pure white cards
val SurfaceVariantLight= Color(0xFFF1F3F4)   // chip / tab background
val OutlineLight       = Color(0xFFDADCE0)   // dividers / borders

// Light theme text
val TextPrimary        = Color(0xFF202124)   // Google "almost black"
val TextSecondary      = Color(0xFF5F6368)   // Google secondary gray

// Dark theme
val BackgroundDark     = Color(0xFF121212)
val SurfaceDark        = Color(0xFF1E1E1E)
val SurfaceVariantDark = Color(0xFF2C2C2C)
val GoogleBlueDark     = Color(0xFF7BAAF7)   // accessible on dark
val GoogleBlueContainerDark = Color(0xFF1A3461)
val TextPrimaryDark    = Color(0xFFE8EAED)
val TextSecondaryDark  = Color(0xFF9AA0A6)

// Semantic
val DangerRed          = Color(0xFFD93025)   // Google red
val DangerLight        = Color(0xFFFCE8E6)
val SuccessGreen       = Color(0xFF137333)   // Google green
val SuccessLight       = Color(0xFFE6F4EA)

// Aliases kept for backward-compat
val CustomCyan         = GoogleBlue
val CustomCyanHover    = GoogleBlueHover
val CustomCyanLight    = GoogleBlueContainer
val CardBackground     = SurfaceLight
val BorderLight        = OutlineLight
val CyanDark           = GoogleBlueDark
val CyanContainerDark  = GoogleBlueContainerDark
val CardBackgroundDark = SurfaceDark
val TextSecondaryDark  = TextSecondaryDark
