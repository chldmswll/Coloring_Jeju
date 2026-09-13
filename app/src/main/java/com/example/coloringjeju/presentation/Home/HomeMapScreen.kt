package com.example.coloringjeju.presentation.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coloringjeju.core.local.assets.RecommendedSpot
import com.example.coloringjeju.core.local.assets.RecommendedSpots
import com.example.coloringjeju.core.local.datastore.SavedSpot
import com.example.coloringjeju.core.local.datastore.SavedSpotsStore
import com.example.coloringjeju.core.network.TourApiResult
import com.example.coloringjeju.core.network.TourRepository
import com.example.coloringjeju.core.network.model.TourSpot
import com.example.coloringjeju.presentation.Home.components.HomeAddPlaceSheet
import com.example.coloringjeju.presentation.Home.components.HomeTitleHeader
import com.example.coloringjeju.presentation.Home.components.JejuMapView
import com.example.coloringjeju.presentation.Home.components.MapPinData
import com.example.coloringjeju.presentation.Home.components.PlaceDetailContent
import com.example.coloringjeju.presentation.Home.components.RainbowProgressRow
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.components.SegmentedControl
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

private const val TAB_RECOMMENDED = "추천 지도"
private const val TAB_MY = "MY 지도"
private const val DESCRIPTION_LOADING = "설명을 불러오는 중…"

/**
 * Which place the detail [ModalBottomSheet] is showing, and where it was opened from. All three
 * render the exact same [PlaceDetailContent] — a map pin, a search row, and a MY 지도 marker open
 * one identical sheet.
 *
 * Each case holds a snapshot of its place rather than a lookup key, so the sheet survives the place
 * being removed from MY 지도 while it's open: the button just flips back to "MY 지도에 추가".
 */
private sealed interface PlaceSheet {
    data class Recommended(val pin: MapPinData) : PlaceSheet
    data class Search(val spot: TourSpot) : PlaceSheet
    data class Saved(val spot: SavedSpot) : PlaceSheet
}

/**
 * 01 · 홈·지도 — the map tab: today's rainbow progress, the place map, and the "내 지도에 여행지
 * 추가하기" sheet. That sheet is a real [BottomSheetScaffold] (drag the handle up/down — no button);
 * tapping a map pin, a search row, or a MY 지도 marker opens that place's detail as one shared
 * [ModalBottomSheet].
 *
 * [SavedSpotsStore] is the single source of truth for MY 지도. Adding a place — from the search
 * list or from a 추천 지도 pin's sheet — writes it there, and MY 지도 draws one marker per saved
 * place carrying that place's own TourAPI photo. A marker's photo stays grayscale until the place
 * has a [SavedSpot.verifiedColor], which the 스탬프 mission writes on completion (see
 * [com.example.coloringjeju.presentation.MainTabsScreen]) — so "인증하면 색이 들어온다" is the same
 * fact on the map as on the stamp list.
 *
 * 추천 지도 shows [RecommendedSpots] — 한국관광공사's 중심관광지 ranking joined to TourAPI, shipped
 * as an asset. Its pins read their saved/verified state out of the same store, so a place never
 * looks verified on one tab and unverified on the other.
 *
 * [selectedTab]/[onSelectTab] are lifted the same way as the other tab-root screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapScreen(
    modifier: Modifier = Modifier,
    selectedTab: Int = MainTabs.HOME,
    onSelectTab: (Int) -> Unit = {},
) {
    val colors = ColoringTheme.colors
    val context = LocalContext.current
    val savedStore = remember { SavedSpotsStore.get(context) }
    val savedSpots by savedStore.spots.collectAsStateWithLifecycle()
    val savedIds = savedSpots.map { it.contentId }.toSet()

    var mapTab by remember { mutableStateOf(TAB_RECOMMENDED) }
    var sheet by remember { mutableStateOf<PlaceSheet?>(null) }

    val recommendedPins = remember { RecommendedSpots.load(context).map { it.toMapPin() } }

    // `areaBasedList`/`searchKeyword` don't return `overview`, and neither does what MY 지도 keeps
    // on disk for a place saved from search — so the description comes from a follow-up
    // `detailCommon` call once the sheet is open.
    var fetchedOverview by remember { mutableStateOf<String?>(null) }
    val overviewId = when (val open = sheet) {
        is PlaceSheet.Search -> open.spot.contentId
        is PlaceSheet.Saved -> open.spot.contentId.takeIf { open.spot.description.isNullOrBlank() }
        else -> null
    }
    LaunchedEffect(overviewId) {
        fetchedOverview = null
        if (overviewId == null) return@LaunchedEffect
        when (val result = TourRepository.detail(overviewId)) {
            is TourApiResult.Success -> fetchedOverview = result.data.overview
            is TourApiResult.Error -> Unit // the sheet falls back to the address/headline line
        }
    }

    /**
     * Add or remove a place from MY 지도 — the one path both the search list and the sheet use.
     * The store pushes the new list back through [SavedSpotsStore.spots], so the map, this screen
     * and the 스탬프 tab all update off the same write.
     */
    val toggleSaved: (SavedSpot) -> Unit = { spot ->
        if (spot.contentId in savedIds) savedStore.remove(spot.contentId) else savedStore.add(spot)
    }

    val visiblePins = if (mapTab == TAB_MY) {
        savedSpots.map { it.toMapPin() }
    } else {
        recommendedPins.map { pin ->
            val saved = savedSpots.firstOrNull { it.contentId == pin.contentId }
            pin.copy(saved = saved != null, fillColor = saved?.verifiedColor?.let(::Color))
        }
    }

    val sheetScaffoldState = rememberBottomSheetScaffoldState()

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        HomeTitleHeader(subtitle = "이번 여행의 무지개를 채워보세요")

        BottomSheetScaffold(
            modifier = Modifier.weight(1f),
            scaffoldState = sheetScaffoldState,
            sheetPeekHeight = 120.dp,
            sheetContainerColor = colors.cream,
            sheetShape = RoundedCornerShape(topStart = ColoringTheme.radius.xl, topEnd = ColoringTheme.radius.xl),
            sheetContent = {
                HomeAddPlaceSheet(
                    savedIds = savedIds,
                    onToggleSaved = { spot -> spot.toSavedSpot()?.let(toggleSaved) },
                    onSpotClick = { sheet = PlaceSheet.Search(it) },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                RainbowProgressRow(colors = rainbowSlots(savedSpots))
                SegmentedControl(
                    options = listOf(TAB_RECOMMENDED, TAB_MY),
                    selected = mapTab,
                    onSelect = { mapTab = it },
                    label = { it },
                )
                JejuMapView(
                    pins = visiblePins,
                    onPinClick = { pin ->
                        val saved = savedSpots.firstOrNull { it.contentId == pin.contentId }
                        sheet = if (mapTab == TAB_MY && saved != null) {
                            PlaceSheet.Saved(saved)
                        } else {
                            PlaceSheet.Recommended(pin)
                        }
                    },
                )
            }
        }

        BottomTabBar(
            items = MainTabs.items,
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.padding(12.dp),
        )
    }

    val openSheet = sheet
    if (openSheet != null) {
        val bodyModifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)

        // One sheet, one content composable — only the place backing it differs.
        val place = when (openSheet) {
            is PlaceSheet.Recommended -> openSheet.pin.toSavedSpot()
            is PlaceSheet.Search -> openSheet.spot.toSavedSpot()
            is PlaceSheet.Saved -> openSheet.spot
        }
        val headline = when (openSheet) {
            is PlaceSheet.Search -> openSheet.spot.addr1.ifBlank { openSheet.spot.category?.label.orEmpty() }
            else -> place?.headline.orEmpty()
        }
        val description = place?.description?.ifBlank { null }
            ?: fetchedOverview
            ?: DESCRIPTION_LOADING

        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.cream,
        ) {
            PlaceDetailContent(
                name = place?.title.orEmpty(),
                tag = place?.category.orEmpty(),
                headline = headline,
                description = description,
                isSaved = place != null && place.contentId in savedIds,
                imageUrl = place?.image,
                onToggleSaved = { place?.let(toggleSaved) },
                modifier = bodyModifier,
            )
        }
    }
}

/** MY 지도 marker for a saved place: its TourAPI photo, grayscale until a color has been earned. */
private fun SavedSpot.toMapPin() = MapPinData(
    label = title,
    emoji = "",
    fillColor = verifiedColor?.let(::Color),
    lat = lat,
    lng = lng,
    tag = category,
    headline = headline,
    description = description.orEmpty(),
    saved = true,
    contentId = contentId,
    imageUrl = image,
)

/** A 추천 지도 pin as a MY 지도 record — keeps its handwritten headline/description. */
private fun MapPinData.toSavedSpot(): SavedSpot? {
    val id = contentId ?: return null
    return SavedSpot(
        contentId = id,
        title = label,
        image = imageUrl,
        category = tag,
        lat = lat,
        lng = lng,
        addedAt = System.currentTimeMillis(),
        headline = headline,
        description = description,
    )
}

/**
 * A TourAPI search result as a MY 지도 record. Null when the entry has no coordinates — it can't be
 * placed on the map, so it can't be saved either. `description` is left null so the detail sheet
 * pulls the real `overview` from `detailCommon` instead of persisting a placeholder.
 */
private fun TourSpot.toSavedSpot(): SavedSpot? {
    val spotLat = lat ?: return null
    val spotLng = lng ?: return null
    return SavedSpot(
        contentId = contentId,
        title = title,
        image = image ?: thumbnail,
        category = category?.label.orEmpty(),
        lat = spotLat,
        lng = spotLng,
        addedAt = System.currentTimeMillis(),
        headline = addr1,
        description = null,
    )
}

/** 오늘의 무지개 — 인증해서 색을 얻은 순서대로 6칸을 채운다. */
private fun rainbowSlots(saved: List<SavedSpot>): List<Color?> {
    val earned = saved.sortedBy { it.addedAt }.mapNotNull { it.verifiedColor }.map(::Color)
    return List(6) { earned.getOrNull(it) }
}

/**
 * A 추천 지도 record as a map pin. `saved`/`fillColor` are deliberately left empty here — the screen
 * fills them in from [SavedSpotsStore], so a place never looks verified on one tab and unverified on
 * the other.
 */
private fun RecommendedSpot.toMapPin() = MapPinData(
    label = title,
    emoji = emoji,
    fillColor = null,
    lat = lat,
    lng = lng,
    tag = category,
    headline = headline,
    description = description,
    saved = false,
    contentId = contentId,
    imageUrl = imageUrl,
    rank = rank,
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeMapScreenPreview() {
    ColoringJejuTheme { HomeMapScreen() }
}
