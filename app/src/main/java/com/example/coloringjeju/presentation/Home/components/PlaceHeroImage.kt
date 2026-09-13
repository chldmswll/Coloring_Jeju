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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * Hero photo slot on the place detail sheet/screen — the place's real TourAPI photo
 * (`firstimage`), center-cropped.
 *
 * The brand gradient it used to be is now only the backdrop: it shows through while the photo
 * loads, and stays as-is for the places TourAPI has no photo for. That gap is real — 숙박 is only
 * ~63% covered — so this never renders an empty box.
 */
@Composable
fun PlaceHeroImage(imageUrl: String?, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(ColoringTheme.radius.lg))
            .background(
                Brush.verticalGradient(listOf(colors.tealLight, colors.primaryDark)),
            ),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
