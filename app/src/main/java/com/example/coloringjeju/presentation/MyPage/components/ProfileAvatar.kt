package com.example.coloringjeju.presentation.MyPage.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * Circular profile photo — [photoUri] comes from
 * [com.example.coloringjeju.core.local.datastore.ProfilePhotoStore]; null shows a plain placeholder
 * circle before one's ever been picked.
 */
@Composable
fun ProfileAvatar(
    photoUri: Uri?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val colors = ColoringTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.mint),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "프로필 사진",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        } else {
            Text("🙂", style = ColoringTheme.typography.title)
        }
    }
}
