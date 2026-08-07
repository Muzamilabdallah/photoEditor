package com.muzamil.photoeditor.canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.muzamil.photoeditor.state.PhotoEditorState
import com.muzamil.photoeditor.state.rememberPhotoEditorState

/**
 * The editing surface alone, without any surrounding chrome — for apps that
 * want to build their own toolbar, top bar, and dialogs around the editor.
 *
 * Renders the loaded bitmap and all elements, and handles every editing
 * gesture (tap-to-select, tap-outside-to-deselect, drag/rotate/scale,
 * freehand drawing), wiring them into [state] automatically. Everything else
 * is driven through [state]'s API from your own UI:
 *
 * ```
 * val state = rememberPhotoEditorState()
 * Column {
 *     PhotoEditorCanvas(state, Modifier.weight(1f))
 *     MyToolbar(
 *         onText = { state.addText("Hello") },
 *         onDraw = { state.selectTool(EditorTool.Drawing) },
 *         onShape = { state.addShape(EditorShape.Circle) },
 *         onUndo = { state.undo() },
 *         onSave = { state.saveEditedImage(context) },
 *     )
 * }
 * ```
 *
 * Load an image with [PhotoEditorState.loadImage]; decode it with
 * [com.muzamil.photoeditor.image.decodeEditableBitmap] first if it comes from
 * a uri — the editor requires a software bitmap. For the batteries-included
 * editor (theme, toolbar, tool panels), add the `photoeditor-ui-default`
 * artifact and use its `PhotoEditor` composable instead.
 *
 * [selectionColor] tints the selected element's border and corner handles —
 * pass your accent color so the selection chrome matches your UI. While an
 * element is selected, its live screen bounds are published on
 * [PhotoEditorState.selectedElementBounds] for anchoring contextual UI, and
 * [PhotoEditorState.isInteracting] is true for as long as a canvas gesture is
 * in flight — fade your own toolbars out while it is set so they never cover
 * the stroke or element being moved.
 */
@Composable
fun PhotoEditorCanvas(
    state: PhotoEditorState = rememberPhotoEditorState(),
    modifier: Modifier = Modifier,
    selectionColor: Color = DefaultSelectionColor,
) {
    val editorState = state.editorState
    EditorCanvas(
        bitmap = editorState.backgroundBitmap,
        elements = editorState,
        currentTool = editorState.currentTool,
        selectedElementId = editorState.selectedElementId,
        onCanvasTap = { state.deselectElement() },
        onStartDrawing = state::startDrawing,
        onContinueDrawing = state::continueDrawing,
        onFinishDrawing = state::finishDrawing,
        onElementTap = state::selectElement,
        onUpdateElement = state::updateElementTransform,
        onContainerSizeChanged = state::setContainerSize,
        selectionColor = selectionColor,
        onSelectedBoundsChanged = state::reportSelectedElementBounds,
        onInteractionChanged = state::setInteracting,
        modifier = modifier,
    )
}

/** Matches the blue the canvas used before [selectionColor] was themable. */
private val DefaultSelectionColor = Color(0xFF2962FF)
