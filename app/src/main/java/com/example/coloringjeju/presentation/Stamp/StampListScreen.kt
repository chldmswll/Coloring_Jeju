package com.example.coloringjeju.presentation.Stamp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.auth.AuthRepository
import com.example.coloringjeju.core.group.GroupRepository
import com.example.coloringjeju.core.group.model.GroupSpot
import com.example.coloringjeju.core.group.model.MapSource
import com.example.coloringjeju.core.group.model.TravelGroup
import com.example.coloringjeju.core.group.model.toSavedSpot
import com.example.coloringjeju.core.local.datastore.SavedSpot
import com.example.coloringjeju.presentation.Stamp.components.StampMissionList
import com.example.coloringjeju.presentation.Stamp.components.StampProgressBar
import com.example.coloringjeju.presentation.Stamp.components.VerifyButton
import com.example.coloringjeju.presentation.Stamp.components.toStampMission
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.components.MapSourceDropdown
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * 02 · 스탬프목록 — one mission per place on the current `MY 지도 ▾` selection
 * ([MapSourceDropdown]), and its verification state. [spots] (personal) is the very same list
 * 홈·지도's MY 지도 tab draws (both read
 * [com.example.coloringjeju.core.local.datastore.SavedSpotsStore]); switching the dropdown to a
 * joined group instead lists that group's shared [GroupRepository.groupSpots], so a place can never
 * be on one screen's list and missing from the matching one.
 *
 * A root tab screen, so it carries no back button; tapping an unverified row selects it and reveals
 * "인증하기" in the header, which hands that place *and* which source it came from off to
 * [onVerifyPlace] (→ 위치 인증) — [com.example.coloringjeju.presentation.MainTabsScreen] needs the
 * source too, since completing the mission has to write the captured color back to the right place
 * (personal store vs. the group's Firestore doc), which both checks off its row here and turns its
 * map marker from grayscale to color.
 */
@Composable
fun StampListScreen(
    modifier: Modifier = Modifier,
    spots: List<SavedSpot> = emptyList(),
    selectedTab: Int = MainTabs.STAMP,
    onSelectTab: (Int) -> Unit = {},
    onVerifyPlace: (SavedSpot, MapSource) -> Unit = { _, _ -> },
) {
    val colors = ColoringTheme.colors

    val uid = remember { AuthRepository.currentUser?.uid.orEmpty() }
    var groups by remember { mutableStateOf<List<TravelGroup>>(emptyList()) }
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) GroupRepository.myGroups(uid).collect { groups = it }
    }

    var source by remember { mutableStateOf<MapSource>(MapSource.Personal) }
    var groupSpots by remember { mutableStateOf<List<GroupSpot>>(emptyList()) }
    LaunchedEffect(source) {
        val current = source
        groupSpots = emptyList()
        if (current is MapSource.Group) GroupRepository.groupSpots(current.group.code).collect { groupSpots = it }
    }

    val visibleSpots = when (source) {
        MapSource.Personal -> spots
        is MapSource.Group -> groupSpots.map { it.toSavedSpot() }
    }

    val done = visibleSpots.count { it.isVerified }
    var selectedId by remember { mutableStateOf<String?>(null) }

    // Derived, not stored: a place that was verified or removed from the list while selected simply
    // stops being the selection instead of leaving a dangling id behind the 인증하기 button.
    val selectedSpot = visibleSpots.firstOrNull { it.contentId == selectedId && !it.isVerified }

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(
            title = "스탬프",
            subtitle = "$done / ${visibleSpots.size} 완료",
            trailing = selectedSpot?.let { spot ->
                { VerifyButton(onClick = { onVerifyPlace(spot, source) }) }
            },
        )
        MapSourceDropdown(
            selected = source,
            groups = groups,
            onSelect = { source = it; selectedId = null },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        StampProgressBar(
            progress = if (visibleSpots.isEmpty()) 0f else done / visibleSpots.size.toFloat(),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (visibleSpots.isEmpty()) {
            Box(modifier = Modifier.weight(1f).padding(40.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (source == MapSource.Personal) {
                        "아직 스탬프가 없어요.\n홈·지도에서 MY 지도에 여행지를 추가하면\n이곳에 인증 미션이 생겨요."
                    } else {
                        "아직 이 그룹 지도에 추가된 곳이 없어요.\n홈·지도에서 그룹 지도에 여행지를 추가해보세요."
                    },
                    style = ColoringTheme.typography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            StampMissionList(
                missions = visibleSpots.map { it.toStampMission() },
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
