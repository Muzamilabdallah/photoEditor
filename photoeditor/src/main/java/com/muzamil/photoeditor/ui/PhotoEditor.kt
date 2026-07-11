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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.ui.theme.PhotoEditorTheme
import com.muzamil.photoeditor.viewmodel.PhotoEditorViewModel
import kotlin.math.max

/**
 * The public entry point of the photo editor.
 *
 * Drop-in usage — the editor owns its ViewModel, theme, photo picking, and
 * bitmap decoding:
 * ```
 * setContent { PhotoEditor() }
 * ```
 *
 * Every piece is overridable:
 * - [bitmap] — supply an image directly instead of (or in addition to) the
 *   built-in picker; it is (re)loaded whenever the value changes.
 * - [onPickImage] — replace the built-in [ActivityResultContracts.PickVisualMedia]
 *   launcher with your own picking flow. Call [PhotoEditorViewModel.loadImage]
 *   with the result.
 * - [viewModel] — hoist the ViewModel to observe editor state or drive it
 *   programmatically.
 * - [saveDestination] — where the save button writes the result:
 *   [SaveDestination.Gallery] (public MediaStore, default) or
 *   [SaveDestination.File] for a path you control, e.g. app-private storage:
 *   `SaveDestination.File(File(context.filesDir, "edits/out.jpg"))`.
 */
@Composable
fun PhotoEditor(
    modifier: Modifier = Modifier,
    bitmap: Bitmap? = null,
    viewModel: PhotoEditorViewModel = viewModel(),
    onPickImage: (() -> Unit)? = null,
    saveDestination: SaveDestination = SaveDestination.Gallery(),
) {
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { picked ->
            decodeEditableBitmap(context.contentResolver, picked)?.let(viewModel::loadImage)
        }
    }

    LaunchedEffect(bitmap) {
        bitmap?.let(viewModel::loadImage)
    }

    PhotoEditorTheme {
        EditorHostContent(
            viewModel = viewModel,
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
