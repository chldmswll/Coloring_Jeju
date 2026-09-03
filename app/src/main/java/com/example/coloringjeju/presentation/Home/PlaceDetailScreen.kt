package com.example.coloringjeju.presentation.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.presentation.Home.components.PlaceDetailContent
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * 07 · 여행지 상세 — a recommended place's detail page. The same content also appears as a
 * draggable bottom sheet when a pin on [com.example.coloringjeju.presentation.Home.HomeMapScreen]'s
 * map is tapped (see [PlaceDetailContent]).
 */
@Composable
fun PlaceDetailScreen(
    modifier: Modifier = Modifier,
    name: String = "한라산",
    tag: String = "자연",
    headline: String = "제주의 가장 높은 봉우리",
    description: String = "해발 1,947m의 한라산은 계절마다 다른 풍경을 보여주는 제주 대표 명소예요. " +
        "가벼운 산책부터 정상 탐방까지, 나만의 여행 루트를 만들어 보세요.",
) {
    val colors = ColoringTheme.colors
    var isSaved by remember { mutableStateOf(false) }
    PlaceDetailContent(
        name = name,
        tag = tag,
        headline = headline,
        description = description,
        isSaved = isSaved,
        onToggleSaved = { isSaved = !isSaved },
        modifier = modifier
            .fillMaxSize()
            .background(colors.cream)
            .padding(20.dp),
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PlaceDetailScreenPreview() {
    ColoringJejuTheme { PlaceDetailScreen() }
}
