package com.example.coloringjeju.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.group.model.MapSource
import com.example.coloringjeju.core.group.model.TravelGroup
import com.example.coloringjeju.core.group.model.label
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `MY 지도 ▾` pill — picks between the traveler's own map and any [TravelGroup] they've joined.
 * Shared by 홈·지도's MY 지도 tab and 스탬프's list so both read the exact same [MapSource].
 */
@Composable
fun MapSourceDropdown(
    selected: MapSource,
    groups: List<TravelGroup>,
    onSelect: (MapSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(ColoringTheme.shapes.full)
                .background(colors.white)
                .border(1.dp, colors.border, ColoringTheme.shapes.full)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(selected.label(), style = ColoringTheme.typography.subtitle, color = colors.textPrimary)
            Text(" ▾", style = ColoringTheme.typography.subtitle, color = colors.textTertiary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("MY 지도") }, onClick = { expanded = false; onSelect(MapSource.Personal) })
            groups.forEach { group ->
                DropdownMenuItem(text = { Text(group.name) }, onClick = { expanded = false; onSelect(MapSource.Group(group)) })
            }
        }
    }
}
