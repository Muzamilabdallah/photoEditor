package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
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
    onEmojiClick: () -> Unit,
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
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
                icon = Icons.Default.Face,
                label = "Emoji",
                isSelected = false,
                onClick = onEmojiClick
            )
            ToolButton(
                icon = Icons.Default.Create,
                label = "Draw",
                isSelected = currentTool == EditorTool.Drawing,
                onClick = { onToolSelected(EditorTool.Drawing) }
            )
            ShapeKind.values().forEach { kind ->
                ShapeToolButton(
                    kind = kind,
                    label = kind.label(),
                    isSelected = currentTool == EditorTool.Shape(kind),
                    onClick = { onToolSelected(EditorTool.Shape(kind)) }
                )
            }
        }
    }
}

private fun ShapeKind.label(): String = when (this) {
    ShapeKind.Rectangle -> "Rect"
    ShapeKind.Circle -> "Circle"
    ShapeKind.Line -> "Line"
    ShapeKind.Triangle -> "Tri"
    ShapeKind.Star -> "Star"
    ShapeKind.Arrow -> "Arrow"
    ShapeKind.Heart -> "Heart"
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
        modifier = Modifier.width(72.dp)
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
        modifier = Modifier.width(72.dp)
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
                    ShapeKind.Triangle -> drawPath(
                        path = trianglePath(w, h),
                        color = tint,
                        style = stroke
                    )
                    ShapeKind.Star -> drawPath(
                        path = starPath(w, h),
                        color = tint,
                        style = stroke
                    )
                    ShapeKind.Arrow -> drawPath(
                        path = arrowPath(w, h),
                        color = tint,
                        style = stroke
                    )
                    ShapeKind.Heart -> drawPath(
                        path = heartPath(w, h),
                        color = tint,
                        style = stroke
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
