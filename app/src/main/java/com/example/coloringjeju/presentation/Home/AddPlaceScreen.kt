package com.example.coloringjeju.presentation.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import com.example.coloringjeju.presentation.Home.components.AddPlaceSheetContent
import com.example.coloringjeju.presentation.Home.components.HomeMiniMapPreview
import com.example.coloringjeju.presentation.Home.components.HomeTitleHeader
import com.example.coloringjeju.presentation.Home.components.MiniMapDot
import com.example.coloringjeju.presentation.Home.components.PlaceUi
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

/** 03 · 홈·지도 (여행지 추가) — the map tab with the add-place sheet pulled fully open. */
@Composable
fun AddPlaceScreen(modifier: Modifier = Modifier, onClose: () -> Unit = {}) {
    val colors = ColoringTheme.colors
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("전체") }
    var places by remember {
        mutableStateOf(
            listOf(
                PlaceUi("카멜리아힐", "자연", added = true),
                PlaceUi("산굼부리", "자연", added = false),
            ),
        )
    }

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        HomeTitleHeader(subtitle = "나의 여행지 위치를 채워보세요")

        HomeMiniMapPreview(
            dots = listOf(
                MiniMapDot(null, 0.30f, 0.30f),
                MiniMapDot(null, 0.68f, 0.28f),
                MiniMapDot(colors.teal, 0.22f, 0.68f),
                MiniMapDot(colors.orange, 0.52f, 0.62f),
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        AddPlaceSheetContent(
            query = query,
            onQueryChange = { query = it },
            categories = listOf("전체", "자연", "카페", "맛집"),
            selectedCategory = category,
            onSelectCategory = { category = it },
            places = places,
            onToggleAdded = { toggled ->
                places = places.map { if (it == toggled) it.copy(added = !it.added) else it }
            },
            onClose = onClose,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AddPlaceScreenPreview() {
    ColoringJejuTheme { AddPlaceScreen() }
}
