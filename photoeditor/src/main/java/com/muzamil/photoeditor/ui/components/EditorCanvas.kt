package com.muzamil.photoeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorState
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.export.buildSmoothDrawingPath
import com.muzamil.photoeditor.geometry.CropGeometry

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
    onContinueDrawing: (List<Offset>) -> Unit,
    onFinishDrawing: () -> Unit,
    onElementTap: (String) -> Unit,
    onUpdateElement: (String, Float, Float, Float, Float) -> Unit,
    onContainerSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val scaleFactor = remember(bitmap, canvasSize) {
        bitmap?.let {
            val scaleX = canvasSize.width / it.width.toFloat()
            val scaleY = canvasSize.height / it.height.toFloat()
            if (scaleX == 0f || scaleY == 0f) 1f else minOf(scaleX, scaleY)
        } ?: 1f
    }

    // Publish scaleFactor so the ViewModel can hand it to the exporter — text and
    // emoji sizes are stored in sp and need this to convert to bitmap-pixels.
//    LaunchedEffect(scaleFactor) {
//        onScaleFactorChanged(scaleFactor)
//    }

    // The image is displayed with ContentScale.Crop; CropGeometry is the single
    // source of truth for the screen↔ratio mapping, shared by pointer input and
    // the drawing renderer below. Do NOT use scaleFactor (minOf/Fit) for this.
    val cropGeometry = remember(bitmap, canvasSize) {
        CropGeometry(
            bitmapWidth = bitmap?.width?.toFloat() ?: 0f,
            bitmapHeight = bitmap?.height?.toFloat() ?: 0f,
            containerWidth = canvasSize.width,
            containerHeight = canvasSize.height,
        )
    }
    fun screenToRatio(screenOffset: Offset): Offset = cropGeometry.screenToRatio(screenOffset)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // or remove background entirely
            .clipToBounds()
            .onSizeChanged {
                canvasSize = Size(it.width.toFloat(), it.height.toFloat())
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner Box now fills the container — Crop handles overflow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    containerSize = it
                    onContainerSizeChanged(
                        IntSize(
                            it.width,
                            it.height
                        )
                    )  // ← was: compute scale and call onScaleFactorChanged
                }
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop, // ← was Fit
                    modifier = Modifier.fillMaxSize()
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentTool) {
                        if (currentTool == EditorTool.Drawing) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    onStartDrawing(screenToRatio(offset))
                                },
                                onDrag = { change, _ ->
                                    // Include the high-frequency samples the input
                                    // system batched between frames — one drag event
                                    // per frame alone is too sparse for smooth curves.
                                    val points = buildList {
                                        change.historical.forEach { add(screenToRatio(it.position)) }
                                        add(screenToRatio(change.position))
                                    }
                                    onContinueDrawing(points)
                                },
                                onDragEnd = { onFinishDrawing() },
                                onDragCancel = { onFinishDrawing() }
                            )
                        } else {
                            // Taps on elements are consumed by DraggableElement above;
                            // only taps on empty canvas reach this layer.
                            detectTapGestures { offset ->
                                onCanvasTap(screenToRatio(offset))
                            }
                        }
                    }) {
                if (!cropGeometry.isValid) return@Canvas

                elements.elements.forEach { element ->
                    if (element is EditElement.Drawing && element.points.isNotEmpty()) {
                        // Same smoothing helper as the exporter; CropGeometry maps
                        // normalized → screen coords (crop scale + offsets).
                        val path = buildSmoothDrawingPath(
                            element.points,
                            cropGeometry::ratioToScreen,
                        )
                        drawPath(
                            path = path.asComposePath(),
                            color = element.color,
                            style = Stroke(
                                width = element.strokeWidth * cropGeometry.scale,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
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
