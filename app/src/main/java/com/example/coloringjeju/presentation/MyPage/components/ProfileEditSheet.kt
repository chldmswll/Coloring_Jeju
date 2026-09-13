package com.example.coloringjeju.presentation.MyPage.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.coloringjeju.presentation.Auth.components.AuthTextField
import com.example.coloringjeju.ui.components.PrimaryButton
import com.example.coloringjeju.ui.theme.ColoringTheme

/**
 * The sheet 마이페이지's ✏️ button opens — edits [com.google.firebase.auth.FirebaseUser.getDisplayName]
 * and the local profile photo ([com.example.coloringjeju.core.local.datastore.ProfilePhotoStore]).
 *
 * The photo applies immediately on picking (there's nowhere further to "save" it to — see
 * [ProfileAvatar]'s doc), so [onPickPhoto] just hands the picked [Uri] straight to the store; only
 * the name is staged locally here until [onSave] is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditSheet(
    initialName: String,
    photoUri: Uri?,
    onPickPhoto: (Uri) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    errorMessage: String? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    val colors = ColoringTheme.colors

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPickPhoto(uri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.cream,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("프로필 수정", style = ColoringTheme.typography.title, color = colors.textPrimary)
            Spacer(Modifier.height(20.dp))

            ProfileAvatar(
                photoUri = photoUri,
                size = 80.dp,
                modifier = Modifier.clickable {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            Text(
                "탭하여 사진 변경",
                style = ColoringTheme.typography.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(24.dp))
            AuthTextField(
                label = "이름",
                value = name,
                onValueChange = { name = it },
                placeholder = "표시할 이름을 입력해주세요",
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { onSave(name.trim()) }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorMessage != null) {
                Text(
                    errorMessage,
                    style = ColoringTheme.typography.caption,
                    color = colors.orange,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (isSaving) "저장 중…" else "저장",
                onClick = { onSave(name.trim()) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
