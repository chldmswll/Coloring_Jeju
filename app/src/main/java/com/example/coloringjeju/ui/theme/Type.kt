package com.example.coloringjeju.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ---------- Typography ----------
 * Role-based tokens: display / title / subtitle / body / button / caption.
 * Same role -> same style everywhere in the app.
 *
 * Font is designed around Pretendard. No webfont is bundled here, so it falls back to the
 * system sans-serif (FontFamily.Default) — matching the CSS token's fallback stack. To use the
 * real font, drop the Pretendard files into res/font and point ColoringFontFamily at them.
 */
val ColoringFontFamily = FontFamily.Default

@Immutable
data class ColoringTypography(
    val display: TextStyle = TextStyle(
        fontFamily = ColoringFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.W800,
        lineHeight = 31.sp, // 24 * 1.3
    ),
    val title: TextStyle = TextStyle(
        fontFamily = ColoringFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp, // 18 * 1.35 (rounded)
    ),
    val subtitle: TextStyle = TextStyle(
        fontFamily = ColoringFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp, // 14 * 1.4
    ),
    val body: TextStyle = TextStyle(
        fontFamily = ColoringFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp, // 14 * 1.5
    ),
    val button: TextStyle = TextStyle(
        fontFamily = ColoringFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 19.sp, // 16 * 1.2
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = ColoringFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 17.sp, // 12 * 1.4
    ),
)

val LocalColoringTypography = staticCompositionLocalOf { ColoringTypography() }

/** Maps role-based tokens onto Material3's [Typography] slots so stock M3 components inherit the brand type scale. */
fun coloringMaterialTypography(
    colors: ColoringColors,
    type: ColoringTypography,
): Typography = Typography(
    displaySmall = type.display.copy(color = colors.textPrimary),
    headlineSmall = type.display.copy(color = colors.textPrimary),
    titleLarge = type.title.copy(color = colors.textPrimary),
    titleMedium = type.subtitle.copy(color = colors.textPrimary),
    titleSmall = type.subtitle.copy(color = colors.textPrimary),
    bodyLarge = type.body.copy(color = colors.textPrimary),
    bodyMedium = type.body.copy(color = colors.textSecondary),
    bodySmall = type.caption.copy(color = colors.textSecondary),
    labelLarge = type.button.copy(color = colors.textPrimary),
    labelMedium = type.caption.copy(color = colors.textSecondary),
    labelSmall = type.caption.copy(color = colors.textTertiary),
)
