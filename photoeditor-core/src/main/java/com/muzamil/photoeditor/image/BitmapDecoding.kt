package com.muzamil.photoeditor.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlin.math.max

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
 * Use this to prepare images for
 * [com.muzamil.photoeditor.state.PhotoEditorState.loadImage] from your own
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
