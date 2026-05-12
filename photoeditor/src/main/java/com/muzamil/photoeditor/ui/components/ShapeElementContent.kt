package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.ShapeKind

/**
 * Renders a [EditElement.Shape] inside a [DraggableElement]. Sizes itself
 * according to the shape's [EditElement.Shape.widthRatio]/[heightRatio] in
 * the parent (which equals the bitmap's pixel dimensions).
 */
@Composable
internal fun ShapeElementContent(
    element: EditElement.Shape,
    isSelected: Boolean,
    parentWidthPx: Int,
    parentHeightPx: Int,
    scaleFactor: Float,
    modifier: Modifier = Modifier
) {
    val widthPx = (parentWidthPx * element.widthRatio).coerceAtLeast(1f)
    val heightPx = (parentHeightPx * element.heightRatio).coerceAtLeast(1f)

    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    // strokeWidth is stored in bitmap-px (see EditElement.Shape kdoc); multiply by
    // scaleFactor so the preview thickness matches what the exporter will draw.
    val previewStrokeWidth = element.strokeWidth * scaleFactor

    Box(modifier = modifier.size(widthDp, heightDp)) {
        Canvas(modifier = Modifier.size(widthDp, heightDp)) {
            val w = size.width
            val h = size.height
            val drawStyle = if (element.filled) Fill else Stroke(
                width = previewStrokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            when (element.kind) {
                ShapeKind.Rectangle -> drawRect(
                    color = element.color,
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    style = drawStyle
                )
                ShapeKind.Circle -> drawOval(
                    color = element.color,
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    style = drawStyle
                )
                ShapeKind.Line -> drawLine(
                    color = element.color,
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = previewStrokeWidth,
                    cap = StrokeCap.Round
                )
                ShapeKind.Triangle -> drawShapePath(
                    path = trianglePath(w, h),
                    color = element.color,
                    style = drawStyle
                )
                ShapeKind.Star -> drawShapePath(
                    path = starPath(w, h),
                    color = element.color,
                    style = drawStyle
                )
                ShapeKind.Arrow -> drawShapePath(
                    path = arrowPath(w, h),
                    color = element.color,
                    style = drawStyle
                )
                ShapeKind.Heart -> drawShapePath(
                    path = heartPath(w, h),
                    color = element.color,
                    style = drawStyle
                )
            }

            if (isSelected) {
                drawRect(
                    color = Color.Blue,
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

private fun DrawScope.drawShapePath(
    path: Path,
    color: Color,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle
) {
    drawPath(path = path, color = color, style = style)
}
