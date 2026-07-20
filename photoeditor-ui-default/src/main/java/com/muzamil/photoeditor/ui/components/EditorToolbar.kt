package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorColors
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorShapes
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorTypography

/**
 * The bottom tool nav: four equal-width tabs sharing the panels' selection
 * language — accentContainer pill + accent tint when active.
 */
@Composable
internal fun EditorToolbar(
    currentTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    onEmojiClick: () -> Unit,
    onShapeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPhotoEditorColors.current
    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(thickness = 0.5.dp, color = colors.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolTab(
                icon = Icons.Default.Edit,
                label = "Text",
                isSelected = currentTool == EditorTool.Text,
                onClick = { onToolSelected(EditorTool.Text) },
                modifier = Modifier.weight(1f),
            )
            ToolTab(
                icon = Icons.Default.Face,
                label = "Emoji",
                isSelected = false,
                onClick = onEmojiClick,
                modifier = Modifier.weight(1f),
            )
            ToolTab(
                icon = Icons.Default.Create,
                label = "Draw",
                isSelected = currentTool == EditorTool.Drawing,
                onClick = { onToolSelected(EditorTool.Drawing) },
                modifier = Modifier.weight(1f),
            )
            ToolTab(
                icon = Icons.Default.Star,
                label = "Shape",
                isSelected = currentTool == EditorTool.Shape,
                onClick = onShapeClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToolTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPhotoEditorColors.current
    val shapes = LocalPhotoEditorShapes.current
    val tint = if (isSelected) colors.accent else colors.onSurfaceVariant

    Column(
        modifier = modifier
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 26.dp)
                .background(
                    color = if (isSelected) colors.accentContainer else Color.Transparent,
                    shape = shapes.chip,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = LocalPhotoEditorTypography.current.chipLabel,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = tint,
        )
    }
}
