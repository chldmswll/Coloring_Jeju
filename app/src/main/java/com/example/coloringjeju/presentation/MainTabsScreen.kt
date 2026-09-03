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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.coloringjeju.presentation.Camera.CameraColorExtractScreen
import com.example.coloringjeju.presentation.Collection.CollectionScreen
import com.example.coloringjeju.presentation.Collection.components.CollectedPiece
import com.example.coloringjeju.presentation.Home.HomeMapScreen
import com.example.coloringjeju.presentation.LocationVerify.LocationVerifyScreen
import com.example.coloringjeju.presentation.Stamp.StampListScreen
import com.example.coloringjeju.ui.components.MainTabs
import com.example.coloringjeju.ui.theme.ColoringJejuTheme

/**
 * Switches between the 3 tab-bar destinations that actually have a screen behind them —
 * 홈·지도 / 스탬프 / 조각모음 — by holding the selected tab here and passing it down, rather than
 * each screen tracking its own. No Navigation Compose: this is a plain state switch, since only
 * these 3 screens need to move between each other (그룹/마이 have no screen yet).
 *
 * Every sub-flow's state (which place is being verified, its captured photo, …) is `remember`ed
 * here too — one level above the `when` — so jumping to another tab via the bottom bar and coming
 * back to this one resumes exactly where it left off instead of resetting to that tab's root
 * screen. [HomeMapScreen] manages its own "내 지도에 여행지 추가하기" sheet and place-detail sheet
 * internally (real draggable bottom sheets), so it needs none of this.
 *
 * The stamp tab: picking a place in [StampListScreen] and tapping "인증하기" swaps it for
 * [LocationVerifyScreen] for that place. "카메라 열기" there launches the device's own camera
 * ([ActivityResultContracts.TakePicturePreview]); once a photo comes back it swaps to
 * [CameraColorExtractScreen] showing that photo in the viewfinder. "미션 완료" there pops a
 * congratulations toast, saves the photo + picked color + place as a [CollectedPiece] — shown as a
 * card in 조각모음 — and steps back down to [StampListScreen]. Each screen's back button steps one
 * level back down this same chain without saving anything.
 */
@Composable
fun MainTabsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(MainTabs.HOME) }
    var verifyingPlace by remember { mutableStateOf<String?>(null) }
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
                photo != null -> CameraColorExtractScreen(
                    modifier = modifier.fillMaxSize(),
                    placeName = place ?: "천지연폭포",
                    photo = photo,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onBack = { capturedPhoto = null },
                    onRetake = { cameraLauncher.launch(null) },
                    onComplete = { color ->
                        collectedPieces = collectedPieces + CollectedPiece(
                            placeName = place ?: "천지연폭포",
                            photo = photo,
                            color = color,
                        )
                        capturedPhoto = null
                        verifyingPlace = null
                        Toast.makeText(context, "축하합니다!\n미션에 성공했어요!", Toast.LENGTH_SHORT).apply {
                            setGravity(Gravity.CENTER, 0, 0)
                        }.show()
                    },
                )
                place != null -> LocationVerifyScreen(
                    modifier = modifier.fillMaxSize(),
                    placeName = place,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onBack = { verifyingPlace = null },
                    onOpenCamera = { cameraLauncher.launch(null) },
                )
                else -> StampListScreen(
                    modifier = modifier.fillMaxSize(),
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onVerifyPlace = { verifyingPlace = it },
                )
            }
        }
        MainTabs.COLLECTION -> CollectionScreen(
            modifier = modifier.fillMaxSize(),
            pieces = collectedPieces,
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
