# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

WhatsApp-style photo editor delivered as a reusable Android library (`:photoeditor`) plus a thin demo app (`:app`). Built with Jetpack Compose. Kotlin 2.0.21, AGP 8.13.0, compileSdk 36, minSdk 24, Java 11 toolchain, Compose BOM 2024.09.00.

- `:photoeditor` — the library. Gradle namespace `com.muzamil.photoeditor.lib` (intentionally distinct from `:app` so AGP doesn't merge R classes), Kotlin source under `com.muzamil.photoeditor.*` for a clean public API.
- `:app` — demo consumer. Hosts `MainActivity`, runs the photo picker, and embeds the library's editor screen. Has no editor code of its own.

## Common commands

All commands use the Gradle wrapper at the repo root.

- Build everything: `./gradlew :photoeditor:assembleDebug :app:assembleDebug`
- Library only: `./gradlew :photoeditor:assembleDebug`
- Install demo on device/emulator: `./gradlew :app:installDebug`
- Unit tests (JVM): `./gradlew :app:testDebugUnitTest`
- Instrumented tests (requires device/emulator): `./gradlew :app:connectedDebugAndroidTest`
- Run a single unit test: `./gradlew :app:testDebugUnitTest --tests "com.muzamil.photoeditor.ExampleUnitTest.addition_isCorrect"`
- Lint: `./gradlew :photoeditor:lintDebug :app:lintDebug` (reports under each module's `build/reports/`)
- Clean: `./gradlew clean`

## Architecture

The app is a single-Activity Compose application for editing a picked image by overlaying text, emojis, freehand drawings, and predefined shapes (rectangle / circle / line), then exporting the composite to MediaStore.

README.md at the repo root documents the consumer-facing install and wiring snippet; don't re-document those here — keep this file focused on what Claude needs to work inside the code.

### Data flow

`MainActivity` (in `:app`) → `MyEditorScreen` (state adapter, in library `ui/EditorHost.kt`) → `PhotoEditorScreen` (stateless UI). State is owned by `PhotoEditorViewModel` as a single `MutableStateFlow<EditorState>`. The UI is fully driven by this flow; all mutations go through ViewModel methods. `MyEditorScreen` exists solely to collect state with `collectAsStateWithLifecycle` and wire ViewModel methods into the stateless `PhotoEditorScreen`'s callback parameters.

### Element model (`domain/EditElement.kt`)

`EditElement` is a sealed class with subtypes `Text`, `Emoji`, `Drawing`, and `Shape`. **Every coordinate is stored as a ratio in `0f..1f`** relative to the source bitmap:

- `Text`/`Emoji`/`Shape` — `offsetRatioX`/`offsetRatioY` (the element's center).
- `Drawing.points` — each `Offset` is `(x, y)` in ratio space.
- `Shape.widthRatio`/`heightRatio` — the shape's bounding box as a fraction of bitmap dimensions.

This is the key invariant that lets the same coordinates render correctly at any Compose canvas size and again at the bitmap's native resolution during export. Stroke widths for `Drawing` and `Shape` are stored in **bitmap-space pixels** (not ratios) — preview multiplies by `scaleFactor` to render, export uses them raw.

`EditorTool` (sealed) tracks which tool is active: `None`, `Text`, `Drawing`, `Shape(kind: ShapeKind)`. `ShapeKind` is an enum (`Rectangle`, `Circle`, `Line`). `EditorState` bundles elements, the background bitmap, selection, tool, and per-tool default styling (text color, drawing color/stroke, shape color/stroke/filled).

### Rendering paths (two places, must stay in sync)

1. **On-screen preview** — `ui/components/EditorCanvas.kt` does all the gesture handling and ratio↔pixel math; `DraggableElement` wraps text/emoji/shape in a `transformable` modifier that does rotation-compensated pan math (see `ui/components/DraggableElement.kt` — the trig affects drag feel directly). `Shape` renders via `ShapeElementContent.kt`, sizing itself to `widthRatio * parent` × `heightRatio * parent`. Drawings are rendered inside `EditorCanvas`'s `Canvas`, multiplying ratios by `bitmap.width/height` and then by `scaleFactor` to land in screen space.

2. **Export** — `Context.saveEditedImage` in `export/ImageExporter.kt`. This re-draws all elements onto a copy of the background bitmap using `android.graphics.Canvas` (not Compose), sorted by `zIndex`, then writes via MediaStore (API 29+) or legacy `Environment.DIRECTORY_PICTURES` (<29). Ratio coordinates get multiplied by bitmap dimensions here.

**When adding or modifying an element type or its transform, update both rendering paths**, or exported images will diverge from the preview. This also means adding a `when`-branch in `EditorCanvas.kt` (preview), `ImageExporter.kt` (export), and `PhotoEditorViewModel.updateElementTransform` (gesture update).

### History (undo/redo)

`history/HistoryManager` keeps snapshots of `List<EditElement>` in two deques, bounded at 50. The ViewModel calls `pushToHistory()` after mutations that should be undoable (add element, delete, finish drawing, clearAll). Transform updates during a drag do **not** push to history — only the terminal action does. History does not track background bitmap or style defaults.

### Library module layout (`photoeditor/src/main/java/com/muzamil/photoeditor/`)

- `domain/EditElement.kt` — sealed `EditElement` (Text/Emoji/Drawing/Shape), `ShapeKind`, `EditorState`, `EditorTool`. Pure data.
- `export/ImageExporter.kt` — `saveEditedImage` extension on `Context`. The bitmap-side rendering twin of `EditorCanvas`.
- `history/HistoryManager.kt` — undo/redo deque (internal).
- `viewmodel/PhotoEditorViewModel.kt` — single ViewModel; sole owner of `MutableStateFlow<EditorState>`.
- `ui/PhotoEditorScreen.kt` — top-level `Scaffold`, `EditorTopBar`, `EmptyState`. Currently `internal` — public `PhotoEditor(...)` entry point is still to come.
- `ui/EditorHost.kt` — `MyEditorScreen` state adapter (currently the public entry point consumers call; to be replaced by the `PhotoEditor` composable).
- `ui/components/` — `EditorCanvas`, `EditorToolbar` (`ToolButton` + `ShapeToolButton`), `TextInputDialog`, `DraggableElement`, `TextElementContent`/`EmojiElementContent`, `ShapeElementContent`. All `internal`.
- `ui/theme/` — `Color`, `Theme` (`PhotoEditorTheme`), `Type`. Public so consumers can wrap their own UIs.

### `:app` layout

Just `MainActivity.kt` — owns the `PhotoEditorViewModel`, runs `ActivityResultContracts.PickVisualMedia`, decodes bitmaps, and hands them to `MyEditorScreen`. (The Kotlin package `com.muzamil.photoeditor` is shared with the library — this is fine because the AGP namespaces differ, so R classes don't collide.)

### Permissions

`READ_MEDIA_IMAGES` is declared by the **library** manifest (`photoeditor/src/main/AndroidManifest.xml`) and merged into any consumer app — `:app`'s manifest no longer declares it. The photo picker (`PickVisualMedia`) doesn't require runtime permission on modern Android.