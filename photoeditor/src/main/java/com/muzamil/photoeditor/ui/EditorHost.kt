package com.muzamil.photoeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muzamil.photoeditor.viewmodel.PhotoEditorViewModel

@Composable
fun MyEditorScreen(viewModel: PhotoEditorViewModel, onPickImage: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    PhotoEditorScreen(
        bitmap = state.backgroundBitmap,
        editorState = state,
        canUndo = canUndo,
        canRedo = canRedo,
        onToolSelected = viewModel::selectTool,
        onAddText = viewModel::addText,
        onAddShape = viewModel::addShape,
        onStartDrawing = viewModel::startDrawing,
        onContinueDrawing = viewModel::continueDrawing,
        onFinishDrawing = viewModel::finishDrawing,
        onElementSelected = viewModel::selectElement,
        onDeleteSelected = viewModel::deleteSelectedElement,
        onUpdateElement = { id, ratioX, ratioY, rotation, scale ->
            viewModel.updateElementTransform(id, ratioX, ratioY, rotation, scale)
        },
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onClearAll = viewModel::clearAll,
        onPickImage = onPickImage,
        onSaveImage = {
            viewModel.saveEditedImage(textMeasurer, context = context)
        },
        onScaleFactorChanged = viewModel::setPreviewScaleFactor
    )
}

