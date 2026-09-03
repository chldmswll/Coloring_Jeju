package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
 * `.stamp-card` — a visited-place / mission row with a check indicator.
 *
 * [isDone] and [isSelected] are deliberately separate, matching the style guide's color rule
 * (green = verified/complete, yellow = currently selected): [isDone] marks the place as already
 * verified (mint card, green check) — permanent, set from data. [isSelected] marks it as picked
 * for the next 인증하기 action (yellow ring) — transient, driven by the list's own selection state.
 * A place can't be both; [isDone] wins if somehow both are true. Pass [onClick] to let the row
 * itself be tapped (e.g. to toggle that selection).
 */
@Composable
fun StampCard(
    title: String,
    subtitle: String,
    isDone: Boolean,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    val colors = ColoringTheme.colors
    val borderColor = when {
        isDone -> colors.primary
        isSelected -> colors.yellow
        else -> colors.border
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ColoringTheme.shapes.md)
            .background(if (isDone) colors.mint else colors.white)
            .border(if (isDone || isSelected) 2.dp else 1.5.dp, borderColor, ColoringTheme.shapes.md)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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

        StampCheck(isDone = isDone, isSelected = isSelected)
    }
}

@Composable
private fun StampCheck(isDone: Boolean, isSelected: Boolean) {
    val colors = ColoringTheme.colors
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                when {
                    isDone -> Modifier.background(colors.primary)
                    isSelected -> Modifier.background(colors.yellow)
                    else -> Modifier.border(1.5.dp, colors.border, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isDone) {
            Text("✓", style = ColoringTheme.typography.caption, color = colors.white)
        } else if (isSelected) {
            // Yellow is a light fill — dark forest text keeps the check legible, same as
            // .btn-icon-yellow's content color.
            Text("✓", style = ColoringTheme.typography.caption, color = colors.forest)
        }
    }
}
