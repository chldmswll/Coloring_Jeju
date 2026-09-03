package com.example.coloringjeju.presentation.Stamp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/** The "인증하기" pill that appears in the header once a place is selected in [StampMissionList]. */
@Composable
fun VerifyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Text(
        "인증하기",
        style = ColoringTheme.typography.subtitle,
        color = colors.white,
        modifier = modifier
            .clip(ColoringTheme.shapes.full)
            .background(colors.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
