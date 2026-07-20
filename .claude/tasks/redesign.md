# Task: Redesign the editor top bar, add typography tokens, polish empty state

Continuation of the photoEditor default UI work. The Shape panel is done and follows the unified selection language. This task covers three things in `:photoeditor-ui-default` (plus one small state addition in `:photoeditor-core`): the editor top bar, a typography token system, and the "no image" empty state.

## 1. Core addition: history state (in `:photoeditor-core`)

Expose undo/redo availability from the engine as observable state so any UI (default or consumer-built) can drive button enabled-states:

```kotlin
@Immutable
data class EditorHistoryState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)
```

Expose it from the editor state holder (e.g. `PhotoEditorState.historyState: State<EditorHistoryState>` or a `StateFlow`, matching the existing state pattern in core). If undo/redo stacks already exist internally, derive it from them — do not duplicate history logic.

## 2. Editor top bar (in `:photoeditor-ui-default`)

Create `EditorTopBar.kt` as a stateless composable:

```kotlin
@Composable
fun EditorTopBar(
    historyState: EditorHistoryState,
    onConfirm: () -> Unit,
    onAddImage: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showConfirm: Boolean = true, // hidden when no image is loaded
)
```

Layout: a single `Row`, height 56.dp, horizontal padding 14.dp, background `colors.surface`, bottom hairline divider `colors.outline` (0.5.dp).

- **Confirm (primary action), at the start:** 40.dp circular button, `accent` background, `onAccent` check icon (20.dp). This is the ONLY accent-filled element in the bar — it must visually dominate. contentDescription = "Apply and save".
- **Spacer(weight = 1f)** between confirm and the utility cluster.
- **Utility cluster at the end, in order: Add image, Undo, Redo, Close.** Each: 36.dp circular button, `surfaceVariant` background, icon 18.dp, 8.dp gap between buttons.
    - Add image: image/photo-plus icon (NOT a bare "+" — the plus alone is ambiguous). contentDescription = "Add image".
    - Undo/Redo: use proper undo/redo icons (curved arrows — `Icons.AutoMirrored.Filled.Undo` / `.Redo`), NOT plain ← → arrows. Plain arrows read as navigation. contentDescriptions = "Undo" / "Redo".
    - Undo enabled only when `historyState.canUndo`; Redo only when `canRedo`. Disabled state: icon tint `onSurfaceVariant` at 38% alpha, button not clickable. Animate tint change (150ms).
    - Close: X icon, neutral styling (never red, never accent — it must not compete with confirm). contentDescription = "Close editor".
- All icons tinted `onSurface` when enabled (except confirm which uses `onAccent`).
- RTL: the layout must mirror correctly with `AutoMirrored` icons for undo/redo; test with `LayoutDirection.Rtl` preview (the sample app targets Arabic users).

## 3. Typography tokens (in `:photoeditor-ui-default` theme)

Add a `PhotoEditorTypography` to the theme, following the same CompositionLocal pattern as `PhotoEditorColors`:

```kotlin
@Immutable
data class PhotoEditorTypography(
    val panelTitle: TextStyle,   // 15.sp, Medium        — panel headers ("Shape")
    val sectionLabel: TextStyle, // 12.sp, Normal        — "Color", "Stroke"
    val body: TextStyle,         // 14.sp, Medium        — "Fill shape", primary rows
    val bodySecondary: TextStyle,// 12.sp, Normal        — descriptions ("Solid instead of outline")
    val chipLabel: TextStyle,    // 11.sp, Normal/Medium — shape chip + nav labels
    val valueLabel: TextStyle,   // 13.sp, Medium        — value pills ("8 px")
    val button: TextStyle,       // 14.sp, Medium        — "Done", "Pick Image"
)
```

- Default `FontFamily` = system default (`FontFamily.Default`) so it inherits whatever the host app sets — do NOT bundle a font. Consumers override the whole set via `PhotoEditorTheme(typography = ...)`.
- Color is NOT part of TextStyle here — text color always comes from `PhotoEditorColors` at the call site (onSurface / onSurfaceVariant / accent), so typography and color remain independently themeable.
- Replace every hardcoded `fontSize`/`fontWeight` in the existing panels (Shape panel included) with these tokens. After this task, `grep -rn "\.sp" photoeditor-ui-default/src` should only match the theme files.

## 4. Empty state ("No image selected" screen)

- Replace the check-circle icon with an image-outline icon (e.g. photo/landscape icon) inside a 96.dp `surfaceVariant` circle with `onSurfaceVariant` tint — a check icon means "done" and is wrong for an empty state.
- Title "No image selected" → `typography.panelTitle`... actually use a larger style: add `val screenTitle: TextStyle // 20.sp, Medium` to the typography set for this. Color `onSurface`.
- Subtitle "Tap the image icon above to pick a photo" → `bodySecondary`, color `onSurfaceVariant`, but reword to "Pick a photo to start editing" (the current text points at a "+" that this task renames to an image icon).
- "Pick Image" button: accent pill (same style as Done — `accent` bg, `onAccent` text, `button` typography, 20.dp radius, 12.dp/24.dp padding) with a photo-plus leading icon (18.dp).
- Hide the confirm button in the top bar on this screen (`showConfirm = false`) — there is nothing to confirm without an image.

## 5. Emoji panel: customizable emoji list

- The Emoji panel takes its list as a parameter with a curated default:

```kotlin
@Composable
fun EmojiToolPanel(
    emojis: List<String> = PhotoEditorDefaults.emojis,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
)
```

- `PhotoEditorDefaults.emojis` lives in `:photoeditor-ui-default`: a curated list of universal + regionally relevant emojis (include Arabic-market-relevant entries alongside universal ones). Restrict to Unicode ≤ 15 so glyphs render on older Android system emoji fonts — no tofu on API 24+.
- Plain `List<String>` — no wrapper type. Render emojis as text via the system emoji font (`Text` composable); do NOT bundle emoji image assets.
- Core's add-emoji API (`addEmoji(emoji: String, ...)` or existing equivalent) accepts any string and must have no knowledge of the default list — remove any hardcoded emoji list from `:photoeditor-core` if one exists. Any string renders at sticker size; document this as a supported text-sticker shortcut, do not validate input.
- Sample app: show one screen using the default list and one passing a custom domain-specific list, to prove the parameter works.

## 6. Theme bridge: colors from the app's MaterialTheme (opt-in)

- The library OWNS its palette; it must never read `MaterialTheme` implicitly. Default stays `PhotoEditorDefaults.darkColors()` (editor-native dark is correct for photo editing regardless of host app theme).
- Add an opt-in bridge in `PhotoEditorDefaults`:

```kotlin
@Composable
fun colorsFromMaterialTheme(
    scheme: ColorScheme = MaterialTheme.colorScheme
): PhotoEditorColors = PhotoEditorColors(
    surface = scheme.surfaceContainer,
    surfaceVariant = scheme.surfaceContainerHigh,
    onSurface = scheme.onSurface,
    onSurfaceVariant = scheme.onSurfaceVariant,
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    accentContainer = scheme.primaryContainer,
    onAccentContainer = scheme.onPrimaryContainer,
    outline = scheme.outlineVariant,
)
```

- Document the three theming tiers in THEMING.md: (1) do nothing → editor-native dark; (2) `PhotoEditorTheme(colors = PhotoEditorDefaults.colorsFromMaterialTheme())` → follows app brand/dynamic color; (3) construct `PhotoEditorColors(...)` manually → full control, no Material dependency required.

## 7. Drawing swatch colors: shared, overridable palette

- Add `PhotoEditorDefaults.swatchColors: List<Color>` — the current 8 (black, white, red, orange, yellow, green, blue, purple). This is the CONTENT palette users draw with; it is completely separate from the `PhotoEditorColors` theme tokens (chrome). Do not mix the two mechanisms or names.
- Every panel with a color picker (Shape, Draw, Text) takes `swatchColors: List<Color> = PhotoEditorDefaults.swatchColors` as a parameter. All tools share the same default so a color picked in one tool is available in the others; per-panel override remains possible.
- **Contrast must be computed, not enumerated.** Replace the hardcoded white/yellow → dark check special case with luminance: `if (color.luminance() > 0.5f) Color.Black else Color.White` for the selected-swatch checkmark. This must work for any consumer-supplied color (pastels, neons).
- **Layout must handle any list length.** Replace the fixed edge-to-edge 8-swatch row with a wrapping layout (`FlowRow` or lazy grid) using fixed 10.dp gaps, so 3, 8, or 20 swatches all render correctly. Selected-ring and checkmark behavior unchanged.
- Core needs NO changes: the engine already stores full `Color` values per element and must remain palette-agnostic (Mode A consumers may use an HSV picker). Verify nothing in `:photoeditor-core` references the swatch list.
- Sample app: one screen passing a custom palette (including at least one light pastel) to prove the override and the luminance checkmark work.

## Constraints
- Same rules as before: stateless composables, no ViewModels inside the lib, all colors/sizes from theme tokens, no hardcoded colors, no new dependencies, `ui-default → core` one-way.
- `EditorTopBar` is container-free like the panels — plain Row content, the consumer/sample places it.
- Add previews: top bar (enabled/disabled history, LTR + RTL, light + dark), empty state (light + dark).
- Run build + lint for both modules and the sample app; fix failures.

## Deliverables
1. `EditorHistoryState` in core, derived from existing undo/redo stacks.
2. `EditorTopBar` per spec, wired into the sample app replacing the current header.
3. `PhotoEditorTypography` tokens + all panels migrated off hardcoded sp values.
4. Polished empty state per spec.
5. `EmojiToolPanel` with `emojis: List<String> = PhotoEditorDefaults.emojis` parameter; core emoji API list-agnostic; sample screens for default and custom lists.
6. `PhotoEditorDefaults.colorsFromMaterialTheme()` bridge.
7. Shared overridable `PhotoEditorDefaults.swatchColors` across Shape/Draw/Text panels, with luminance-based checkmark contrast and a wrapping swatch layout; sample screen with a custom palette.
8. Previews (RTL included) and THEMING.md covering the three color tiers plus a typography override example.