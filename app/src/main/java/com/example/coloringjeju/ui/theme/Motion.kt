package com.example.coloringjeju.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/* ---------- Motion ---------- */
object ColoringMotion {
    val EaseStandard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    const val DURATION_FAST = 120
    const val DURATION_BASE = 200

    fun <T> fastTween() = tween<T>(durationMillis = DURATION_FAST, easing = EaseStandard)
    fun <T> baseTween() = tween<T>(durationMillis = DURATION_BASE, easing = EaseStandard)
}
