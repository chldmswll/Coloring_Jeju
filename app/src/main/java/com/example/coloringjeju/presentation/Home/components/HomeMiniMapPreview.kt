package com.example.coloringjeju.presentation.Home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/** A plain marker on [HomeMiniMapPreview] — no label, just a colored (or outlined) dot. */
data class MiniMapDot(val fillColor: Color?, val xFraction: Float, val yFraction: Float)

/**
 * The compact, label-free map preview shown above the "내 지도에 여행지 추가하기" sheet, e.g. while
 * building out MY 지도.
 */
@Composable
fun HomeMiniMapPreview(dots: List<MiniMapDot>, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(ColoringTheme.shapes.xl)
            .background(colors.mint),
    ) {
        dots.forEach { dot ->
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * dot.xFraction - 18.dp,
                        y = maxHeight * dot.yFraction - 18.dp,
                    )
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(dot.fillColor ?: colors.white)
                    .border(1.5.dp, if (dot.fillColor != null) colors.forest else colors.border, CircleShape),
            )
        }
    }
}
