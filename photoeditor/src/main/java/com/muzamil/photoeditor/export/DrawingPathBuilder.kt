package com.muzamil.photoeditor.export

import android.graphics.Path
import androidx.compose.ui.geometry.Offset

/**
 * Builds a quadratic-bezier smoothed path through [points], mapping each
 * ratio-space point into pixel space with [map] first. Shared by the
 * on-screen preview ([com.muzamil.photoeditor.ui.components.EditorCanvas])
 * and the bitmap exporter so strokes render identically in both paths.
 *
 * Midpoint smoothing: each raw point becomes the control point of a quad
 * curve whose endpoints are the midpoints of its neighbouring segments, so
 * the path passes near every sample without polyline corners.
 */
internal fun buildSmoothDrawingPath(points: List<Offset>, map: (Offset) -> Offset): Path {
    val path = Path()
    if (points.isEmpty()) return path

    val first = map(points.first())
    path.moveTo(first.x, first.y)
    if (points.size == 1) {
        // Zero-length segment so a round-cap stroke renders a dot.
        path.lineTo(first.x, first.y)
        return path
    }

    var prev = first
    for (i in 1 until points.size) {
        val current = map(points[i])
        path.quadTo(prev.x, prev.y, (prev.x + current.x) / 2f, (prev.y + current.y) / 2f)
        prev = current
    }
    path.lineTo(prev.x, prev.y)
    return path
}
