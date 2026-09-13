package com.example.coloringjeju.presentation

import android.graphics.Bitmap
import android.view.Gravity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coloringjeju.core.group.GroupRepository
import com.example.coloringjeju.core.group.model.MapSource
import com.example.coloringjeju.core.local.datastore.SavedSpot
import com.example.coloringjeju.core.local.datastore.SavedSpotsStore
import com.example.coloringjeju.presentation.Camera.CameraColorExtractScreen
import com.example.coloringjeju.presentation.Collection.CollectionScreen
import com.example.coloringjeju.presentation.Collection.components.CollectedPiece
import com.example.coloringjeju.presentation.Group.GroupListScreen
import com.example.coloringjeju.presentation.Home.HomeMapScreen
import com.example.coloringjeju.presentation.LocationVerify.LocationVerifyScreen
import com.example.coloringjeju.presentation.MyPage.MyPageScreen
import com.example.coloringjeju.presentation.Stamp.StampListScreen
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import kotlinx.coroutines.launch

/**
 * Switches between the 5 tab-bar destinations — 홈·지도 / 스탬프 / 조각모음 / 그룹 / 마이페이지 —
 * by holding the selected tab here and passing it down, rather than each screen tracking its own.
 * No Navigation Compose: this is a plain state switch.
 *
 * Every sub-flow's state (which place is being verified, its captured photo, …) is `remember`ed
 * here too — one level above the `when` — so jumping to another tab via the bottom bar and coming
 * back to this one resumes exactly where it left off instead of resetting to that tab's root
 * screen. [HomeMapScreen] manages its own "내 지도에 여행지 추가하기" sheet and place-detail sheet
 * internally (real draggable bottom sheets), so it needs none of this.
 *
 * MY 지도 and 스탬프 are two views of one list: [SavedSpotsStore] is collected here and handed to
 * [StampListScreen], while [HomeMapScreen] collects the same shared store. Adding a place on the
 * map creates its stamp mission; completing that mission writes the captured color back onto the
 * place, which checks off its stamp row *and* turns its map marker from grayscale to color.
 *
 * The stamp tab: picking a place in [StampListScreen] and tapping "인증하기" swaps it for
 * [LocationVerifyScreen] for that place. "카메라 열기" there launches the device's own camera
 * ([ActivityResultContracts.TakePicturePreview]); once a photo comes back it swaps to
 * [CameraColorExtractScreen] showing that photo in the viewfinder. "미션 완료" there pops a
 * congratulations toast, saves the photo + picked color + place as a [CollectedPiece] — shown as a
 * card in 조각모음 — and steps back down to [StampListScreen]. [verifyingSource] (carried alongside
 * [verifyingPlace] from [StampListScreen]'s own `MY 지도 ▾` selection) decides where the color is
 * written back to: [SavedSpotsStore] for a personal place, or [GroupRepository] for a group's shared
 * one — either way that write is what checks off the stamp row *and* turns the matching map marker
 * from grayscale to color. Each screen's back button steps one level back down this same chain
 * without saving anything.
 */
@Composable
fun MainTabsScreen(modifier: Modifier = Modifier, onLoggedOut: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedStore = remember { SavedSpotsStore.get(context) }
    val savedSpots by savedStore.spots.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(MainTabs.HOME) }
    var verifyingPlace by remember { mutableStateOf<SavedSpot?>(null) }
    var verifyingSource by remember { mutableStateOf<MapSource>(MapSource.Personal) }
    var capturedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var collectedPieces by remember { mutableStateOf(listOf<CollectedPiece>()) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) capturedPhoto = bitmap
    }

    when (selectedTab) {
        MainTabs.STAMP -> {
            val photo = capturedPhoto
            val place = verifyingPlace
            when {
                photo != null && place != null -> CameraColorExtractScreen(
                    modifier = modifier.fillMaxSize(),
                    placeName = place.title,
                    photo = photo,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onBack = { capturedPhoto = null },
                    onRetake = { cameraLauncher.launch(null) },
                    onComplete = { color ->
                        collectedPieces = collectedPieces + CollectedPiece(
                            placeName = place.title,
                            photo = photo,
                            color = color,
                        )
                        // The same write checks off the stamp row and colors the place's map
                        // marker — until it runs, both stay in their unverified state.
                        when (val source = verifyingSource) {
                            MapSource.Personal -> savedStore.markVerified(place.contentId, color.toArgb())
                            is MapSource.Group -> scope.launch {
                                GroupRepository.markSpotVerified(source.group.code, place.contentId, color.toArgb())
                            }
                        }
                        capturedPhoto = null
                        verifyingPlace = null
                        verifyingSource = MapSource.Personal
                        Toast.makeText(context, "축하합니다!\n미션에 성공했어요!", Toast.LENGTH_SHORT).apply {
                            setGravity(Gravity.CENTER, 0, 0)
                        }.show()
                    },
                )
                place != null -> LocationVerifyScreen(
                    modifier = modifier.fillMaxSize(),
                    placeName = place.title,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onBack = { verifyingPlace = null },
                    onOpenCamera = { cameraLauncher.launch(null) },
                )
                else -> StampListScreen(
                    modifier = modifier.fillMaxSize(),
                    spots = savedSpots,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onVerifyPlace = { spot, source -> verifyingPlace = spot; verifyingSource = source },
                )
            }
        }
        MainTabs.COLLECTION -> CollectionScreen(
            modifier = modifier.fillMaxSize(),
            pieces = collectedPieces,
            selectedTab = selectedTab,
            onSelectTab = { selectedTab = it },
        )
        MainTabs.GROUP -> GroupListScreen(
            modifier = modifier.fillMaxSize(),
            selectedTab = selectedTab,
            onSelectTab = { selectedTab = it },
        )
        MainTabs.MY -> MyPageScreen(
            modifier = modifier.fillMaxSize(),
            onLoggedOut = onLoggedOut,
            selectedTab = selectedTab,
            onSelectTab = { selectedTab = it },
        )
        else -> HomeMapScreen(
            modifier = modifier.fillMaxSize(),
            selectedTab = selectedTab,
            onSelectTab = { selectedTab = it },
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun MainTabsScreenPreview() {
    ColoringJejuTheme { MainTabsScreen() }
}
