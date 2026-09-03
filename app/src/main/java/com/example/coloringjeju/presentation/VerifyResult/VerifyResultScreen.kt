package com.example.coloringjeju.presentation.VerifyResult

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.presentation.VerifyResult.components.CompareColorRow
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BorderedStatusCard
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.CircularProgress
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.components.PrimaryButton
import com.example.coloringjeju.ui.components.SecondaryButton
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * 06 · 인증결과 — how closely the captured photo's color matched the place's target color.
 * [selectedTab]/[onSelectTab] are lifted like the other 스탬프-flow screens (see
 * [com.example.coloringjeju.presentation.LocationVerify.LocationVerifyScreen]).
 */
@Composable
fun VerifyResultScreen(
    modifier: Modifier = Modifier,
    placeName: String = "천지연폭포",
    similarity: Float = 0.78f,
    selectedTab: Int = MainTabs.STAMP,
    onSelectTab: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    onPostToGroup: () -> Unit = {},
    onReturnToMap: () -> Unit = {},
) {
    val colors = ColoringTheme.colors

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(title = "인증 결과", subtitle = placeName, onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CircularProgress(progress = similarity)
            BorderedStatusCard(title = "인증 성공! 🎉", subtitle = "70% 이상 일치 · 스탬프에 이 색이 채워져요")
            CompareColorRow(targetColor = colors.tealLight, capturedColor = colors.teal)
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(text = "그룹 피드에 올리기", onClick = onPostToGroup, modifier = Modifier.fillMaxWidth())
            SecondaryButton(text = "지도로 돌아가기", onClick = onReturnToMap, modifier = Modifier.fillMaxWidth())
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
private fun VerifyResultScreenPreview() {
    ColoringJejuTheme { VerifyResultScreen() }
}
