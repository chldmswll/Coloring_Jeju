package com.example.coloringjeju.presentation.LocationVerify.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

/**
 * `위치 인증`'s big mint radius card — concentric rings closing in on a checkmark once the
 * traveler is within range, plus the "반경 200m" caption.
 */
@Composable
fun LocationRadiusIndicator(radiusLabel: String, inRange: Boolean, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(ColoringTheme.shapes.xl)
            .background(colors.mint),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .border(2.dp, colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(if (inRange) colors.primary else colors.white),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", style = ColoringTheme.typography.display, color = if (inRange) colors.white else colors.primary)
                }
            }
        }
        Text(
            radiusLabel,
            style = ColoringTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 20.dp),
        )
    }
}
