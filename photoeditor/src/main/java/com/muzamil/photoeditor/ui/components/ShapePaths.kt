package com.muzamil.photoeditor.ui.components

import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Compose-side shape path builders. Each path is laid out in a (0,0)..(w,h)
 * bounding box so it can be drawn directly inside a sized Canvas. The export
 * path-builders in [com.muzamil.photoeditor.export] mirror these geometries
 * around the origin instead.
 */
internal fun trianglePath(w: Float, h: Float): Path = Path().apply {
    moveTo(w / 2f, 0f)
    lineTo(w, h)
    lineTo(0f, h)
    close()
}

internal fun starPath(w: Float, h: Float, points: Int = 5): Path = Path().apply {
    val cx = w / 2f
    val cy = h / 2f
    val outer = min(w, h) / 2f
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

internal fun arrowPath(w: Float, h: Float): Path = Path().apply {
    val bodyTop = h * 0.35f
    val bodyBottom = h * 0.65f
    val headStart = w * 0.65f
    moveTo(0f, bodyTop)
    lineTo(headStart, bodyTop)
    lineTo(headStart, h * 0.15f)
    lineTo(w, h / 2f)
    lineTo(headStart, h * 0.85f)
    lineTo(headStart, bodyBottom)
    lineTo(0f, bodyBottom)
    close()
}

internal fun heartPath(w: Float, h: Float): Path = Path().apply {
    val topY = h * 0.25f
    moveTo(w / 2f, topY)
    cubicTo(0f, -h * 0.1f, -w * 0.1f, h * 0.55f, w / 2f, h)
    cubicTo(w * 1.1f, h * 0.55f, w, -h * 0.1f, w / 2f, topY)
    close()
}
