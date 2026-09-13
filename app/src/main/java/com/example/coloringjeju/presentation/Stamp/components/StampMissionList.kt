package com.example.coloringjeju.presentation.Stamp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.local.datastore.SavedSpot
import com.example.coloringjeju.ui.components.StampCard
import com.example.coloringjeju.ui.theme.ColoringTheme

/** One row's data in [StampMissionList], e.g. "곽지해수욕장 · 자연 · 인증완료". */
data class StampMissionUi(
    val contentId: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val isDone: Boolean,
)

/**
 * A MY 지도 place as its 스탬프 mission. The two lists are the same places by construction — both
 * are just views of [com.example.coloringjeju.core.local.datastore.SavedSpotsStore] — so the
 * [contentId] carried here is what flips the map marker to color once the mission is verified.
 */
fun SavedSpot.toStampMission() = StampMissionUi(
    contentId = contentId,
    title = title,
    subtitle = "$category · ${if (isVerified) "인증완료" else "미인증"}",
    emoji = categoryEmoji(category),
    isDone = isVerified,
)

/** Stand-in for a place's own photo in the 40dp card slot — one per TourAPI category label. */
private fun categoryEmoji(category: String) = when (category) {
    "자연" -> "🌿"
    "문화" -> "🏛"
    "레포츠" -> "🏄"
    "쇼핑" -> "🛍"
    "카페" -> "☕"
    "맛집" -> "🍽"
    else -> "📍"
}

/**
 * `스탬프` list — every place on MY 지도 and whether its color has been verified yet.
 * Tapping a not-yet-verified row toggles it as [selectedId] (yellow ring, picked for the next
 * 인증하기) — kept visually distinct from an already-[StampMissionUi.isDone] row (mint card, green
 * check) so "verified" and "about to verify" never look the same.
 */
@Composable
fun StampMissionList(
    missions: List<StampMissionUi>,
    selectedId: String?,
    onToggleSelect: (StampMissionUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(missions, key = { it.contentId }) { mission ->
            val selected = !mission.isDone && mission.contentId == selectedId
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
