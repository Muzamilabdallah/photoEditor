package io.github.photoforge.ui.emojitool

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.photoforge.ui.theme.LocalPhotoEditorColors
import io.github.photoforge.ui.theme.LocalPhotoEditorShapes
import io.github.photoforge.ui.theme.LocalPhotoEditorTypography
import io.github.photoforge.ui.theme.PhotoEditorDefaults
import io.github.photoforge.ui.theme.PhotoEditorTheme
import io.github.photoforge.ui.theme.darkPhotoEditorColors
import io.github.photoforge.ui.theme.lightPhotoEditorColors

/**
 * The Emoji tool's bottom panel: a grid of tappable emojis. Stateless —
 * tapping an emoji is the terminal action (the caller typically adds it and
 * closes the panel), so there is no Done button.
 *
 * [emojis] is a plain list of strings rendered with the system emoji font;
 * the default is [PhotoEditorDefaults.emojis]. Any string works — short text
 * renders at sticker size, a supported text-sticker shortcut.
 *
 * Styling comes entirely from [PhotoEditorTheme] tokens, matching the Shape
 * panel's layout language.
 */
@Composable
fun EmojiToolPanel(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    emojis: List<String> = PhotoEditorDefaults.emojis,
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PanelHeader(onDismiss = onDismiss)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colors.surfaceVariant, shapes.control)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emoji,
                        fontSize = PhotoEditorDefaults.emojiGlyphSize,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(onDismiss: () -> Unit) {
    val colors = LocalPhotoEditorColors.current
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close emoji panel",
                tint = colors.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = "Emoji",
            style = LocalPhotoEditorTypography.current.panelTitle,
            color = colors.onSurface,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Light", showBackground = true)
@Composable
private fun EmojiToolPanelLightPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        EmojiToolPanel(onEmojiSelected = {}, onDismiss = {})
    }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun EmojiToolPanelDarkPreview() {
    PhotoEditorTheme(colors = darkPhotoEditorColors()) {
        EmojiToolPanel(onEmojiSelected = {}, onDismiss = {})
    }
}

@Preview(name = "Custom list", showBackground = true)
@Composable
private fun EmojiToolPanelCustomListPreview() {
    PhotoEditorTheme(colors = lightPhotoEditorColors()) {
        EmojiToolPanel(
            onEmojiSelected = {},
            onDismiss = {},
            emojis = listOf("🍽️", "🥗", "🍰", "☕", "⭐", "SALE"),
        )
    }
}
