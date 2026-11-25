package com.muzamil.photoeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.photoforge.canvas.EditorShapePaths
import io.github.photoforge.canvas.PhotoEditorCanvas
import io.github.photoforge.domain.EditorColors
import io.github.photoforge.domain.EditorShape
import io.github.photoforge.domain.EditorTool
import io.github.photoforge.image.decodeEditableBitmap
import io.github.photoforge.state.rememberPhotoEditorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mode A sample: a hand-rolled editor UI over :photoeditor-core ONLY.
 *
 * Every import above comes from photoeditor-core (or the Compose foundation
 * it re-exports) — nothing from :photoeditor-ui-default — proving the core
 * API is sufficient for a fully custom UI: [PhotoEditorCanvas] handles all
 * canvas gestures, and this toolbar drives the rest through
 * `PhotoEditorState` and the shared tool state types ([EditorShape],
 * [EditorColors], `ShapeToolState`).
 */
@Composable
fun CustomUiSample() {
    val context = LocalContext.current
    val state = rememberPhotoEditorState()
    var pickedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedImageUri = uri
    }

    LaunchedEffect(pickedImageUri) {
        val uri = pickedImageUri ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            decodeEditableBitmap(context.contentResolver, uri)
        }?.let(state::loadImage)
    }

    val editorState = state.editorState
    val drawingActive = editorState.currentTool == EditorTool.Drawing

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF101114))) {
        PhotoEditorCanvas(state = state, modifier = Modifier.weight(1f))


        // The custom toolbar: plain Compose, no library UI.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B1C20))
                .navigationBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Shape strip — driven by core's EditorShape + ShapeToolState.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EditorShape.entries.forEach { shape ->
                    val selected = editorState.shapeTool.selectedShape == shape
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (selected) Color(0xFF33415C) else Color(0xFF26272C),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) Color(0xFF7EA6FF) else Color(0xFF3A3B41),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                state.updateShapeTool(shape = shape)
                                state.addShape(shape)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawPath(
                                path = EditorShapePaths.path(shape, size.width, size.height),
                                color = Color.White,
                                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                }
            }

            // Color strip — core's shared palette.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorColors.Palette.forEach { color ->
                    val selected = editorState.shapeTool.selectedColor == color &&
                            editorState.drawingColor == color
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(color, CircleShape)
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) Color(0xFF7EA6FF) else Color(0xFF3A3B41),
                                CircleShape
                            )
                            .clickable {
                                state.updateShapeTool(color = color)
                                state.updateDrawingStyle(color = color)
                            }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToolbarButton("Pick") {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                ToolbarButton("Text") { state.addText("Hello") }
                ToolbarButton(
                    label = if (drawingActive) "Draw ✓" else "Draw",
                    emphasized = drawingActive,
                ) {
                    state.selectTool(if (drawingActive) EditorTool.None else EditorTool.Drawing)
                }
                ToolbarButton("Undo", enabled = state.canUndo) { state.undo() }
                ToolbarButton("Redo", enabled = state.canRedo) { state.redo() }
                ToolbarButton("Save", enabled = editorState.backgroundBitmap != null) {
                    state.saveEditedImage(context)
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                when {
                    emphasized -> Color(0xFF33415C)
                    enabled -> Color(0xFF26272C)
                    else -> Color(0xFF1E1F24)
                },
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = if (enabled) Color.White else Color(0xFF6A6C71),
                fontSize = 13.sp,
            ),
        )
    }
}
