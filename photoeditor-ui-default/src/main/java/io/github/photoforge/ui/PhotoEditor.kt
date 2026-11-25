package io.github.photoforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.photoforge.export.SaveDestination
import io.github.photoforge.state.PhotoEditorState
import io.github.photoforge.state.rememberPhotoEditorState
import io.github.photoforge.ui.theme.PhotoEditorColors
import io.github.photoforge.ui.theme.PhotoEditorDefaults
import io.github.photoforge.ui.theme.PhotoEditorTheme
import io.github.photoforge.ui.theme.PhotoEditorTypography
import io.github.photoforge.ui.theme.darkPhotoEditorColors
import io.github.photoforge.ui.theme.lightPhotoEditorColors

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
 *   changes. Decode uris with
 *   [io.github.photoforge.image.decodeEditableBitmap], which caps the size.
 * - [state] — hoist the state holder via [rememberPhotoEditorState] to
 *   observe editor state or drive it programmatically.
 * - [onPickImage] — invoked by the pick-image button; when null, the pick
 *   affordances are hidden entirely (supply [bitmap] instead).
 * - [saveDestination] — where the save button writes the result:
 *   [SaveDestination.Gallery] (public MediaStore, default) or
 *   [SaveDestination.File] for a path you control, e.g. app-private storage:
 *   `SaveDestination.File(File(context.filesDir, "edits/out.jpg"))`.
 * - [colors] — restyle the whole UI by overriding a few [PhotoEditorColors]
 *   tokens; defaults follow the system light/dark setting. Pass
 *   [PhotoEditorDefaults.colorsFromMaterialTheme] to follow the host app's
 *   Material theme instead (see THEMING.md).
 * - [typography] — override the [PhotoEditorTypography] text-style tokens;
 *   null uses the defaults (system font, built-in sizes).
 * - [onClose] — invoked by the close button in the top bar; when null, the
 *   close button is hidden.
 * - [emojis] — the Emoji panel's grid. Plain strings rendered with the system
 *   emoji font; any string works and renders at sticker size, so short text
 *   is a supported text-sticker shortcut.
 * - [swatchColors] — the content palette shared by every color picker (Shape,
 *   Draw, Text). Separate from [colors], which themes the editor's chrome.
 *
 * Edits (elements, tool, styles) survive configuration changes and process
 * death via [PhotoEditorState]'s saver. The bitmap itself is too large for
 * saved state — persist your image uri (e.g. with `rememberSaveable`) and
 * re-decode on restore, as the demo app does.
 *
 * To build a fully custom UI instead, depend on `photoeditor-core` alone and
 * use [io.github.photoforge.canvas.PhotoEditorCanvas] — the editing
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
    typography: PhotoEditorTypography? = null,
    onClose: (() -> Unit)? = null,
    emojis: List<String> = PhotoEditorDefaults.emojis,
    swatchColors: List<Color> = PhotoEditorDefaults.swatchColors,
) {
    LaunchedEffect(bitmap) {
        bitmap?.let(state::loadImage)
    }

    PhotoEditorTheme(
        colors = colors ?: if (isSystemInDarkTheme()) darkPhotoEditorColors()
        else lightPhotoEditorColors(),
        typography = typography ?: PhotoEditorTypography(),
    ) {
        EditorHostContent(
            state = state,
            onPickImage = onPickImage,
            modifier = modifier,
            onClose = onClose,
            saveDestination = saveDestination,
            emojis = emojis,
            swatchColors = swatchColors,
        )
    }
}
