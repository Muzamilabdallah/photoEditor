package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorColors
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorTypography
import com.muzamil.photoeditor.ui.theme.PhotoEditorDefaults

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
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    swatchColors: List<Color> = PhotoEditorDefaults.swatchColors,
) {
    val colors = LocalPhotoEditorColors.current
    val typography = LocalPhotoEditorTypography.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.94f))
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = colors.outline)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
            ) {
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete element",
                            tint = colors.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close style panel",
                        tint = colors.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            when (context) {
                is StyleContext.TextStyle -> {
                    ColorSwatchRow(
                        selected = context.color,
                        onSelect = context.onColor,
                        colors = swatchColors,
                    )
                    LabeledSlider(
                        label = "Size",
                        value = context.fontSize.value,
                        range = 12f..96f,
                        onChange = { context.onFontSize(TextUnit(it, TextUnitType.Sp)) }
                    )
                }
                is StyleContext.EmojiStyle -> {
                    LabeledSlider(
                        label = "Size",
                        value = context.size.value,
                        range = 24f..160f,
                        onChange = { context.onSize(TextUnit(it, TextUnitType.Sp)) }
                    )
                }
                is StyleContext.DrawingStyle -> {
                    ColorSwatchRow(
                        selected = context.color,
                        onSelect = context.onColor,
                        colors = swatchColors,
                    )
                    LabeledSlider(
                        label = "Stroke",
                        value = context.strokeWidth,
                        range = 1f..50f,
                        onChange = context.onStroke
                    )
                }
                is StyleContext.ShapeStyle -> {
                    ColorSwatchRow(
                        selected = context.color,
                        onSelect = context.onColor,
                        colors = swatchColors,
                    )
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
                            style = typography.body,
                            color = colors.onSurface
                        )
                        Switch(
                            checked = context.filled,
                            onCheckedChange = context.onFilled,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = colors.accent,
                                checkedThumbColor = colors.onAccent,
                                uncheckedTrackColor = colors.surfaceVariant,
                                uncheckedThumbColor = colors.onSurfaceVariant,
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    val colors = LocalPhotoEditorColors.current
    val typography = LocalPhotoEditorTypography.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = typography.sectionLabel,
            color = colors.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 48.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.surfaceVariant,
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toInt().toString(),
            style = typography.valueLabel,
            color = colors.onSurface,
            modifier = Modifier.widthIn(min = 28.dp)
        )
    }
}
