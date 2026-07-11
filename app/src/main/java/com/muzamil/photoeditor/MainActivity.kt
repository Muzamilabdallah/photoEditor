package com.muzamil.photoeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.muzamil.photoeditor.ui.PhotoEditor

/**
 * Demo consumer of the :photoeditor library. [PhotoEditor] owns its ViewModel,
 * theme, photo picking, and bitmap decoding — this is the entire integration.
 *
 * To drive the editor yourself (custom picker, observing state), hoist the
 * ViewModel: `PhotoEditor(viewModel = myViewModel, onPickImage = { ... })`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoEditor()
        }
    }
}
