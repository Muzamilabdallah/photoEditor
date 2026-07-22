# Task: Edge-to-edge insets handling + per-element selection toolbar

Two independent features for the photoEditor default UI. Both are presentation-layer (`:photoeditor-ui-default`) except the small selection-action wiring, which uses core's existing element API.

## Feature 1 — Edge-to-edge / window insets

Problem: on Android 15+ (edge-to-edge is enforced) the editor's top bar renders *behind* the system status bar, so the confirm/add/undo/redo/close controls are partially hidden and untappable. The bottom tool nav can collide with the gesture navigation bar the same way.

Requirements:
- The library must NOT call `enableEdgeToEdge()` or touch the Activity window itself — that is the host app's decision. The library only reacts to whatever insets exist.
- `EditorTopBar` consumes the status-bar / top insets: add `Modifier.windowInsetsPadding(WindowInsets.statusBars)` (or `.safeDrawing.only(WindowInsetsSides.Top)`) so its content sits below the status bar. The bar background should extend *into* the inset area (draw background full-height, pad only the content) so it looks intentional, not like a floating gap.
- The bottom tool nav + any open tool panel consume the bottom insets: `WindowInsets.navigationBars` (or `.safeDrawing.only(WindowInsetsSides.Bottom)` + IME when a text field is present), so controls clear the gesture bar. When the Text tool shows a keyboard, also pad for `WindowInsets.ime` so the input isn't covered.
- Provide the whole editor scaffold as a composable that wires these insets correctly by default, so a consumer gets correct behavior with zero inset code:

```kotlin
@Composable
fun PhotoEditorScaffold(
    state: PhotoEditorState,
    topBar: @Composable () -> Unit = { /* default EditorTopBar */ },
    bottomBar: @Composable () -> Unit = { /* default tool nav */ },
    modifier: Modifier = Modifier,
)
```

- The canvas itself stays full-bleed (edge-to-edge behind the bars is fine and desirable for a photo editor); only the interactive chrome pads for insets.
- Do NOT hardcode status bar height (e.g. 24.dp) — always derive from `WindowInsets`. Different devices, notches, and the WhatsApp-style caption bar in the screenshots all vary.
- Previews: wrap preview content so insets don't crash `@Preview` (insets are 0 there, which is fine).

## Feature 2 — Per-element selection toolbar (contextual, attached to the element)

Problem: the current design shows a global Delete + X bar pinned to the bottom of the screen. Replace it with a compact action toolbar anchored to the *selected element's bounding box*, like Samsung Gallery (reference screenshot: a small pill with copy / edit / delete floating just outside the selection box).

Requirements:
- When an element (emoji, shape, text) is selected, render a small floating action pill anchored to the selection bounds. Default position: just above the top edge of the bounding box, horizontally centered; if the box is too close to the top inset, flip to just below the bottom edge. Never let the pill render off-screen or under the status/nav bar insets.
- Pill contents depend on element type, but keep it minimal — the primary/default per the request is a **delete icon** at minimum. Suggested full set: duplicate, (type-specific edit), delete. For a first pass, ship delete always, duplicate always, and edit only for text/shape. Each icon 20.dp, 44.dp touch target, pill background `surfaceVariant` with a subtle shadow/elevation so it reads above the canvas, `outline` hairline border.
- Delete icon uses a neutral/`onSurface` tint (NOT red fill — matches the minimal look in the reference; destructive intent is clear from the trash glyph).
- The selection box itself: thin `accent` border (1.5.dp) with round drag handles at the corners for resize (the screenshots already have corner handles — keep them, restyle to `accent`). Keep the existing resize/rotate behavior; this task only changes the *action affordance*, not the transform gestures.
- Wire actions through core's existing element API: delete → `state.removeElement(id)`, duplicate → `state.duplicateElement(id)` (add to core if missing, deriving from existing add + copy-transform logic), edit → re-open the relevant tool panel bound to that element. Do not add new engine concepts beyond these.
- Remove the old bottom Delete/X bar and the bottom "Size" slider's delete/close affordances that duplicated this. The Size slider stays for the selected element but its delete/dismiss moves to the contextual pill + tapping empty canvas to deselect.
- Deselect on tap outside the element; the pill and handles disappear on deselect.
- The anchored pill must position correctly in RTL layouts.

## Constraints
- Stateless composables, theme tokens only, no hardcoded colors/sizes, no new third-party deps, `ui-default → core` one-way.
- The pill positioning must use the element's actual on-canvas bounds (post-transform), accounting for canvas pan/zoom if the canvas supports it — anchor in canvas coordinate space then convert to screen, so the pill tracks the element when it moves.
- Insets: use `androidx.compose.foundation.layout` WindowInsets APIs only; no accompanist insets (deprecated).
- Run build + lint for both modules and the sample app; fix failures. Test the sample app on an API 35 emulator (edge-to-edge enforced) to confirm the top bar clears the status bar.

## Deliverables
1. `PhotoEditorScaffold` (or equivalent) wiring status-bar, navigation-bar, and IME insets by default; top bar background bleeds into the status-bar area, content padded.
2. Canvas remains full-bleed; only chrome pads.
3. Contextual selection-action pill anchored to the selected element's bounds (delete minimum; duplicate + conditional edit), replacing the global bottom Delete/X bar.
4. `state.removeElement` / `state.duplicateElement` in core if not already present.
5. Restyled selection box + corner handles using `accent`.
6. Tap-outside-to-deselect.
7. Previews and an API-35 sample run confirming insets are correct in both LTR and RTL.