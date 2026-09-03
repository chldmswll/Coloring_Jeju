package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.picker-dot` row — lets a user pick which coloring-map color they're marking a place with.
 * The selected dot gets a white ring + deepest-green outline and a check mark, per the style guide.
 * The rings are drawn outside the 40dp bounds (like the CSS `box-shadow` ring) so selecting a dot
 * never reflows the row.
 */
@Composable
fun ColorSwatchPicker(
    options: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (isSelected) {
                            Modifier.drawBehind {
                                drawCircle(color = colors.white, radius = size.minDimension / 2f + 4.dp.toPx())
                                drawCircle(color = colors.primaryDeepest, radius = size.minDimension / 2f + 6.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
                            }
                        } else {
                            Modifier
                        },
                    )
                    .clip(CircleShape)
                    .background(option)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Text("✓", style = ColoringTheme.typography.subtitle, color = colors.white)
                }
            }
        }
    }
}
