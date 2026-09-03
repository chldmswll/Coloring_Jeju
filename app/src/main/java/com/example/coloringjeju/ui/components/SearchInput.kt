package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.search-input` — pill search field, e.g. "🔍 장소 검색 (예: 우도, 카페)".
 * A real editable field styled to match the token pill; swap [leadingIcon] for a vector icon
 * once one is available.
 */
@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: String = "🔍",
) {
    val colors = ColoringTheme.colors
    Row(
        modifier = modifier
            .clip(ColoringTheme.shapes.full)
            .background(colors.white)
            .border(1.5.dp, colors.border, ColoringTheme.shapes.full)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(leadingIcon, style = ColoringTheme.typography.subtitle)
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f, fill = false)) {
            if (value.isEmpty()) {
                Text(placeholder, style = ColoringTheme.typography.subtitle, color = colors.textTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = ColoringTheme.typography.subtitle.copy(color = colors.textPrimary),
                singleLine = true,
                cursorBrush = SolidColor(colors.primary),
            )
        }
    }
}
