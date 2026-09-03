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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.presentation.Home.components.HomeAddPlaceSheet
import com.example.coloringjeju.presentation.Home.components.HomeTitleHeader
import com.example.coloringjeju.presentation.Home.components.JejuMapView
import com.example.coloringjeju.presentation.Home.components.MapPinData
import com.example.coloringjeju.presentation.Home.components.PlaceDetailContent
import com.example.coloringjeju.presentation.Home.components.RainbowProgressRow
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.components.SegmentedControl
import com.example.coloringjeju.ui.theme.ColoringColors
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * 01 · 홈·지도 — the map tab: today's rainbow progress, the place map, and the "내 지도에 여행지
 * 추가하기" sheet. That sheet is a real [BottomSheetScaffold] (drag the handle up/down — no
 * button), and tapping a map pin opens its place detail as a [ModalBottomSheet] (same drag
 * behavior, dismiss by dragging down or tapping outside).
 *
 * 추천 지도 shows every pin; MY 지도 shows only the ones marked [MapPinData.saved] — toggled from
 * the detail sheet's pill/button, which reads "+ MY 지도에 추가" when not saved and "MY 지도에서
 * 삭제" once it is.
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
    var mapTab by remember { mutableStateOf("추천 지도") }
    var pins by remember { mutableStateOf(jejuMapPins(colors)) }
    var selectedPin by remember { mutableStateOf<MapPinData?>(null) }

    val sheetScaffoldState = rememberBottomSheetScaffoldState()
    val visiblePins = if (mapTab == "MY 지도") pins.filter { it.saved } else pins

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        HomeTitleHeader(subtitle = "이번 여행의 무지개를 채워보세요")

        BottomSheetScaffold(
            modifier = Modifier.weight(1f),
            scaffoldState = sheetScaffoldState,
            sheetPeekHeight = 120.dp,
            sheetContainerColor = colors.cream,
            sheetShape = RoundedCornerShape(topStart = ColoringTheme.radius.xl, topEnd = ColoringTheme.radius.xl),
            sheetContent = { HomeAddPlaceSheet() },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                RainbowProgressRow(colors = listOf(colors.pink, colors.primary, colors.orange, null, null, null))
                SegmentedControl(
                    options = listOf("추천 지도", "MY 지도"),
                    selected = mapTab,
                    onSelect = { mapTab = it },
                    label = { it },
                )
                JejuMapView(pins = visiblePins, onPinClick = { selectedPin = it })
            }
        }

        BottomTabBar(
            items = MainTabs.items,
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.padding(12.dp),
        )
    }

    val pin = selectedPin
    if (pin != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedPin = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.cream,
        ) {
            PlaceDetailContent(
                name = pin.label,
                tag = pin.tag,
                headline = pin.headline,
                description = pin.description,
                isSaved = pin.saved,
                onToggleSaved = {
                    pins = pins.map { if (it.label == pin.label) it.copy(saved = !it.saved) else it }
                    selectedPin = null
                },
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            )
        }
    }
}

// 홈 화면 추천 스팟 6곳 — 고정 데이터 (좌표는 팀원이 준 lib/featured-spots.ts 기준).
// TourAPI에 이 6곳의 contentId가 없어 상세 설명은 그대로 손으로 쓴 텍스트를 유지.
private fun jejuMapPins(colors: ColoringColors) = listOf(
    MapPinData(
        label = "한라산", emoji = "⛰", fillColor = null, lat = 33.3617, lng = 126.5292,
        tag = "자연", headline = "제주의 가장 높은 봉우리",
        description = "해발 1,947m의 한라산은 계절마다 다른 풍경을 보여주는 제주 대표 명소예요. " +
            "가벼운 산책부터 정상 탐방까지, 나만의 여행 루트를 만들어 보세요.",
        saved = false,
    ),
    MapPinData(
        label = "성산일출봉", emoji = "🌅", fillColor = colors.orange, lat = 33.4581, lng = 126.9425,
        tag = "자연", headline = "유네스코가 인정한 일출 명소",
        description = "화산 분화구가 만든 웅장한 봉우리로, 정상에서 보는 일출이 특히 아름다워요.",
        saved = true,
    ),
    MapPinData(
        label = "우도", emoji = "🐄", fillColor = null, lat = 33.5054, lng = 126.9515,
        tag = "자연", headline = "제주 앞바다의 작은 섬",
        description = "에메랄드빛 바다와 땅콩 아이스크림으로 유명한, 자전거로 둘러보기 좋은 섬이에요.",
        saved = false,
    ),
    MapPinData(
        label = "협재해수욕장", emoji = "🏖", fillColor = colors.teal, lat = 33.3941, lng = 126.2396,
        tag = "자연", headline = "에메랄드빛 협재 해변",
        description = "고운 백사장과 투명한 바다색으로 유명한 제주 대표 해변이에요. 비양도를 배경으로 노을이 특히 아름다워요.",
        saved = true,
    ),
    MapPinData(
        label = "천지연폭포", emoji = "🌊", fillColor = null, lat = 33.2465, lng = 126.5581,
        tag = "자연", headline = "폭포와 원시림이 만나는 곳",
        description = "울창한 난대림 사이로 떨어지는 폭포가 인상적인 서귀포 대표 명소예요.",
        saved = false,
    ),
    MapPinData(
        label = "월정리해변", emoji = "🏖", fillColor = null, lat = 33.5563, lng = 126.7961,
        tag = "자연", headline = "카페 거리를 낀 코발트빛 해변",
        description = "새하얀 모래와 코발트빛 바다, 해변을 따라 늘어선 감성 카페로 유명한 제주 동쪽 명소예요.",
        saved = false,
    ),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeMapScreenPreview() {
    ColoringJejuTheme { HomeMapScreen() }
}
