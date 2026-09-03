package com.example.coloringjeju.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringMotion
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.segmented` — pill-shaped tab switcher for two or three mutually exclusive views
 * (e.g. 추천 지도 / MY 지도). For 4+ independent filters use [FilterChipRow] instead.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Row(
        modifier = modifier
            .clip(ColoringTheme.shapes.full)
            .background(colors.white)
            .border(1.dp, colors.border, ColoringTheme.shapes.full)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isActive = option == selected
            val background by animateColorAsState(
                targetValue = if (isActive) colors.mint else colors.white.copy(alpha = 0f),
                animationSpec = ColoringMotion.baseTween(),
                label = "segmentBackground",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isActive) colors.primaryDeepest else colors.textSecondary,
                animationSpec = ColoringMotion.baseTween(),
                label = "segmentContent",
            )
            Text(
                text = label(option),
                style = ColoringTheme.typography.subtitle,
                color = contentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(ColoringTheme.shapes.full)
                    .background(background)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}

/** `.chip` — a single filter chip. Compose one per filter and lay them out in a [Row]/`FlowRow`. */
@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    val background = if (selected) colors.yellow else colors.white
    val borderColor = if (selected) colors.yellow else colors.border
    val contentColor = if (selected) colors.forest else colors.textSecondary

    Text(
        text = text,
        style = ColoringTheme.typography.subtitle,
        color = contentColor,
        modifier = modifier
            .clip(ColoringTheme.shapes.full)
            .background(background)
            .border(1.5.dp, borderColor, ColoringTheme.shapes.full)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * Convenience row of [FilterChip]s for a single-select filter list, mirroring the style guide's
 * `#chips` demo. Scrolls horizontally so it degrades gracefully once there are more options than
 * fit one screen width (e.g. TourAPI's category set).
 */
@Composable
fun <T> FilterChipRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                text = label(option),
                selected = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}
