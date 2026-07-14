package com.muzamil.photoeditor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Design tokens for the default editor UI. Every color the built-in panels,
 * toolbar, and chrome draw comes from here — override a few tokens via
 * [PhotoEditorTheme] to restyle the whole UI without forking it (see
 * THEMING.md).
 */
@Immutable
data class PhotoEditorColors(
    /** Panel background. */
    val surface: Color,
    /** Unselected chip background. */
    val surfaceVariant: Color,
    /** Primary text/icons. */
    val onSurface: Color,
    /** Secondary text/icons. */
    val onSurfaceVariant: Color,
    /** Selection tint, active states, Done button. */
    val accent: Color,
    /** Text/icon on accent. */
    val onAccent: Color,
    /** Light accent tint for selected chip backgrounds. */
    val accentContainer: Color,
    val onAccentContainer: Color,
    /** Hairline borders. */
    val outline: Color,
)

@Immutable
data class PhotoEditorShapes(
    val panel: CornerBasedShape = RoundedCornerShape(16.dp),
    val chip: CornerBasedShape = RoundedCornerShape(12.dp),
    val control: CornerBasedShape = RoundedCornerShape(8.dp),
)

fun lightPhotoEditorColors(): PhotoEditorColors = PhotoEditorColors(
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F1F4),
    onSurface = Color(0xFF1A1B1E),
    onSurfaceVariant = Color(0xFF6A6C71),
    accent = Color(0xFF2563EB),
    onAccent = Color(0xFFFFFFFF),
    accentContainer = Color(0xFFDBEAFE),
    onAccentContainer = Color(0xFF1E40AF),
    outline = Color(0xFFE2E3E7),
)

fun darkPhotoEditorColors(): PhotoEditorColors = PhotoEditorColors(
    surface = Color(0xFF1C1D21),
    surfaceVariant = Color(0xFF2A2C31),
    onSurface = Color(0xFFE9EAEE),
    onSurfaceVariant = Color(0xFF9EA1A8),
    accent = Color(0xFF60A5FA),
    onAccent = Color(0xFF0E1726),
    accentContainer = Color(0xFF1E3A5F),
    onAccentContainer = Color(0xFFBFDBFE),
    outline = Color(0xFF3A3C42),
)

val LocalPhotoEditorColors = staticCompositionLocalOf { lightPhotoEditorColors() }

val LocalPhotoEditorShapes = staticCompositionLocalOf { PhotoEditorShapes() }

/**
 * Themes the default editor UI. All built-in components read [colors] and
 * [shapes] through composition locals; a Material 3 color scheme is derived
 * from the same tokens internally so the few M3 components used (dialogs,
 * sliders, text fields) match — consumers never need to touch Material.
 */
@Composable
fun PhotoEditorTheme(
    colors: PhotoEditorColors = if (isSystemInDarkTheme()) darkPhotoEditorColors()
    else lightPhotoEditorColors(),
    shapes: PhotoEditorShapes = PhotoEditorShapes(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPhotoEditorColors provides colors,
        LocalPhotoEditorShapes provides shapes,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = Typography,
            content = content,
        )
    }
}

private fun PhotoEditorColors.toMaterialColorScheme() =
    (if (surface.luminance() > 0.5f) lightColorScheme() else darkColorScheme()).copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outline,
    )
