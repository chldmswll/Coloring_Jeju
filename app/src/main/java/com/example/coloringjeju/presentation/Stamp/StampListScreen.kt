package com.example.coloringjeju.presentation.Stamp

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
import com.example.coloringjeju.presentation.Stamp.components.StampMissionList
import com.example.coloringjeju.presentation.Stamp.components.StampMissionUi
import com.example.coloringjeju.presentation.Stamp.components.StampProgressBar
import com.example.coloringjeju.presentation.Stamp.components.VerifyButton
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

private val stampMissions = listOf(
    StampMissionUi("성산일출봉", "주황·붉은 (일출) · 인증완료", "🌅", isDone = true),
    StampMissionUi("천지연폭포", "청록 계열 · 미방문", "🌊", isDone = false),
    StampMissionUi("우도", "에메랄드 계열 · 미방문", "🐄", isDone = false),
    StampMissionUi("만장굴", "짙은 회갈 (현무암) · 미방문", "🕳", isDone = false),
)

/**
 * 02 · 스탬프목록 — every collectible place and its verification state. A root tab screen, so it
 * carries no back button; tapping an unverified row selects it and reveals "인증하기" in the header,
 * which hands the place off to [onVerifyPlace] (→ 위치 인증).
 */
@Composable
fun StampListScreen(
    modifier: Modifier = Modifier,
    selectedTab: Int = MainTabs.STAMP,
    onSelectTab: (Int) -> Unit = {},
    onVerifyPlace: (String) -> Unit = {},
) {
    val colors = ColoringTheme.colors
    val done = stampMissions.count { it.isDone }
    var selectedTitle by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(
            title = "스탬프",
            subtitle = "$done / ${stampMissions.size} 완료",
            trailing = selectedTitle?.let { title ->
                { VerifyButton(onClick = { onVerifyPlace(title) }) }
            },
        )
        StampProgressBar(
            progress = done / stampMissions.size.toFloat(),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        StampMissionList(
            missions = stampMissions,
            selectedTitle = selectedTitle,
            onToggleSelect = { mission ->
                selectedTitle = if (selectedTitle == mission.title) null else mission.title
            },
            modifier = Modifier.weight(1f).padding(20.dp),
        )
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
    ColoringJejuTheme { StampListScreen() }
}
