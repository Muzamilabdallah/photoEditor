package com.muzamil.photoeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.state.PhotoEditorState

/**
 * Adapter between [PhotoEditorState] and the stateless [PhotoEditorScreen]:
 * wires the state holder's snapshot state and methods into the screen's
 * parameters.
 */
@Composable
internal fun EditorHostContent(
    state: PhotoEditorState,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
    saveDestination: SaveDestination = SaveDestination.Gallery(),
) {
    val context = LocalContext.current
    val editorState = state.editorState
    PhotoEditorScreen(
        bitmap = editorState.backgroundBitmap,
        editorState = editorState,
        canUndo = state.canUndo,
        canRedo = state.canRedo,
        saveSuccess = state.saveSuccess,
        onSaveSuccessShown = state::clearSaveSuccess,
        onToolSelected = state::selectTool,
        onAddText = state::addText,
        onAddEmoji = state::addEmoji,
        onAddShape = state::addShape,
        onStartDrawing = state::startDrawing,
        onContinueDrawing = state::continueDrawing,
        onFinishDrawing = state::finishDrawing,
        onElementSelected = state::selectElement,
        onDeselect = state::deselectElement,
        onDeleteSelected = state::deleteSelectedElement,
        onUpdateElement = state::updateElementTransform,
        onUndo = state::undo,
        onRedo = state::redo,
        onClearAll = state::clearAll,
        onPickImage = onPickImage,
        onSaveImage = {
            state.saveEditedImage(context = context, destination = saveDestination)
        },
        onContainerSizeChanged = state::setContainerSize,
        onUpdateTextDefault = state::updateTextStyle,
        onUpdateDrawingDefault = state::updateDrawingStyle,
        onUpdateShapeDefault = state::updateShapeStyle,
        onUpdateSelectedTextStyle = state::updateSelectedTextStyle,
        onUpdateSelectedEmojiSize = state::updateSelectedEmojiSize,
        onUpdateSelectedDrawingStyle = state::updateSelectedDrawingStyle,
        onUpdateSelectedShapeStyle = state::updateSelectedShapeStyle,
        modifier = modifier,
    )
}
