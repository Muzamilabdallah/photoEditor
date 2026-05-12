package com.muzamil.photoeditor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorState
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.domain.ShapeKind
import com.muzamil.photoeditor.ui.components.EditorCanvas
import com.muzamil.photoeditor.ui.components.EditorStylePanel
import com.muzamil.photoeditor.ui.components.EditorToolbar
import com.muzamil.photoeditor.ui.components.EmojiPickerDialog
import com.muzamil.photoeditor.ui.components.StyleContext
import com.muzamil.photoeditor.ui.components.TextInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoEditorScreen(
    bitmap: Bitmap?,
    editorState: EditorState,
    canUndo: Boolean,
    canRedo: Boolean,
    onPickImage: () -> Unit,
    onSaveImage: () -> Unit,
    onToolSelected: (EditorTool) -> Unit,
    onAddText: (String, Offset) -> Unit,
    onAddEmoji: (String) -> Unit,
    onAddShape: (ShapeKind) -> Unit,
    onStartDrawing: (Offset) -> Unit,
    onContinueDrawing: (Offset) -> Unit,
    onFinishDrawing: () -> Unit,
    onElementSelected: (String) -> Unit,
    onUpdateElement: (String, Float, Float, Float, Float) -> Unit,
    onDeleteSelected: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearAll: () -> Unit,
    onScaleFactorChanged: (Float) -> Unit,
    onUpdateTextDefault: (Color?, TextUnit?) -> Unit,
    onUpdateDrawingDefault: (Color?, Float?) -> Unit,
    onUpdateShapeDefault: (Color?, Float?, Boolean?) -> Unit,
    onUpdateSelectedTextStyle: (Color?, TextUnit?) -> Unit,
    onUpdateSelectedEmojiSize: (TextUnit) -> Unit,
    onUpdateSelectedDrawingStyle: (Color?, Float?) -> Unit,
    onUpdateSelectedShapeStyle: (Color?, Float?, Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTextDialog by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val textDialogPosition by remember { mutableStateOf(Offset.Zero) }

    val styleContext = remember(editorState) { editorState.toStyleContext(
        onUpdateTextDefault = onUpdateTextDefault,
        onUpdateDrawingDefault = onUpdateDrawingDefault,
        onUpdateShapeDefault = onUpdateShapeDefault,
        onUpdateSelectedTextStyle = onUpdateSelectedTextStyle,
        onUpdateSelectedEmojiSize = onUpdateSelectedEmojiSize,
        onUpdateSelectedDrawingStyle = onUpdateSelectedDrawingStyle,
        onUpdateSelectedShapeStyle = onUpdateSelectedShapeStyle
    ) }

    Scaffold(
        topBar = {
            EditorTopBar(
                hasBitmap = bitmap != null,
                hasSelection = editorState.selectedElementId != null,
                hasElements = editorState.elements.isNotEmpty(),
                canUndo = canUndo,
                canRedo = canRedo,
                onPickImage = onPickImage,
                onSaveImage = onSaveImage,
                onUndo = onUndo,
                onRedo = onRedo,
                onDeleteSelected = onDeleteSelected,
                onClearAll = onClearAll
            )
        },
        bottomBar = {
            Column {
                styleContext?.let { ctx -> EditorStylePanel(context = ctx) }
                EditorToolbar(
                    currentTool = editorState.currentTool,
                    onToolSelected = { tool ->
                        when (tool) {
                            EditorTool.Text -> showTextDialog = true
                            is EditorTool.Shape -> onAddShape(tool.kind)
                            else -> Unit
                        }
                        onToolSelected(tool)
                    },
                    onEmojiClick = { showEmojiPicker = true }
                )
            }
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
                EditorCanvas(
                    bitmap = bitmap,
                    elements = editorState,
                    currentTool = editorState.currentTool,
                    selectedElementId = editorState.selectedElementId,
                    onCanvasTap = { /* future: place text at tapped offset */ },
                    onStartDrawing = onStartDrawing,
                    onContinueDrawing = onContinueDrawing,
                    onFinishDrawing = onFinishDrawing,
                    onElementTap = onElementSelected,
                    onUpdateElement = onUpdateElement,
                    onScaleFactorChanged = onScaleFactorChanged,
                    modifier = Modifier.fillMaxSize()
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

            editorState.error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) { Text(errorMessage) }
            }
        }
    }

    if (showTextDialog) {
        TextInputDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { text ->
                onAddText(text, textDialogPosition)
                showTextDialog = false
            }
        )
    }

    if (showEmojiPicker) {
        EmojiPickerDialog(
            onDismiss = { showEmojiPicker = false },
            onPick = { emoji ->
                onAddEmoji(emoji)
                showEmojiPicker = false
            }
        )
    }
}

private fun EditorState.toStyleContext(
    onUpdateTextDefault: (Color?, TextUnit?) -> Unit,
    onUpdateDrawingDefault: (Color?, Float?) -> Unit,
    onUpdateShapeDefault: (Color?, Float?, Boolean?) -> Unit,
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
        currentTool is EditorTool.Shape -> StyleContext.ShapeStyle(
            color = shapeColor,
            strokeWidth = shapeStrokeWidth,
            filled = shapeFilled,
            onColor = { onUpdateShapeDefault(it, null, null) },
            onStroke = { onUpdateShapeDefault(null, it, null) },
            onFilled = { onUpdateShapeDefault(null, null, it) }
        )
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    hasBitmap: Boolean,
    hasSelection: Boolean,
    hasElements: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onPickImage: () -> Unit,
    onSaveImage: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearAll: () -> Unit
) {
    TopAppBar(
        title = { Text("Photo Editor") },
        navigationIcon = {
            IconButton(onClick = onPickImage) {
                Icon(Icons.Default.Add, contentDescription = "Pick Image")
            }
        },
        actions = {
            if (hasBitmap) {
                IconButton(onClick = onSaveImage) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Undo",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Redo",
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            if (hasSelection) {
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            if (hasElements) {
                IconButton(onClick = onClearAll) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear All")
                }
            }
        }
    )
}

@Composable
private fun EmptyState(onPickImage: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No image selected",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tap the image icon above to pick a photo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPickImage) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick Image")
        }
    }
}

