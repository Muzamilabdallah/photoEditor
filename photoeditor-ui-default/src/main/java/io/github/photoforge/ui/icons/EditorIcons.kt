package io.github.photoforge.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-built icons the default UI needs beyond material-icons-core, so the
 * library does not pull in the material-icons-extended artifact. Path data is
 * adapted from Material Symbols (Apache-2.0), 24dp viewport. Undo/Redo set
 * `autoMirror` so they flip in RTL layouts.
 */
internal object EditorIcons {

    val Undo: ImageVector by lazy {
        materialIcon(name = "Undo", autoMirror = true) {
            // Curved arrow sweeping right-to-left with head at the top-left.
            moveTo(12.5f, 8.0f)
            curveToRelative(-2.65f, 0.0f, -5.05f, 0.99f, -6.9f, 2.6f)
            lineTo(2.0f, 7.0f)
            verticalLineToRelative(9.0f)
            horizontalLineToRelative(9.0f)
            lineToRelative(-3.62f, -3.62f)
            curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
            curveToRelative(3.54f, 0.0f, 6.55f, 2.31f, 7.6f, 5.5f)
            lineToRelative(2.37f, -0.78f)
            curveTo(21.08f, 11.03f, 17.15f, 8.0f, 12.5f, 8.0f)
            close()
        }
    }

    val Redo: ImageVector by lazy {
        materialIcon(name = "Redo", autoMirror = true) {
            // Mirror image of Undo: head at the top-right.
            moveTo(18.4f, 10.6f)
            curveTo(16.55f, 8.99f, 14.15f, 8.0f, 11.5f, 8.0f)
            curveToRelative(-4.65f, 0.0f, -8.58f, 3.03f, -9.96f, 7.22f)
            lineTo(3.9f, 16.0f)
            curveToRelative(1.05f, -3.19f, 4.05f, -5.5f, 7.6f, -5.5f)
            curveToRelative(1.95f, 0.0f, 3.73f, 0.72f, 5.12f, 1.88f)
            lineTo(13.0f, 16.0f)
            horizontalLineToRelative(9.0f)
            verticalLineTo(7.0f)
            lineToRelative(-3.6f, 3.6f)
            close()
        }
    }

    val AddPhoto: ImageVector by lazy {
        materialIcon(name = "AddPhoto") {
            // Photo frame with a "+" spliced into the top-right corner
            // (add_photo_alternate).
            moveTo(19.0f, 7.0f)
            verticalLineToRelative(2.99f)
            horizontalLineToRelative(-2.0f)
            lineTo(17.0f, 7.0f)
            horizontalLineToRelative(-3.0f)
            lineTo(14.0f, 5.0f)
            horizontalLineToRelative(3.0f)
            lineTo(17.0f, 2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-3.0f)
            close()
            moveTo(16.0f, 11.0f)
            verticalLineToRelative(-1.01f)
            lineTo(13.01f, 9.99f)
            lineTo(13.01f, 7.0f)
            lineTo(5.0f, 7.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(10.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(10.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineToRelative(-1.0f)
            close()
            moveTo(5.0f, 19.0f)
            lineToRelative(2.5f, -3.21f)
            lineToRelative(1.79f, 2.15f)
            lineToRelative(2.5f, -3.22f)
            lineTo(15.0f, 19.0f)
            lineTo(5.0f, 19.0f)
            close()
        }
    }

    val Duplicate: ImageVector by lazy {
        materialIcon(name = "Duplicate") {
            // Two offset pages (content_copy).
            moveTo(16.0f, 1.0f)
            horizontalLineTo(4.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(3.0f)
            horizontalLineToRelative(12.0f)
            verticalLineTo(1.0f)
            close()
            moveTo(19.0f, 5.0f)
            horizontalLineTo(8.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(11.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(7.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(19.0f, 21.0f)
            horizontalLineTo(8.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(11.0f)
            verticalLineToRelative(14.0f)
            close()
        }
    }

    val ImagePlaceholder: ImageVector by lazy {
        materialIcon(name = "ImagePlaceholder") {
            // Outlined photo frame with a mountain + sun (image icon).
            moveTo(19.0f, 5.0f)
            verticalLineToRelative(14.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(14.0f)
            close()
            moveTo(19.0f, 3.0f)
            horizontalLineTo(5.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(5.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(14.14f, 11.86f)
            lineToRelative(-3.0f, 3.87f)
            lineTo(9.0f, 13.14f)
            lineTo(6.0f, 17.0f)
            horizontalLineToRelative(12.0f)
            lineToRelative(-3.86f, -5.14f)
            close()
        }
    }

    private inline fun materialIcon(
        name: String,
        autoMirror: Boolean = false,
        pathContent: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = pathContent)
    }.build()
}
