package io.github.photoforge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.photoforge.domain.EditorHistoryState
import io.github.photoforge.ui.icons.EditorIcons
import io.github.photoforge.ui.theme.LocalPhotoEditorColors
import io.github.photoforge.ui.theme.PhotoEditorTheme
import io.github.photoforge.ui.theme.darkPhotoEditorColors
import io.github.photoforge.ui.theme.lightPhotoEditorColors

/**
 * The editor's top bar: a confirm (apply-and-save) button at the start and a
 * utility cluster — add image, undo, redo, close — at the end. Stateless and
 * container-free (a plain row over a hairline divider); the caller places it.
 *
 * Confirm is the single accent-filled element so it visually dominates; the
 * utility buttons stay neutral. Undo/redo enablement follows [historyState]
 * (see `PhotoEditorState.historyState`). [onAddImage] and [onClose] follow the
 * library's convention that a null callback hides its affordance. The bar
 * mirrors correctly in RTL layouts.
 *
 * In edge-to-edge windows the bar consumes [windowInsets] (status bar and any
 * display cutout by default) itself, M3-`TopAppBar` style: the background
 * bleeds behind the status bar while the controls pad below it. Pass
 * `WindowInsets(0)` if an ancestor already applies these insets.
 */
@Composable
fun EditorTopBar(
    historyState: EditorHistoryState,
    onConfirm: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
    onAddImage: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    showConfirm: Boolean = true,
    windowInsets: WindowInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
) {
    val colors = LocalPhotoEditorColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .windowInsetsPadding(windowInsets)
                .height(EditorTopBarHeight)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showConfirm) {
                ConfirmButton(onClick = onConfirm)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (onAddImage != null) {
                UtilityButton(
                    icon = EditorIcons.AddPhoto,
                    contentDescription = "Add image",
                    onClick = onAddImage,
                )
            }
            UtilityButton(
                icon = EditorIcons.Undo,
                contentDescription = "Undo",
                onClick = onUndo,
                enabled = historyState.canUndo,
            )
            UtilityButton(
                icon = EditorIcons.Redo,
                contentDescription = "Redo",
                onClick = onRedo,
                enabled = historyState.canRedo,
            )
            if (onClose != null) {
                UtilityButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close editor",
                    onClick = onClose,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.outline)
    }
}

/** Content height of [EditorTopBar], excluding any consumed window insets. */
internal val EditorTopBarHeight = 56.dp

@Composable
private fun ConfirmButton(onClick: () -> Unit) {
    val colors = LocalPhotoEditorColors.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(colors.accent, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Apply and save",
            tint = colors.onAccent,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun UtilityButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = LocalPhotoEditorColors.current
    val tint by animateColorAsState(
        targetValue = if (enabled) colors.onSurface
        else colors.onSurfaceVariant.copy(alpha = 0.38f),
        animationSpec = tween(150),
        label = "utilityButtonTint",
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(colors.surfaceVariant, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Light, history enabled", showBackground = true)
@Composable
private fun EditorTopBarLightPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        EditorTopBar(
            historyState = EditorHistoryState(canUndo = true, canRedo = true),
            onConfirm = {}, onUndo = {}, onRedo = {},
            onAddImage = {}, onClose = {},
        )
    }
}

@Preview(name = "Dark, history enabled", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun EditorTopBarDarkPreview() {
    PhotoEditorTheme(colors = darkPhotoEditorColors()) {
        EditorTopBar(
            historyState = EditorHistoryState(canUndo = true, canRedo = true),
            onConfirm = {}, onUndo = {}, onRedo = {},
            onAddImage = {}, onClose = {},
        )
    }
}

@Preview(name = "History disabled, no image", showBackground = true)
@Composable
private fun EditorTopBarDisabledPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        EditorTopBar(
            historyState = EditorHistoryState(),
            onConfirm = {}, onUndo = {}, onRedo = {},
            onAddImage = {}, onClose = {},
            showConfirm = false,
        )
    }
}

@Preview(name = "RTL", showBackground = true)
@Composable
private fun EditorTopBarRtlPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            EditorTopBar(
                historyState = EditorHistoryState(canUndo = true, canRedo = false),
                onConfirm = {}, onUndo = {}, onRedo = {},
                onAddImage = {}, onClose = {},
            )
        }
    }
}
