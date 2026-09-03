package com.example.coloringjeju.presentation.Stamp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.components.StampCard
import com.example.coloringjeju.ui.theme.ColoringTheme

/** One row's data in [StampMissionList], e.g. "성산일출봉 · 주황·붉은 (일출) · 인증완료". */
data class StampMissionUi(val title: String, val subtitle: String, val emoji: String, val isDone: Boolean)

/**
 * `스탬프` list — every collectible place and whether its color has been verified yet.
 * Tapping a not-yet-verified row toggles it as [selectedTitle] (yellow ring, picked for the next
 * 인증하기) — kept visually distinct from an already-[isDone] row (mint card, green check) so
 * "verified" and "about to verify" never look the same.
 */
@Composable
fun StampMissionList(
    missions: List<StampMissionUi>,
    selectedTitle: String?,
    onToggleSelect: (StampMissionUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(missions) { mission ->
            val selected = !mission.isDone && mission.title == selectedTitle
            StampCard(
                title = mission.title,
                subtitle = mission.subtitle,
                isDone = mission.isDone,
                isSelected = selected,
                onClick = if (mission.isDone) null else ({ onToggleSelect(mission) }),
                icon = { StampMissionIcon(mission) },
            )
        }
    }
}

@Composable
private fun StampMissionIcon(mission: StampMissionUi) {
    val colors = ColoringTheme.colors
    Text(
        if (mission.isDone) "✓" else mission.emoji,
        style = ColoringTheme.typography.title,
        color = if (mission.isDone) colors.primary else colors.textPrimary,
    )
}
