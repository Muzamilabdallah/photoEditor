package com.muzamil.photoeditor.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.muzamil.photoeditor.domain.sizeUnit
import kotlin.math.roundToInt

/**
 * Where the photo sits on screen: the bitmap is scaled by `min(scaleX, scaleY)`
 * (ContentScale.Fit) so it is fully visible, and centred with letterbox bars
 * on the spare axis.
 *
 * This is the single source of truth for the preview's screen ↔ bitmap mapping.
 * `EditorCanvas` lays the image, the gesture layer and every element out in a
 * box of exactly [imageSize] at [imageOffset], so inside that box an element's
 * bitmap ratio is simply its fraction of the box. Everything else that needs
 * screen pixels (stroke widths, font sizes, shape boxes) multiplies a size
 * ratio by [sizeUnitPx].
 */
internal data class ImageGeometry(
    val bitmapWidth: Float,
    val bitmapHeight: Float,
    val containerWidth: Float,
    val containerHeight: Float,
) {
    val isValid: Boolean =
        bitmapWidth > 0f && bitmapHeight > 0f && containerWidth > 0f && containerHeight > 0f

    /** Screen pixels per bitmap pixel under Fit (whole image visible). */
    val scale: Float =
        if (isValid) minOf(containerWidth / bitmapWidth, containerHeight / bitmapHeight) else 1f

    /**
     * The image's on-screen size and top-left, rounded to whole pixels — the
     * exact box the canvas lays the image out in. Zero while unmeasured.
     */
    val imageSize: IntSize =
        if (isValid) IntSize((bitmapWidth * scale).roundToInt(), (bitmapHeight * scale).roundToInt())
        else IntSize.Zero
    val imageOffset: IntOffset =
        if (isValid) IntOffset(
            ((containerWidth - imageSize.width) / 2f).roundToInt(),
            ((containerHeight - imageSize.height) / 2f).roundToInt(),
        ) else IntOffset.Zero

    /** The image's on-screen rectangle in container pixels. */
    val imageRect: Rect = Rect(
        imageOffset.x.toFloat(),
        imageOffset.y.toFloat(),
        (imageOffset.x + imageSize.width).toFloat(),
        (imageOffset.y + imageSize.height).toFloat(),
    )

    /** Screen pixels of one size-ratio unit: the image's shorter side on screen. */
    val sizeUnitPx: Float = sizeUnit(imageSize.width.toFloat(), imageSize.height.toFloat())

    /**
     * Container pixel position → bitmap ratio, clamped to `0f..1f` so a touch
     * in the letterbox maps to the nearest image edge. [Offset.Zero] while
     * unmeasured.
     */
    fun screenToRatio(screen: Offset): Offset {
        if (!isValid) return Offset.Zero
        return Offset(
            ((screen.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f),
            ((screen.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f),
        )
    }

    /** Bitmap ratio → container pixel position. */
    fun ratioToScreen(ratio: Offset): Offset {
        if (!isValid) return Offset.Zero
        return Offset(
            imageRect.left + ratio.x * imageRect.width,
            imageRect.top + ratio.y * imageRect.height,
        )
    }
}
