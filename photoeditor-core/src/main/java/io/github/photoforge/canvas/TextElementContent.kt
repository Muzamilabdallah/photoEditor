package io.github.photoforge.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.photoforge.domain.EditElement
import io.github.photoforge.domain.TextWrapWidthRatio

// BasicText (not material3.Text): core must stay free of material3 so Mode A
// consumers don't inherit it.

// Selection chrome (border + corner handles) is drawn by DraggableElement, so
// all element types share one selection language; content only adds a faint
// legibility backdrop while selected.

@Composable
internal fun TextElementContent(
    element: EditElement.Text,
    isSelected: Boolean,
    sizeUnitPx: Float,
    imageWidthPx: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Wrap where the exporter wraps: TextWrapWidthRatio of the image width.
    val maxWidth = with(density) { (imageWidthPx * TextWrapWidthRatio).toDp() }
    PixelSizedText(textSizePx = element.fontSizeRatio * sizeUnitPx) { fontSize ->
        BasicText(
            text = element.text,
            style = TextStyle(
                color = element.color,
                fontSize = fontSize,
                fontWeight = element.fontWeight,
                // Must match textLayout() in ImageExporter line for line.
                textAlign = TextAlign.Center,
                lineBreak = LineBreak.Simple,
                hyphens = Hyphens.None,
            ),
            modifier = modifier
                .selectionBackdrop(isSelected)
                .widthIn(max = maxWidth)
        )
    }
}

@Composable
internal fun EmojiElementContent(
    element: EditElement.Emoji,
    isSelected: Boolean,
    sizeUnitPx: Float,
    modifier: Modifier = Modifier
) {
    PixelSizedText(textSizePx = element.sizeRatio * sizeUnitPx) { fontSize ->
        BasicText(
            text = element.emoji,
            style = TextStyle(fontSize = fontSize),
            modifier = modifier.selectionBackdrop(isSelected)
        )
    }
}

/**
 * Runs [content] with a font size that renders at exactly [textSizePx] screen
 * pixels. The text is part of the photo, so it must scale with the image and
 * ignore the system font-size setting: `fontScale` is pinned to 1 so sp → px
 * is a plain multiplication by density (no non-linear font scaling).
 */
@Composable
private fun PixelSizedText(
    textSizePx: Float,
    content: @Composable (fontSize: androidx.compose.ui.unit.TextUnit) -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        content((textSizePx / density.density).sp)
    }
}

private fun Modifier.selectionBackdrop(isSelected: Boolean): Modifier =
    background(
        if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent,
        RoundedCornerShape(4.dp)
    ).padding(8.dp)
