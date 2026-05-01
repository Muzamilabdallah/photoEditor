package com.muzamil.photoeditor.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.withSave
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.ShapeKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@RequiresApi(Build.VERSION_CODES.O)
suspend fun Context.saveEditedImage(
    backgroundBitmap: Bitmap,
    elements: List<EditElement>,
    previewScaleFactor: Float,
    quality: Int = 95,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
): Result<String> = withContext(Dispatchers.IO) {
    try {
        // Create a mutable copy of the background bitmap
        val resultBitmap = backgroundBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val bitmapWidth = resultBitmap.width.toFloat()
        val bitmapHeight = resultBitmap.height.toFloat()
        val displayMetrics = resources.displayMetrics

        // Sort elements by zIndex to render in correct order
        val sortedElements = elements.sortedBy { it.zIndex }

        sortedElements.forEach { element ->
            when (element) {
                is EditElement.Text -> {
                    val paint = Paint().apply {
                        color = element.color.toArgb()
                        // fontSize is sp → device-px; divide by the preview scale to land in
                        // bitmap-px, so the exported text covers the same fraction of the
                        // image that the preview showed.
                        val spPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP,
                            element.fontSize.value,
                            displayMetrics
                        )
                        textSize = spPx / previewScaleFactor
                        typeface = if (element.fontWeight == FontWeight.Bold) {
                            Typeface.DEFAULT_BOLD
                        } else {
                            Typeface.DEFAULT
                        }
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }

                    val centerX = element.offsetRatioX * bitmapWidth
                    val centerY = element.offsetRatioY * bitmapHeight

                    // Vertical centering: drawText draws baseline at y; offset baseline so
                    // the glyphs' visual center lands on the translated origin.
                    val fm = paint.fontMetrics
                    val baselineY = -(fm.ascent + fm.descent) / 2f

                    canvas.withSave {
                        translate(centerX, centerY)
                        rotate(element.rotation)
                        scale(element.scale, element.scale)
                        drawText(element.text, 0f, baselineY, paint)
                    }
                }



                is EditElement.Emoji -> {
                    val x = element.offsetRatioX * bitmapWidth
                    val y = element.offsetRatioY * bitmapHeight

                    // sp → device-px, then divided by previewScaleFactor so the emoji covers
                    // the same fraction of the bitmap it did in the preview.
                    val textSizePx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        element.size.value,
                        displayMetrics
                    ) / previewScaleFactor

                    val paint = Paint().apply {
                        textSize = textSizePx
                        isAntiAlias = true
                    }

                    // Measure from Paint — TextMeasurer would re-apply density and double
                    // the size. We want dimensions in the same px space as textSizePx.
                    val emojiWidth = paint.measureText(element.emoji)
                    val fm = paint.fontMetrics
                    val emojiHeight = fm.bottom - fm.top

                    canvas.withSave {
                        translate(x, y)
                        scale(element.scale, element.scale)
                        rotate(element.rotation, 0f, 0f)
                        translate(-emojiWidth / 2f, -emojiHeight / 2f)

                        drawText(
                            element.emoji,
                            0f,
                            -fm.top,
                            paint
                        )
                    }
                }

                is EditElement.Drawing -> {
                    val paint = Paint().apply {
                        color = element.color.toArgb()
                        strokeWidth = element.strokeWidth
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        isAntiAlias = true
                    }

                    // Points are ratios (0..1); multiply by bitmap dimensions.
                    for (i in 0 until element.points.size - 1) {
                        val start = element.points[i]
                        val end = element.points[i + 1]

                        canvas.drawLine(
                            start.x * bitmapWidth,
                            start.y * bitmapHeight,
                            end.x * bitmapWidth,
                            end.y * bitmapHeight,
                            paint
                        )
                    }
                }

                is EditElement.Shape -> {
                    val paint = Paint().apply {
                        color = element.color.toArgb()
                        strokeWidth = element.strokeWidth
                        style = if (element.filled) Paint.Style.FILL else Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        isAntiAlias = true
                    }

                    val centerX = element.offsetRatioX * bitmapWidth
                    val centerY = element.offsetRatioY * bitmapHeight
                    val halfW = (element.widthRatio * bitmapWidth) / 2f
                    val halfH = (element.heightRatio * bitmapHeight) / 2f

                    canvas.withSave {
                        translate(centerX, centerY)
                        rotate(element.rotation)
                        scale(element.scale, element.scale)

                        when (element.kind) {
                            ShapeKind.Rectangle -> drawRect(-halfW, -halfH, halfW, halfH, paint)
                            ShapeKind.Circle -> drawOval(-halfW, -halfH, halfW, halfH, paint)
                            ShapeKind.Line -> drawLine(-halfW, 0f, halfW, 0f, paint)
                        }
                    }
                }
            }
        }

        // Save to file
        val fileName = "edited_image_${System.currentTimeMillis()}.jpg"
        val outputStream: OutputStream?
        val filePath: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            outputStream = uri?.let { contentResolver.openOutputStream(it) }
            filePath = uri?.toString() ?: ""
        } else {
            // Use legacy storage for older Android versions
            val imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val image = File(imagesDir, fileName)
            outputStream = FileOutputStream(image)
            filePath = image.absolutePath
        }

        outputStream?.use { stream ->
            resultBitmap.compress(format, quality, stream)
        }

        // Clean up
        resultBitmap.recycle()

        Result.success(filePath)
    } catch (e: Exception) {
        Result.failure(e)
    }
}