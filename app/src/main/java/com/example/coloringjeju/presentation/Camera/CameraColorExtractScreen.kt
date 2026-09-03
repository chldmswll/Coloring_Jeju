package com.example.coloringjeju.presentation.Camera

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.presentation.Camera.components.ViewfinderPlaceholder
import com.example.coloringjeju.presentation.Camera.components.extractTopColors
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.ColorSwatchPicker
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.components.PrimaryButton
import com.example.coloringjeju.ui.components.SecondaryButton
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 05 · 촬영·색추출 — the photo just taken with the device camera (see [photo]) and its
 * representative color. Once [photo] is set, its top 3 colors by coverage are extracted
 * (`Palette`, off the main thread) and replace the placeholder swatches — see [extractTopColors].
 * [selectedTab]/[onSelectTab] are lifted the same way as the tab-root screens so the bottom bar
 * can jump out of this sub-flow at any point. "미션 완료" hands the currently-picked color back
 * via [onComplete] so the caller can save it into 조각모음.
 */
@Composable
fun CameraColorExtractScreen(
    modifier: Modifier = Modifier,
    placeName: String = "천지연폭포",
    photo: Bitmap? = null,
    selectedTab: Int = MainTabs.STAMP,
    onSelectTab: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    onRetake: () -> Unit = {},
    onComplete: (Color) -> Unit = {},
) {
    val colors = ColoringTheme.colors
    var extractedColors by remember { mutableStateOf(listOf(colors.tealLight, colors.teal, colors.primaryDeepest)) }
    var selectedColor by remember { mutableStateOf(extractedColors.first()) }

    LaunchedEffect(photo) {
        val photoBitmap = photo ?: return@LaunchedEffect
        val topColors = withContext(Dispatchers.Default) { photoBitmap.extractTopColors() }
        if (topColors.isNotEmpty()) {
            extractedColors = topColors
            selectedColor = topColors.first()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(title = "촬영 · 색 추출", subtitle = placeName, onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ViewfinderPlaceholder(photo = photo?.asImageBitmap())
            Text(
                "추출된 대표 색상 · 색을 선택해주세요",
                style = ColoringTheme.typography.caption,
                color = colors.textSecondary,
            )
            ColorSwatchPicker(options = extractedColors, selected = selectedColor, onSelect = { selectedColor = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton(text = "다시 찍기", onClick = onRetake, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "미션 완료",
                onClick = { onComplete(selectedColor) },
                modifier = Modifier.weight(1f),
                enabled = photo != null,
            )
        }

        BottomTabBar(
            items = MainTabs.items,
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CameraColorExtractScreenPreview() {
    ColoringJejuTheme { CameraColorExtractScreen() }
}
