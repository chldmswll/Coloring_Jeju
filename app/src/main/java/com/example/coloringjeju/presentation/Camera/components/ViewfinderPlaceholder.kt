package com.example.coloringjeju.presentation.Camera.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * The camera slot on `촬영 · 색 추출`. Before a photo comes back (or if the user backs out of the
 * camera without taking one) this is a fixed-height placeholder; once [photo] is set, the box
 * takes on the photo's own aspect ratio instead, so the shot is shown in full rather than cropped
 * to fit a fixed box.
 */
@Composable
fun ViewfinderPlaceholder(photo: ImageBitmap?, modifier: Modifier = Modifier) {
    val colors = ColoringTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (photo != null) {
                    Modifier.aspectRatio(photo.width.toFloat() / photo.height.toFloat())
                } else {
                    Modifier.height(340.dp)
                },
            )
            .clip(ColoringTheme.shapes.xl)
            .background(colors.forest),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = "촬영한 사진",
                modifier = Modifier.fillMaxWidth().aspectRatio(photo.width.toFloat() / photo.height.toFloat()),
                contentScale = ContentScale.FillWidth,
            )
        } else {
            Text("뷰파인더 (촬영 사진)", style = ColoringTheme.typography.subtitle, color = colors.white.copy(alpha = 0.8f))
        }
    }
}
