package com.example.coloringjeju.presentation.Home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * Shared title block for the map tab — "제주 컬러 지도" plus a state-dependent subtitle
 * (e.g. "이번 여행의 무지개를 채워보세요" on the map, "나의 여행지 위치를 채워보세요" while adding a place).
 */
@Composable
fun HomeTitleHeader(subtitle: String, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("제주 컬러 지도", style = ColoringTheme.typography.display, color = colors.textPrimary)
        Text(
            subtitle,
            style = ColoringTheme.typography.body,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
