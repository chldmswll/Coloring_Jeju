package com.example.coloringjeju.presentation.Collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.components.PrimaryButton
import com.example.coloringjeju.ui.theme.ColoringTheme

/** The "여행 마무리하기" card on `조각모음` — bundles the trip's stamps into a downloadable pamphlet. */
@Composable
fun FinishTripCard(onCreatePamphlet: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ColoringTheme.shapes.lg)
            .background(colors.mint)
            .border(1.5.dp, colors.primary, ColoringTheme.shapes.lg)
            .padding(20.dp),
    ) {
        Text("여행 마무리하기", style = ColoringTheme.typography.title, color = colors.primaryDeepest)
        Text(
            "방문 장소 · 인증 사진 · 대표 색 · 날짜 · 그룹원을 자동으로 모아 팜플렛을 만들어요.",
            style = ColoringTheme.typography.body,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        PrimaryButton(text = "팜플렛 만들기", onClick = onCreatePamphlet, modifier = Modifier.fillMaxWidth())
    }
}
