package com.muzamil.photoeditor.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.withSave
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorShape
import com.muzamil.photoeditor.domain.inRenderOrder
import com.muzamil.photoeditor.domain.sizeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws [elements] onto a mutable ARGB_8888 copy of [source] at the source's
 * full resolution and returns it; [source] is not modified. The caller owns
 * the result (recycle it when done).
 *
 * Pure: it depends only on the bitmap and the elements — no preview, screen
 * size or display metrics — because every element is stored relative to the
 * bitmap (see [EditElement]): positions as fractions of its width/height,
 * sizes as fractions of its shorter side. That is what lets the on-screen
 * preview and this export agree at any screen size, and lets you export from
 * a worker or a test without a laid-out editor.
 */
fun renderEdits(source: Bitmap, elements: List<EditElement>): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val bitmapW = result.width.toFloat()
    val bitmapH = result.height.toFloat()
    val unit = sizeUnit(bitmapW, bitmapH)

    fun Float.toBitmapX() = this * bitmapW
    fun Float.toBitmapY() = this * bitmapH

    elements.inRenderOrder().forEach { element ->
        when (element) {

            // ── Text ─────────────────────────────────────────────────────────
            is EditElement.Text -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color     = element.color.toArgb()
                    textSize  = element.fontSizeRatio * unit
                    typeface  = if (element.fontWeight == FontWeight.Bold)
                        Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    textAlign = Paint.Align.CENTER
                }
                val fm = paint.fontMetrics
                // Shift baseline so glyphs are vertically centred on (0,0).
                val baselineY = -(fm.ascent + fm.descent) / 2f

                canvas.withSave {
                    translate(element.offsetRatioX.toBitmapX(), element.offsetRatioY.toBitmapY())
                    rotate(element.rotation)
                    scale(element.scale, element.scale)
                    drawText(element.text, 0f, baselineY, paint)
                }
            }

            // ── Emoji ────────────────────────────────────────────────────────
            is EditElement.Emoji -> {
                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = element.sizeRatio * unit
                }
                // Lay the glyph out the way Compose's BasicText does, so its
                // box (and therefore its centre) matches the preview: no font
                // padding, and line height taken from the fallback emoji font
                // rather than the default font's metrics.
                val layout = emojiLayout(element.emoji, paint)

                canvas.withSave {
                    translate(element.offsetRatioX.toBitmapX(), element.offsetRatioY.toBitmapY())
                    rotate(element.rotation)
                    scale(element.scale, element.scale)
                    // Centre the laid-out line box on (0,0) in local space.
                    translate(-layout.width / 2f, -layout.height / 2f)
                    layout.draw(this)
                }
            }

            // ── Drawing ──────────────────────────────────────────────────────
            is EditElement.Drawing -> {
                if (element.points.isEmpty()) return@forEach
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color       = element.color.toArgb()
                    strokeWidth = element.strokeWidthRatio * unit
                    style       = Paint.Style.STROKE
                    strokeCap   = Paint.Cap.ROUND
                    strokeJoin  = Paint.Join.ROUND
                }
                val path = buildSmoothDrawingPath(element.points) { p ->
                    Offset(p.x * bitmapW, p.y * bitmapH)
                }
                canvas.drawPath(path, paint)
            }

            // ── Shape ────────────────────────────────────────────────────────
            is EditElement.Shape -> {
                val halfW = element.widthRatio * unit / 2f
                val halfH = element.heightRatio * unit / 2f

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color       = element.color.toArgb()
                    strokeWidth = element.strokeWidthRatio * unit
                    style       = if (element.filled) Paint.Style.FILL else Paint.Style.STROKE
                    strokeCap   = Paint.Cap.ROUND
                    strokeJoin  = Paint.Join.ROUND
                }

                canvas.withSave {
                    translate(element.offsetRatioX.toBitmapX(), element.offsetRatioY.toBitmapY())
                    rotate(element.rotation)
                    scale(element.scale, element.scale)
                    when (element.kind) {
                        EditorShape.Rect     -> drawRect(-halfW, -halfH, halfW, halfH, paint)
                        EditorShape.Circle   -> drawOval(-halfW, -halfH, halfW, halfH, paint)
                        EditorShape.Line     -> drawLine(-halfW, 0f, halfW, 0f, paint)
                        EditorShape.Triangle -> drawPath(trianglePath(halfW, halfH), paint)
                        EditorShape.Star     -> drawPath(starPath(halfW, halfH), paint)
                        EditorShape.Arrow    -> drawPath(arrowPath(halfW, halfH), paint)
                        EditorShape.Heart    -> drawPath(heartPath(halfW, halfH), paint)
                    }
                }
            }
        }
    }
    return result
}

/**
 * Renders [elements] onto [backgroundBitmap] with [renderEdits] and writes the
 * result to [destination].
 *
 * @param backgroundBitmap  Original source bitmap (not modified).
 * @param elements          All editor elements.
 * @param quality           Compression quality (0–100).
 * @param format            Output bitmap compress format.
 * @param destination       Where to write the result: [SaveDestination.Gallery]
 *                          (public, default) or [SaveDestination.File] (a path
 *                          the caller controls, e.g. app-private storage).
 * @return the saved image's location: a `content://` uri string for
 *         MediaStore saves, an absolute file path otherwise.
 */
suspend fun Context.saveEditedImage(
    backgroundBitmap: Bitmap,
    elements: List<EditElement>,
    quality: Int = 95,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    destination: SaveDestination = SaveDestination.Gallery(),
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val resultBitmap = renderEdits(backgroundBitmap, elements)
        try {
            writeBitmap(resultBitmap, quality, format, destination)
        } finally {
            resultBitmap.recycle()
        }
    }
}

private fun Context.writeBitmap(
    resultBitmap: Bitmap,
    quality: Int,
    format: Bitmap.CompressFormat,
    destination: SaveDestination,
): String {
    // ── Save to storage ──────────────────────────────────────────────────
    val (extension, mimeType) = when (format) {
        Bitmap.CompressFormat.PNG -> "png" to "image/png"
        Bitmap.CompressFormat.JPEG -> "jpg" to "image/jpeg"
        else -> "webp" to "image/webp"
    }

    val filePath = when (destination) {
        is SaveDestination.File -> {
            val file = destination.file
            file.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    throw IOException("Cannot create directory $parent")
                }
            }
            FileOutputStream(file).use { resultBitmap.compress(format, quality, it) }
            file.absolutePath
        }

        is SaveDestination.Gallery -> {
            val fileName = "edited_image_${System.currentTimeMillis()}.$extension"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = destination.subFolder
                    ?.let { "${Environment.DIRECTORY_PICTURES}/$it" }
                    ?: Environment.DIRECTORY_PICTURES
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues,
                ) ?: throw IOException("MediaStore insert failed — cannot create image entry")

                try {
                    val stream = contentResolver.openOutputStream(uri)
                        ?: throw IOException("Cannot open output stream for $uri")
                    stream.use { resultBitmap.compress(format, quality, it) }
                    // Make the entry visible to other apps only once fully written.
                    contentResolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                        null,
                        null,
                    )
                } catch (e: Exception) {
                    // Don't leave an orphaned pending entry behind.
                    contentResolver.delete(uri, null, null)
                    throw e
                }
                uri.toString()
            } else {
                // Direct write to the public directory — needs
                // WRITE_EXTERNAL_STORAGE held at runtime on API <29.
                val base = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                val dir = destination.subFolder?.let { File(base, it) } ?: base
                if (!dir.exists() && !dir.mkdirs()) {
                    throw IOException("Cannot create directory $dir")
                }
                val file = File(dir, fileName)
                FileOutputStream(file).use { resultBitmap.compress(format, quality, it) }
                file.absolutePath
            }
        }
    }

    return filePath
}

// ── Shape path helpers ────────────────────────────────────────────────────────

private fun trianglePath(halfW: Float, halfH: Float) = Path().apply {
    moveTo(0f, -halfH)
    lineTo(halfW,  halfH)
    lineTo(-halfW, halfH)
    close()
}

private fun starPath(halfW: Float, halfH: Float, points: Int = 5) = Path().apply {
    val outer = min(halfW, halfH)
    val inner = outer * 0.4f
    val step  = (PI / points).toFloat()
    var angle = (-PI / 2).toFloat()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val x = r * cos(angle)
        val y = r * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
        angle += step
    }
    close()
}

private fun arrowPath(halfW: Float, halfH: Float) = Path().apply {
    val bodyTop    = -halfH * 0.3f
    val bodyBottom =  halfH * 0.3f
    val headStart  =  halfW * 0.3f
    moveTo(-halfW,   bodyTop)
    lineTo(headStart, bodyTop)
    lineTo(headStart, -halfH * 0.7f)
    lineTo(halfW,  0f)
    lineTo(headStart,  halfH * 0.7f)
    lineTo(headStart, bodyBottom)
    lineTo(-halfW,  bodyBottom)
    close()
}

private fun heartPath(halfW: Float, halfH: Float) = Path().apply {
    moveTo(0f, -halfH * 0.5f)
    cubicTo(-halfW,        -1.2f * halfH,  -1.2f * halfW,  0.1f * halfH,  0f, halfH)
    cubicTo( 1.2f * halfW,  0.1f * halfH,   halfW,         -1.2f * halfH, 0f, -halfH * 0.5f)
    close()
}

/**
 * A single-line layout of [text], sized to its own width, with the same line
 * metrics Compose's `BasicText` uses: no font padding, and (API 28+) line
 * height from fallback fonts. Emoji come from a fallback font whose ascent and
 * descent differ from the default font's, so centring on `Paint.fontMetrics`
 * would put the glyph lower than the preview does.
 */
private fun emojiLayout(text: String, paint: TextPaint): StaticLayout {
    val width = kotlin.math.ceil(Layout.getDesiredWidth(text, paint)).toInt().coerceAtLeast(1)
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setUseLineSpacingFromFallbacks(true)
        }
        .build()
}
