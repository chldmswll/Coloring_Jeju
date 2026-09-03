package com.example.coloringjeju.presentation.VerifyResult.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/** "목표 ↔ 내 사진" — the target color next to the color actually captured, e.g. on `인증 결과`. */
@Composable
fun CompareColorRow(targetColor: Color, capturedColor: Color, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorLabel("목표", targetColor)
        Text("↔", style = ColoringTheme.typography.title, color = colors.textTertiary)
        ColorLabel("내 사진", capturedColor)
    }
}

@Composable
private fun ColorLabel(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = ColoringTheme.typography.caption, color = ColoringTheme.colors.textSecondary)
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(56.dp)
                .clip(ColoringTheme.shapes.md)
                .background(color),
        )
    }
}
