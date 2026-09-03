package com.example.coloringjeju.presentation.Home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.components.ColoringIconButton
import com.example.coloringjeju.ui.components.ColoringIconButtonVariant
import com.example.coloringjeju.ui.components.FilterChipRow
import com.example.coloringjeju.ui.components.SearchInput
import com.example.coloringjeju.ui.theme.ColoringTheme

/** A recommended place row's data, e.g. "카멜리아힐 · 자연". */
data class PlaceUi(val name: String, val category: String, val added: Boolean)

/**
 * The full, expanded "내 지도에 여행지 추가하기" sheet — search, category filter, and the
 * recommended-place list — as its own static screen (see
 * [com.example.coloringjeju.presentation.Home.AddPlaceScreen], 03). The live map tab instead uses
 * [HomeAddPlaceSheet], a real draggable sheet built from the same pieces.
 */
@Composable
fun AddPlaceSheetContent(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    places: List<PlaceUi>,
    onToggleAdded: (PlaceUi) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ColoringTheme.shapes.xl)
            .background(colors.cream)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(ColoringTheme.shapes.full)
                .background(colors.border),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "내 지도에 여행지 추가하기",
                style = ColoringTheme.typography.title,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            ColoringIconButton(onClick = onClose, variant = ColoringIconButtonVariant.Outline) {
                Text("×", style = ColoringTheme.typography.title, color = colors.textSecondary)
            }
        }

        SearchInput(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "장소 검색 (예: 우도, 카페)",
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        FilterChipRow(
            options = categories,
            selected = selectedCategory,
            onSelect = onSelectCategory,
            label = { it },
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(
            "추천 여행지 ${places.size}곳",
            style = ColoringTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(places) { place ->
                PlaceListItem(
                    name = place.name,
                    tag = place.category,
                    added = place.added,
                    onToggleAdded = { onToggleAdded(place) },
                )
            }
        }
    }
}
