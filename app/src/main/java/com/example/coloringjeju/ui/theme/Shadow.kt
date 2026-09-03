package com.example.coloringjeju.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ---------- Shadow ----------
 * CSS reference: sm 0 1px 2px rgba(43,46,36,.06) / md 0 4px 12px rgba(43,46,36,.08) / lg 0 8px 24px rgba(43,46,36,.12)
 * Compose has no blur-radius API on shadow(), so each token is approximated with an elevation
 * plus the same tinted, semi-transparent shadow color used in the CSS token.
 */
@Immutable
data class ColoringShadowToken(val elevation: Dp, val color: Color)

@Immutable
data class ColoringShadows(
    val sm: ColoringShadowToken = ColoringShadowToken(2.dp, TextPrimary.copy(alpha = 0.06f)),
    val md: ColoringShadowToken = ColoringShadowToken(6.dp, TextPrimary.copy(alpha = 0.08f)),
    val lg: ColoringShadowToken = ColoringShadowToken(12.dp, TextPrimary.copy(alpha = 0.12f)),
)

val LocalColoringShadows = staticCompositionLocalOf { ColoringShadows() }

/** Applies a [ColoringShadowToken] as this element's drop shadow, clipped to [shape]. */
fun Modifier.coloringShadow(
    token: ColoringShadowToken,
    shape: Shape = RoundedCornerShape(0.dp),
): Modifier = this.shadow(
    elevation = token.elevation,
    shape = shape,
    ambientColor = token.color,
    spotColor = token.color,
)
