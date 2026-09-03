package com.example.coloringjeju.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme
import kotlin.math.roundToInt

/**
 * `.progress-track` / `.progress-fill` — linear progress with a caption above it
 * (e.g. "선형 진행바 (3/6)").
 */
@Composable
fun LinearProgress(
    progress: Float,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val colors = ColoringTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            caption,
            style = ColoringTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(ColoringTheme.shapes.full)
                .background(colors.border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(ColoringTheme.shapes.full)
                    .background(colors.primary),
            )
        }
    }
}

/**
 * `.progress-ring` — circular gauge (e.g. 색 유사도 78%). [progress] is 0f..1f;
 * [valueLabel] defaults to the rounded percentage, [captionLabel] to "유사도".
 */
@Composable
fun CircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    valueLabel: String = "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
    captionLabel: String = "유사도",
    size: androidx.compose.ui.unit.Dp = 96.dp,
) {
    val colors = ColoringTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = 8.dp.toPx()
                val diameter = this.size.minDimension - strokeWidth
                val topLeft = androidx.compose.ui.geometry.Offset(
                    (this.size.width - diameter) / 2f,
                    (this.size.height - diameter) / 2f,
                )
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = colors.border,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = colors.primary,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(valueLabel, style = ColoringTheme.typography.title, color = colors.textPrimary)
                Text(captionLabel, style = ColoringTheme.typography.caption, color = colors.textSecondary)
            }
        }
    }
}
