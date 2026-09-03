package com.example.coloringjeju.presentation.Home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.components.ColoringTag
import com.example.coloringjeju.ui.components.PrimaryButton
import com.example.coloringjeju.ui.components.SecondaryButton
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * A recommended place's detail body — title row (+ add/remove pill), hero photo, tag, headline,
 * description, and a matching full-width button. Shared by the standalone detail screen
 * ([com.example.coloringjeju.presentation.Home.PlaceDetailScreen], 07) and the bottom sheet shown
 * when a map pin is tapped, so both stay visually identical.
 *
 * [isSaved] drives both the pill and the bottom button: false shows "+ MY 지도에 추가" (add),
 * true shows "MY 지도에서 삭제" (remove) — matching whichever tab (추천 지도 / MY 지도) the pin
 * was opened from, since MY 지도 only ever shows places that are already saved.
 */
@Composable
fun PlaceDetailContent(
    name: String,
    tag: String,
    headline: String,
    description: String,
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = ColoringTheme.typography.display, color = colors.textPrimary, modifier = Modifier.weight(1f))
            AddToMapPill(isSaved = isSaved, onClick = onToggleSaved)
        }

        PlaceHeroImage(modifier = Modifier.padding(top = 16.dp))

        ColoringTag(tag, modifier = Modifier.padding(top = 16.dp))
        Text(
            headline,
            style = ColoringTheme.typography.title,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            description,
            style = ColoringTheme.typography.body,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = colors.border)

        if (isSaved) {
            SecondaryButton(text = "MY 지도에서 삭제", onClick = onToggleSaved, modifier = Modifier.fillMaxWidth())
        } else {
            PrimaryButton(text = "MY 지도에 추가", onClick = onToggleSaved, modifier = Modifier.fillMaxWidth())
        }
    }
}
