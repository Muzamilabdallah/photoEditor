package com.muzamil.photoeditor.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.state.PhotoEditorState
import com.muzamil.photoeditor.state.rememberPhotoEditorState
import com.muzamil.photoeditor.ui.theme.PhotoEditorTheme
import kotlin.math.max

/**
 * The public entry point of the photo editor.
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
 *   [decodeEditableBitmap].
 * - [state] — hoist the state holder via [rememberPhotoEditorState] to
 *   observe editor state or drive it programmatically.
 * - [onPickImage] — invoked by the pick-image button; when null, the pick
 *   affordances are hidden entirely (supply [bitmap] instead).
 * - [saveDestination] — where the save button writes the result:
 *   [SaveDestination.Gallery] (public MediaStore, default) or
 *   [SaveDestination.File] for a path you control, e.g. app-private storage:
 *   `SaveDestination.File(File(context.filesDir, "edits/out.jpg"))`.
 *
 * Edits (elements, tool, styles) survive configuration changes and process
 * death via [PhotoEditorState]'s saver. The bitmap itself is too large for
 * saved state — persist your image uri (e.g. with `rememberSaveable`) and
 * re-decode on restore, as the demo app does.
 *
 * To build a fully custom UI instead, use [PhotoEditorCanvas] — the editing
 * surface alone — and drive [PhotoEditorState] from your own controls.
 */
@Composable
fun PhotoEditor(
    modifier: Modifier = Modifier,
    bitmap: Bitmap? = null,
    state: PhotoEditorState = rememberPhotoEditorState(),
    onPickImage: (() -> Unit)? = null,
    saveDestination: SaveDestination = SaveDestination.Gallery(),
) {
    LaunchedEffect(bitmap) {
        bitmap?.let(state::loadImage)
    }

    PhotoEditorTheme {
        EditorHostContent(
            state = state,
            onPickImage = onPickImage,
            modifier = modifier,
            saveDestination = saveDestination,
        )
    }
}

/**
 * Largest edge allowed for a decoded source image. Bigger images are
 * downsampled — protects against OOM on huge camera originals and against
 * exceeding Canvas texture limits during preview.
 */
private const val MAX_DECODE_DIMENSION = 4096

/**
 * Decodes [uri] into a software, editable bitmap, downsampling anything larger
 * than 4096px on its largest edge. Software allocation is required: the
 * exporter copies the bitmap's pixels, which hardware bitmaps don't allow.
 * Returns null if the uri can't be decoded.
 *
 * Use this to prepare images for [PhotoEditorState.loadImage] from your own
 * picking flow — bitmaps decoded any other way may be hardware-backed and
 * crash the exporter. Performs blocking IO; call from a background
 * dispatcher.
 */
fun decodeEditableBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val maxDim = max(info.size.width, info.size.height)
            if (maxDim > MAX_DECODE_DIMENSION) {
                decoder.setTargetSampleSize((maxDim + MAX_DECODE_DIMENSION - 1) / MAX_DECODE_DIMENSION)
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_DECODE_DIMENSION) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }
} catch (_: Exception) {
    null
}