package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/** `.status-banner` — a positive confirmation banner, e.g. "✓ 인증 성공!". */
@Composable
fun StatusBanner(text: String, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Text(
        text = text,
        style = ColoringTheme.typography.subtitle,
        color = colors.primaryDeepest,
        modifier = modifier
            .clip(ColoringTheme.shapes.full)
            .background(colors.mint)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

/** `.tag` — a small category label, e.g. on a stamp card or list item ("자연"). */
@Composable
fun ColoringTag(text: String, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Text(
        text = text,
        style = ColoringTheme.typography.caption,
        color = colors.primaryDark,
        modifier = modifier
            .clip(ColoringTheme.shapes.full)
            .background(colors.mint)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
