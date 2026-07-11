package com.muzamil.photoeditor.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.state.PhotoEditorState
import com.muzamil.photoeditor.state.rememberPhotoEditorState
import com.muzamil.photoeditor.ui.theme.PhotoEditorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * The public entry point of the photo editor.
 *
 * Drop-in usage — the editor owns its state, theme, photo picking, and
 * bitmap decoding:
 * ```
 * setContent { PhotoEditor() }
 * ```
 *
 * Every piece is overridable:
 * - [bitmap] — supply an image directly instead of (or in addition to) the
 *   built-in picker; it is (re)loaded whenever the value changes.
 * - [state] — hoist the state holder via [rememberPhotoEditorState] to
 *   observe editor state or drive it programmatically.
 * - [onPickImage] — replace the built-in [ActivityResultContracts.PickVisualMedia]
 *   launcher with your own picking flow. Call [PhotoEditorState.loadImage]
 *   with the result.
 * - [saveDestination] — where the save button writes the result:
 *   [SaveDestination.Gallery] (public MediaStore, default) or
 *   [SaveDestination.File] for a path you control, e.g. app-private storage:
 *   `SaveDestination.File(File(context.filesDir, "edits/out.jpg"))`.
 *
 * Edits (elements, tool, styles) survive configuration changes and process
 * death via [PhotoEditorState]'s saver; the picked image uri is persisted
 * here and re-decoded on restore.
 */
@Composable
fun PhotoEditor(
    modifier: Modifier = Modifier,
    bitmap: Bitmap? = null,
    state: PhotoEditorState = rememberPhotoEditorState(),
    onPickImage: (() -> Unit)? = null,
    saveDestination: SaveDestination = SaveDestination.Gallery(),
) {
    val context = LocalContext.current

    // The bitmap itself can't be retained across recreation (too large for
    // saved state) — the uri can. On restore, the LaunchedEffect below
    // re-decodes it into the (bitmap-less) restored editor state.
    var pickedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedImageUri = uri
    }

    LaunchedEffect(bitmap) {
        bitmap?.let(state::loadImage)
    }

    LaunchedEffect(pickedImageUri) {
        val uri = pickedImageUri ?: return@LaunchedEffect
        val decoded = withContext(Dispatchers.IO) {
            decodeEditableBitmap(context.contentResolver, uri)
        }
        decoded?.let(state::loadImage)
    }

    PhotoEditorTheme {
        EditorHostContent(
            state = state,
            onPickImage = onPickImage ?: {
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
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
 * than [MAX_DECODE_DIMENSION]. Software allocation is required: the exporter
 * copies the bitmap's pixels, which hardware bitmaps don't allow.
 * Returns null if the uri can't be decoded.
 */
internal fun decodeEditableBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? = try {
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
