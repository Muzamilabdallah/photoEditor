package com.muzamil.photoeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Demo consumer of the photoeditor library, showing both installation modes:
 *
 * - **Default UI** ([DefaultUiSample]) — Mode B: `photoeditor-ui-default`'s
 *   `PhotoEditor` composable with the built-in panels and theme.
 * - **Custom UI** ([CustomUiSample]) — Mode A: `photoeditor-core` alone;
 *   `PhotoEditorCanvas` plus a hand-rolled toolbar driving the state holder.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var mode by rememberSaveable { mutableStateOf<String?>(null) }

            BackHandler(enabled = mode != null) { mode = null }

            when (mode) {
                "default" -> DefaultUiSample()
                "custom" -> CustomUiSample()
                else -> ModeChooser(onPick = { mode = it })
            }
        }
    }
}

@Composable
private fun ModeChooser(onPick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101114))
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        BasicText(
            text = "photoeditor samples",
            style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium),
        )
        ModeCard(
            title = "Default UI (core + ui-default)",
            description = "The batteries-included PhotoEditor with built-in panels and theme.",
            onClick = { onPick("default") },
        )
        ModeCard(
            title = "Custom UI (core only)",
            description = "PhotoEditorCanvas with a hand-rolled toolbar driving the core API.",
            onClick = { onPick("custom") },
        )
    }
}

@Composable
private fun ModeCard(title: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B1C20), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF3A3B41), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(
            text = title,
            style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium),
        )
        BasicText(
            text = description,
            style = TextStyle(color = Color(0xFF9EA1A8), fontSize = 13.sp),
        )
    }
}
