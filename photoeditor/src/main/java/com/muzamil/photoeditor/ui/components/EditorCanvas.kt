package com.muzamil.photoeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorState
import com.muzamil.photoeditor.domain.EditorTool

/**
 * The main editing surface. Shows the source bitmap, freehand drawings, and
 * draggable text/emoji elements. All gesture handling and ratio↔pixel math
 * lives here so the surrounding screen stays focused on layout.
 */
@Composable
internal fun EditorCanvas(
    bitmap: Bitmap?,
    elements: EditorState,
    currentTool: EditorTool,
    selectedElementId: String?,
    onCanvasTap: (Offset) -> Unit,
    onStartDrawing: (Offset) -> Unit,
    onContinueDrawing: (Offset) -> Unit,
    onFinishDrawing: () -> Unit,
    onElementTap: (String) -> Unit,
    onUpdateElement: (String, Float, Float, Float, Float) -> Unit,
    onScaleFactorChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val scaleFactor = remember(bitmap, canvasSize) {
        bitmap?.let {
            val scaleX = canvasSize.width / it.width.toFloat()
            val scaleY = canvasSize.height / it.height.toFloat()
            if (scaleX == 0f || scaleY == 0f) 1f else minOf(scaleX, scaleY)
        } ?: 1f
    }

    LaunchedEffect(bitmap, scaleFactor, canvasSize) {
        bitmap?.let {
            val displayWidth = it.width * scaleFactor
            val displayHeight = it.height * scaleFactor
            imageOffset = Offset(
                (canvasSize.width - displayWidth) / 2,
                (canvasSize.height - displayHeight) / 2
            )
        }
    }

    // Publish scaleFactor so the ViewModel can hand it to the exporter — text and
    // emoji sizes are stored in sp and need this to convert to bitmap-pixels.
    LaunchedEffect(scaleFactor) {
        onScaleFactorChanged(scaleFactor)
    }

    fun screenToBitmapCoordinates(screenOffset: Offset): Offset =
        Offset(
            (screenOffset.x - imageOffset.x) / scaleFactor,
            (screenOffset.y - imageOffset.y) / scaleFactor
        )

    // Drawing points are stored as ratios (0..1) relative to the bitmap so
    // they stay correct at any preview size or bitmap resolution.
    fun screenToRatio(screenOffset: Offset): Offset {
        val b = bitmap ?: return Offset.Zero
        val bmp = screenToBitmapCoordinates(screenOffset)
        return Offset(bmp.x / b.width, bmp.y / b.height)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.LightGray)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        // This inner Box is sized to exactly match the fitted image
        Box(
            modifier = Modifier
                .then(
                    if (bitmap != null && canvasSize != Size.Zero) {
                        val scaleX = canvasSize.width / bitmap.width.toFloat()
                        val scaleY = canvasSize.height / bitmap.height.toFloat()
                        val scale = minOf(scaleX, scaleY)
                        val displayWidth = (bitmap.width * scale).toInt()
                        val displayHeight = (bitmap.height * scale).toInt()
                        Modifier.size(
                            width = with(density) { displayWidth.toDp() },
                            height = with(density) { displayHeight.toDp() }
                        )
                    } else {
                        Modifier
                    }
                )
                .onSizeChanged { containerSize = it }
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Drawing layer — captures freehand strokes and renders all Drawing elements.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentTool) {
                        when (currentTool) {
                            EditorTool.Drawing -> detectDragGestures(
                                onDragStart = { offset -> onStartDrawing(screenToRatio(offset)) },
                                onDrag = { change, _ -> onContinueDrawing(screenToRatio(change.position)) },
                                onDragEnd = { onFinishDrawing() }
                            )
                            EditorTool.Text -> detectTapGestures { offset ->
                                onCanvasTap(screenToBitmapCoordinates(offset))
                            }
                            EditorTool.None, is EditorTool.Shape -> Unit
                        }
                    }
            ) {
                val bmp = bitmap ?: return@Canvas
                val bmpW = bmp.width.toFloat()
                val bmpH = bmp.height.toFloat()
                elements.elements.forEach { element ->
                    if (element is EditElement.Drawing && element.points.size > 1) {
                        for (i in 0 until element.points.size - 1) {
                            val p1 = element.points[i]
                            val p2 = element.points[i + 1]
                            val start = Offset(p1.x * bmpW, p1.y * bmpH) * scaleFactor + imageOffset
                            val end = Offset(p2.x * bmpW, p2.y * bmpH) * scaleFactor + imageOffset
                            drawLine(
                                color = element.color,
                                start = start,
                                end = end,
                                strokeWidth = element.strokeWidth * scaleFactor,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // Draggable text/emoji layer.
            if (containerSize.width > 0 && containerSize.height > 0) {
                val parentWidth = containerSize.width
                val parentHeight = containerSize.height
                elements.elements.forEach { element ->
                    key(element.id) {
                        when (element) {
                            is EditElement.Text -> DraggableElement(
                                element = element,
                                isSelected = element.id == selectedElementId,
                                parentWidth = parentWidth,
                                parentHeight = parentHeight,
                                onUpdate = { rx, ry, rot, sc ->
                                    onUpdateElement(element.id, rx, ry, rot, sc)
                                },
                                onTap = {
                                    if (currentTool != EditorTool.Drawing) onElementTap(element.id)
                                }
                            ) { selected -> TextElementContent(element, selected) }

                            is EditElement.Emoji -> DraggableElement(
                                element = element,
                                isSelected = element.id == selectedElementId,
                                parentWidth = parentWidth,
                                parentHeight = parentHeight,
                                onUpdate = { rx, ry, rot, sc ->
                                    onUpdateElement(element.id, rx, ry, rot, sc)
                                },
                                onTap = {
                                    if (currentTool != EditorTool.Drawing) onElementTap(element.id)
                                }
                            ) { selected -> EmojiElementContent(element, selected) }

                            is EditElement.Shape -> DraggableElement(
                                element = element,
                                isSelected = element.id == selectedElementId,
                                parentWidth = parentWidth,
                                parentHeight = parentHeight,
                                onUpdate = { rx, ry, rot, sc ->
                                    onUpdateElement(element.id, rx, ry, rot, sc)
                                },
                                onTap = {
                                    if (currentTool != EditorTool.Drawing) onElementTap(element.id)
                                }
                            ) { selected ->
                                ShapeElementContent(
                                    element = element,
                                    isSelected = selected,
                                    parentWidthPx = parentWidth,
                                    parentHeightPx = parentHeight,
                                    scaleFactor = scaleFactor
                                )
                            }

                            is EditElement.Drawing -> Unit // rendered in Canvas above
                        }
                    }
                }
            }
        }
    }
}
