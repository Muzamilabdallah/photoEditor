package com.muzamil.photoeditor.domain

import androidx.compose.runtime.Immutable

/**
 * Snapshot of undo/redo availability, for driving toolbar enabled states.
 *
 * Read it from `PhotoEditorState.historyState`; both the built-in top bar and
 * custom UIs bind their undo/redo buttons to these flags.
 */
@Immutable
data class EditorHistoryState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)
