package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.domain.EditorColors
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorColors

/**
 * The one selection language for colors, shared by every tool panel:
 * 34.dp swatches; the selected one gets a double ring (2.dp panel-surface gap
 * inside a 2.dp accent ring) plus a checkmark — a ring alone disappears when
 * the swatch color is close to the ring color.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorSwatchRow(
    selected: Color,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = EditorColors.Palette,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) {
        colors.forEach { color ->
            ColorSwatch(
                color = color,
                isSelected = color == selected,
                onClick = { onSelect(color) },
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalPhotoEditorColors.current
    val name = color.editorColorName()

    // Selection handling sits on the outer, minimum-44dp box; the 34.dp
    // swatch inside stays visually compact.
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .semantics { contentDescription = "Color $name" }
            .selectable(selected = isSelected, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .border(2.dp, tokens.accent, CircleShape)
                    .padding(2.dp)
                    .background(tokens.surface, CircleShape)
                    .padding(2.dp)
                    .background(color, CircleShape)
                    .border(0.5.dp, tokens.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    // Dark check on light swatches (white, yellow), white elsewhere.
                    tint = if (color.luminance() > 0.6f) Color(0xFF111111) else Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(color, CircleShape)
                    .border(0.5.dp, tokens.outline, CircleShape),
            )
        }
    }
}

private fun Color.editorColorName(): String = when (this) {
    EditorColors.Black -> "black"
    EditorColors.White -> "white"
    EditorColors.Red -> "red"
    EditorColors.Orange -> "orange"
    EditorColors.Yellow -> "yellow"
    EditorColors.Green -> "green"
    EditorColors.Blue -> "blue"
    EditorColors.Purple -> "purple"
    else -> "custom"
}
