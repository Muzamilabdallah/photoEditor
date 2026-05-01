package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.domain.ShapeKind

@Composable
internal fun EditorToolbar(
    currentTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolButton(
                icon = Icons.Default.Check,
                label = "Select",
                isSelected = currentTool == EditorTool.None,
                onClick = { onToolSelected(EditorTool.None) }
            )
            ToolButton(
                icon = Icons.Default.Edit,
                label = "Text",
                isSelected = currentTool == EditorTool.Text,
                onClick = { onToolSelected(EditorTool.Text) }
            )
            ToolButton(
                icon = Icons.Default.Create,
                label = "Draw",
                isSelected = currentTool == EditorTool.Drawing,
                onClick = { onToolSelected(EditorTool.Drawing) }
            )
            ShapeToolButton(
                kind = ShapeKind.Rectangle,
                label = "Rect",
                isSelected = currentTool == EditorTool.Shape(ShapeKind.Rectangle),
                onClick = { onToolSelected(EditorTool.Shape(ShapeKind.Rectangle)) }
            )
            ShapeToolButton(
                kind = ShapeKind.Circle,
                label = "Circle",
                isSelected = currentTool == EditorTool.Shape(ShapeKind.Circle),
                onClick = { onToolSelected(EditorTool.Shape(ShapeKind.Circle)) }
            )
            ShapeToolButton(
                kind = ShapeKind.Line,
                label = "Line",
                isSelected = currentTool == EditorTool.Shape(ShapeKind.Line),
                onClick = { onToolSelected(EditorTool.Shape(ShapeKind.Line)) }
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ShapeToolButton(
    kind: ShapeKind,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = 3f, cap = StrokeCap.Round)
                when (kind) {
                    ShapeKind.Rectangle -> drawRect(
                        color = tint,
                        topLeft = Offset.Zero,
                        size = Size(w, h),
                        style = stroke
                    )
                    ShapeKind.Circle -> drawOval(
                        color = tint,
                        topLeft = Offset.Zero,
                        size = Size(w, h),
                        style = stroke
                    )
                    ShapeKind.Line -> drawLine(
                        color = tint,
                        start = Offset(0f, h / 2f),
                        end = Offset(w, h / 2f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}
