package com.example.coloringjeju.presentation.Group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.core.group.GroupRepository
import com.example.coloringjeju.core.group.model.GroupSpot
import com.example.coloringjeju.core.group.model.TravelGroup
import com.example.coloringjeju.presentation.Group.components.GroupRecordRow
import com.example.coloringjeju.ui.components.AppHeader
import com.example.coloringjeju.ui.components.BottomTabBar
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.components.SecondaryButton
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.example.coloringjeju.ui.theme.ColoringTheme
import kotlinx.coroutines.launch

/**
 * One [TravelGroup]'s detail — opened by tapping its card in [GroupListScreen]. Shows who's in it
 * ([TravelGroup.members]), and its 기록 list — every place a member has added to the group's shared
 * map ([GroupRepository.groupSpots], live), with whether each one's color mission is done. 그룹
 * 나가기 lives here rather than on the list card, since leaving is a step you take once you're
 * looking at the group, not a swipe-away action on a summary row.
 *
 * [uid] is the signed-in user's own id, so leaving updates the right membership entry;
 * [onLeft] fires after a successful leave so [GroupListScreen] can step back to the list (its own
 * live [GroupRepository.myGroups] listener also drops this group from [group] right after).
 */
@Composable
fun GroupDetailScreen(
    group: TravelGroup,
    uid: String,
    onBack: () -> Unit,
    onLeft: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: Int = MainTabs.GROUP,
    onSelectTab: (Int) -> Unit = {},
) {
    val colors = ColoringTheme.colors
    val scope = rememberCoroutineScope()

    var records by remember { mutableStateOf<List<GroupSpot>>(emptyList()) }
    LaunchedEffect(group.code) {
        GroupRepository.groupSpots(group.code).collect { records = it }
    }
    var isLeaving by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(colors.offWhite)) {
        AppHeader(
            title = group.name,
            subtitle = "코드 ${group.code} · 멤버 ${group.memberUids.size}명",
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text("멤버", style = ColoringTheme.typography.title, color = colors.textPrimary)
            Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.memberUids.forEach { memberUid ->
                    val name = group.members[memberUid] ?: "알 수 없음"
                    Text(
                        if (memberUid == group.ownerUid) "$name (방장)" else name,
                        style = ColoringTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("기록 ${records.size}개", style = ColoringTheme.typography.title, color = colors.textPrimary)
            if (records.isEmpty()) {
                Text(
                    "아직 이 그룹 지도에 추가된 기록이 없어요.\n홈·지도에서 MY 지도 ▾ 드롭다운을 이 그룹으로 바꾸고 여행지를 추가해보세요.",
                    style = ColoringTheme.typography.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            } else {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    records.forEach { spot ->
                        GroupRecordRow(spot = spot, addedByName = group.members[spot.addedByUid] ?: "알 수 없음")
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SecondaryButton(
                text = if (isLeaving) "나가는 중…" else "그룹 나가기",
                onClick = {
                    isLeaving = true
                    scope.launch {
                        GroupRepository.leaveGroup(group.code, uid)
                        isLeaving = false
                        onLeft()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
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
private fun GroupDetailScreenPreview() {
    ColoringJejuTheme {
        GroupDetailScreen(
            group = TravelGroup(
                code = "ABC123",
                name = "제주 여행 친구들",
                ownerUid = "u1",
                memberUids = listOf("u1", "u2"),
                members = mapOf("u1" to "지민", "u2" to "수아"),
            ),
            uid = "u1",
            onBack = {},
            onLeft = {},
        )
    }
}
