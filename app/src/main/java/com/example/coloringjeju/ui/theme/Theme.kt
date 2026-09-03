package com.example.coloringjeju.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Maps design-system tokens onto an M3 [androidx.compose.material3.ColorScheme] so any stock
 * Material3 component (Scaffold, TextField, Snackbar, ...) also inherits the brand palette.
 * Prefer [ColoringTheme.colors] for anything the style guide defines explicitly.
 */
private fun coloringColorScheme(colors: ColoringColors) = lightColorScheme(
    primary = colors.primary,
    onPrimary = colors.white,
    primaryContainer = colors.mint,
    onPrimaryContainer = colors.primaryDeepest,
    secondary = colors.teal,
    onSecondary = colors.white,
    secondaryContainer = colors.mint,
    onSecondaryContainer = colors.primaryDeepest,
    tertiary = colors.yellow,
    onTertiary = colors.forest,
    background = colors.offWhite,
    onBackground = colors.textPrimary,
    surface = colors.white,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.offWhite,
    onSurfaceVariant = colors.textSecondary,
    outline = colors.border,
    outlineVariant = colors.border,
)

/**
 * Root theme for the Coloring Korea design system.
 *
 * The reference tokens define a single palette (no dark-mode variant), so this theme is
 * intentionally not dark/dynamic-color aware — extend [coloringColorScheme] first if a dark
 * palette is added to tokens.css down the line.
 *
 * Access tokens anywhere below this via [ColoringTheme], e.g. `ColoringTheme.colors.primary`,
 * `ColoringTheme.spacing.space4`, `ColoringTheme.shapes.full`.
 */
@Composable
fun ColoringJejuTheme(content: @Composable () -> Unit) {
    val colors = ColoringColors()
    val spacing = ColoringSpacing()
    val radius = ColoringRadius()
    val shapes = ColoringShapes()
    val shadows = ColoringShadows()
    val typography = ColoringTypography()

    CompositionLocalProvider(
        LocalColoringColors provides colors,
        LocalColoringSpacing provides spacing,
        LocalColoringRadius provides radius,
        LocalColoringShapes provides shapes,
        LocalColoringShadows provides shadows,
        LocalColoringTypography provides typography,
    ) {
        // Note: M3's Shapes(...) constructor is internal in this Compose version, so the
        // Material shape scale is left at its default here. Use ColoringTheme.shapes directly
        // (as every component in ui/components does) to apply the design-system radii.
        MaterialTheme(
            colorScheme = coloringColorScheme(colors),
            typography = coloringMaterialTypography(colors, typography),
            content = content,
        )
    }
}

/** Single entry point for design tokens, mirroring `MaterialTheme.*` ergonomics. */
object ColoringTheme {
    val colors: ColoringColors
        @Composable get() = LocalColoringColors.current

    val spacing: ColoringSpacing
        @Composable get() = LocalColoringSpacing.current

    val radius: ColoringRadius
        @Composable get() = LocalColoringRadius.current

    val shapes: ColoringShapes
        @Composable get() = LocalColoringShapes.current

    val shadows: ColoringShadows
        @Composable get() = LocalColoringShadows.current

    val typography: ColoringTypography
        @Composable get() = LocalColoringTypography.current
}
