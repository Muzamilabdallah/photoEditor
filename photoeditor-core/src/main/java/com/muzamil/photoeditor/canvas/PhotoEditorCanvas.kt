package com.muzamil.photoeditor.canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 */
@Composable
fun PhotoEditorCanvas(
    state: PhotoEditorState = rememberPhotoEditorState(),
    modifier: Modifier = Modifier,
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
        modifier = modifier,
    )
}
