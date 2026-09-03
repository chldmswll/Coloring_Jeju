package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.pin-dot` + `.pin-label` — a single map legend entry (e.g. 협재 / 한라산 / 성산일출봉).
 * Pass `fillColor = null` for the outlined "unvisited" pin.
 */
@Composable
fun MapPinItem(
    label: String,
    fillColor: Color?,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(fillColor ?: colors.white)
                .then(
                    if (fillColor == null) Modifier.border(1.5.dp, colors.border, CircleShape) else Modifier,
                ),
        )
        Text(label, style = ColoringTheme.typography.caption, color = colors.textSecondary)
    }
}

/** Row of [MapPinItem]s, mirroring the style guide's `.pin-row` demo. */
@Composable
fun MapPinRow(items: List<Pair<String, Color?>>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        items.forEach { (label, color) -> MapPinItem(label = label, fillColor = color) }
    }
}
