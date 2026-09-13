package com.example.coloringjeju.presentation.Group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.group.model.TravelGroup
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * One row in [com.example.coloringjeju.presentation.Group.GroupListScreen] — name, member count,
 * and the invite [TravelGroup.code] (tap to copy so it's easy to text a friend). Tapping the rest of
 * the card opens [com.example.coloringjeju.presentation.Group.GroupDetailScreen] (members, 기록
 * list, and the 나가기 button live there instead of here).
 */
@Composable
fun GroupCard(
    group: TravelGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ColoringTheme.shapes.md)
            .background(colors.white)
            .border(1.5.dp, colors.border, ColoringTheme.shapes.md)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(group.name, style = ColoringTheme.typography.subtitle, color = colors.textPrimary)
        Text(
            "멤버 ${group.memberUids.size}명",
            style = ColoringTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )

        // Its own nested clickable — tapping just this row copies the code without opening the
        // detail screen the rest of the card opens.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 10.dp)
                .clickable { clipboard.setText(AnnotatedString(group.code)) },
        ) {
            Text("코드 ${group.code}", style = ColoringTheme.typography.subtitle, color = colors.primary)
            Spacer(Modifier.width(6.dp))
            Text("(탭하여 복사)", style = ColoringTheme.typography.caption, color = colors.textTertiary)
        }
    }
}
