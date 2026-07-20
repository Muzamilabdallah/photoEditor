package com.muzamil.photoeditor.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorHistoryState
import com.muzamil.photoeditor.domain.EditorShape
import com.muzamil.photoeditor.domain.EditorState
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.ui.components.EditorStylePanel
import com.muzamil.photoeditor.ui.components.EditorToolbar
import com.muzamil.photoeditor.ui.components.EditorTopBar
import com.muzamil.photoeditor.ui.components.StyleContext
import com.muzamil.photoeditor.ui.components.TextInputDialog
import com.muzamil.photoeditor.ui.emojitool.EmojiToolPanel
import com.muzamil.photoeditor.ui.icons.EditorIcons
import com.muzamil.photoeditor.ui.shapetool.ShapeToolPanel
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorColors
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorTypography
import com.muzamil.photoeditor.ui.theme.PhotoEditorTheme
import com.muzamil.photoeditor.ui.theme.darkPhotoEditorColors
import com.muzamil.photoeditor.ui.theme.lightPhotoEditorColors

private val ToolbarHeight = 72.dp

@Composable
internal fun PhotoEditorScreen(
    bitmap: Bitmap?,
    editorState: EditorState,
    historyState: EditorHistoryState,
    saveSuccess: String?,
    onSaveSuccessShown: () -> Unit,
    onPickImage: (() -> Unit)?,
    onClose: (() -> Unit)?,
    onSaveImage: () -> Unit,
    onToolSelected: (EditorTool) -> Unit,
    onAddText: (String, Offset) -> Unit,
    onAddEmoji: (String) -> Unit,
    onAddShape: () -> Unit,
    onUpdateShapeTool: (EditorShape?, Color?, Float?, Boolean?) -> Unit,
    onDeselect: () -> Unit,
    onDeleteSelected: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    emojis: List<String>,
    swatchColors: List<Color>,

    onUpdateTextDefault: (Color?, TextUnit?) -> Unit,
    onUpdateDrawingDefault: (Color?, Float?) -> Unit,
    onUpdateSelectedTextStyle: (Color?, TextUnit?) -> Unit,
    onUpdateSelectedEmojiSize: (TextUnit) -> Unit,
    onUpdateSelectedDrawingStyle: (Color?, Float?) -> Unit,
    onUpdateSelectedShapeStyle: (Color?, Float?, Boolean?) -> Unit,
    modifier: Modifier = Modifier,
    // The editing surface, injected by the host — core's PhotoEditorCanvas
    // wires its own gestures into the state holder.
    canvasContent: @Composable () -> Unit,
) {
    var showTextDialog by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val textDialogPosition by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess != null) {
            snackbarHostState.showSnackbar("Image saved")
            onSaveSuccessShown()
        }
    }

    val styleContext = remember(editorState) {
        editorState.toStyleContext(
            onUpdateTextDefault = onUpdateTextDefault,
            onUpdateDrawingDefault = onUpdateDrawingDefault,
            onUpdateSelectedTextStyle = onUpdateSelectedTextStyle,
            onUpdateSelectedEmojiSize = onUpdateSelectedEmojiSize,
            onUpdateSelectedDrawingStyle = onUpdateSelectedDrawingStyle,
            onUpdateSelectedShapeStyle = onUpdateSelectedShapeStyle
        )
    }

    val selected = editorState.selectedElementId?.let { id ->
        editorState.elements.firstOrNull { it.id == id }
    }
    val selectionIsStyleable = selected is EditElement.Text ||
            selected is EditElement.Emoji ||
            selected is EditElement.Drawing ||
            selected is EditElement.Shape
    // Text gets no default-style panel: the sheet pops up only once the text
    // is drawn and selected (see the TextInputDialog onConfirm below).
    val toolIsDrawing = editorState.currentTool == EditorTool.Drawing
    val showStylePanel = (toolIsDrawing || selectionIsStyleable) && styleContext != null
    val showShapePanel = editorState.currentTool == EditorTool.Shape

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            EditorTopBar(
                historyState = historyState,
                onConfirm = onSaveImage,
                onUndo = onUndo,
                onRedo = onRedo,
                onAddImage = onPickImage,
                onClose = onClose,
                // Nothing to confirm without an image.
                showConfirm = bitmap != null,
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (bitmap == null) {
                EmptyState(onPickImage = onPickImage)
            } else {
                canvasContent()
            }

            if (showShapePanel) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { onToolSelected(EditorTool.None) }
                        }
                )
            }

            if (showEmojiPicker) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showEmojiPicker = false }
                        }
                )
            }

            if (editorState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            AnimatedVisibility(
                visible = showStylePanel,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ToolbarHeight)
            ) {
                styleContext?.let { ctx ->
                    EditorStylePanel(
                        context = ctx,
                        onClose = {
                            onDeselect()
                            onToolSelected(EditorTool.None)
                        },
                        onDelete = if (selected != null) onDeleteSelected else null,
                        swatchColors = swatchColors,
                    )
                }
            }

            AnimatedVisibility(
                visible = showShapePanel,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ToolbarHeight)
            ) {
                ShapeToolPanel(
                    state = editorState.shapeTool,
                    onShapeSelected = { onUpdateShapeTool(it, null, null, null) },
                    onColorSelected = { onUpdateShapeTool(null, it, null, null) },
                    onStrokeWidthChanged = { onUpdateShapeTool(null, null, it, null) },
                    onFilledChanged = { onUpdateShapeTool(null, null, null, it) },
                    onDone = {
                        onAddShape()
                        onToolSelected(EditorTool.None)
                    },
                    onDismiss = { onToolSelected(EditorTool.None) },
                    swatchColors = swatchColors,
                )
            }

            AnimatedVisibility(
                visible = showEmojiPicker,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ToolbarHeight)
            ) {
                EmojiToolPanel(
                    onEmojiSelected = { emoji ->
                        onAddEmoji(emoji)
                        showEmojiPicker = false
                    },
                    onDismiss = { showEmojiPicker = false },
                    emojis = emojis,
                )
            }

            EditorToolbar(
                currentTool = editorState.currentTool,
                onToolSelected = { tool ->
                    if (tool == EditorTool.Text) showTextDialog = true
                    onToolSelected(tool)
                },
                onEmojiClick = { showEmojiPicker = true },
                onShapeClick = { onToolSelected(EditorTool.Shape) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            editorState.error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ToolbarHeight)
                        .padding(16.dp)
                ) { Text(errorMessage) }
            }
        }
    }

    if (showTextDialog) {
        TextInputDialog(
            onDismiss = {
                showTextDialog = false
                onToolSelected(EditorTool.None)
            },
            onConfirm = { text ->
                showTextDialog = false
                // Reset the tool BEFORE adding: selectTool clears the selection,
                // and onAddText selects the new text so the style sheet opens.
                onToolSelected(EditorTool.None)
                onAddText(text, textDialogPosition)
            }
        )
    }

}

private fun EditorState.toStyleContext(
    onUpdateTextDefault: (Color?, TextUnit?) -> Unit,
    onUpdateDrawingDefault: (Color?, Float?) -> Unit,
    onUpdateSelectedTextStyle: (Color?, TextUnit?) -> Unit,
    onUpdateSelectedEmojiSize: (TextUnit) -> Unit,
    onUpdateSelectedDrawingStyle: (Color?, Float?) -> Unit,
    onUpdateSelectedShapeStyle: (Color?, Float?, Boolean?) -> Unit
): StyleContext? {
    val selected = selectedElementId?.let { id -> elements.firstOrNull { it.id == id } }
    return when {
        selected is EditElement.Text -> StyleContext.TextStyle(
            color = selected.color,
            fontSize = selected.fontSize,
            onColor = { onUpdateSelectedTextStyle(it, null) },
            onFontSize = { onUpdateSelectedTextStyle(null, it) }
        )

        selected is EditElement.Emoji -> StyleContext.EmojiStyle(
            size = selected.size,
            onSize = onUpdateSelectedEmojiSize
        )

        selected is EditElement.Drawing -> StyleContext.DrawingStyle(
            color = selected.color,
            strokeWidth = selected.strokeWidth,
            onColor = { onUpdateSelectedDrawingStyle(it, null) },
            onStroke = { onUpdateSelectedDrawingStyle(null, it) }
        )

        selected is EditElement.Shape -> StyleContext.ShapeStyle(
            color = selected.color,
            strokeWidth = selected.strokeWidth,
            filled = selected.filled,
            onColor = { onUpdateSelectedShapeStyle(it, null, null) },
            onStroke = { onUpdateSelectedShapeStyle(null, it, null) },
            onFilled = { onUpdateSelectedShapeStyle(null, null, it) }
        )

        currentTool == EditorTool.Text -> StyleContext.TextStyle(
            color = textColor,
            fontSize = textSize,
            onColor = { onUpdateTextDefault(it, null) },
            onFontSize = { onUpdateTextDefault(null, it) }
        )

        currentTool == EditorTool.Drawing -> StyleContext.DrawingStyle(
            color = drawingColor,
            strokeWidth = drawingStrokeWidth,
            onColor = { onUpdateDrawingDefault(it, null) },
            onStroke = { onUpdateDrawingDefault(null, it) }
        )

        else -> null
    }
}

@Composable
private fun EmptyState(onPickImage: (() -> Unit)?) {
    val colors = LocalPhotoEditorColors.current
    val typography = LocalPhotoEditorTypography.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(colors.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = EditorIcons.ImagePlaceholder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = colors.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No image selected",
            style = typography.screenTitle,
            color = colors.onSurface
        )
        if (onPickImage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pick a photo to start editing",
                style = typography.bodySecondary,
                color = colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Accent pill matching the panels' Done button.
            Row(
                modifier = Modifier
                    .background(colors.accent, RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClick = onPickImage)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = EditorIcons.AddPhoto,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Pick Image",
                    style = typography.button,
                    color = colors.onAccent
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Empty state, light", showBackground = true)
@Composable
private fun EmptyStateLightPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        EmptyState(onPickImage = {})
    }
}

@Preview(name = "Empty state, dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun EmptyStateDarkPreview() {
    PhotoEditorTheme(colors = darkPhotoEditorColors()) {
        EmptyState(onPickImage = {})
    }
}
