# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

WhatsApp-style photo editor delivered as a reusable Android library (`:photoeditor`) plus a thin demo app (`:app`). Built with Jetpack Compose. Kotlin 2.0.21, AGP 8.13.0, compileSdk 36, minSdk 24, Java 11 toolchain, Compose BOM 2024.09.00.

- `:photoeditor` — the library. Gradle namespace `com.muzamil.photoeditor.lib` (intentionally distinct from `:app` so AGP doesn't merge R classes), Kotlin source under `com.muzamil.photoeditor.*` for a clean public API.
- `:app` — demo consumer. Hosts `MainActivity`: the system photo picker (`PickVisualMedia`) + `decodeEditableBitmap` + `PhotoEditor(state, onPickImage)`. Image sourcing deliberately lives here, not in the library.

Publishing: `:photoeditor` applies `maven-publish` (release variant + sources jar, coordinates `com.github.Muzamilabdallah:photoeditor`). `jitpack.yml` at the repo root builds it on JitPack with JDK 17 via `:photoeditor:publishToMavenLocal`. Validate locally with `./gradlew :photoeditor:publishToMavenLocal`.

## Common commands

All commands use the Gradle wrapper at the repo root.

- Build everything: `./gradlew :photoeditor:assembleDebug :app:assembleDebug`
- Library only: `./gradlew :photoeditor:assembleDebug`
- Install demo on device/emulator: `./gradlew :app:installDebug`
- Unit tests (JVM): `./gradlew :photoeditor:testDebugUnitTest` (library — the real test suite) and `./gradlew :app:testDebugUnitTest`
- Instrumented tests (requires device/emulator): `./gradlew :app:connectedDebugAndroidTest`
- Run a single unit test: `./gradlew :app:testDebugUnitTest --tests "com.muzamil.photoeditor.ExampleUnitTest.addition_isCorrect"`
- Lint: `./gradlew :photoeditor:lintDebug :app:lintDebug` (reports under each module's `build/reports/`)
- Clean: `./gradlew clean`

## Architecture

The app is a single-Activity Compose application for editing a picked image by overlaying text, emojis, freehand drawings, and predefined shapes (rectangle / circle / line), then exporting the composite to MediaStore.

README.md at the repo root documents the consumer-facing install and wiring snippet; don't re-document those here — keep this file focused on what Claude needs to work inside the code.

### Data flow

`MainActivity` (in `:app` — owns photo picking and uri persistence) → `PhotoEditor` (public entry point, `ui/PhotoEditor.kt` — owns theme and default state holder; **no picker**: consumers pass a `bitmap` or wire `onPickImage`, which is nullable — null hides all pick affordances) → `EditorHostContent` (adapter, `ui/EditorHost.kt`) → `PhotoEditorScreen` (stateless UI). Consumers who want their own chrome skip all of this and use the public `PhotoEditorCanvas` (`ui/PhotoEditorCanvas.kt`) — the editing surface alone, auto-wired to the state holder. State is owned by `PhotoEditorState` (`state/PhotoEditorState.kt`), a plain Compose state holder created via `rememberPhotoEditorState()` — deliberately **not** an AAC ViewModel (Compose-library idiom, no lifecycle dependency). It exposes `editorState`/`canUndo`/`canRedo`/`saveSuccess` as snapshot state; all mutations go through its methods. Retention across rotation/process death is handled by a custom `Saver` (`state/PhotoEditorStateSaver.kt`) that flattens elements/tool/styles to Bundle-safe primitives — the bitmap is intentionally excluded (too large); persisting the image *uri* and re-decoding on restore is the consumer's job (the demo's `MainActivity` shows the pattern with `rememberSaveable`). Undo history is not retained.

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

**When adding or modifying an element type or its transform, update both rendering paths**, or exported images will diverge from the preview. This also means adding a `when`-branch in `EditorCanvas.kt` (preview), `ImageExporter.kt` (export), and `PhotoEditorState.updateElementTransform` (gesture update) — plus the element's branch in `state/PhotoEditorStateSaver.kt` if its fields change.

### History (undo/redo)

`history/HistoryManager` keeps snapshots of `List<EditElement>` in two deques, bounded at 50. `PhotoEditorState` calls `pushToHistory()` after mutations that should be undoable (add element, delete, finish drawing, clearAll). Transform updates during a drag do **not** push to history — only the terminal action does. History does not track background bitmap or style defaults.

### Library module layout (`photoeditor/src/main/java/com/muzamil/photoeditor/`)

- `domain/EditElement.kt` — sealed `EditElement` (Text/Emoji/Drawing/Shape), `ShapeKind`, `EditorState`, `EditorTool`. Pure data.
- `export/ImageExporter.kt` — `saveEditedImage` extension on `Context`. The bitmap-side rendering twin of `EditorCanvas`.
- `export/DrawingPathBuilder.kt` — `buildSmoothDrawingPath`, the bezier-smoothing helper shared by preview and export drawing rendering.
- `geometry/CropGeometry.kt` — the ContentScale.Crop screen↔ratio transform used by `EditorCanvas` for both pointer input and drawing rendering (internal, unit-tested).
- `history/HistoryManager.kt` — undo/redo deque (internal).
- `state/PhotoEditorState.kt` — public state holder + `rememberPhotoEditorState()`; sole owner of the editor state (snapshot state).
- `state/PhotoEditorStateSaver.kt` — custom `Saver` (internal): elements/tool/styles → Bundle-safe primitives.
- `ui/PhotoEditor.kt` — public `PhotoEditor(...)` entry point: theme + default state holder; no picker (consumer provides `bitmap` or `onPickImage`). Also home of the public `decodeEditableBitmap` helper: software allocation — hardware bitmaps would crash the exporter's pixel copy — and ≤4096px downsampling.
- `ui/PhotoEditorCanvas.kt` — public `PhotoEditorCanvas(state)`: the bare editing surface for consumers building their own chrome; wires `EditorCanvas`'s gesture callbacks into `PhotoEditorState`.
- `ui/PhotoEditorScreen.kt` — top-level `Scaffold`, `EditorTopBar`, `EmptyState`. `internal`.
- `ui/EditorHost.kt` — `EditorHostContent` adapter (internal): wires `PhotoEditorState` into `PhotoEditorScreen`'s callbacks.
- `ui/components/` — `EditorCanvas`, `EditorToolbar` (`ToolButton` + `ShapeToolButton`), `TextInputDialog`, `DraggableElement`, `TextElementContent`/`EmojiElementContent`, `ShapeElementContent`. All `internal`.
- `ui/theme/` — `Color`, `Theme` (`PhotoEditorTheme`), `Type`. Public so consumers can wrap their own UIs.

### `:app` layout

Just `MainActivity.kt` — hosts the `PickVisualMedia` picker, persists the picked uri via `rememberSaveable`, decodes with `decodeEditableBitmap` on `Dispatchers.IO`, and passes `state` + `onPickImage` to `PhotoEditor`. This is the reference integration for consumers. (The Kotlin package `com.muzamil.photoeditor` is shared with the library — this is fine because the AGP namespaces differ, so R classes don't collide.)

### Permissions

MediaStore writes (and the demo app's `PickVisualMedia` picker) need no permission on API 29+, so the library declares none for them (`READ_MEDIA_IMAGES` was deliberately removed — Play flags it and nothing here needs it). The **library** manifest (`photoeditor/src/main/AndroidManifest.xml`) declares only `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion=28`, used by `SaveDestination.Gallery` legacy public-directory writes on API 24–28; requesting it at runtime there is the consumer's job. `SaveDestination.File` (app-private paths) needs no permission.

### Save destinations

`export/SaveDestination.kt` — public sealed interface consumed by `saveEditedImage`, `PhotoEditorState.saveEditedImage`, and the `PhotoEditor` composable's `saveDestination` param. `Gallery(subFolder)` = MediaStore under `Pictures/` (IS_PENDING protocol on Q+, direct public-dir write below); `File(file)` = exact caller-controlled path, parents auto-created.