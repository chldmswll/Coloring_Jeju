package com.example.coloringjeju.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ---------- Radius ----------
 * Buttons / chips / tabs -> full. Cards / sheets -> md~lg.
 */
@Immutable
data class ColoringRadius(
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val full: Dp = 999.dp,
)

val LocalColoringRadius = staticCompositionLocalOf { ColoringRadius() }

/** Ready-to-use [Shape]s matching [ColoringRadius]. `full` uses percent rounding so it stays pill-shaped at any height. */
@Immutable
data class ColoringShapes(
    val sm: Shape = RoundedCornerShape(8.dp),
    val md: Shape = RoundedCornerShape(12.dp),
    val lg: Shape = RoundedCornerShape(16.dp),
    val xl: Shape = RoundedCornerShape(20.dp),
    val full: Shape = RoundedCornerShape(percent = 50),
)

val LocalColoringShapes = staticCompositionLocalOf { ColoringShapes() }
