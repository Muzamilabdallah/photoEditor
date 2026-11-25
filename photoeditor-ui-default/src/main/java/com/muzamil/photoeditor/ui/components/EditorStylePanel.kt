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
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorColors
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorTypography
import com.muzamil.photoeditor.ui.theme.PhotoEditorDefaults

/**
 * Slider "levels" for size ratios. Core stores every size as a fraction of the
 * image's shorter side (see `EditElement`); the sliders show that ×1000 as a
 * plain number (text 70 = 7% of the shorter side, stroke 6 = 0.6%), so the
 * same number looks the same on every photo. No unit is shown on purpose.
 */
internal const val LevelsPerRatio = 1000f
internal fun Float.ratioToLevel(): Float = this * LevelsPerRatio
internal fun Float.levelToRatio(): Float = this / LevelsPerRatio

/** All sizes here are ratios of the image's shorter side (see `EditElement`). */
internal sealed class StyleContext {
    data class TextStyle(
        val color: Color,
        val fontSizeRatio: Float,
        val onColor: (Color) -> Unit,
        val onFontSize: (Float) -> Unit
    ) : StyleContext()

    data class EmojiStyle(
        val sizeRatio: Float,
        val onSize: (Float) -> Unit
    ) : StyleContext()

    data class DrawingStyle(
        val color: Color,
        val strokeWidthRatio: Float,
        val onColor: (Color) -> Unit,
        val onStroke: (Float) -> Unit
    ) : StyleContext()

    data class ShapeStyle(
        val color: Color,
        val strokeWidthRatio: Float,
        val filled: Boolean,
        val onColor: (Color) -> Unit,
        val onStroke: (Float) -> Unit,
        val onFilled: (Boolean) -> Unit
    ) : StyleContext()
}

/**
 * Styling controls for the current tool default or the selected element.
 * [onClose] is null when dismissal is handled elsewhere (selected elements
 * deselect by tapping the canvas; their actions live in the selection pill),
 * which hides the header row entirely.
 */
@Composable
internal fun EditorStylePanel(
    context: StyleContext,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    swatchColors: List<Color> = PhotoEditorDefaults.swatchColors,
    // Fired when a slider is released, so a whole drag is one undo step.
    onSliderReleased: () -> Unit = {},
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
            if (onClose != null) {
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
                            contentDescription = "Close style panel",
                            tint = colors.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                        value = context.fontSizeRatio.ratioToLevel(),
                        range = 20f..200f,
                        onChangeFinished = onSliderReleased,
                        onChange = { context.onFontSize(it.levelToRatio()) }
                    )
                }
                is StyleContext.EmojiStyle -> {
                    LabeledSlider(
                        label = "Size",
                        value = context.sizeRatio.ratioToLevel(),
                        range = 40f..400f,
                        onChangeFinished = onSliderReleased,
                        onChange = { context.onSize(it.levelToRatio()) }
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
                        value = context.strokeWidthRatio.ratioToLevel(),
                        range = 1f..50f,
                        onChangeFinished = onSliderReleased,
                        onChange = { context.onStroke(it.levelToRatio()) }
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
                        value = context.strokeWidthRatio.ratioToLevel(),
                        range = 1f..50f,
                        onChangeFinished = onSliderReleased,
                        onChange = { context.onStroke(it.levelToRatio()) }
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
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit = {},
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
            onValueChangeFinished = onChangeFinished,
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
