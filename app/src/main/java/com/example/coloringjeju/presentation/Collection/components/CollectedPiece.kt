package com.example.coloringjeju.presentation.Collection.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

/**
 * One finished stamp mission — captured photo, the color picked from it, and which place it's
 * for. Created when 촬영·색추출's "미션 완료" is pressed; rendered as a card in [PieceGrid].
 */
data class CollectedPiece(
    val placeName: String,
    val photo: Bitmap,
    val color: Color,
)
