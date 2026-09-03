package com.example.coloringjeju.presentation.Camera.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

/**
 * The photo's top [maxColors] colors by how much of the image they cover, most-prominent first —
 * feeds [com.example.coloringjeju.ui.components.ColorSwatchPicker] on `촬영 · 색 추출` once a photo
 * comes back from the camera. Runs `Palette`'s quantization, which is CPU-bound — call this off
 * the main thread (see [CameraColorExtractScreen]'s `Dispatchers.Default`).
 */
fun Bitmap.extractTopColors(maxColors: Int = 3): List<Color> =
    Palette.from(this)
        .maximumColorCount(24)
        .generate()
        .swatches
        .sortedByDescending { it.population }
        .take(maxColors)
        .map { Color(it.rgb) }
