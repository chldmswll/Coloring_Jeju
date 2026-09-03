package com.example.coloringjeju.presentation.LocationVerify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.presentation.LocationVerify.components.LocationRadiusIndicator
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BorderedStatusCard
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.components.PrimaryButton
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * 04 · 위치인증 — confirms the traveler is close enough to the place to unlock its camera mission.
 * [selectedTab]/[onSelectTab] are lifted like the tab-root screens, so the bottom bar can jump
 * straight to 홈/조각 from here — the 스탬프 flow's state (this screen) is kept by the caller and
 * picked back up when the traveler returns to that tab, rather than being reset.
 */
@Composable
fun LocationVerifyScreen(
    modifier: Modifier = Modifier,
    placeName: String = "천지연폭포",
    selectedTab: Int = MainTabs.STAMP,
    onSelectTab: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
) {
    val colors = ColoringTheme.colors

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(title = "위치 인증", subtitle = placeName, onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LocationRadiusIndicator(radiusLabel = "반경 200m", inRange = true)
            BorderedStatusCard(title = "인증 가능 지역이에요", subtitle = "현재 위치 · 목적지에서 약 40m")
        }

        Column(modifier = Modifier.padding(20.dp)) {
            PrimaryButton(text = "카메라 열기", onClick = onOpenCamera, modifier = Modifier.fillMaxWidth())
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
private fun LocationVerifyScreenPreview() {
    ColoringJejuTheme { LocationVerifyScreen() }
}
