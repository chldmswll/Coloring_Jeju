package com.example.coloringjeju.presentation.Auth

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.auth.AuthRepository
import com.example.coloringjeju.core.auth.AuthResult
import com.example.coloringjeju.core.auth.AutoLoginPreferences
import com.example.coloringjeju.presentation.Auth.components.AuthTextField
import com.example.coloringjeju.ui.components.PrimaryButton
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

/** Which form is on screen — 로그인 is the default landing, 회원가입 a text link away and back. */
private enum class AuthMode { LOGIN, SIGN_UP }

/**
 * Email/password 로그인·회원가입 gate — [com.example.coloringjeju.MainActivity] shows this in front
 * of [com.example.coloringjeju.presentation.MainTabsScreen] whenever [AuthRepository.currentUser] is
 * null, and swaps to the tabs once [onAuthenticated] fires.
 *
 * Holds its own form state the same way [com.example.coloringjeju.presentation.MainTabsScreen] holds
 * tab state — no ViewModel anywhere in this codebase, so a plain `remember` plus an [AuthRepository]
 * suspend call from [rememberCoroutineScope] is the pattern here too. Firebase persists the session
 * locally on its own; this screen's only job is to produce one [FirebaseUser] and get out of the way.
 */
@Composable
fun AuthScreen(
    onAuthenticated: (FirebaseUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var autoLogin by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val colors = ColoringTheme.colors

    fun submit() {
        val trimmedEmail = email.trim()
        val validationError = when {
            trimmedEmail.isEmpty() || password.isEmpty() -> "이메일과 비밀번호를 입력해주세요."
            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> "이메일 형식이 올바르지 않아요."
            password.length < 6 -> "비밀번호는 6자 이상으로 입력해주세요."
            mode == AuthMode.SIGN_UP && password != passwordConfirm -> "비밀번호가 서로 일치하지 않아요."
            else -> null
        }
        if (validationError != null) {
            errorMessage = validationError
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = if (mode == AuthMode.LOGIN) {
                AuthRepository.signIn(trimmedEmail, password)
            } else {
                AuthRepository.signUp(trimmedEmail, password)
            }
            isLoading = false
            when (result) {
                is AuthResult.Success -> {
                    // 회원가입 직후는 늘 로그인 상태를 유지하고, 로그인은 체크박스를 따른다 — 다음 실행 시
                    // MainActivity가 이 값을 보고 세션을 그대로 둘지 로그아웃시킬지 정한다.
                    AutoLoginPreferences.setEnabled(context, if (mode == AuthMode.LOGIN) autoLogin else true)
                    onAuthenticated(result.user)
                }
                is AuthResult.Error -> errorMessage = result.message
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "컬러링 제주",
            style = ColoringTheme.typography.display,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (mode == AuthMode.LOGIN) "로그인하고 여행을 이어가요" else "회원가입하고 여행을 시작해요",
            style = ColoringTheme.typography.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 32.dp),
        )

        AuthTextField(
            label = "이메일",
            value = email,
            onValueChange = { email = it; errorMessage = null },
            placeholder = "you@example.com",
            keyboardType = KeyboardType.Email,
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        )
        Spacer(Modifier.height(16.dp))
        AuthTextField(
            label = "비밀번호",
            value = password,
            onValueChange = { password = it; errorMessage = null },
            placeholder = "6자 이상 입력해주세요",
            isPassword = true,
            imeAction = if (mode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next,
            keyboardActions = if (mode == AuthMode.LOGIN) {
                KeyboardActions(onDone = { submit() })
            } else {
                KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            },
        )
        if (mode == AuthMode.SIGN_UP) {
            Spacer(Modifier.height(16.dp))
            AuthTextField(
                label = "비밀번호 확인",
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it; errorMessage = null },
                placeholder = "비밀번호를 다시 입력해주세요",
                isPassword = true,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable(enabled = !isLoading) { autoLogin = !autoLogin },
            ) {
                Checkbox(
                    checked = autoLogin,
                    onCheckedChange = { autoLogin = it },
                    enabled = !isLoading,
                    colors = CheckboxDefaults.colors(checkedColor = colors.primary),
                )
                Text("자동 로그인", style = ColoringTheme.typography.caption, color = colors.textSecondary)
            }
        }

        val currentError = errorMessage
        if (currentError != null) {
            Text(
                currentError,
                style = ColoringTheme.typography.caption,
                color = colors.orange,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = when {
                isLoading -> "처리 중…"
                mode == AuthMode.LOGIN -> "로그인"
                else -> "회원가입"
            },
            onClick = ::submit,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            if (mode == AuthMode.LOGIN) "아직 계정이 없으신가요? 회원가입" else "이미 계정이 있으신가요? 로그인",
            style = ColoringTheme.typography.caption,
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading) {
                    mode = if (mode == AuthMode.LOGIN) AuthMode.SIGN_UP else AuthMode.LOGIN
                    errorMessage = null
                },
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AuthScreenPreview() {
    ColoringJejuTheme { AuthScreen(onAuthenticated = {}) }
}
