package com.example.coloringjeju.presentation.Group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.group.model.GroupSpot
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * One row in [com.example.coloringjeju.presentation.Group.GroupDetailScreen]'s 기록 list — a place
 * someone in the group added, who added it, and whether its color mission is done yet. [addedByName]
 * is looked up from [com.example.coloringjeju.core.group.model.TravelGroup.members] by
 * [GroupSpot.addedByUid] — falls back to a placeholder for a member who has since left the group.
 */
@Composable
fun GroupRecordRow(
    spot: GroupSpot,
    addedByName: String,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(ColoringTheme.shapes.md)
            .background(if (spot.isVerified) colors.mint else colors.white)
            .border(1.5.dp, if (spot.isVerified) colors.primary else colors.border, ColoringTheme.shapes.md)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(spot.verifiedColor?.let { Color(it) } ?: colors.offWhite),
            contentAlignment = Alignment.Center,
        ) {
            if (!spot.isVerified) Text("📍", style = ColoringTheme.typography.caption)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(spot.title, style = ColoringTheme.typography.subtitle, color = colors.textPrimary)
            Text(
                "$addedByName 추가 · ${if (spot.isVerified) "인증완료" else "미인증"}",
                style = ColoringTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
