package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val EditorPalette = listOf(
    Color.Black,
    Color.White,
    Color.Red,
    Color(0xFFFF9800), // orange
    Color.Yellow,
    Color(0xFF4CAF50), // green
    Color.Blue,
    Color(0xFF673AB7), // purple
)

internal sealed class StyleContext {
    data class TextStyle(
        val color: Color,
        val fontSize: TextUnit,
        val onColor: (Color) -> Unit,
        val onFontSize: (TextUnit) -> Unit
    ) : StyleContext()

    data class EmojiStyle(
        val size: TextUnit,
        val onSize: (TextUnit) -> Unit
    ) : StyleContext()

    data class DrawingStyle(
        val color: Color,
        val strokeWidth: Float,
        val onColor: (Color) -> Unit,
        val onStroke: (Float) -> Unit
    ) : StyleContext()

    data class ShapeStyle(
        val color: Color,
        val strokeWidth: Float,
        val filled: Boolean,
        val onColor: (Color) -> Unit,
        val onStroke: (Float) -> Unit,
        val onFilled: (Boolean) -> Unit
    ) : StyleContext()
}

@Composable
internal fun EditorStylePanel(
    context: StyleContext,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                when (context) {
                    is StyleContext.TextStyle -> {
                        ColorRow(selected = context.color, onSelect = context.onColor)
                        LabeledSlider(
                            label = "Size",
                            value = context.fontSize.value,
                            range = 12f..96f,
                            onChange = { context.onFontSize(it.sp) }
                        )
                    }
                    is StyleContext.EmojiStyle -> {
                        LabeledSlider(
                            label = "Size",
                            value = context.size.value,
                            range = 24f..160f,
                            onChange = { context.onSize(it.sp) }
                        )
                    }
                    is StyleContext.DrawingStyle -> {
                        ColorRow(selected = context.color, onSelect = context.onColor)
                        LabeledSlider(
                            label = "Stroke",
                            value = context.strokeWidth,
                            range = 1f..50f,
                            onChange = context.onStroke
                        )
                    }
                    is StyleContext.ShapeStyle -> {
                        ColorRow(selected = context.color, onSelect = context.onColor)
                        LabeledSlider(
                            label = "Stroke",
                            value = context.strokeWidth,
                            range = 1f..50f,
                            onChange = context.onStroke
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Filled",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(checked = context.filled, onCheckedChange = context.onFilled)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ColorRow(
    selected: Color,
    onSelect: (Color) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorPalette.forEach { color ->
            ColorSwatch(
                color = color,
                isSelected = color == selected,
                onClick = { onSelect(color) }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (isSelected) 32.dp else 28.dp)
            .background(color, CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
internal fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 56.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toInt().toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .widthIn(min = 28.dp)
                .padding(PaddingValues(start = 4.dp))
        )
    }
}
