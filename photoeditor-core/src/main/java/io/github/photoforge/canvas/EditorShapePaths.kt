package io.github.photoforge.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import io.github.photoforge.domain.EditorShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Builds every [EditorShape] as a Compose [Path] laid out in a
 * `(0,0)..(width,height)` bounding box, ready to draw inside a sized Canvas —
 * used by the editing surface and the built-in shape picker chips, and public
 * so custom UIs can render matching shape icons. The export path-builders in
 * `io.github.photoforge.export` mirror these geometries around the origin
 * instead.
 */
object EditorShapePaths {

    fun path(shape: EditorShape, width: Float, height: Float): Path = when (shape) {
        EditorShape.Rect -> Path().apply {
            addRect(Rect(0f, 0f, width, height))
        }

        EditorShape.Circle -> Path().apply {
            addOval(Rect(0f, 0f, width, height))
        }

        EditorShape.Line -> Path().apply {
            moveTo(0f, height / 2f)
            lineTo(width, height / 2f)
        }

        EditorShape.Triangle -> Path().apply {
            moveTo(width / 2f, 0f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        EditorShape.Star -> Path().apply {
            val points = 5
            val cx = width / 2f
            val cy = height / 2f
            val outer = min(width, height) / 2f
            val inner = outer * 0.4f
            val step = (PI / points).toFloat()
            var angle = -PI.toFloat() / 2f
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) outer else inner
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
                angle += step
            }
            close()
        }

        EditorShape.Heart -> Path().apply {
            val topY = height * 0.25f
            moveTo(width / 2f, topY)
            cubicTo(0f, -height * 0.1f, -width * 0.1f, height * 0.55f, width / 2f, height)
            cubicTo(width * 1.1f, height * 0.55f, width, -height * 0.1f, width / 2f, topY)
            close()
        }

        EditorShape.Arrow -> Path().apply {
            val bodyTop = height * 0.35f
            val bodyBottom = height * 0.65f
            val headStart = width * 0.65f
            moveTo(0f, bodyTop)
            lineTo(headStart, bodyTop)
            lineTo(headStart, height * 0.15f)
            lineTo(width, height / 2f)
            lineTo(headStart, height * 0.85f)
            lineTo(headStart, bodyBottom)
            lineTo(0f, bodyBottom)
            close()
        }
    }
}
