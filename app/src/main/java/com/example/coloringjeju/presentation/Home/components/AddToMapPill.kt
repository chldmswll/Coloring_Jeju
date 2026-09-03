package com.example.coloringjeju.presentation.Home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * The compact pill beside a place's title on [PlaceDetailContent] — "+ MY 지도에 추가" (filled,
 * [isSaved] false) or "MY 지도에서 삭제" (outline, [isSaved] true). Kept as one component so both
 * states share sizing/shape; only the fill and label flip.
 */
@Composable
fun AddToMapPill(isSaved: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Text(
        if (isSaved) "MY 지도에서 삭제" else "+ MY 지도에 추가",
        style = ColoringTheme.typography.caption,
        color = if (isSaved) colors.textSecondary else colors.white,
        modifier = modifier
            .clip(ColoringTheme.shapes.full)
            .then(
                if (isSaved) {
                    Modifier.background(colors.white).border(1.5.dp, colors.border, ColoringTheme.shapes.full)
                } else {
                    Modifier.background(colors.primary)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
