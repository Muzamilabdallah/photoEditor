package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.muzamil.photoeditor.domain.EditElement
import kotlin.math.*

@Composable
internal fun DraggableElement(
    element: EditElement,
    isSelected: Boolean,
    parentWidth: Int,
    parentHeight: Int,
    onUpdate: (offsetRatioX: Float, offsetRatioY: Float, rotation: Float, scale: Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isSelected: Boolean) -> Unit
) {
    var childWidth by remember(element.id) { mutableIntStateOf(0) }
    var childHeight by remember(element.id) { mutableIntStateOf(0) }

    // offsetRatioX/Y stores the **center** of the element as a fraction of the parent.
    // This matches the bitmap-side export path (see ImageExporter) so preview and saved
    // images agree on where an element is anchored.
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

        // Keep the element's center inside the parent by its rotated half-extent.
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
            .transformable(transformableState)
            .pointerInput(element.id) {
                detectTapGestures { onTap() }
            }
    ) {
        content(isSelected)
    }
}