package com.example.coloringjeju.presentation.Home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coloringjeju.core.auth.AuthRepository
import com.example.coloringjeju.core.group.GroupRepository
import com.example.coloringjeju.core.group.model.GroupSpot
import com.example.coloringjeju.core.group.model.MapSource
import com.example.coloringjeju.core.group.model.TravelGroup
import com.example.coloringjeju.core.group.model.toGroupSpot
import com.example.coloringjeju.core.group.model.toSavedSpot
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
import com.example.coloringjeju.ui.components.MapSourceDropdown
import com.example.coloringjeju.ui.components.SegmentedControl
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme
import kotlinx.coroutines.launch

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
 * 추천 지도 shows the fixed recommendation set; its pins read their saved/verified state out of the
 * same store, so a place never looks verified on one tab and unverified on the other.
 *
 * MY 지도 tab also carries a `MY 지도 ▾` [MapSourceDropdown]: switching it to a joined
 * [TravelGroup] swaps the map to that group's *shared* Firestore map ([GroupRepository.groupSpots])
 * instead of the personal one, and the detail sheet gains a second "그룹 지도에 추가/삭제" button
 * (independent of the MY 지도 one) that targets whichever group is currently selected — even while
 * still browsing 추천 지도, so a recommended place can be pushed straight to the group.
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
    val scope = rememberCoroutineScope()
    val savedStore = remember { SavedSpotsStore.get(context) }
    val savedSpots by savedStore.spots.collectAsStateWithLifecycle()
    val savedIds = savedSpots.map { it.contentId }.toSet()

    val uid = remember { AuthRepository.currentUser?.uid.orEmpty() }
    var groups by remember { mutableStateOf<List<TravelGroup>>(emptyList()) }
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) GroupRepository.myGroups(uid).collect { groups = it }
    }

    // Which map MY 지도 shows — the personal one, or one joined group's shared one. Independent of
    // [mapTab]: it stays selected even while browsing 추천 지도, so the sheet's group button still
    // knows which group to target there too.
    var mapSource by remember { mutableStateOf<MapSource>(MapSource.Personal) }
    var groupSpots by remember { mutableStateOf<List<GroupSpot>>(emptyList()) }
    LaunchedEffect(mapSource) {
        val source = mapSource
        groupSpots = emptyList()
        if (source is MapSource.Group) GroupRepository.groupSpots(source.group.code).collect { groupSpots = it }
    }
    val groupSpotIds = groupSpots.map { it.contentId }.toSet()

    var mapTab by remember { mutableStateOf(TAB_RECOMMENDED) }
    var sheet by remember { mutableStateOf<PlaceSheet?>(null) }

    val recommendedPins = remember { jejuMapPins() }

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

    /** Add/remove a place on a group's *shared* map — independent of [toggleSaved]'s personal one. */
    val toggleGroupSaved: (SavedSpot, TravelGroup) -> Unit = { spot, group ->
        scope.launch {
            if (spot.contentId in groupSpotIds) {
                GroupRepository.removeSpot(group.code, spot.contentId)
            } else {
                GroupRepository.addSpot(group.code, spot.toGroupSpot(uid))
            }
        }
    }

    val visiblePins = if (mapTab == TAB_MY) {
        when (val source = mapSource) {
            MapSource.Personal -> savedSpots.map { it.toMapPin() }
            is MapSource.Group -> groupSpots.map { it.toSavedSpot().toMapPin() }
        }
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SegmentedControl(
                        options = listOf(TAB_RECOMMENDED, TAB_MY),
                        selected = mapTab,
                        onSelect = { mapTab = it },
                        label = { it },
                    )
                    if (mapTab == TAB_MY) {
                        MapSourceDropdown(selected = mapSource, groups = groups, onSelect = { mapSource = it })
                    }
                }
                JejuMapView(
                    pins = visiblePins,
                    onPinClick = { pin ->
                        sheet = if (mapTab == TAB_MY) {
                            when (mapSource) {
                                MapSource.Personal -> savedSpots.firstOrNull { it.contentId == pin.contentId }
                                is MapSource.Group -> groupSpots.firstOrNull { it.contentId == pin.contentId }?.toSavedSpot()
                            }?.let(PlaceSheet::Saved) ?: PlaceSheet.Recommended(pin)
                        } else {
                            val saved = savedSpots.firstOrNull { it.contentId == pin.contentId }
                            if (saved != null) PlaceSheet.Saved(saved) else PlaceSheet.Recommended(pin)
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
            is PlaceSheet.Search -> openSheet.spot.addr1.ifBlank { openSheet.spot.category.label }
            else -> place?.headline.orEmpty()
        }
        val description = place?.description?.ifBlank { null }
            ?: fetchedOverview
            ?: DESCRIPTION_LOADING
        val activeGroup = (mapSource as? MapSource.Group)?.group

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
                groupName = activeGroup?.name,
                isSavedToGroup = place != null && place.contentId in groupSpotIds,
                onToggleGroupSaved = activeGroup?.let { group -> { place?.let { toggleGroupSaved(it, group) } } },
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
        category = category.label,
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

// 홈 화면 추천 스팟 6곳 — 고정 데이터 (좌표는 팀원이 준 lib/featured-spots.ts 기준).
// contentId/imageUrl은 TourAPI(searchKeyword2)에서 각 장소를 찾아 받아온 값 — 사진은 마커와 상세
// 시트 히어로 이미지에 함께 쓰인다. 설명 문구는 API의 overview보다 짧고 앱 톤에 맞아 손으로 쓴
// 텍스트를 그대로 둔다. saved/fillColor는 여기서 정하지 않고 SavedSpotsStore에서 읽어 채운다.
private fun jejuMapPins() = listOf(
    MapPinData(
        label = "한라산", emoji = "⛰", fillColor = null, lat = 33.3617, lng = 126.5292,
        tag = "자연", headline = "제주의 가장 높은 봉우리",
        description = "해발 1,947m의 한라산은 계절마다 다른 풍경을 보여주는 제주 대표 명소예요. " +
            "가벼운 산책부터 정상 탐방까지, 나만의 여행 루트를 만들어 보세요.",
        saved = false,
        contentId = "127635",
        imageUrl = "https://tong.visitkorea.or.kr/cms/resource_photo/41/3460441_image2_1.jpg",
    ),
    MapPinData(
        label = "성산일출봉", emoji = "🌅", fillColor = null, lat = 33.4581, lng = 126.9425,
        tag = "자연", headline = "유네스코가 인정한 일출 명소",
        description = "화산 분화구가 만든 웅장한 봉우리로, 정상에서 보는 일출이 특히 아름다워요.",
        saved = false,
        contentId = "126435",
        imageUrl = "https://tong.visitkorea.or.kr/cms/resource_photo/26/3052926_image2_1.jpg",
    ),
    MapPinData(
        label = "우도", emoji = "🐄", fillColor = null, lat = 33.5054, lng = 126.9515,
        tag = "자연", headline = "제주 앞바다의 작은 섬",
        description = "에메랄드빛 바다와 땅콩 아이스크림으로 유명한, 자전거로 둘러보기 좋은 섬이에요.",
        saved = false,
        contentId = "127336",
        imageUrl = "https://tong.visitkorea.or.kr/cms/resource/71/3554771_image2_1.jpg",
    ),
    MapPinData(
        label = "협재해수욕장", emoji = "🏖", fillColor = null, lat = 33.3941, lng = 126.2396,
        tag = "자연", headline = "에메랄드빛 협재 해변",
        description = "고운 백사장과 투명한 바다색으로 유명한 제주 대표 해변이에요. 비양도를 배경으로 노을이 특히 아름다워요.",
        saved = false,
        contentId = "127490",
        imageUrl = "https://tong.visitkorea.or.kr/cms/resource/66/3096066_image2_1.jpg",
    ),
    MapPinData(
        label = "천지연폭포", emoji = "🌊", fillColor = null, lat = 33.2465, lng = 126.5581,
        tag = "자연", headline = "폭포와 원시림이 만나는 곳",
        description = "울창한 난대림 사이로 떨어지는 폭포가 인상적인 서귀포 대표 명소예요.",
        saved = false,
        contentId = "126438",
        imageUrl = "https://tong.visitkorea.or.kr/cms/resource/43/4094043_image2_1.jpg",
    ),
    MapPinData(
        label = "월정리해변", emoji = "🏖", fillColor = null, lat = 33.5563, lng = 126.7961,
        tag = "자연", headline = "카페 거리를 낀 코발트빛 해변",
        description = "새하얀 모래와 코발트빛 바다, 해변을 따라 늘어선 감성 카페로 유명한 제주 동쪽 명소예요.",
        saved = false,
        contentId = "1918639",
        imageUrl = "https://tong.visitkorea.or.kr/cms/resource/93/4075293_image2_1.jpg",
    ),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeMapScreenPreview() {
    ColoringJejuTheme { HomeMapScreen() }
}
