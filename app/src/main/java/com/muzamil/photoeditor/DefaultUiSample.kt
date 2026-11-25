package com.muzamil.photoeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.photoforge.image.decodeEditableBitmap
import io.github.photoforge.state.rememberPhotoEditorState
import io.github.photoforge.ui.PhotoEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mode B sample: the batteries-included editor from :photoeditor-ui-default.
 *
 * The library ships no photo picking — image sourcing belongs to the app —
 * so this hosts the system photo picker, decodes the result with
 * [decodeEditableBitmap], and hands the bitmap to the editor.
 *
 * The picked uri is persisted with rememberSaveable and re-decoded on
 * restore, so the image survives rotation and process death alongside the
 * edits (which the library's state saver retains on its own).
 */
@Composable
fun DefaultUiSample(onClose: (() -> Unit)? = null) {
    val context = LocalContext.current
    val state = rememberPhotoEditorState()
    var pickedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedImageUri = uri
    }

    LaunchedEffect(pickedImageUri) {
        val uri = pickedImageUri ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            decodeEditableBitmap(context.contentResolver, uri)
        }?.let(state::loadImage)
    }

    PhotoEditor(
        state = state,
        onPickImage = {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onClose = onClose,
    )
}
