package com.muzamil.photoeditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.muzamil.photoeditor.canvas.PhotoEditorCanvas
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.state.PhotoEditorState
import com.muzamil.photoeditor.ui.theme.PhotoEditorDefaults

/**
 * Adapter between [PhotoEditorState] and the stateless [PhotoEditorScreen]:
 * wires the state holder's snapshot state and methods into the screen's
 * parameters, and injects core's [PhotoEditorCanvas] as the editing surface.
 */
@Composable
internal fun EditorHostContent(
    state: PhotoEditorState,
    onPickImage: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    saveDestination: SaveDestination = SaveDestination.Gallery(),
    emojis: List<String> = PhotoEditorDefaults.emojis,
    swatchColors: List<Color> = PhotoEditorDefaults.swatchColors,
) {
    val context = LocalContext.current
    val editorState = state.editorState
    PhotoEditorScreen(
        bitmap = editorState.backgroundBitmap,
        editorState = editorState,
        historyState = state.historyState,
        saveSuccess = state.saveSuccess,
        onSaveSuccessShown = state::clearSaveSuccess,
        onToolSelected = state::selectTool,
        onAddText = { text, position ->
            // Select the new text so the style panel pops up bound to it.
            state.selectElement(state.addText(text, position))
        },
        onAddEmoji = state::addEmoji,
        onAddShape = { state.addShape() },
        onUpdateShapeTool = state::updateShapeTool,
        onDeselect = state::deselectElement,
        onDeleteSelected = state::deleteSelectedElement,
        onUndo = state::undo,
        onRedo = state::redo,
        emojis = emojis,
        swatchColors = swatchColors,
        onPickImage = onPickImage,
        onClose = onClose,
        onSaveImage = {
            state.saveEditedImage(context = context, destination = saveDestination)
        },
        onUpdateTextDefault = state::updateTextStyle,
        onUpdateDrawingDefault = state::updateDrawingStyle,
        onUpdateSelectedTextStyle = state::updateSelectedTextStyle,
        onUpdateSelectedEmojiSize = state::updateSelectedEmojiSize,
        onUpdateSelectedDrawingStyle = state::updateSelectedDrawingStyle,
        onUpdateSelectedShapeStyle = state::updateSelectedShapeStyle,
        modifier = modifier,
        canvasContent = {
            PhotoEditorCanvas(state = state, modifier = Modifier.fillMaxSize())
        },
    )
}
