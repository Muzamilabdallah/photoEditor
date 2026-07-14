package com.muzamil.photoeditor.domain

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.UUID

sealed class EditElement {
    abstract val id: String
    abstract val zIndex: Int
    abstract val offsetRatioX: Float
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
        val fontSize: TextUnit = 28.sp,
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
        val size: TextUnit = 48.sp,
        override val zIndex: Int = 0
    ) : EditElement()

    data class Drawing(
        override val id: String = UUID.randomUUID().toString(),
        /**
         * Points stored as ratio [Offset]s where x and y are each in `0f..1f`,
         * relative to the source bitmap's dimensions. Same convention as
         * [Text.offsetRatioX]/[Text.offsetRatioY], so drawings survive any
         * preview-size ↔ bitmap-size conversion.
         */
        val points: List<Offset>,
        val color: Color,
        /** Stroke width in bitmap-space pixels. */
        val strokeWidth: Float,
        override val zIndex: Int = 0
    ) : EditElement() {
        override val offsetRatioX: Float = 0f
        override val offsetRatioY: Float = 0f
        override val rotation: Float = 0f
        override val scale: Float = 1f
    }

    /**
     * A predefined shape (see [EditorShape]). Position is the shape's
     * center, expressed as ratios. [widthRatio] and [heightRatio] are the
     * shape's bounding-box size as fractions of the source bitmap's
     * dimensions, so the same shape renders consistently in the preview and
     * the exported bitmap.
     */
    data class Shape(
        override val id: String = UUID.randomUUID().toString(),
        val kind: EditorShape,
        override val offsetRatioX: Float = 0.5f,
        override val offsetRatioY: Float = 0.5f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        val widthRatio: Float = 0.2f,
        val heightRatio: Float = 0.2f,
        val color: Color = EditorColors.Red,
        /** Stroke width in bitmap-space pixels. Ignored when [filled] is true. */
        val strokeWidth: Float = 8f,
        val filled: Boolean = false,
        override val zIndex: Int = 0
    ) : EditElement()
}

/** The shape primitives the editor can place. */
enum class EditorShape { Rect, Circle, Line, Triangle, Star, Heart, Arrow }

data class EditorState(
    val elements: List<EditElement> = emptyList(),
    val backgroundBitmap: Bitmap? = null,
    val selectedElementId: String? = null,
    val currentTool: EditorTool = EditorTool.None,
    val drawingColor: Color = Color.Black,
    val drawingStrokeWidth: Float = 5f,
    val textColor: Color = Color.Black,
    val textSize: TextUnit = 24.sp,
    /** The Shape tool's pending style: what the next added shape looks like. */
    val shapeTool: ShapeToolState = ShapeToolState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /**
     * previewDisplayWidthPx / bitmap.width — how much the bitmap is scaled down (or up)
     * to fit the preview canvas. Kept in state so [Context.saveEditedImage] can scale
     * text/emoji sizes that are stored in device-pixels up to bitmap-pixels. 1f until
     * the first preview layout pass reports it.
     */
    val previewScaleFactor: Float = 1f
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