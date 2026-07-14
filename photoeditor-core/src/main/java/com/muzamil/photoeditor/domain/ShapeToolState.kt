package com.muzamil.photoeditor.domain

import androidx.compose.ui.graphics.Color

/**
 * The Shape tool's pending style — which shape the next tap places and how it
 * is drawn. Lives in core (not the UI module) so a custom UI can drive the
 * canvas with the same type the built-in `ShapeToolPanel` uses.
 *
 * Held in [EditorState.shapeTool]; mutate it through
 * `PhotoEditorState.updateShapeTool`.
 */
data class ShapeToolState(
    val selectedShape: EditorShape = EditorShape.Rect,
    val selectedColor: Color = EditorColors.Red,
    /** Stroke width in bitmap-space pixels. Ignored when [isFilled] is true. */
    val strokeWidthPx: Float = 8f,
    val isFilled: Boolean = false,
)
