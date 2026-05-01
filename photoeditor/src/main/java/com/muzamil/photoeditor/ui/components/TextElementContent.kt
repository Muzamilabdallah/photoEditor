package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muzamil.photoeditor.domain.EditElement

@Composable
internal fun TextElementContent(
    element: EditElement.Text,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = element.text,
        color = element.color,
        fontSize = element.fontSize,
        fontWeight = element.fontWeight,
        modifier = modifier
            .background(
                if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color.Blue else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
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
    Text(
        text = element.emoji,
        fontSize = element.size,
        modifier = modifier
            .background(
                if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color.Blue else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    )
}