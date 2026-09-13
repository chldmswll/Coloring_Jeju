package com.example.coloringjeju.presentation.MyPage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coloringjeju.core.auth.AuthRepository
import com.example.coloringjeju.core.auth.AuthResult
import com.example.coloringjeju.core.local.datastore.ProfilePhotoStore
import com.example.coloringjeju.presentation.MyPage.components.ProfileAvatar
import com.example.coloringjeju.presentation.MyPage.components.ProfileEditSheet
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.ColoringIconButton
import com.example.coloringjeju.ui.components.ColoringIconButtonVariant
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.components.SecondaryButton
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme
import kotlinx.coroutines.launch

/**
 * 마이페이지 — the account slot at the right end of [MainTabs.items]. Shows who's signed in (from
 * [AuthRepository.currentUser], mirrored into [user] so a name/photo edit shows up immediately
 * without waiting on a recomposition trigger elsewhere) plus a 로그아웃 button, and the ✏️ button in
 * the header opens [ProfileEditSheet] to change the display name and photo.
 *
 * [onLoggedOut] is required, not optional, because a logout with nowhere to report back to would
 * leave [com.example.coloringjeju.presentation.MainTabsScreen] showing tabs for a session that's
 * already gone — the caller (ultimately [com.example.coloringjeju.MainActivity]) must swap back to
 * [com.example.coloringjeju.presentation.Auth.AuthScreen] when it fires.
 */
@Composable
fun MyPageScreen(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: Int = MainTabs.MY,
    onSelectTab: (Int) -> Unit = {},
) {
    val colors = ColoringTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoStore = remember { ProfilePhotoStore.get(context) }
    val photoUri by photoStore.photoUri.collectAsStateWithLifecycle()

    var user by remember { mutableStateOf(AuthRepository.currentUser) }
    var showEditSheet by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(
            title = "마이페이지",
            subtitle = "계정 정보",
            trailing = {
                ColoringIconButton(
                    onClick = { saveError = null; showEditSheet = true },
                    variant = ColoringIconButtonVariant.Outline,
                ) {
                    Text("✏️", style = ColoringTheme.typography.subtitle)
                }
            },
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ColoringTheme.shapes.md)
                    .background(colors.white)
                    .border(1.5.dp, colors.border, ColoringTheme.shapes.md)
                    .padding(18.dp),
            ) {
                ProfileAvatar(photoUri = photoUri)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        user?.displayName?.ifBlank { null } ?: "이름을 설정해주세요",
                        style = ColoringTheme.typography.subtitle,
                        color = colors.textPrimary,
                    )
                    Text(
                        user?.email ?: "알 수 없음",
                        style = ColoringTheme.typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SecondaryButton(
                text = "로그아웃",
                onClick = {
                    AuthRepository.signOut()
                    onLoggedOut()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        BottomTabBar(
            items = MainTabs.items,
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.padding(12.dp),
        )
    }

    if (showEditSheet) {
        ProfileEditSheet(
            initialName = user?.displayName.orEmpty(),
            photoUri = photoUri,
            onPickPhoto = { photoStore.save(it) },
            isSaving = isSaving,
            errorMessage = saveError,
            onDismiss = { showEditSheet = false },
            onSave = { name ->
                if (name.isBlank()) {
                    saveError = "이름을 입력해주세요."
                    return@ProfileEditSheet
                }
                isSaving = true
                saveError = null
                scope.launch {
                    when (val result = AuthRepository.updateProfile(name)) {
                        is AuthResult.Success -> {
                            user = result.user
                            isSaving = false
                            showEditSheet = false
                        }
                        is AuthResult.Error -> {
                            isSaving = false
                            saveError = result.message
                        }
                    }
                }
            },
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun MyPageScreenPreview() {
    ColoringJejuTheme { MyPageScreen(onLoggedOut = {}) }
}
