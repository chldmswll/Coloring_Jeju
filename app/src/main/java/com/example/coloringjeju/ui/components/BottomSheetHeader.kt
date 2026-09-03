package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.sheet` header — drag handle + title/subtitle, meant as the top of a
 * `ModalBottomSheet`'s `sheetContent` (e.g. "내 지도에 여행지 추가하기").
 * Background uses [ColoringTheme.colors]' cream surface, matching the style guide.
 */
@Composable
fun BottomSheetHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.cream)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(ColoringTheme.shapes.full)
                .background(colors.border),
        )
        Spacer(Modifier.height(16.dp))
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(title, style = ColoringTheme.typography.title, color = colors.textPrimary)
            Text(
                subtitle,
                style = ColoringTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
