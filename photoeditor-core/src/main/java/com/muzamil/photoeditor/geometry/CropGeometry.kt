package com.muzamil.photoeditor.geometry

import androidx.compose.ui.geometry.Offset

/**
 * The ContentScale.Crop transform between a bitmap and the container it is
 * displayed in: the bitmap is scaled by `maxOf(scaleX, scaleY)` so it fully
 * covers the container, and the overflow is cropped symmetrically.
 *
 * This is the single source of truth for mapping between screen (container)
 * pixels and bitmap-normalised ratio coordinates (`0f..1f` across the full
 * bitmap) in the preview. Both pointer input and drawing rendering in
 * `EditorCanvas` must go through this class — using different math on the two
 * sides is exactly the bug that made strokes land away from the finger.
 */
internal data class CropGeometry(
    val bitmapWidth: Float,
    val bitmapHeight: Float,
    val containerWidth: Float,
    val containerHeight: Float,
) {
    val isValid: Boolean =
        bitmapWidth > 0f && bitmapHeight > 0f && containerWidth > 0f && containerHeight > 0f

    /** Screen pixels per bitmap pixel under Crop (container fully covered). */
    val scale: Float =
        if (isValid) maxOf(containerWidth / bitmapWidth, containerHeight / bitmapHeight) else 1f

    /** Scaled-bitmap pixels hidden left of / above the container's origin. */
    val cropOffsetX: Float = if (isValid) (bitmapWidth * scale - containerWidth) / 2f else 0f
    val cropOffsetY: Float = if (isValid) (bitmapHeight * scale - containerHeight) / 2f else 0f

    /**
     * Container-local pixel position → bitmap-normalised ratio, clamped to
     * `0f..1f` so drags that leave the container can't store out-of-range
     * points. Returns [Offset.Zero] while the geometry is unmeasured.
     */
    fun screenToRatio(screen: Offset): Offset {
        if (!isValid) return Offset.Zero
        return Offset(
            ((screen.x + cropOffsetX) / (bitmapWidth * scale)).coerceIn(0f, 1f),
            ((screen.y + cropOffsetY) / (bitmapHeight * scale)).coerceIn(0f, 1f),
        )
    }

    /** Bitmap-normalised ratio → container-local pixel position. */
    fun ratioToScreen(ratio: Offset): Offset {
        if (!isValid) return Offset.Zero
        return Offset(
            ratio.x * bitmapWidth * scale - cropOffsetX,
            ratio.y * bitmapHeight * scale - cropOffsetY,
        )
    }
}
