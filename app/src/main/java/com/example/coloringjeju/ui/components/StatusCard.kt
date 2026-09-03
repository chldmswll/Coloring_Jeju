package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.status-card` — a full-width bordered mint card carrying a bold headline and a caption,
 * e.g. "인증 가능 지역이에요" / "현재 위치 · 목적지에서 약 40m" or "인증 성공! 🎉" / "70% 이상 일치 · ...".
 * Distinct from [StatusBanner] (a compact pill) — use this where the confirmation needs its own block.
 */
@Composable
fun BorderedStatusCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ColoringTheme.shapes.lg)
            .background(colors.mint)
            .border(1.5.dp, colors.primary, ColoringTheme.shapes.lg)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            style = ColoringTheme.typography.subtitle,
            color = colors.primaryDeepest,
            textAlign = TextAlign.Center,
        )
        Text(
            subtitle,
            style = ColoringTheme.typography.caption,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
