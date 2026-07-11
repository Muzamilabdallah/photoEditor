# photoEditor

WhatsApp-style photo editor for Android, delivered as a Jetpack Compose library (`:photoeditor`) with a demo app (`:app`). Lets users overlay text, emoji, freehand drawings, and shape primitives (rectangle / circle / line) on a picked image and export the composite via MediaStore.

## Installation

### Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 11+ on `PATH`
- `minSdk 24`, `compileSdk 36`

### Try the demo app

Clone, then install on a connected device or running emulator:

```bash
git clone <this-repo-url>
cd photoEditor2
./gradlew :app:installDebug
```

Launch "Photo Editor" from the app drawer, tap the **+** icon to pick an image, then edit and save — results land in `Pictures/` via MediaStore.

### Use the `:photoeditor` library in your own project

**Option 1 — JitPack (recommended)**

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then add the dependency to your app module (see [jitpack.io/#Muzamilabdallah/photoEditor](https://jitpack.io/#Muzamilabdallah/photoEditor) for the latest tag):

```kotlin
dependencies {
    implementation("com.github.Muzamilabdallah.photoEditor:photoeditor:<tag>")
}
```

Alternatively, the library can be consumed as a local Gradle module. Two options:

**Option 2 — copy the module into your repo**

1. Copy the `photoeditor/` directory into your project root.
2. In your `settings.gradle.kts`:
   ```kotlin
   include(":photoeditor")
   ```
3. In your app module's `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(project(":photoeditor"))
   }
   ```

**Option 3 — include the repo as a composite build**

In your consumer project's `settings.gradle.kts`:

```kotlin
includeBuild("/absolute/path/to/photoEditor2") {
    dependencySubstitution {
        substitute(module("com.muzamil.photoeditor:photoeditor"))
            .using(project(":photoeditor"))
    }
}
```

Then add `implementation("com.muzamil.photoeditor:photoeditor")` to your app module.

### Wire it up

No permissions needed on Android 10+ (the photo picker and MediaStore writes are permission-free; on API 24–28 public gallery saves need `WRITE_EXTERNAL_STORAGE` at runtime — the library declares it with `maxSdkVersion=28`, your app requests it). The `PhotoEditor` composable owns its state, theme, photo picking, and bitmap decoding — the entire integration is:

```kotlin
import com.muzamil.photoeditor.ui.PhotoEditor

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoEditor()
        }
    }
}
```

Every piece is overridable when you need more control:

```kotlin
val editorState = rememberPhotoEditorState() // hoist state to observe/drive the editor
                                             // (e.g. editorState.undo(), editorState.canUndo)
PhotoEditor(
    bitmap = myBitmap,                        // supply an image directly
    state = editorState,
    onPickImage = { launchMyOwnPicker() },    // replace the built-in photo picker
    saveDestination =                         // where the save button writes:
        SaveDestination.Gallery("MyApp"),     //   Pictures/MyApp via MediaStore, or…
        // SaveDestination.File(File(filesDir, "edits/out.jpg")) — app-private path
)
```

Edits survive rotation and process death: `PhotoEditorState` ships a custom `Saver`, and the picked image uri is persisted and re-decoded on restore.

See `app/src/main/java/com/muzamil/photoeditor/MainActivity.kt` for the working demo.