package com.muzamil.photoeditor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.domain.EditElement

// BasicText (not material3.Text): core must stay free of material3 so Mode A
// consumers don't inherit it.

// Selection chrome (border + corner handles) is drawn by DraggableElement, so
// all element types share one selection language; content only adds a faint
// legibility backdrop while selected.

@Composable
internal fun TextElementContent(
    element: EditElement.Text,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = element.text,
        style = TextStyle(
            color = element.color,
            fontSize = element.fontSize,
            fontWeight = element.fontWeight,
        ),
        modifier = modifier
            .background(
                if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    )
}

@Composable
internal fun EmojiElementContent(
    element: EditElement.Emoji,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = element.emoji,
        style = TextStyle(fontSize = element.size),
        modifier = modifier
            .background(
                if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    )
}
