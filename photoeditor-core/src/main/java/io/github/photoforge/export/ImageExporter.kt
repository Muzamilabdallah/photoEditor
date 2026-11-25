package io.github.photoforge.export

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.withSave
import io.github.photoforge.domain.EditElement
import io.github.photoforge.domain.EditorShape
import io.github.photoforge.domain.TextWrapWidthRatio
import io.github.photoforge.domain.inRenderOrder
import io.github.photoforge.domain.sizeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException
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
                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color    = element.color.toArgb()
                    textSize = element.fontSizeRatio * unit
                    typeface = typefaceFor(element.fontWeight)
                }
                // Same box as the preview's BasicText: wraps at
                // TextWrapWidthRatio of the image width, lines centred.
                val layout = textLayout(element.text, paint, maxWidth = (bitmapW * TextWrapWidthRatio).toInt())
                drawCentred(canvas, element, layout, bitmapW, bitmapH)
            }

            // ── Emoji ────────────────────────────────────────────────────────
            is EditElement.Emoji -> {
                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = element.sizeRatio * unit
                }
                val layout = textLayout(element.emoji, paint, maxWidth = Int.MAX_VALUE)
                drawCentred(canvas, element, layout, bitmapW, bitmapH)
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
 * result to [destination] in [format]. Runs on [Dispatchers.IO]; throws on
 * failure (including [CancellationException] if the caller is cancelled).
 * Prefer `PhotoEditorState.export`, which wraps this in a `Result`.
 *
 * @return the saved image: a `content://` uri for MediaStore saves (API 29+),
 *         a `file://` uri otherwise.
 */
suspend fun Context.saveEditedImage(
    backgroundBitmap: Bitmap,
    elements: List<EditElement>,
    destination: SaveDestination = SaveDestination.Gallery(),
    format: ExportFormat = ExportFormat.Jpeg(),
): Uri = withContext(Dispatchers.IO) {
    val resultBitmap = renderEdits(backgroundBitmap, elements)
    try {
        writeBitmap(resultBitmap, format, destination)
    } finally {
        resultBitmap.recycle()
    }
}

/**
 * Renders [elements] onto [backgroundBitmap] and encodes it into [stream] in
 * [format] — e.g. to upload the bytes without writing a file. The stream is
 * not closed. Runs on [Dispatchers.Default]; throws on failure.
 */
suspend fun writeEditedImage(
    backgroundBitmap: Bitmap,
    elements: List<EditElement>,
    stream: OutputStream,
    format: ExportFormat = ExportFormat.Jpeg(),
) = withContext(Dispatchers.Default) {
    val resultBitmap = renderEdits(backgroundBitmap, elements)
    try {
        if (!format.compress(resultBitmap, stream)) throw IOException("Encoding as $format failed")
    } finally {
        resultBitmap.recycle()
    }
}

private fun OutputStream.encode(bitmap: Bitmap, format: ExportFormat) = use {
    if (!format.compress(bitmap, it)) throw IOException("Encoding as $format failed")
}

private fun Context.writeBitmap(
    resultBitmap: Bitmap,
    format: ExportFormat,
    destination: SaveDestination,
): Uri = when (destination) {
    is SaveDestination.File -> {
        val file = destination.file
        file.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) {
                throw IOException("Cannot create directory $parent")
            }
        }
        FileOutputStream(file).encode(resultBitmap, format)
        Uri.fromFile(file)
    }

    is SaveDestination.Gallery -> {
        val fileName = "edited_image_${System.currentTimeMillis()}.${format.extension}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = destination.subFolder
                ?.let { "${Environment.DIRECTORY_PICTURES}/$it" }
                ?: Environment.DIRECTORY_PICTURES
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
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
                stream.encode(resultBitmap, format)
                // Make the entry visible to other apps only once fully written.
                contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                )
            } catch (e: Throwable) {
                // Don't leave an orphaned pending entry behind (also on cancellation).
                contentResolver.delete(uri, null, null)
                throw e
            }
            uri
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
            FileOutputStream(file).encode(resultBitmap, format)
            Uri.fromFile(file)
        }
    }
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
 * Lays [text] out with the same settings the preview's `BasicText` uses (see
 * `TextElementContent`), so the box — and therefore its centre, its line
 * breaks and its height — matches what was on screen:
 * - width: the widest line, capped at [maxWidth] (wrapping beyond it);
 * - lines centred, simple line breaking, no hyphenation;
 * - no font padding, and (API 28+) line height from fallback fonts, which
 *   is where emoji and many non-Latin scripts come from;
 * - direction from the first strong character, so Arabic/Hebrew lay out RTL.
 */
// Layout.BREAK_STRATEGY_SIMPLE equals LineBreaker.BREAK_STRATEGY_SIMPLE (0);
// LineBreaker only exists from API 29 and minSdk is 24, hence the suppression.
@SuppressLint("WrongConstant")
internal fun textLayout(text: CharSequence, paint: TextPaint, maxWidth: Int): StaticLayout {
    val desired = kotlin.math.ceil(Layout.getDesiredWidth(text, paint)).toInt()
    val width = desired.coerceIn(1, maxWidth.coerceAtLeast(1))
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setIncludePad(false)
        .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setUseLineSpacingFromFallbacks(true)
        }
        .build()
}

/** Draws [layout] centred on [element]'s position, with its rotation and scale. */
private fun drawCentred(
    canvas: Canvas,
    element: EditElement,
    layout: StaticLayout,
    bitmapW: Float,
    bitmapH: Float,
) = canvas.withSave {
    translate(element.offsetRatioX * bitmapW, element.offsetRatioY * bitmapH)
    rotate(element.rotation)
    scale(element.scale, element.scale)
    translate(-layout.width / 2f, -layout.height / 2f)
    layout.draw(this)
}

/** The system sans-serif at [weight]; exact weights on API 28+, bold/regular before. */
private fun typefaceFor(weight: FontWeight): Typeface =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Typeface.create(Typeface.DEFAULT, weight.weight, false)
    } else {
        if (weight.weight >= FontWeight.SemiBold.weight) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
