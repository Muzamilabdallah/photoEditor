package com.muzamil.photoeditor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EMOJIS = listOf(
    "😀", "😂", "😍", "🤩", "😎", "🥳", "😭", "😡",
    "😱", "🤔", "😴", "🙄", "🤯", "🥶", "🥵", "🤤",
    "❤️", "💔", "💯", "🔥", "✨", "⭐", "🌟", "💫",
    "👍", "👎", "👏", "🙌", "🙏", "💪", "✌️", "🤘",
    "🎉", "🎊", "🎁", "🎂", "🍕", "🍔", "🍟", "🍩",
    "☕", "🍺", "🍷", "🥤", "🍎", "🍓", "🍌", "🍉",
    "🐶", "🐱", "🐭", "🐰", "🦊", "🐻", "🐼", "🦁",
    "🌍", "🌞", "🌙", "☁️", "🌈", "⚡", "❄️", "🌊",
    "❓", "❗", "✅", "❌", "⚠️", "🚀", "💡", "📌"
)

@Composable
internal fun EmojiPickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick an emoji") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(EMOJIS) { emoji ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onPick(emoji) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
