package com.example.coloringjeju.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.btn-primary` — main call to action. Use sparingly, one per screen/section.
 * Pass `enabled = false` for the `.btn-disabled` state.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ColoringTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ColoringTheme.shapes.full,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.white,
            disabledContainerColor = colors.border,
            disabledContentColor = colors.textTertiary,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp),
    ) {
        Text(text, style = ColoringTheme.typography.button)
    }
}

/** `.btn-secondary` — secondary/alternative action, paired next to a primary button. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ColoringTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ColoringTheme.shapes.full,
        border = BorderStroke(1.5.dp, colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.white,
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textTertiary,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp),
    ) {
        Text(text, style = ColoringTheme.typography.button)
    }
}

/** Background/content variants for [ColoringIconButton], matching `.btn-icon-*` in the style guide. */
enum class ColoringIconButtonVariant { Yellow, Primary, Outline }

/**
 * `.btn-icon` — 44dp round icon button. [ColoringIconButtonVariant.Yellow] marks a floating "add"
 * action, [ColoringIconButtonVariant.Primary] a confirm action, [ColoringIconButtonVariant.Outline]
 * a neutral action like back/close.
 */
@Composable
fun ColoringIconButton(
    onClick: () -> Unit,
    variant: ColoringIconButtonVariant,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = ColoringTheme.colors
    val background = when (variant) {
        ColoringIconButtonVariant.Yellow -> colors.yellow
        ColoringIconButtonVariant.Primary -> colors.primary
        ColoringIconButtonVariant.Outline -> colors.white
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (variant == ColoringIconButtonVariant.Outline) {
                    Modifier.border(1.5.dp, colors.border, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
