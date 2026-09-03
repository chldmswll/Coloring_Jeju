package com.example.coloringjeju.presentation.Stamp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * The bare progress track under the "스탬프" header — unlike [com.example.coloringjeju.ui.components.LinearProgress]
 * it carries no caption, since the header's "3 / 6 완료" subtitle already says that.
 */
@Composable
fun StampProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(ColoringTheme.shapes.full)
            .background(colors.border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(ColoringTheme.shapes.full)
                .background(colors.primary),
        )
    }
}
