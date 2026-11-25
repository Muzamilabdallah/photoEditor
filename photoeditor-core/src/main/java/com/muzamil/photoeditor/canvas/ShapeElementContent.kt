package com.muzamil.photoeditor.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorShape

/**
 * Renders a [EditElement.Shape] inside a [DraggableElement]. Its box and
 * stroke are size ratios (fractions of the image's shorter side), so both
 * scale by [sizeUnitPx] — the image's shorter side on screen — exactly as the
 * exporter scales them by the bitmap's shorter side.
 */
@Composable
internal fun ShapeElementContent(
    element: EditElement.Shape,
    isSelected: Boolean,
    sizeUnitPx: Float,
    modifier: Modifier = Modifier
) {
    val widthPx = (element.widthRatio * sizeUnitPx).coerceAtLeast(1f)
    val heightPx = (element.heightRatio * sizeUnitPx).coerceAtLeast(1f)

    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    val previewStrokeWidth = element.strokeWidthRatio * sizeUnitPx

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
                // A line has no interior — always stroked, even when "filled".
                EditorShape.Line -> drawLine(
                    color = element.color,
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = previewStrokeWidth,
                    cap = StrokeCap.Round
                )

                else -> drawPath(
                    path = EditorShapePaths.path(element.kind, w, h),
                    color = element.color,
                    style = drawStyle
                )
            }
            // Selection chrome comes from DraggableElement; nothing extra here.
        }
    }
}
