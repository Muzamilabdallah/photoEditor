package io.github.photoforge.canvas

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import io.github.photoforge.domain.EditElement
import kotlin.math.*

@Composable
internal fun DraggableElement(
    element: EditElement,
    isSelected: Boolean,
    parentWidth: Int,
    parentHeight: Int,
    selectionColor: Color,
    onUpdate: (offsetRatioX: Float, offsetRatioY: Float, rotation: Float, scale: Float) -> Unit,
    onTap: () -> Unit,
    onSelectedBoundsChanged: (Rect) -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * False while the Draw tool is active: the element takes no pointer input
     * at all, so a stroke can start on top of it instead of dragging it.
     */
    interactive: Boolean = true,
    content: @Composable (isSelected: Boolean) -> Unit
) {
    var childWidth by remember(element.id) { mutableIntStateOf(0) }
    var childHeight by remember(element.id) { mutableIntStateOf(0) }

    // offsetRatioX/Y is the element's **centre** as a fraction of the parent,
    // which EditorCanvas lays out at exactly the image's on-screen rect, so it
    // is also the fraction of the bitmap the exporter uses.
    val transformableState = rememberTransformableState { scaleChange, panChange, rotationChange ->
        val newRotation = element.rotation + rotationChange

        val angle = newRotation * PI.toFloat() / 180f
        val cos = cos(angle)
        val sin = sin(angle)

        val rotatedPanX = panChange.x * cos - panChange.y * sin
        val rotatedPanY = panChange.x * sin + panChange.y * cos

        val newScale = (element.scale * scaleChange).coerceIn(0.5f, 5f)

        val scaledWidth = childWidth * newScale
        val scaledHeight = childHeight * newScale

        val visualHalfWidth = (abs(scaledWidth * cos) + abs(scaledHeight * sin)) / 2f
        val visualHalfHeight = (abs(scaledWidth * sin) + abs(scaledHeight * cos)) / 2f

        // Keep the element inside the image by its rotated half-extent.
        val minX = visualHalfWidth
        val maxX = parentWidth - visualHalfWidth
        val minY = visualHalfHeight
        val maxY = parentHeight - visualHalfHeight

        val newCenter = Offset(
            x = (element.offsetRatioX * parentWidth + element.scale * rotatedPanX).coerceIn(
                minimumValue = minOf(minX, maxX),
                maximumValue = maxOf(minX, maxX)
            ),
            y = (element.offsetRatioY * parentHeight + element.scale * rotatedPanY).coerceIn(
                minimumValue = minOf(minY, maxY),
                maximumValue = maxOf(minY, maxY)
            ),
        )

        onUpdate(
            newCenter.x / parentWidth,
            newCenter.y / parentHeight,
            newRotation,
            newScale
        )
    }

    // Report drag/rotate/scale so the surrounding UI can fade its chrome while
    // the finger is down. Only the element under the pointer transforms, so the
    // last writer here is always the active one.
    LaunchedEffect(transformableState.isTransformInProgress) {
        onInteractionChanged(transformableState.isTransformInProgress)
    }

    // Publish the selected element's axis-aligned screen bounds (the visual
    // extent of the scaled, rotated box) so UI outside the canvas — e.g. a
    // contextual action toolbar — can anchor to the element.
    if (isSelected) {
        val bounds = run {
            val angle = element.rotation * PI.toFloat() / 180f
            val cos = cos(angle)
            val sin = sin(angle)
            val scaledWidth = childWidth * element.scale
            val scaledHeight = childHeight * element.scale
            val halfW = (abs(scaledWidth * cos) + abs(scaledHeight * sin)) / 2f
            val halfH = (abs(scaledWidth * sin) + abs(scaledHeight * cos)) / 2f
            val centerX = element.offsetRatioX * parentWidth
            val centerY = element.offsetRatioY * parentHeight
            Rect(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH)
        }
        LaunchedEffect(bounds) {
            if (childWidth > 0 && childHeight > 0) onSelectedBoundsChanged(bounds)
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged {
                childWidth = it.width
                childHeight = it.height
            }
            .graphicsLayer {
                // Translate so the element's center lands on (offsetRatioX, offsetRatioY).
                translationX = element.offsetRatioX * parentWidth - childWidth / 2f
                translationY = element.offsetRatioY * parentHeight - childHeight / 2f
                rotationZ = element.rotation
                scaleX = element.scale
                scaleY = element.scale
            }
            .then(
                if (interactive) {
                    Modifier
                        .transformable(transformableState)
                        .pointerInput(element.id) {
                            detectTapGestures { onTap() }
                        }
                } else {
                    // No pointer input → touches fall through to the canvas's
                    // gesture layer underneath.
                    Modifier
                }
            )
            .drawWithContent {
                drawContent()
                if (isSelected) {
                    // The layer above scales this box's drawing, so divide by
                    // the element scale to keep the chrome visually constant.
                    val stroke = SelectionStrokeWidth.toPx() / element.scale
                    val handleRadius = SelectionHandleRadius.toPx() / element.scale
                    drawRect(
                        color = selectionColor,
                        style = Stroke(width = stroke),
                    )
                    val corners = listOf(
                        Offset(0f, 0f),
                        Offset(size.width, 0f),
                        Offset(0f, size.height),
                        Offset(size.width, size.height),
                    )
                    corners.forEach { corner ->
                        drawCircle(color = Color.White, radius = handleRadius, center = corner)
                        drawCircle(
                            color = selectionColor,
                            radius = handleRadius,
                            center = corner,
                            style = Stroke(width = stroke),
                        )
                    }
                }
            }
    ) {
        content(isSelected)
    }
}

private val SelectionStrokeWidth = 1.5.dp
private val SelectionHandleRadius = 4.dp
