package com.muzamil.photoeditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.muzamil.photoeditor.canvas.PhotoEditorCanvas
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.state.PhotoEditorState
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorColors
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
        selectedElementBounds = state.selectedElementBounds,
        isInteracting = state.isInteracting,
        saveSuccess = state.saveSuccess,
        onSaveSuccessShown = state::clearSaveSuccess,
        onToolSelected = state::selectTool,
        onAddText = { text, position ->
            // Select the new text so the style panel pops up bound to it.
            state.selectElement(state.addText(text, position))
        },
        // Select the new emoji/shape so they come up ready to move/style,
        // just like a freshly added text.
        onAddEmoji = { emoji -> state.selectElement(state.addEmoji(emoji)) },
        onAddShape = { state.selectElement(state.addShape()) },
        onUpdateShapeTool = state::updateShapeTool,
        onDeselect = state::deselectElement,
        onDeleteSelected = state::deleteSelectedElement,
        onDuplicateSelected = {
            // Select the copy so the pill and style panel follow it.
            editorState.selectedElementId
                ?.let(state::duplicateElement)
                ?.let(state::selectElement)
        },
        onUpdateTextContent = state::updateTextContent,
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
            PhotoEditorCanvas(
                state = state,
                modifier = Modifier.fillMaxSize(),
                selectionColor = LocalPhotoEditorColors.current.accent,
            )
        },
    )
}
