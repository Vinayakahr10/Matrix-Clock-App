package com.dotmatrix.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dotmatrix.app.viewmodel.FontFamilyOption
import com.dotmatrix.app.viewmodel.FontSizeOption

// ── Size scale factors ────────────────────────────────────────────────────────
private fun Float.scale(option: FontSizeOption): Float = when (option) {
    FontSizeOption.SMALL  -> this * 0.875f
    FontSizeOption.MEDIUM -> this
    FontSizeOption.LARGE  -> this * 1.175f
}

// ── Font family resolver ──────────────────────────────────────────────────────
fun resolveFontFamily(option: FontFamilyOption): FontFamily = when (option) {
    FontFamilyOption.DEFAULT   -> FontFamily.Default
    FontFamilyOption.SERIF     -> FontFamily.Serif
    FontFamilyOption.MONOSPACE -> FontFamily.Monospace
    FontFamilyOption.ROUNDED   -> FontFamily.Cursive
}

// ── Static default (used as fallback) ────────────────────────────────────────
val Typography = buildTypography(FontSizeOption.MEDIUM, FontFamilyOption.DEFAULT)

// ── Dynamic builder ───────────────────────────────────────────────────────────
// NOTE: Colors are intentionally NOT set here — they are inherited from
// MaterialTheme.colorScheme at the composable level so they adapt to every theme mode.
fun buildTypography(sizeOption: FontSizeOption, familyOption: FontFamilyOption): Typography {
    val ff = resolveFontFamily(familyOption)
    return Typography(
        displayLarge = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.Bold,
            fontSize      = 96f.scale(sizeOption).sp,
            lineHeight    = 102f.scale(sizeOption).sp,
            letterSpacing = (-1.0).sp,
        ),
        displayMedium = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.Bold,
            fontSize      = 56f.scale(sizeOption).sp,
            lineHeight    = 64f.scale(sizeOption).sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.SemiBold,
            fontSize      = 36f.scale(sizeOption).sp,
            lineHeight    = 44f.scale(sizeOption).sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.Bold,
            fontSize      = 32f.scale(sizeOption).sp,
            lineHeight    = 40f.scale(sizeOption).sp,
            letterSpacing = 0.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.Medium,
            fontSize      = 18f.scale(sizeOption).sp,
            lineHeight    = 26f.scale(sizeOption).sp,
            letterSpacing = 0.5.sp
            // color intentionally omitted — inherits from onSurface
        ),
        bodyMedium = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.Medium,
            fontSize      = 16f.scale(sizeOption).sp,
            lineHeight    = 24f.scale(sizeOption).sp,
            letterSpacing = 0.25.sp
            // color intentionally omitted
        ),
        bodySmall = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.Medium,
            fontSize      = 14f.scale(sizeOption).sp,
            lineHeight    = 20f.scale(sizeOption).sp,
            letterSpacing = 0.4.sp
            // color intentionally omitted
        ),
        titleLarge = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.ExtraBold,
            fontSize      = 26f.scale(sizeOption).sp,
            lineHeight    = 32f.scale(sizeOption).sp,
            letterSpacing = 0.sp
            // color intentionally omitted
        ),
        titleMedium = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.Bold,
            fontSize      = 20f.scale(sizeOption).sp,
            lineHeight    = 28f.scale(sizeOption).sp,
            letterSpacing = 0.15.sp
            // color intentionally omitted
        ),
        labelLarge = TextStyle(
            fontFamily    = ff,
            fontWeight    = FontWeight.Bold,
            fontSize      = 16f.scale(sizeOption).sp,
            lineHeight    = 24f.scale(sizeOption).sp,
            letterSpacing = 0.1.sp,
        )
    )
}
