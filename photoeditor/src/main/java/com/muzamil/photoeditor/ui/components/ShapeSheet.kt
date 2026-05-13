package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.domain.ShapeKind

@Composable
internal fun ShapeSheet(
    currentKind: ShapeKind?,
    color: Color,
    strokeWidth: Float,
    filled: Boolean,
    onPickShape: (ShapeKind) -> Unit,
    onColor: (Color) -> Unit,
    onStroke: (Float) -> Unit,
    onFilled: (Boolean) -> Unit,
    onDone: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
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
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDone) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                ShapeGrid(currentKind = currentKind, onPickShape = onPickShape)
                ColorRow(selected = color, onSelect = onColor)
                LabeledSlider(
                    label = "Stroke",
                    value = strokeWidth,
                    range = 1f..50f,
                    onChange = onStroke
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
                    Switch(checked = filled, onCheckedChange = onFilled)
                }
            }
        }
    }
}

@Composable
private fun ShapeGrid(
    currentKind: ShapeKind?,
    onPickShape: (ShapeKind) -> Unit
) {
    val cells: List<ShapeKind?> = listOf(
        ShapeKind.Rectangle,
        ShapeKind.Circle,
        ShapeKind.Line,
        ShapeKind.Triangle,
        ShapeKind.Star,
        ShapeKind.Heart,
        ShapeKind.Arrow,
        null
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(4).forEach { rowCells ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowCells.forEach { kind ->
                    if (kind == null) {
                        Spacer(modifier = Modifier.width(72.dp))
                    } else {
                        ShapeToolButton(
                            kind = kind,
                            label = kind.label(),
                            isSelected = kind == currentKind,
                            onClick = { onPickShape(kind) }
                        )
                    }
                }
            }
        }
    }
}
