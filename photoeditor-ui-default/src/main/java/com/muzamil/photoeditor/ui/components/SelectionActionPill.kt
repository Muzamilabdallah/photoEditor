package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.ui.icons.EditorIcons
import com.muzamil.photoeditor.ui.theme.LocalPhotoEditorColors
import com.muzamil.photoeditor.ui.theme.PhotoEditorTheme
import com.muzamil.photoeditor.ui.theme.darkPhotoEditorColors
import com.muzamil.photoeditor.ui.theme.lightPhotoEditorColors
import kotlin.math.roundToInt

/**
 * Places the [SelectionActionPill] anchored to the selected element's
 * on-canvas [bounds]: horizontally centered just above the box, flipping to
 * just below it when the box is too close to the top edge, and clamped inside
 * the canvas so it never renders off-screen.
 *
 * Fill the same box the canvas fills so [bounds] (canvas-container pixels)
 * and placement coordinates agree. That box already sits between the top bar
 * and the bottom toolbar, both of which consume their own vertical window
 * insets — so only the horizontal insets (display cutouts in landscape) are
 * still this layout's problem. Placement math is in absolute coordinates, so
 * anchoring is correct in RTL; the pill's own content mirrors naturally.
 */
@Composable
internal fun SelectionActionsOverlay(
    bounds: Rect,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
) {
    val insets = WindowInsets.safeDrawing
    Layout(
        content = {
            SelectionActionPill(
                onDuplicate = onDuplicate,
                onDelete = onDelete,
                onEdit = onEdit,
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val pill = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val gap = AnchorGap.roundToPx()
            val minY = gap
            val maxY = constraints.maxHeight - gap - pill.height
            val above = (bounds.top - gap - pill.height).roundToInt()
            val y = (if (above >= minY) above else (bounds.bottom + gap).roundToInt())
                .coerceIn(minY, maxOf(minY, maxY))

            val minX = insets.getLeft(this@Layout, layoutDirection) + gap
            val maxX = constraints.maxWidth - insets.getRight(this@Layout, layoutDirection) -
                    gap - pill.width
            val x = (bounds.center.x - pill.width / 2f).roundToInt()
                .coerceIn(minX, maxOf(minX, maxX))

            // place (not placeRelative): x/y come from absolute element
            // bounds, so they must not be mirrored again in RTL.
            pill.place(x, y)
        }
    }
}

/**
 * Compact contextual actions for the selected element: duplicate, optional
 * type-specific edit, delete. Neutral [Icon] tints throughout — destructive
 * intent is clear from the trash glyph, and a red accent would fight the
 * minimal look.
 */
@Composable
internal fun SelectionActionPill(
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
) {
    val colors = LocalPhotoEditorColors.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .shadow(elevation = 3.dp, shape = shape)
            .background(colors.surfaceVariant, shape)
            .border(0.5.dp, colors.outline, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillAction(
            icon = EditorIcons.Duplicate,
            contentDescription = "Duplicate element",
            onClick = onDuplicate,
        )
        if (onEdit != null) {
            PillAction(
                icon = Icons.Default.Edit,
                contentDescription = "Edit element",
                onClick = onEdit,
            )
        }
        PillAction(
            icon = Icons.Default.Delete,
            contentDescription = "Delete element",
            onClick = onDelete,
        )
    }
}

@Composable
private fun PillAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = LocalPhotoEditorColors.current.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

private val AnchorGap = 8.dp

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Pill, light", showBackground = true)
@Composable
private fun SelectionActionPillLightPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        SelectionActionPill(onDuplicate = {}, onDelete = {}, onEdit = {})
    }
}

@Preview(name = "Pill, dark, no edit", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SelectionActionPillDarkPreview() {
    PhotoEditorTheme(colors = darkPhotoEditorColors()) {
        SelectionActionPill(onDuplicate = {}, onDelete = {})
    }
}

@Preview(name = "Pill, RTL", showBackground = true)
@Composable
private fun SelectionActionPillRtlPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            SelectionActionPill(onDuplicate = {}, onDelete = {}, onEdit = {})
        }
    }
}
