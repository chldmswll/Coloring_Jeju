package com.example.coloringjeju.presentation.Auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * `.form-field` — labeled text field for the 로그인/회원가입 forms, e.g. "이메일" above a
 * "you@example.com" box. A bordered card-style box ([ColoringTheme.shapes.md]) rather than
 * [com.example.coloringjeju.ui.components.SearchInput]'s pill, since these stack in a form instead
 * of standing alone.
 */
@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = ColoringTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = ColoringTheme.typography.caption, color = colors.textSecondary)
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .clip(ColoringTheme.shapes.md)
                .background(colors.white)
                .border(1.5.dp, colors.border, ColoringTheme.shapes.md)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (value.isEmpty()) {
                Text(placeholder, style = ColoringTheme.typography.body, color = colors.textTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = ColoringTheme.typography.body.copy(color = colors.textPrimary),
                singleLine = true,
                cursorBrush = SolidColor(colors.primary),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                    capitalization = KeyboardCapitalization.None,
                ),
                keyboardActions = keyboardActions,
            )
        }
    }
}
