package com.example.coloringjeju.presentation.Stamp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.local.datastore.SavedSpot
import com.example.coloringjeju.presentation.Stamp.components.StampMissionList
import com.example.coloringjeju.presentation.Stamp.components.StampProgressBar
import com.example.coloringjeju.presentation.Stamp.components.VerifyButton
import com.example.coloringjeju.presentation.Stamp.components.toStampMission
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * 02 · 스탬프목록 — one mission per place on MY 지도, and its verification state. [spots] is the
 * very same list 홈·지도's MY 지도 tab draws (both read
 * [com.example.coloringjeju.core.local.datastore.SavedSpotsStore]), so a place can never be on one
 * and missing from the other; adding a place on the map is what puts a stamp here.
 *
 * A root tab screen, so it carries no back button; tapping an unverified row selects it and reveals
 * "인증하기" in the header, which hands that place off to [onVerifyPlace] (→ 위치 인증). Completing
 * the mission writes the captured color back onto the place, which both checks off its row here and
 * turns its map marker from grayscale to color.
 */
@Composable
fun StampListScreen(
    modifier: Modifier = Modifier,
    spots: List<SavedSpot> = emptyList(),
    selectedTab: Int = MainTabs.STAMP,
    onSelectTab: (Int) -> Unit = {},
    onVerifyPlace: (SavedSpot) -> Unit = {},
) {
    val colors = ColoringTheme.colors
    val done = spots.count { it.isVerified }
    var selectedId by remember { mutableStateOf<String?>(null) }

    // Derived, not stored: a place that was verified or removed from MY 지도 while selected simply
    // stops being the selection instead of leaving a dangling id behind the 인증하기 button.
    val selectedSpot = spots.firstOrNull { it.contentId == selectedId && !it.isVerified }

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(
            title = "스탬프",
            subtitle = "$done / ${spots.size} 완료",
            trailing = selectedSpot?.let { spot ->
                { VerifyButton(onClick = { onVerifyPlace(spot) }) }
            },
        )
        StampProgressBar(
            progress = if (spots.isEmpty()) 0f else done / spots.size.toFloat(),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (spots.isEmpty()) {
            Box(modifier = Modifier.weight(1f).padding(40.dp), contentAlignment = Alignment.Center) {
                Text(
                    "아직 스탬프가 없어요.\n홈·지도에서 MY 지도에 여행지를 추가하면\n이곳에 인증 미션이 생겨요.",
                    style = ColoringTheme.typography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            StampMissionList(
                missions = spots.map { it.toStampMission() },
                selectedId = selectedId,
                onToggleSelect = { mission ->
                    selectedId = if (selectedId == mission.contentId) null else mission.contentId
                },
                modifier = Modifier.weight(1f).padding(20.dp),
            )
        }
        BottomTabBar(
            items = MainTabs.items,
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun StampListScreenPreview() {
    ColoringJejuTheme {
        StampListScreen(
            spots = listOf(
                SavedSpot("126435", "성산일출봉", null, "자연", 33.4581, 126.9425, 1L, verifiedColor = -0xbcd8),
                SavedSpot("127490", "협재해수욕장", null, "자연", 33.3941, 126.2396, 2L),
                SavedSpot("127336", "우도", null, "자연", 33.5054, 126.9515, 3L),
            ),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun StampListScreenEmptyPreview() {
    ColoringJejuTheme { StampListScreen() }
}
