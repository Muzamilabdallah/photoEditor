package com.muzamil.photoeditor.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorState
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.domain.inRenderOrder
import com.muzamil.photoeditor.export.buildSmoothDrawingPath
import com.muzamil.photoeditor.geometry.ImageGeometry

/**
 * The main editing surface. Shows the whole source bitmap (ContentScale.Fit,
 * letterboxed) and every element on top of it, and handles all gestures.
 *
 * The image, the drawing gesture layer and the elements share one box laid
 * out at exactly the image's on-screen rect (see [ImageGeometry]). Inside it,
 * an element's bitmap ratio is just its fraction of the box, so the preview
 * and the exporter use the same numbers with no screen-dependent conversion.
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
    selectionColor: Color,
    onSelectedBoundsChanged: (Rect) -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val geometry = remember(bitmap, containerSize) {
        ImageGeometry(
            bitmapWidth = bitmap?.width?.toFloat() ?: 0f,
            bitmapHeight = bitmap?.height?.toFloat() ?: 0f,
            containerWidth = containerSize.width.toFloat(),
            containerHeight = containerSize.height.toFloat(),
        )
    }
    val imageSize = geometry.imageSize
    val imageOffset = geometry.imageOffset

    // While drawing, elements ignore touches so a stroke can start anywhere,
    // including on top of an element.
    val elementsInteractive = currentTool != EditorTool.Drawing

    // Elements report bounds relative to the image box; the public contract
    // (PhotoEditorState.selectedElementBounds) is in canvas-container pixels.
    val reportBounds: (Rect) -> Unit = { bounds ->
        onSelectedBoundsChanged(bounds.translate(imageOffset.x.toFloat(), imageOffset.y.toFloat()))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .onSizeChanged { containerSize = it }
            // Empty-canvas taps, including the letterbox, deselect. Element
            // taps are consumed by DraggableElement before they get here.
            .pointerInput(currentTool, geometry) {
                if (currentTool != EditorTool.Drawing) {
                    detectTapGestures { offset -> onCanvasTap(geometry.screenToRatio(offset)) }
                }
            }
    ) {
        if (bitmap == null || !geometry.isValid) return@Box

        Box(
            modifier = Modifier.exactBox(imageSize, imageOffset)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            // Drawing gesture layer, sized to the image. Draws nothing itself;
            // strokes are painted in the z-ordered loop that follows.
            fun toRatio(local: Offset) = Offset(
                (local.x / imageSize.width).coerceIn(0f, 1f),
                (local.y / imageSize.height).coerceIn(0f, 1f),
            )
            if (currentTool == EditorTool.Drawing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageSize) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    onInteractionChanged(true)
                                    onStartDrawing(toRatio(offset))
                                },
                                onDrag = { change, _ ->
                                    // Include the high-frequency samples the input
                                    // system batched between frames — one drag event
                                    // per frame alone is too sparse for smooth curves.
                                    val points = buildList {
                                        change.historical.forEach { add(toRatio(it.position)) }
                                        add(toRatio(change.position))
                                    }
                                    onContinueDrawing(points)
                                },
                                onDragEnd = {
                                    onFinishDrawing()
                                    onInteractionChanged(false)
                                },
                                onDragCancel = {
                                    onFinishDrawing()
                                    onInteractionChanged(false)
                                }
                            )
                        }
                )
            }

            // All elements in one bottom-to-top pass, in the same order the
            // exporter paints them, so a stroke drawn over a text is over it on
            // screen too. Drawings are non-interactive full-size layers (no
            // pointer input), so touches pass through them.
            val parentWidth = imageSize.width
            val parentHeight = imageSize.height
            val sizeUnitPx = geometry.sizeUnitPx
            elements.elements.inRenderOrder().forEach { element ->
                key(element.id) {
                    when (element) {
                        is EditElement.Text -> DraggableElement(
                            element = element,
                            isSelected = element.id == selectedElementId,
                            parentWidth = parentWidth,
                            parentHeight = parentHeight,
                            selectionColor = selectionColor,
                            onUpdate = { rx, ry, rot, sc ->
                                onUpdateElement(element.id, rx, ry, rot, sc)
                            },
                            onTap = { onElementTap(element.id) },
                            interactive = elementsInteractive,
                            onSelectedBoundsChanged = reportBounds,
                            onInteractionChanged = onInteractionChanged,
                        ) { selected -> TextElementContent(element, selected, sizeUnitPx) }

                        is EditElement.Emoji -> DraggableElement(
                            element = element,
                            isSelected = element.id == selectedElementId,
                            parentWidth = parentWidth,
                            parentHeight = parentHeight,
                            selectionColor = selectionColor,
                            onUpdate = { rx, ry, rot, sc ->
                                onUpdateElement(element.id, rx, ry, rot, sc)
                            },
                            onTap = { onElementTap(element.id) },
                            interactive = elementsInteractive,
                            onSelectedBoundsChanged = reportBounds,
                            onInteractionChanged = onInteractionChanged,
                        ) { selected -> EmojiElementContent(element, selected, sizeUnitPx) }

                        is EditElement.Shape -> DraggableElement(
                            element = element,
                            isSelected = element.id == selectedElementId,
                            parentWidth = parentWidth,
                            parentHeight = parentHeight,
                            selectionColor = selectionColor,
                            onUpdate = { rx, ry, rot, sc ->
                                onUpdateElement(element.id, rx, ry, rot, sc)
                            },
                            onTap = { onElementTap(element.id) },
                            interactive = elementsInteractive,
                            onSelectedBoundsChanged = reportBounds,
                            onInteractionChanged = onInteractionChanged,
                        ) { selected ->
                            ShapeElementContent(
                                element = element,
                                isSelected = selected,
                                sizeUnitPx = sizeUnitPx,
                            )
                        }

                        is EditElement.Drawing -> DrawingLayer(element, sizeUnitPx)
                    }
                }
            }
        }
    }
}

/**
 * Lays the content out at exactly [size] pixels at [offset] in the parent —
 * no dp rounding, so the box matches [ImageGeometry]'s rect.
 */
private fun Modifier.exactBox(size: IntSize, offset: IntOffset): Modifier =
    this
        .offset { offset }
        .layout { measurable, _ ->
            val placeable = measurable.measure(Constraints.fixed(size.width, size.height))
            layout(size.width, size.height) { placeable.place(0, 0) }
        }

/**
 * One freehand stroke, painted at its own z-position among the overlays and
 * clipped to the image, as it will be in the exported bitmap.
 */
@Composable
private fun DrawingLayer(element: EditElement.Drawing, sizeUnitPx: Float) {
    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
        if (element.points.isEmpty()) return@Canvas
        // Same smoothing helper as the exporter; ratios scale to this box.
        val path = buildSmoothDrawingPath(element.points) { p ->
            Offset(p.x * size.width, p.y * size.height)
        }
        drawPath(
            path = path.asComposePath(),
            color = element.color,
            style = Stroke(
                width = element.strokeWidthRatio * sizeUnitPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
