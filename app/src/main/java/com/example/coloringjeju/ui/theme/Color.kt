package com.example.coloringjeju.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/* ============================================================
 * Coloring Korea · Design System — Color tokens
 * Single source of truth. Mirrors tokens.css / tokens.json 1:1.
 * Do not use raw hex values in feature code — add a token here first.
 * ============================================================ */

// ---------- Color / Brand ----------
val BrandPrimary = Color(0xFF4E9E6E)
val BrandPrimaryDark = Color(0xFF2D8445)
val BrandPrimaryDeepest = Color(0xFF1F5C30)
val BrandForest = Color(0xFF33413C)

// ---------- Color / Accent ----------
val AccentYellow = Color(0xFFFFC42F)
val AccentTeal = Color(0xFF3FA99B)
val AccentTealLight = Color(0xFF5FC1B3)
val AccentPink = Color(0xFFF7A8A8)
val AccentOrange = Color(0xFFF0916A)

// ---------- Color / Surface ----------
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceCream = Color(0xFFFFFCEC)
val SurfaceMint = Color(0xFFE9F2E4)
val SurfaceOffWhite = Color(0xFFF8F6EF)
val SurfaceBorder = Color(0xFFE3E3E3)

// ---------- Color / Text ----------
val TextPrimary = Color(0xFF2B2E24)
val TextSecondary = Color(0xFF7A7E70)
val TextTertiary = Color(0xFFABAFA2)

/**
 * Role-based color tokens, grouped exactly like tokens.json (`color.brand.*`,
 * `color.accent.*`, `color.surface.*`, `color.text.*`).
 *
 * Usage guide (see README):
 * - [primary] (green): primary action buttons, verified/complete state, progress fill only.
 * - [yellow]: active tab indicator, active segment/chip, floating action (+) — "currently selected" only.
 * - [mint]: success/complete card or banner background. Default content cards use white + border.
 * - text hierarchy: [textPrimary] for headings/core body, [textSecondary] for descriptions,
 *   [textTertiary] for captions/placeholders/disabled state only.
 */
@Immutable
data class ColoringColors(
    // Brand
    val primary: Color = BrandPrimary,
    val primaryDark: Color = BrandPrimaryDark,
    val primaryDeepest: Color = BrandPrimaryDeepest,
    val forest: Color = BrandForest,
    // Accent
    val yellow: Color = AccentYellow,
    val teal: Color = AccentTeal,
    val tealLight: Color = AccentTealLight,
    val pink: Color = AccentPink,
    val orange: Color = AccentOrange,
    // Surface
    val white: Color = SurfaceWhite,
    val cream: Color = SurfaceCream,
    val mint: Color = SurfaceMint,
    val offWhite: Color = SurfaceOffWhite,
    val border: Color = SurfaceBorder,
    // Text
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextTertiary,
)

val LocalColoringColors = staticCompositionLocalOf { ColoringColors() }
