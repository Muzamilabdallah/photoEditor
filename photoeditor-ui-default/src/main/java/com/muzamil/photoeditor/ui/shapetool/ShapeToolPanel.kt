package com.muzamil.photoeditor.ui.shapetool

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.canvas.EditorShapePaths
import com.muzamil.photoeditor.domain.EditorColors
import com.muzamil.photoeditor.domain.EditorShape
import com.muzamil.photoeditor.domain.ShapeToolState
import com.muzamil.photoeditor.ui.components.ColorSwatchRow
import com.muzamil.photoeditor.ui.components.levelToRatio
import com.muzamil.photoeditor.ui.components.ratioToLevel
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorColors
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorShapes
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorTypography
import com.muzamil.photoeditor.ui.theme.PhotoEditorDefaults
import com.muzamil.photoeditor.ui.theme.PhotoEditorTheme
import com.muzamil.photoeditor.ui.theme.darkPhotoEditorColors
import com.muzamil.photoeditor.ui.theme.lightPhotoEditorColors
import kotlin.math.roundToInt

/**
 * The Shape tool's bottom panel: shape picker grid, color swatches, stroke
 * slider, and fill toggle. Stateless — [state] is hoisted (typically
 * `PhotoEditorState.editorState.shapeTool`) and every change flows out
 * through a callback.
 *
 * Styling comes entirely from [PhotoEditorTheme] tokens; wrap your screen in
 * `PhotoEditorTheme(colors = …)` to restyle it.
 */
@Composable
fun ShapeToolPanel(
    state: ShapeToolState,
    onShapeSelected: (EditorShape) -> Unit,
    onColorSelected: (Color) -> Unit,
    /** Receives the new outline width as a fraction of the image's shorter side. */
    onStrokeWidthChanged: (Float) -> Unit,
    onFilledChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    swatchColors: List<Color> = PhotoEditorDefaults.swatchColors,
) {
    val colors = LocalPhotoEditorColors.current
    val shapes = LocalPhotoEditorShapes.current
    val panelShape = shapes.panel.copy(
        bottomStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, panelShape)
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PanelHeader(onDone = onDone, onDismiss = onDismiss)
        ShapeGrid(selected = state.selectedShape, onShapeSelected = onShapeSelected)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Color")
            ColorSwatchRow(
                selected = state.selectedColor,
                onSelect = onColorSelected,
                colors = swatchColors,
            )
        }
        StrokeControl(
            strokeWidthRatio = state.strokeWidthRatio,
            onStrokeWidthChanged = onStrokeWidthChanged,
        )
        FilledToggleRow(isFilled = state.isFilled, onFilledChanged = onFilledChanged)
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun PanelHeader(onDone: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalPhotoEditorColors.current
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close shape panel",
                tint = colors.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = "Shape",
            style = LocalPhotoEditorTypography.current.panelTitle,
            color = colors.onSurface,
            modifier = Modifier.align(Alignment.Center),
        )
        // Click handling sits on the outer, minimum-44dp box; the pill inside
        // stays visually compact.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .minimumInteractiveComponentSize()
                .clickable(role = Role.Button, onClick = onDone),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .background(colors.accent, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Done",
                    style = LocalPhotoEditorTypography.current.button,
                    color = colors.onAccent,
                )
            }
        }
    }
}

// ── Shape grid ────────────────────────────────────────────────────────────────

private const val GRID_COLUMNS = 4

/**
 * Fixed 4-column grid; rows are derived from the entry count, so new shapes
 * (polygon, rounded rect, speech bubble, …) slot in without layout changes.
 */
@Composable
private fun ShapeGrid(
    selected: EditorShape,
    onShapeSelected: (EditorShape) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EditorShape.entries.chunked(GRID_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { shape ->
                    ShapeChip(
                        shape = shape,
                        isSelected = shape == selected,
                        onClick = { onShapeSelected(shape) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(GRID_COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ShapeChip(
    shape: EditorShape,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPhotoEditorColors.current
    val shapes = LocalPhotoEditorShapes.current
    val tint = if (isSelected) colors.accent else colors.onSurfaceVariant

    Column(
        modifier = modifier
            .background(
                color = if (isSelected) colors.accentContainer else colors.surfaceVariant,
                shape = shapes.chip,
            )
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) colors.accent else colors.outline,
                shape = shapes.chip,
            )
            .selectable(selected = isSelected, role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            drawPath(
                path = EditorShapePaths.path(shape, size.width, size.height),
                color = tint,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        Text(
            text = shape.label(),
            style = LocalPhotoEditorTypography.current.chipLabel,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = tint,
        )
    }
}

internal fun EditorShape.label(): String = when (this) {
    EditorShape.Rect -> "Rect"
    EditorShape.Circle -> "Circle"
    EditorShape.Line -> "Line"
    EditorShape.Triangle -> "Triangle"
    EditorShape.Star -> "Star"
    EditorShape.Heart -> "Heart"
    EditorShape.Arrow -> "Arrow"
}

// ── Stroke ────────────────────────────────────────────────────────────────────

@Composable
private fun StrokeControl(
    strokeWidthRatio: Float,
    onStrokeWidthChanged: (Float) -> Unit,
) {
    // Shown as a whole-number level (ratio × 1000) — relative to the image,
    // so no unit; see LevelsPerRatio.
    val level = strokeWidthRatio.ratioToLevel()
    val colors = LocalPhotoEditorColors.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Stroke")
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(colors.accentContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${level.roundToInt()}",
                    style = LocalPhotoEditorTypography.current.valueLabel,
                    color = colors.onAccentContainer,
                )
            }
        }
        Slider(
            value = level,
            onValueChange = { onStrokeWidthChanged(it.roundToInt().toFloat().levelToRatio()) },
            valueRange = 1f..40f,
            steps = 38,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.surfaceVariant,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Filled toggle ─────────────────────────────────────────────────────────────

@Composable
private fun FilledToggleRow(
    isFilled: Boolean,
    onFilledChanged: (Boolean) -> Unit,
) {
    val colors = LocalPhotoEditorColors.current
    Column {
        HorizontalDivider(thickness = 0.5.dp, color = colors.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Fill shape",
                    style = LocalPhotoEditorTypography.current.body,
                    color = colors.onSurface,
                )
                Text(
                    text = "Solid instead of outline",
                    style = LocalPhotoEditorTypography.current.bodySecondary,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FillToggle(checked = isFilled, onCheckedChange = onFilledChanged)
        }
    }
}

@Composable
private fun FillToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalPhotoEditorColors.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.surfaceVariant,
        animationSpec = tween(150),
        label = "fillToggleTrack",
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) colors.onAccent else colors.onSurfaceVariant,
        animationSpec = tween(150),
        label = "fillToggleKnob",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 23.dp else 3.dp,
        animationSpec = tween(150),
        label = "fillToggleOffset",
    )

    // Toggle handling sits on the outer, minimum-44dp box; the 48×28 track
    // inside stays visually compact.
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .semantics { contentDescription = "Fill shape" }
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .background(trackColor, RoundedCornerShape(14.dp)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = knobOffset)
                    .size(22.dp)
                    .background(knobColor, CircleShape),
            )
        }
    }
}

// ── Shared ────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = LocalPhotoEditorTypography.current.sectionLabel,
        color = LocalPhotoEditorColors.current.onSurfaceVariant,
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Light", showBackground = true)
@Composable
private fun ShapeToolPanelLightPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        ShapeToolPanel(
            state = ShapeToolState(),
            onShapeSelected = {}, onColorSelected = {}, onStrokeWidthChanged = {},
            onFilledChanged = {}, onDone = {}, onDismiss = {},
        )
    }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ShapeToolPanelDarkPreview() {
    PhotoEditorTheme(colors = darkPhotoEditorColors()) {
        ShapeToolPanel(
            state = ShapeToolState(),
            onShapeSelected = {}, onColorSelected = {}, onStrokeWidthChanged = {},
            onFilledChanged = {}, onDone = {}, onDismiss = {},
        )
    }
}

@Preview(name = "Star + white + filled", showBackground = true)
@Composable
private fun ShapeToolPanelFilledStarPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        ShapeToolPanel(
            state = ShapeToolState(
                selectedShape = EditorShape.Star,
                selectedColor = EditorColors.White,
                strokeWidthRatio = 0.024f,
                isFilled = true,
            ),
            onShapeSelected = {}, onColorSelected = {}, onStrokeWidthChanged = {},
            onFilledChanged = {}, onDone = {}, onDismiss = {},
        )
    }
}

@Preview(name = "Heart + yellow, dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ShapeToolPanelHeartDarkPreview() {
    PhotoEditorTheme(colors = darkPhotoEditorColors()) {
        ShapeToolPanel(
            state = ShapeToolState(
                selectedShape = EditorShape.Heart,
                selectedColor = EditorColors.Yellow,
                strokeWidthRatio = 0.002f,
            ),
            onShapeSelected = {}, onColorSelected = {}, onStrokeWidthChanged = {},
            onFilledChanged = {}, onDone = {}, onDismiss = {},
        )
    }
}
