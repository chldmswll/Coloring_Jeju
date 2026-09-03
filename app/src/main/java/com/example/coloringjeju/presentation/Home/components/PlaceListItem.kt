package com.example.coloringjeju.presentation.Home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.coloringjeju.ui.components.ColoringIconButton
import com.example.coloringjeju.ui.components.ColoringIconButtonVariant
import com.example.coloringjeju.ui.theme.ColoringTheme

/** A single recommended-place row inside [AddPlaceSheetContent] / [HomeAddPlaceSheet], e.g. "카멜리아힐 · 자연". */
@Composable
fun PlaceListItem(
    name: String,
    tag: String,
    added: Boolean,
    onToggleAdded: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
) {
    val colors = ColoringTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ColoringTheme.shapes.md)
            .background(colors.white)
            .border(1.5.dp, if (added) colors.primary else colors.border, ColoringTheme.shapes.md)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(ColoringTheme.shapes.sm),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(ColoringTheme.shapes.sm)
                    .background(colors.border),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(name, style = ColoringTheme.typography.subtitle, color = colors.textPrimary)
            Text(
                tag,
                style = ColoringTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        ColoringIconButton(
            onClick = onToggleAdded,
            variant = if (added) ColoringIconButtonVariant.Primary else ColoringIconButtonVariant.Yellow,
        ) {
            Text(
                if (added) "✓" else "+",
                style = ColoringTheme.typography.title,
                color = if (added) colors.white else colors.forest,
            )
        }
    }
}
