package com.example.coloringjeju.presentation.Home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * "오늘의 무지개" — today's rainbow. One dot per collectible color; a `null` entry renders as the
 * uncollected outline state. The caption ("6색 중 3색 수집") is derived from the list so it can
 * never drift out of sync with the dots themselves.
 */
@Composable
fun RainbowProgressRow(colors: List<Color?>, modifier: Modifier = Modifier) {
    val theme = ColoringTheme.colors
    val collected = colors.count { it != null }
    Column(modifier = modifier) {
        Text(
            "오늘의 무지개 · ${colors.size}색 중 ${collected}색 수집",
            style = ColoringTheme.typography.caption,
            color = theme.textSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            colors.forEach { fill ->
                RainbowDot(fill)
            }
        }
    }
}

@Composable
private fun RainbowDot(fill: Color?) {
    val colors = ColoringTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(fill ?: colors.white)
            .border(2.dp, fill ?: colors.textSecondary, CircleShape),
    )
}
