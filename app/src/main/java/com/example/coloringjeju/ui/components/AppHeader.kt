package com.example.coloringjeju.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.app-header` — screen title bar with optional back action and subtitle
 * (e.g. "위치 인증" / "천지연폭포").
 */
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val colors = ColoringTheme.colors
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            ColoringIconButton(onClick = onBack, variant = ColoringIconButtonVariant.Outline) {
                Text("‹", style = ColoringTheme.typography.title, color = colors.textSecondary)
            }
            Spacer(Modifier.width(10.dp))
        }
        Column {
            Text(title, style = ColoringTheme.typography.title, color = colors.textPrimary)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = ColoringTheme.typography.caption,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
