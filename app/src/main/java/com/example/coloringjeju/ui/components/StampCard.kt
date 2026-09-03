package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.stamp-card` — a visited-place / mission row with a check indicator. Set [isDone] once the
 * mission (e.g. a location check-in) is verified.
 */
@Composable
fun StampCard(
    title: String,
    subtitle: String,
    isDone: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val colors = ColoringTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ColoringTheme.shapes.md)
            .background(if (isDone) colors.mint else colors.white)
            .border(1.5.dp, if (isDone) colors.primary else colors.border, ColoringTheme.shapes.md)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ColoringTheme.shapes.sm)
                .background(colors.offWhite),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(title, style = ColoringTheme.typography.subtitle, color = colors.textPrimary)
            Text(
                subtitle,
                style = ColoringTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        StampCheck(checked = isDone)
    }
}

@Composable
private fun StampCheck(checked: Boolean) {
    val colors = ColoringTheme.colors
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(colors.primary)
                } else {
                    Modifier.border(1.5.dp, colors.border, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text("✓", style = ColoringTheme.typography.caption, color = colors.white)
        }
    }
}
