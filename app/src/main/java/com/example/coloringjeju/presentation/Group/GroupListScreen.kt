package com.example.coloringjeju.presentation.Group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.auth.AuthRepository
import com.example.coloringjeju.core.group.GroupRepository
import com.example.coloringjeju.core.group.GroupResult
import com.example.coloringjeju.core.group.model.TravelGroup
import com.example.coloringjeju.presentation.Auth.components.AuthTextField
import com.example.coloringjeju.presentation.Group.components.GroupCard
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.components.PrimaryButton
import com.example.coloringjeju.ui.components.SecondaryButton
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme
import kotlinx.coroutines.launch

/** Which inline form is open below the two buttons — 그룹 만들기's name field, or 참여하기's code field. */
private enum class GroupFormMode { CREATE, JOIN }

/**
 * 그룹 — 친구와 함께 채우는 지도. Lists every [TravelGroup] the signed-in user belongs to (live from
 * [GroupRepository.myGroups]), with buttons to create a new one or join an existing one by its
 * 6자리 code. Once joined, a group shows up in the `MY 지도 ▾` dropdown on 홈·지도 and 스탬프
 * ([com.example.coloringjeju.ui.components.MapSourceDropdown]) — this screen only manages
 * membership, not the shared map itself.
 *
 * Tapping a card opens [GroupDetailScreen] for it ([selectedGroupCode]) — looked up live out of
 * [groups] rather than held as its own snapshot, so a membership change (e.g. leaving from another
 * device) is reflected immediately, and simply falls back to this list once the code no longer
 * matches anything (covers leaving too — no separate "step back" needed).
 */
@Composable
fun GroupListScreen(
    modifier: Modifier = Modifier,
    selectedTab: Int = MainTabs.GROUP,
    onSelectTab: (Int) -> Unit = {},
) {
    val colors = ColoringTheme.colors
    val currentUser = remember { AuthRepository.currentUser }
    val uid = currentUser?.uid.orEmpty()
    val displayName = currentUser?.displayName?.ifBlank { null } ?: currentUser?.email ?: "익명"
    val scope = rememberCoroutineScope()

    var groups by remember { mutableStateOf<List<TravelGroup>>(emptyList()) }
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) GroupRepository.myGroups(uid).collect { groups = it }
    }
    var selectedGroupCode by remember { mutableStateOf<String?>(null) }
    val openGroup = groups.firstOrNull { it.code == selectedGroupCode }

    if (openGroup != null) {
        GroupDetailScreen(
            group = openGroup,
            uid = uid,
            onBack = { selectedGroupCode = null },
            onLeft = { selectedGroupCode = null },
            modifier = modifier,
            selectedTab = selectedTab,
            onSelectTab = onSelectTab,
        )
        return
    }

    var mode by remember { mutableStateOf<GroupFormMode?>(null) }
    var formValue by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun openForm(next: GroupFormMode) {
        mode = next
        formValue = ""
        errorMessage = null
    }

    fun submitForm() {
        val value = formValue.trim()
        val currentMode = mode ?: return
        if (value.isEmpty()) {
            errorMessage = if (currentMode == GroupFormMode.CREATE) "그룹 이름을 입력해주세요." else "그룹 코드를 입력해주세요."
            return
        }
        isSubmitting = true
        errorMessage = null
        scope.launch {
            val result = when (currentMode) {
                GroupFormMode.CREATE -> GroupRepository.createGroup(value, uid, displayName)
                GroupFormMode.JOIN -> GroupRepository.joinGroup(value, uid, displayName)
            }
            isSubmitting = false
            when (result) {
                is GroupResult.Success -> mode = null
                is GroupResult.Error -> errorMessage = result.message
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(title = "그룹", subtitle = "친구와 함께 지도 채우기")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(text = "그룹 만들기", onClick = { openForm(GroupFormMode.CREATE) }, modifier = Modifier.weight(1f))
                SecondaryButton(text = "코드로 참여하기", onClick = { openForm(GroupFormMode.JOIN) }, modifier = Modifier.weight(1f))
            }

            val currentMode = mode
            if (currentMode != null) {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ColoringTheme.shapes.md)
                        .background(colors.white)
                        .border(1.5.dp, colors.border, ColoringTheme.shapes.md)
                        .padding(18.dp),
                ) {
                    AuthTextField(
                        label = if (currentMode == GroupFormMode.CREATE) "그룹 이름" else "그룹 코드",
                        value = formValue,
                        onValueChange = { formValue = it; errorMessage = null },
                        placeholder = if (currentMode == GroupFormMode.CREATE) "예: 제주 여행 친구들" else "6자리 코드 입력",
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = { submitForm() }),
                    )
                    val currentError = errorMessage
                    if (currentError != null) {
                        Text(currentError, style = ColoringTheme.typography.caption, color = colors.orange, modifier = Modifier.padding(top = 10.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(text = "취소", onClick = { mode = null }, modifier = Modifier.weight(1f))
                        PrimaryButton(
                            text = if (isSubmitting) "처리 중…" else "확인",
                            onClick = ::submitForm,
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            if (groups.isEmpty()) {
                Text(
                    "아직 속한 그룹이 없어요 · 그룹을 만들거나 친구의 코드로 참여해보세요",
                    style = ColoringTheme.typography.caption,
                    color = colors.textSecondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    groups.forEach { group ->
                        GroupCard(group = group, onClick = { selectedGroupCode = group.code })
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
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
private fun GroupListScreenPreview() {
    ColoringJejuTheme { GroupListScreen() }
}
