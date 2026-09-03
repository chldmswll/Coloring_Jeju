package com.example.coloringjeju.presentation.Home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.local.datastore.SavedSpot
import com.example.coloringjeju.core.local.datastore.SavedSpotsStore
import com.example.coloringjeju.core.network.TourApiResult
import com.example.coloringjeju.core.network.TourRepository
import com.example.coloringjeju.core.network.model.TourCategory
import com.example.coloringjeju.core.network.model.TourSpot
import com.example.coloringjeju.ui.components.FilterChipRow
import com.example.coloringjeju.ui.components.SearchInput
import com.example.coloringjeju.ui.theme.ColoringTheme
import kotlinx.coroutines.delay

private val CategoryOptions = listOf("전체") + TourCategory.entries.map { it.label }

/**
 * Content of the persistent "내 지도에 여행지 추가하기" sheet hosted by
 * [com.example.coloringjeju.presentation.Home.HomeMapScreen]'s `BottomSheetScaffold`. The scaffold
 * supplies the drag handle and drag gesture (peek ↔ expanded) itself — this is just what sits
 * inside it: the collapsed hint line, then a live TourAPI (KorService2) search/filter/list that
 * comes into view once dragged up. Self-contained — it owns its own query/category/results state
 * and reads/writes [SavedSpotsStore] directly, so [com.example.coloringjeju.presentation.Home.HomeMapScreen]
 * doesn't need to know any of that.
 */
@Composable
fun HomeAddPlaceSheet(modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    val context = LocalContext.current
    val savedStore = remember { SavedSpotsStore(context) }
    var savedIds by remember { mutableStateOf(savedStore.getAll().map { it.contentId }.toSet()) }

    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("전체") }
    var results by remember { mutableStateOf<List<TourSpot>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        loading = true
        errorMessage = null
        if (query.isNotBlank()) delay(400) // debounce typing; the initial blank-query load fires immediately
        when (val result = if (query.isBlank()) TourRepository.areaBasedList() else TourRepository.searchKeyword(query)) {
            is TourApiResult.Success -> results = result.data
            is TourApiResult.Error -> {
                results = emptyList()
                errorMessage = result.message
            }
        }
        loading = false
    }

    val visiblePlaces = results.filter { category == "전체" || it.category.label == category }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("내 지도에 여행지 추가하기", style = ColoringTheme.typography.title, color = colors.textPrimary)
        Text(
            "위로 당겨서 추천 목록 보기 · 총 ${savedIds.size}곳 추가됨",
            style = ColoringTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )

        SearchInput(
            value = query,
            onValueChange = { query = it },
            placeholder = "장소 검색 (예: 우도, 카페)",
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        )

        FilterChipRow(
            options = CategoryOptions,
            selected = category,
            onSelect = { category = it },
            label = { it },
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(
            when {
                loading -> "불러오는 중…"
                errorMessage != null -> errorMessage!!
                else -> "추천 여행지 ${visiblePlaces.size}곳"
            },
            style = ColoringTheme.typography.caption,
            color = if (errorMessage != null) colors.orange else colors.textSecondary,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            visiblePlaces.forEach { spot ->
                PlaceListItem(
                    name = spot.title,
                    tag = spot.category.label,
                    imageUrl = spot.thumbnail ?: spot.image,
                    added = spot.contentId in savedIds,
                    onToggleAdded = {
                        if (spot.contentId in savedIds) {
                            savedStore.remove(spot.contentId)
                            savedIds = savedIds - spot.contentId
                        } else if (spot.lat != null && spot.lng != null) {
                            savedStore.add(
                                SavedSpot(
                                    contentId = spot.contentId,
                                    title = spot.title,
                                    image = spot.thumbnail ?: spot.image,
                                    category = spot.category.label,
                                    lat = spot.lat,
                                    lng = spot.lng,
                                    addedAt = System.currentTimeMillis(),
                                ),
                            )
                            savedIds = savedIds + spot.contentId
                        }
                    },
                )
            }
        }

        // Breathing room at the bottom of the sheet once fully dragged open.
        Spacer(Modifier.height(24.dp))
    }
}
