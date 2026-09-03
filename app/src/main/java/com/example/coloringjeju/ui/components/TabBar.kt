package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/** One destination in [BottomTabBar]. */
data class ColoringTabItem(val label: String)

/**
 * The 3 tabs actually implemented as switchable screens (홈 지도 / 스탬프 / 조각모음). The mock's
 * bottom bar shows 5 slots (그룹, 마이 included) but those two have no screen behind them yet, so
 * every screen's [BottomTabBar] is built from this shared 3-item list instead of repeating it.
 */
object MainTabs {
    const val HOME = 0
    const val STAMP = 1
    const val COLLECTION = 2
    val items = listOf("홈", "스탬프", "조각").map { ColoringTabItem(it) }
}

/**
 * `.tabbar` — bottom navigation. The active tab is marked with a filled yellow dot per the
 * "yellow = currently selected" rule; it is not an icon slot, pair it with real nav icons if needed.
 */
@Composable
fun BottomTabBar(
    items: List<ColoringTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ColoringTheme.shapes.lg)
            .background(colors.white)
            .border(1.dp, colors.border, ColoringTheme.shapes.lg)
            .padding(top = 14.dp, bottom = 10.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        items.forEachIndexed { index, item ->
            val isActive = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onSelect(index) },
            ) {
                val dotModifier = if (isActive) {
                    Modifier.background(colors.yellow, CircleShape)
                } else {
                    Modifier.border(2.dp, colors.border, CircleShape)
                }
                Box(modifier = Modifier.size(22.dp).then(dotModifier))
                Text(
                    text = item.label,
                    style = ColoringTheme.typography.caption,
                    color = if (isActive) colors.primaryDark else colors.textTertiary,
                )
            }
        }
    }
}
