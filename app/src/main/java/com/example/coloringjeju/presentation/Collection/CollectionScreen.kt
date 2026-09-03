package com.example.coloringjeju.presentation.Collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.presentation.Collection.components.CollectedPiece
import com.example.coloringjeju.presentation.Collection.components.FinishTripCard
import com.example.coloringjeju.presentation.Collection.components.PieceGrid
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * 08 · 조각모음 — every finished stamp mission ([pieces]: photo + color + place) as a grid of
 * cards, plus the "여행 마무리하기" pamphlet CTA underneath.
 */
@Composable
fun CollectionScreen(
    modifier: Modifier = Modifier,
    pieces: List<CollectedPiece> = emptyList(),
    selectedTab: Int = MainTabs.COLLECTION,
    onSelectTab: (Int) -> Unit = {},
    onCreatePamphlet: () -> Unit = {},
) {
    val colors = ColoringTheme.colors

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(title = "조각모음", subtitle = "여행을 팜플렛으로")

        Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            if (pieces.isEmpty()) {
                Text(
                    "아직 모은 조각이 없어요 · 스탬프에서 인증하면 여기 쌓여요",
                    style = ColoringTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            } else {
                Text(
                    "모은 조각 ${pieces.size}개",
                    style = ColoringTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                PieceGrid(pieces = pieces, modifier = Modifier.weight(1f))
                Spacer(Modifier.height(16.dp))
            }
            FinishTripCard(onCreatePamphlet = onCreatePamphlet, modifier = Modifier.padding(bottom = 20.dp))
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
private fun CollectionScreenPreview() {
    ColoringJejuTheme { CollectionScreen() }
}
