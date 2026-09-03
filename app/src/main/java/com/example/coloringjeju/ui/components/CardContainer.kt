package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/** `.card` variants — [Default] is the everyday content card, [Mint] marks success/complete state. */
enum class ColoringCardVariant { Default, Mint }

/** `.card-container` — a simple title/subtitle card, e.g. list or summary entries. */
@Composable
fun ColoringCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    variant: ColoringCardVariant = ColoringCardVariant.Default,
) {
    val colors = ColoringTheme.colors
    Column(
        modifier = modifier
            .clip(ColoringTheme.shapes.md)
            .background(if (variant == ColoringCardVariant.Mint) colors.mint else colors.white)
            .then(
                if (variant == ColoringCardVariant.Default) {
                    Modifier.border(1.5.dp, colors.border, ColoringTheme.shapes.md)
                } else {
                    Modifier
                },
            )
            .padding(18.dp),
    ) {
        Text(title, style = ColoringTheme.typography.subtitle, color = colors.textPrimary)
        Text(
            subtitle,
            style = ColoringTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
