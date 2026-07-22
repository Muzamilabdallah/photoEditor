package com.muzamil.photoeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.muzamil.photoeditor.canvas.PhotoEditorCanvas
import com.muzamil.photoeditor.state.PhotoEditorState
import com.muzamil.photoeditor.ui.components.EditorTopBar
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorColors

/**
 * Edge-to-edge editor skeleton for composing your own editor from the public
 * pieces: a full-bleed [content] area (the canvas by default) with [topBar]
 * and [bottomBar] floating over its top and bottom edges.
 *
 * Insets are wired correctly with zero consumer code when the host activity
 * is edge-to-edge (enforced on Android 15+): the canvas stays full-bleed
 * behind the system bars, while the default [EditorTopBar] consumes the
 * status-bar insets itself — background bleeding behind the status bar,
 * controls padded below it. A custom [topBar]/[bottomBar] is responsible for
 * its own insets the same way (`Modifier.windowInsetsPadding` inside its
 * background; see `EditorTopBar`'s `windowInsets` parameter for the pattern).
 *
 * The default top bar binds confirm/undo/redo to [state] and saves to the
 * gallery; pass your own [topBar] for add-image/close buttons or a different
 * save destination. The library never touches the Activity window —
 * enabling edge-to-edge (or not) stays the host app's decision.
 */
@Composable
fun PhotoEditorScaffold(
    state: PhotoEditorState,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {
        val context = LocalContext.current
        EditorTopBar(
            historyState = state.historyState,
            onConfirm = { state.saveEditedImage(context) },
            onUndo = state::undo,
            onRedo = state::redo,
            showConfirm = state.editorState.backgroundBitmap != null,
        )
    },
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {
        PhotoEditorCanvas(
            state = state,
            modifier = Modifier.fillMaxSize(),
            selectionColor = LocalPhotoEditorColors.current.accent,
        )
    },
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        Box(modifier = Modifier.align(Alignment.BottomCenter)) { bottomBar() }
        Box(modifier = Modifier.align(Alignment.TopCenter)) { topBar() }
    }
}
