package com.muzamil.photoeditor.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.state.PhotoEditorState
import com.muzamil.photoeditor.state.rememberPhotoEditorState
import com.muzamil.photoeditor.ui.theme.PhotoEditorColors
import com.muzamil.photoeditor.ui.theme.PhotoEditorTheme

/**
 * The public entry point of the batteries-included photo editor: theme, top
 * bar, tool panels, and the editing canvas.
 *
 * The editor owns its state, theme, and UI chrome; the *image source* is
 * yours. Either pass a [bitmap] directly, or provide [onPickImage] and load
 * the result into the state yourself — the library deliberately ships no
 * photo-picking flow, so it adds no activity-result dependencies and never
 * dictates how images enter your app:
 * ```
 * val state = rememberPhotoEditorState()
 * val context = LocalContext.current
 * val scope = rememberCoroutineScope()
 * val pickImage = rememberLauncherForActivityResult(
 *     ActivityResultContracts.PickVisualMedia()
 * ) { uri ->
 *     uri ?: return@rememberLauncherForActivityResult
 *     scope.launch {
 *         withContext(Dispatchers.IO) {
 *             decodeEditableBitmap(context.contentResolver, uri)
 *         }?.let(state::loadImage)
 *     }
 * }
 * PhotoEditor(
 *     state = state,
 *     onPickImage = {
 *         pickImage.launch(
 *             PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
 *         )
 *     },
 * )
 * ```
 *
 * Parameters:
 * - [bitmap] — supply an image directly; it is (re)loaded whenever the value
 *   changes. Must be a software bitmap — decode uris with
 *   [com.muzamil.photoeditor.image.decodeEditableBitmap].
 * - [state] — hoist the state holder via [rememberPhotoEditorState] to
 *   observe editor state or drive it programmatically.
 * - [onPickImage] — invoked by the pick-image button; when null, the pick
 *   affordances are hidden entirely (supply [bitmap] instead).
 * - [saveDestination] — where the save button writes the result:
 *   [SaveDestination.Gallery] (public MediaStore, default) or
 *   [SaveDestination.File] for a path you control, e.g. app-private storage:
 *   `SaveDestination.File(File(context.filesDir, "edits/out.jpg"))`.
 * - [colors] — restyle the whole UI by overriding a few [PhotoEditorColors]
 *   tokens; defaults follow the system light/dark setting (see THEMING.md).
 *
 * Edits (elements, tool, styles) survive configuration changes and process
 * death via [PhotoEditorState]'s saver. The bitmap itself is too large for
 * saved state — persist your image uri (e.g. with `rememberSaveable`) and
 * re-decode on restore, as the demo app does.
 *
 * To build a fully custom UI instead, depend on `photoeditor-core` alone and
 * use [com.muzamil.photoeditor.canvas.PhotoEditorCanvas] — the editing
 * surface by itself — driving [PhotoEditorState] from your own controls.
 */
@Composable
fun PhotoEditor(
    modifier: Modifier = Modifier,
    bitmap: Bitmap? = null,
    state: PhotoEditorState = rememberPhotoEditorState(),
    onPickImage: (() -> Unit)? = null,
    saveDestination: SaveDestination = SaveDestination.Gallery(),
    colors: PhotoEditorColors? = null,
) {
    LaunchedEffect(bitmap) {
        bitmap?.let(state::loadImage)
    }

    val content: @Composable () -> Unit = {
        EditorHostContent(
            state = state,
            onPickImage = onPickImage,
            modifier = modifier,
            saveDestination = saveDestination,
        )
    }
    if (colors != null) {
        PhotoEditorTheme(colors = colors, content = content)
    } else {
        PhotoEditorTheme(content = content)
    }
}
