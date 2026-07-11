package com.muzamil.photoeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.viewmodel.PhotoEditorViewModel

@Deprecated(
    message = "Use the PhotoEditor composable instead — it wires up the theme, " +
        "ViewModel, and photo picking for you.",
    replaceWith = ReplaceWith(
        "PhotoEditor(viewModel = viewModel, onPickImage = onPickImage)",
        "com.muzamil.photoeditor.ui.PhotoEditor",
    ),
)
@Composable
fun MyEditorScreen(viewModel: PhotoEditorViewModel, onPickImage: () -> Unit) {
    EditorHostContent(viewModel = viewModel, onPickImage = onPickImage)
}

/**
 * State adapter between [PhotoEditorViewModel] and the stateless
 * [PhotoEditorScreen]: collects the state flows and wires ViewModel methods
 * into the screen's callback parameters.
 */
@Composable
internal fun EditorHostContent(
    viewModel: PhotoEditorViewModel,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
    saveDestination: SaveDestination = SaveDestination.Gallery(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val context = LocalContext.current
    PhotoEditorScreen(
        bitmap = state.backgroundBitmap,
        editorState = state,
        canUndo = canUndo,
        canRedo = canRedo,
        saveSuccess = saveSuccess,
        onSaveSuccessShown = viewModel::clearSaveSuccess,
        onToolSelected = viewModel::selectTool,
        onAddText = viewModel::addText,
        onAddEmoji = viewModel::addEmoji,
        onAddShape = viewModel::addShape,
        onStartDrawing = viewModel::startDrawing,
        onContinueDrawing = viewModel::continueDrawing,
        onFinishDrawing = viewModel::finishDrawing,
        onElementSelected = viewModel::selectElement,
        onDeselect = viewModel::deselectElement,
        onDeleteSelected = viewModel::deleteSelectedElement,
        onUpdateElement = { id, ratioX, ratioY, rotation, scale ->
            viewModel.updateElementTransform(id, ratioX, ratioY, rotation, scale)
        },
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onClearAll = viewModel::clearAll,
        onPickImage = onPickImage,
        onSaveImage = {
            viewModel.saveEditedImage(context = context, destination = saveDestination)
        },
        onContainerSizeChanged = viewModel::setContainerSize,
        onUpdateTextDefault = { color, size -> viewModel.updateTextStyle(color, size) },
        onUpdateDrawingDefault = { color, stroke -> viewModel.updateDrawingStyle(color, stroke) },
        onUpdateShapeDefault = { color, stroke, filled ->
            viewModel.updateShapeStyle(color, stroke, filled)
        },
        onUpdateSelectedTextStyle = { color, size ->
            viewModel.updateSelectedTextStyle(color, size)
        },
        onUpdateSelectedEmojiSize = viewModel::updateSelectedEmojiSize,
        onUpdateSelectedDrawingStyle = { color, stroke ->
            viewModel.updateSelectedDrawingStyle(color, stroke)
        },
        onUpdateSelectedShapeStyle = { color, stroke, filled ->
            viewModel.updateSelectedShapeStyle(color, stroke, filled)
        },
        modifier = modifier,
    )
}
