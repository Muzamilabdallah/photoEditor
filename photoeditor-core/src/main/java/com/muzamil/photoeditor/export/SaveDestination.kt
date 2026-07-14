package com.muzamil.photoeditor.export

/**
 * Where [saveEditedImage] writes the composited image.
 */
sealed interface SaveDestination {

    /**
     * Save publicly to the device gallery (`Pictures/`, via MediaStore on
     * API 29+). [subFolder] optionally nests the image, e.g. `"MyApp"` →
     * `Pictures/MyApp`. The file name is generated automatically.
     *
     * On API 28 and below the public Pictures directory is written directly,
     * which requires the consumer app to hold `WRITE_EXTERNAL_STORAGE` at
     * runtime. The library deliberately declares no permissions — if you
     * support API 24–28 and use this destination, declare it in your app
     * manifest with `maxSdkVersion=28` and request it at runtime (the demo
     * app shows the manifest entry). Not needed on API 29+.
     */
    data class Gallery(val subFolder: String? = null) : SaveDestination

    /**
     * Save to an exact file the caller controls — typically app-private
     * storage such as `File(context.filesDir, "edits/out.jpg")`. Parent
     * directories are created as needed; an existing file is overwritten.
     * No permissions required for app-private locations.
     */
    data class File(val file: java.io.File) : SaveDestination
}
