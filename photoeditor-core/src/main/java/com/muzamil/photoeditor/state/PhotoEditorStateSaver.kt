package com.muzamil.photoeditor.state

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorShape
import com.muzamil.photoeditor.domain.EditorState
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.domain.ShapeToolState
import kotlinx.coroutines.CoroutineScope

/**
 * Custom [Saver] for [PhotoEditorState]: flattens the editor state into
 * Bundle-safe primitives so edits survive configuration changes and process
 * death. Deliberately excluded: the background bitmap (far too large for
 * saved instance state — the `PhotoEditor` composable persists the image
 * *uri* and re-decodes it instead) and the undo/redo history.
 */
internal fun photoEditorStateSaver(scope: CoroutineScope): Saver<PhotoEditorState, Any> =
    listSaver(
        save = { state ->
            val s = state.editorState
            listOf(
                s.selectedElementId,
                s.currentTool.toSaveable(),
                s.drawingColor.toArgb(),
                s.drawingStrokeWidth,
                s.textColor.toArgb(),
                s.textSize.value,
                s.shapeTool.selectedShape.name,
                s.shapeTool.selectedColor.toArgb(),
                s.shapeTool.strokeWidthPx,
                s.shapeTool.isFilled,
                ArrayList(s.elements.map { it.toSaveable() }),
            )
        },
        restore = { saved ->
            @Suppress("UNCHECKED_CAST")
            val elementLists = saved[10] as List<List<Any?>>
            PhotoEditorState(
                scope = scope,
                initial = EditorState(
                    elements = elementLists.map { editElementFromSaveable(it) },
                    selectedElementId = saved[0] as String?,
                    currentTool = toolFromSaveable(saved[1] as String),
                    drawingColor = Color(saved[2] as Int),
                    drawingStrokeWidth = saved[3] as Float,
                    textColor = Color(saved[4] as Int),
                    textSize = (saved[5] as Float).sp,
                    shapeTool = ShapeToolState(
                        selectedShape = editorShapeFromSaveable(saved[6] as String),
                        selectedColor = Color(saved[7] as Int),
                        strokeWidthPx = saved[8] as Float,
                        isFilled = saved[9] as Boolean,
                    ),
                ),
            )
        },
    )

private fun EditorTool.toSaveable(): String = when (this) {
    EditorTool.None -> "none"
    EditorTool.Text -> "text"
    EditorTool.Drawing -> "drawing"
    EditorTool.Shape -> "shape"
}

private fun toolFromSaveable(saved: String): EditorTool = when (saved) {
    "text" -> EditorTool.Text
    "drawing" -> EditorTool.Drawing
    "shape" -> EditorTool.Shape
    else -> EditorTool.None
}

// Tolerates renamed/removed enum entries in old saved state.
private fun editorShapeFromSaveable(saved: String): EditorShape =
    runCatching { EditorShape.valueOf(saved) }.getOrDefault(EditorShape.Rect)

// Each element flattens to an ArrayList whose first entry is a type tag; the
// remaining slots are positional and must match between toSaveable and
// editElementFromSaveable for that type.

internal fun EditElement.toSaveable(): ArrayList<Any?> = when (this) {
    is EditElement.Text -> arrayListOf(
        "text", id, zIndex, offsetRatioX, offsetRatioY, rotation, scale,
        text, color.toArgb(), fontSize.value, fontWeight == FontWeight.Bold,
    )

    is EditElement.Emoji -> arrayListOf(
        "emoji", id, zIndex, offsetRatioX, offsetRatioY, rotation, scale,
        emoji, size.value,
    )

    is EditElement.Shape -> arrayListOf(
        "shape", id, zIndex, offsetRatioX, offsetRatioY, rotation, scale,
        kind.name, widthRatio, heightRatio, color.toArgb(), strokeWidth, filled,
    )

    is EditElement.Drawing -> arrayListOf(
        "drawing", id, zIndex, color.toArgb(), strokeWidth,
        FloatArray(points.size * 2).also { arr ->
            points.forEachIndexed { i, p ->
                arr[i * 2] = p.x
                arr[i * 2 + 1] = p.y
            }
        },
    )
}

internal fun editElementFromSaveable(saved: List<Any?>): EditElement = when (saved[0] as String) {
    "text" -> EditElement.Text(
        id = saved[1] as String,
        zIndex = saved[2] as Int,
        offsetRatioX = saved[3] as Float,
        offsetRatioY = saved[4] as Float,
        rotation = saved[5] as Float,
        scale = saved[6] as Float,
        text = saved[7] as String,
        color = Color(saved[8] as Int),
        fontSize = (saved[9] as Float).sp,
        fontWeight = if (saved[10] as Boolean) FontWeight.Bold else FontWeight.Normal,
    )

    "emoji" -> EditElement.Emoji(
        id = saved[1] as String,
        zIndex = saved[2] as Int,
        offsetRatioX = saved[3] as Float,
        offsetRatioY = saved[4] as Float,
        rotation = saved[5] as Float,
        scale = saved[6] as Float,
        emoji = saved[7] as String,
        size = (saved[8] as Float).sp,
    )

    "shape" -> EditElement.Shape(
        id = saved[1] as String,
        zIndex = saved[2] as Int,
        offsetRatioX = saved[3] as Float,
        offsetRatioY = saved[4] as Float,
        rotation = saved[5] as Float,
        scale = saved[6] as Float,
        kind = editorShapeFromSaveable(saved[7] as String),
        widthRatio = saved[8] as Float,
        heightRatio = saved[9] as Float,
        color = Color(saved[10] as Int),
        strokeWidth = saved[11] as Float,
        filled = saved[12] as Boolean,
    )

    "drawing" -> {
        val coords = saved[5] as FloatArray
        EditElement.Drawing(
            id = saved[1] as String,
            zIndex = saved[2] as Int,
            color = Color(saved[3] as Int),
            strokeWidth = saved[4] as Float,
            points = List(coords.size / 2) { i -> Offset(coords[i * 2], coords[i * 2 + 1]) },
        )
    }

    else -> error("Unknown saved element type: ${saved[0]}")
}
