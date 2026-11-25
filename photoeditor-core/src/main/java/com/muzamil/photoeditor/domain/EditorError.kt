package com.muzamil.photoeditor.domain

/**
 * Why an editor operation failed, as a type rather than a message, so apps can
 * react programmatically and show their own (localised) text.
 *
 * Exposed on `EditorState.error` by `PhotoEditorState.saveEditedImage`; the
 * suspend `export…` functions return it inside their failed `Result` as an
 * [EditorException].
 */
sealed interface EditorError {
    /** There is no image loaded to export. */
    data object NoImage : EditorError

    /** Not enough memory to render the full-resolution result. */
    data object OutOfMemory : EditorError

    /** Encoding or writing the file failed; [cause] has the details. */
    data class ExportFailed(val cause: Throwable) : EditorError
}

/** The exception carried by a failed export `Result`; [error] says what went wrong. */
class EditorException(val error: EditorError) :
    Exception(error.toString(), (error as? EditorError.ExportFailed)?.cause)
