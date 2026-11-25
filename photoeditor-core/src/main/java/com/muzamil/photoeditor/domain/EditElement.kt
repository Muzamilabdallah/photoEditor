package com.muzamil.photoeditor.domain

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.util.UUID

/**
 * An overlay on the photo.
 *
 * ## Coordinate model
 * Everything is stored relative to the **source bitmap**, never to the screen,
 * so an edit renders identically at any preview size (rotation, split screen,
 * process-death restore into another window) and at full resolution on export:
 *
 * - **Positions** ([offsetRatioX]/[offsetRatioY], [Drawing.points]) are
 *   fractions of the bitmap's width and height: `(0, 0)` is its top-left
 *   corner, `(1, 1)` its bottom-right.
 * - **Sizes** (font size, emoji size, shape box, stroke widths — every field
 *   ending in `Ratio` that is not a position) are fractions of the bitmap's
 *   **shorter side**. Using one length for both axes keeps circles round and
 *   makes a given value look the same on every photo, whatever its resolution.
 */
sealed class EditElement {
    abstract val id: String
    abstract val zIndex: Int

    /** Element centre, as a fraction of the bitmap width. */
    abstract val offsetRatioX: Float

    /** Element centre, as a fraction of the bitmap height. */
    abstract val offsetRatioY: Float
    abstract val rotation: Float
    abstract val scale: Float

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        val text: String,
        override val offsetRatioX: Float = 0.5f,
        override val offsetRatioY: Float = 0.5f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        val color: Color = Color.White,
        /** Font size as a fraction of the bitmap's shorter side. */
        val fontSizeRatio: Float = DefaultTextSizeRatio,
        val fontWeight: FontWeight = FontWeight.Bold,
        override val zIndex: Int = 0
    ) : EditElement()

    data class Emoji(
        override val id: String = UUID.randomUUID().toString(),
        val emoji: String,
        override val offsetRatioX: Float = 0.5f,
        override val offsetRatioY: Float = 0.5f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        /** Glyph size as a fraction of the bitmap's shorter side. */
        val sizeRatio: Float = DefaultEmojiSizeRatio,
        override val zIndex: Int = 0
    ) : EditElement()

    data class Drawing(
        override val id: String = UUID.randomUUID().toString(),
        /** Points as fractions of the bitmap's width (x) and height (y), each `0f..1f`. */
        val points: List<Offset>,
        val color: Color,
        /** Stroke width as a fraction of the bitmap's shorter side. */
        val strokeWidthRatio: Float,
        override val zIndex: Int = 0
    ) : EditElement() {
        override val offsetRatioX: Float = 0f
        override val offsetRatioY: Float = 0f
        override val rotation: Float = 0f
        override val scale: Float = 1f
    }

    /**
     * A predefined shape (see [EditorShape]), centred on
     * [offsetRatioX]/[offsetRatioY]. [widthRatio], [heightRatio] and
     * [strokeWidthRatio] are fractions of the bitmap's shorter side, so equal
     * width and height give a square box on any photo.
     */
    data class Shape(
        override val id: String = UUID.randomUUID().toString(),
        val kind: EditorShape,
        override val offsetRatioX: Float = 0.5f,
        override val offsetRatioY: Float = 0.5f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        val widthRatio: Float = DefaultShapeSizeRatio,
        val heightRatio: Float = DefaultShapeSizeRatio,
        val color: Color = EditorColors.Red,
        /** Ignored when [filled] is true (except for [EditorShape.Line]). */
        val strokeWidthRatio: Float = DefaultShapeStrokeRatio,
        val filled: Boolean = false,
        override val zIndex: Int = 0
    ) : EditElement()

    companion object {
        /** Default text size: 7% of the shorter side. */
        const val DefaultTextSizeRatio = 0.07f

        /** Default emoji size: 12% of the shorter side. */
        const val DefaultEmojiSizeRatio = 0.12f

        /** Default freehand stroke: 0.6% of the shorter side. */
        const val DefaultDrawingStrokeRatio = 0.006f

        /** Default shape outline: 0.8% of the shorter side. */
        const val DefaultShapeStrokeRatio = 0.008f

        /** Default shape box: a quarter of the shorter side, square. */
        const val DefaultShapeSizeRatio = 0.25f
    }
}

/** The shape primitives the editor can place. */
enum class EditorShape { Rect, Circle, Line, Triangle, Star, Heart, Arrow }

data class EditorState(
    val elements: List<EditElement> = emptyList(),
    val backgroundBitmap: Bitmap? = null,
    val selectedElementId: String? = null,
    val currentTool: EditorTool = EditorTool.None,
    val drawingColor: Color = Color.Black,
    /** Stroke width for the next freehand stroke, as a fraction of the bitmap's shorter side. */
    val drawingStrokeWidthRatio: Float = EditElement.DefaultDrawingStrokeRatio,
    val textColor: Color = Color.Black,
    /** Font size for the next text, as a fraction of the bitmap's shorter side. */
    val textSizeRatio: Float = EditElement.DefaultTextSizeRatio,
    /** The Shape tool's pending style: what the next added shape looks like. */
    val shapeTool: ShapeToolState = ShapeToolState(),
    /** True while [PhotoEditorState.saveEditedImage] is rendering and writing. */
    val isLoading: Boolean = false,
    /** Why the last [PhotoEditorState.saveEditedImage] failed, until cleared. */
    val error: EditorError? = null
)

sealed class EditorTool {
    data object None : EditorTool()
    data object Text : EditorTool()
    data object Drawing : EditorTool()

    /**
     * The Shape tool is active. Which shape (and style) is pending lives in
     * [EditorState.shapeTool] — single source of truth shared with any UI.
     */
    data object Shape : EditorTool()
}

/**
 * Text wraps onto a new line once it is wider than this fraction of the
 * image's width — in the preview and in the export alike, so line breaks
 * never depend on the screen.
 */
internal const val TextWrapWidthRatio = 0.9f

/** The length all element sizes are relative to: the shorter side, in the same units as [width]/[height]. */
internal fun sizeUnit(width: Float, height: Float): Float = minOf(width, height)
