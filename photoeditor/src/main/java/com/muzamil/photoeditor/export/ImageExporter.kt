package com.muzamil.photoeditor.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.withSave
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.ShapeKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Exports the edited image to storage.
 *
 * Coordinate model
 * ────────────────
 * The preview uses ContentScale.Crop, so the inner container fills the screen
 * and some bitmap edges may be hidden. Element positions (offsetRatioX/Y) are
 * stored as fractions of the *container* (0 = left/top edge of visible area,
 * 1 = right/bottom edge of visible area), NOT fractions of the full bitmap.
 *
 * To convert back to bitmap pixels we must undo the crop:
 *
 *   cropScale  = max(containerPx / bitmapPx)      ← same as Crop display scale
 *   visibleW   = containerW / cropScale            ← bitmap pixels visible horizontally
 *   cropOffX   = (bitmapW − visibleW) / 2          ← hidden bitmap pixels on left
 *   bitmapX    = cropOffX + ratio * visibleW
 *
 * Font sizes follow the same logic:
 *   textSizeBitmapPx = spInDevicePx / cropScale
 *
 * @param backgroundBitmap  Original source bitmap (not mutated externally).
 * @param elements          All editor elements in z-order.
 * @param containerSize     Preview container size in **device pixels** (from onSizeChanged).
 * @param quality           JPEG compression quality (0–100).
 * @param format            Output bitmap compress format.
 */
suspend fun Context.saveEditedImage(
    backgroundBitmap: Bitmap,
    elements: List<EditElement>,
    containerSize: IntSize,
    quality: Int = 95,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val resultBitmap = backgroundBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val bitmapW = resultBitmap.width.toFloat()
        val bitmapH = resultBitmap.height.toFloat()

        // ── Crop-scale geometry ──────────────────────────────────────────────
        val scaleX = containerSize.width  / bitmapW
        val scaleY = containerSize.height / bitmapH
        // Crop uses the LARGER scale so the container is fully covered.
        val cropScale = max(scaleX, scaleY)

        // How many bitmap pixels are hidden on each side due to cropping.
        val visibleBitmapW = containerSize.width  / cropScale
        val visibleBitmapH = containerSize.height / cropScale
        val cropOffsetX    = (bitmapW - visibleBitmapW) / 2f
        val cropOffsetY    = (bitmapH - visibleBitmapH) / 2f

        // Convert a container-normalised ratio → bitmap pixel coordinate.
        fun Float.toBitmapX() = cropOffsetX + this * visibleBitmapW
        fun Float.toBitmapY() = cropOffsetY + this * visibleBitmapH

        // Convert sp (as stored in element) → bitmap pixel size.
        val displayMetrics = resources.displayMetrics
        fun spToBitmapPx(sp: Float): Float {
            val devicePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics
            )
            return devicePx / cropScale
        }

        // ── Render elements ──────────────────────────────────────────────────
        elements.sortedBy { it.zIndex }.forEach { element ->
            when (element) {

                // ── Text ─────────────────────────────────────────────────────
                is EditElement.Text -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color     = element.color.toArgb()
                        textSize  = spToBitmapPx(element.fontSize.value)
                        typeface  = if (element.fontWeight == FontWeight.Bold)
                            Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                        textAlign = Paint.Align.CENTER
                    }
                    val centerX  = element.offsetRatioX.toBitmapX()
                    val centerY  = element.offsetRatioY.toBitmapY()
                    val fm       = paint.fontMetrics
                    // Shift baseline so glyphs are vertically centred on (0,0).
                    val baselineY = -(fm.ascent + fm.descent) / 2f

                    canvas.withSave {
                        translate(centerX, centerY)
                        rotate(element.rotation)
                        scale(element.scale, element.scale)
                        drawText(element.text, 0f, baselineY, paint)
                    }
                }

                // ── Emoji ─────────────────────────────────────────────────────
                is EditElement.Emoji -> {
                    val centerX = element.offsetRatioX.toBitmapX()
                    val centerY = element.offsetRatioY.toBitmapY()

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = spToBitmapPx(element.size.value)
                    }
                    val emojiW  = paint.measureText(element.emoji)
                    val fm      = paint.fontMetrics
                    val emojiH  = fm.bottom - fm.top

                    canvas.withSave {
                        translate(centerX, centerY)
                        rotate(element.rotation)
                        scale(element.scale, element.scale)
                        // Centre the glyph on (0,0) in local (post-scale) space.
                        translate(-emojiW / 2f, -emojiH / 2f)
                        drawText(element.emoji, 0f, -fm.top, paint)
                    }
                }

                // ── Drawing ───────────────────────────────────────────────────
                // Drawing points are stored as bitmap-normalised ratios (0–1
                // spans the full bitmap), so no crop offset is needed here.
                is EditElement.Drawing -> {
                    if (element.points.size < 2) return@forEach
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color       = element.color.toArgb()
                        strokeWidth = element.strokeWidth
                        style       = Paint.Style.STROKE
                        strokeCap   = Paint.Cap.ROUND
                        strokeJoin  = Paint.Join.ROUND
                    }
                    for (i in 0 until element.points.size - 1) {
                        val s = element.points[i]
                        val e = element.points[i + 1]
                        canvas.drawLine(
                            s.x * bitmapW, s.y * bitmapH,
                            e.x * bitmapW, e.y * bitmapH,
                            paint,
                        )
                    }
                }

                // ── Shape ─────────────────────────────────────────────────────
                is EditElement.Shape -> {
                    val centerX = element.offsetRatioX.toBitmapX()
                    val centerY = element.offsetRatioY.toBitmapY()

                    // widthRatio / heightRatio are fractions of the *visible* container area,
                    // not the full bitmap — use visibleBitmapW/H to match what the preview shows.
                    val halfW = (element.widthRatio  * visibleBitmapW) / 2f   // ← was bitmapW
                    val halfH = (element.heightRatio * visibleBitmapH) / 2f   // ← was bitmapH

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color       = element.color.toArgb()
                        strokeWidth = element.strokeWidth
                        style       = if (element.filled) Paint.Style.FILL else Paint.Style.STROKE
                        strokeCap   = Paint.Cap.ROUND
                        strokeJoin  = Paint.Join.ROUND
                    }

                    canvas.withSave {
                        translate(centerX, centerY)
                        rotate(element.rotation)
                        scale(element.scale, element.scale)
                        when (element.kind) {
                            ShapeKind.Rectangle -> canvas.drawRect(-halfW, -halfH, halfW, halfH, paint)
                            ShapeKind.Circle    -> canvas.drawOval(-halfW, -halfH, halfW, halfH, paint)
                            ShapeKind.Line      -> canvas.drawLine(-halfW, 0f, halfW, 0f, paint)
                            ShapeKind.Triangle  -> canvas.drawPath(trianglePath(halfW, halfH), paint)
                            ShapeKind.Star      -> canvas.drawPath(starPath(halfW, halfH), paint)
                            ShapeKind.Arrow     -> canvas.drawPath(arrowPath(halfW, halfH), paint)
                            ShapeKind.Heart     -> canvas.drawPath(heartPath(halfW, halfH), paint)
                        }
                    }
                }
            }
        }

        // ── Save to storage ──────────────────────────────────────────────────
        val fileName = "edited_image_${System.currentTimeMillis()}.jpg"
        val outputStream: OutputStream?
        val filePath: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )
            outputStream = uri?.let { contentResolver.openOutputStream(it) }
            filePath     = uri?.toString() ?: ""
        } else {
            val dir   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file  = File(dir, fileName)
            outputStream = FileOutputStream(file)
            filePath     = file.absolutePath
        }

        outputStream?.use { resultBitmap.compress(format, quality, it) }
        resultBitmap.recycle()

        filePath
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