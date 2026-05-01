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

Until a published artifact is available (JitPack — see **Roadmap** below), the library can be consumed as a local Gradle module. Two options:

**Option A — copy the module into your repo**

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

**Option B — include the repo as a composite build**

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

The library contributes `READ_MEDIA_IMAGES` via manifest merging, so your host app doesn't need to declare it. From your `Activity`:

```kotlin
import com.muzamil.photoeditor.ui.MyEditorScreen
import com.muzamil.photoeditor.ui.theme.PhotoEditorTheme
import com.muzamil.photoeditor.viewmodel.PhotoEditorViewModel

class EditorActivity : ComponentActivity() {
    private val viewModel: PhotoEditorViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoEditorTheme {
                MyEditorScreen(viewModel = viewModel) {
                    /* your photo-picker launch here — call viewModel.loadImage(bitmap) when picked */
                }
            }
        }
    }
}
```

See `app/src/main/java/com/muzamil/photoeditor/MainActivity.kt` for a full working example with `ActivityResultContracts.PickVisualMedia`.

### Roadmap

A published JitPack artifact (`com.github.<user>:photoEditor2:<tag>`) is planned — once available, installation will reduce to adding the JitPack repo and a single `implementation` line.