package com.muzamil.photoeditor.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.muzamil.photoeditor.domain.EditorColors

/**
 * Default values for the customizable pieces of the built-in editor UI:
 * the emoji list, the drawing swatch palette, and color-set factories.
 *
 * Note the split between *chrome* and *content*: [PhotoEditorColors] themes
 * the editor's own chrome (panels, buttons, borders), while [swatchColors] is
 * the palette users draw, write, and outline shapes with. The two are
 * independent — restyling the chrome never changes what colors users can pick.
 */
object PhotoEditorDefaults {

    /**
     * Curated emoji set for the built-in Emoji panel: universal staples plus
     * regionally relevant entries. Everything is Unicode ≤ 13.0 (🫖 is the
     * single 13.0 entry; the rest are ≤ 11.0), so glyphs render on older
     * Android system emoji fonts without tofu on API 24+.
     *
     * Any string works as an emoji — `PhotoEditorState.addEmoji` renders
     * whatever it is given at sticker size, so short text works as a
     * text-sticker shortcut. Pass your own list to `PhotoEditor(emojis = ...)`
     * or `EmojiToolPanel(emojis = ...)`.
     */
    val emojis: List<String> = listOf(
        // Faces
        "😀", "😂", "😍", "😎", "🤩", "🥰", "🥳", "😢", "😡", "😱", "🤔", "😴",
        // Hearts & energy
        "❤️", "💔", "💯", "🔥", "✨", "⭐", "🎉", "🎊", "💡", "✅", "❌", "❓",
        // Hands
        "👍", "👎", "👏", "🙏", "💪", "✌️", "🤝", "👌",
        // Regional
        "🌙", "🕌", "🕋", "📿", "🐪", "🏜️", "☕", "🫖", "🌹", "🌴", "🦅", "🐎",
        // Nature, food, celebration
        "🌞", "🌈", "⚡", "❄️", "🌊", "🍉", "🍇", "🍕", "🎂", "🌸", "🎁", "🎈",
    )

    /**
     * The shared content palette every panel with a color picker (Shape, Draw,
     * Text) uses by default, so a color picked in one tool is available in the
     * others. Re-exports core's [EditorColors.Palette] — Mode A and Mode B
     * default to the same colors. Any list works; swatch contrast is computed
     * per color, not enumerated.
     */
    val swatchColors: List<Color> = EditorColors.Palette

    /** The editor-native light color set, same as [lightPhotoEditorColors]. */
    fun lightColors(): PhotoEditorColors = lightPhotoEditorColors()

    /** The editor-native dark color set, same as [darkPhotoEditorColors]. */
    fun darkColors(): PhotoEditorColors = darkPhotoEditorColors()

    /**
     * Opt-in bridge from the host app's Material theme: maps a [ColorScheme]
     * onto editor tokens so the editor follows the app's brand or dynamic
     * color. The library never reads `MaterialTheme` implicitly — use this
     * only when you want the editor to match the app:
     * ```
     * PhotoEditor(colors = PhotoEditorDefaults.colorsFromMaterialTheme())
     * ```
     */
    @Composable
    fun colorsFromMaterialTheme(
        scheme: ColorScheme = MaterialTheme.colorScheme,
    ): PhotoEditorColors = PhotoEditorColors(
        surface = scheme.surfaceContainer,
        surfaceVariant = scheme.surfaceContainerHigh,
        onSurface = scheme.onSurface,
        onSurfaceVariant = scheme.onSurfaceVariant,
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        accentContainer = scheme.primaryContainer,
        onAccentContainer = scheme.onPrimaryContainer,
        outline = scheme.outlineVariant,
    )

    /** Glyph size for emoji grid cells; lives here so all sp values stay in theme files. */
    internal val emojiGlyphSize: TextUnit = 24.sp
}
