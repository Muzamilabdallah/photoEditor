package io.github.photoforge.export

import android.graphics.Bitmap
import android.os.Build
import java.io.OutputStream

/** The file format an edit is exported in. */
sealed interface ExportFormat {
    val mimeType: String
    val extension: String

    /** JPEG at [quality] (0–100). No transparency. */
    data class Jpeg(val quality: Int = 95) : ExportFormat {
        override val mimeType get() = "image/jpeg"
        override val extension get() = "jpg"
    }

    /** Lossless PNG. */
    data object Png : ExportFormat {
        override val mimeType get() = "image/png"
        override val extension get() = "png"
    }

    /**
     * WebP, lossy at [quality] (0–100) or [lossless]. Before API 30 the
     * platform has a single WebP mode: [quality] 100 gives lossless, anything
     * lower lossy, and [lossless] is ignored.
     */
    data class Webp(val quality: Int = 95, val lossless: Boolean = false) : ExportFormat {
        override val mimeType get() = "image/webp"
        override val extension get() = "webp"
    }
}

/** Compresses [bitmap] into [stream] in this format; false if the platform encoder failed. */
internal fun ExportFormat.compress(bitmap: Bitmap, stream: OutputStream): Boolean = when (this) {
    is ExportFormat.Jpeg -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    ExportFormat.Png -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    is ExportFormat.Webp ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val f = if (lossless) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
            bitmap.compress(f, quality, stream)
        } else {
            @Suppress("DEPRECATION")
            bitmap.compress(Bitmap.CompressFormat.WEBP, quality, stream)
        }
}
