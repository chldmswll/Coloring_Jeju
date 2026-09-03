package com.example.coloringjeju.presentation.Home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * Hero photo slot on [PlaceDetailScreen]. Stands in for the place's real photo (e.g. 한라산) with
 * a brand-toned gradient until a photo pipeline is wired up.
 */
@Composable
fun PlaceHeroImage(modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(ColoringTheme.radius.lg))
            .background(
                Brush.verticalGradient(listOf(colors.tealLight, colors.primaryDark)),
            ),
    )
}
