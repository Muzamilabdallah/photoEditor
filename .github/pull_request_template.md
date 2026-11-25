## What & why

## How it was verified
- [ ] `./gradlew :photoeditor-core:testDebugUnitTest`
- [ ] Ran the demo app (if UI or rendering changed)
- [ ] Preview and export still match (if rendering changed)

## Checklist
- [ ] Both rendering paths updated (preview `EditorCanvas` + `ImageExporter`) if an element changed
- [ ] `PhotoEditorStateSaver` updated if the element model changed
- [ ] `CHANGELOG.md` updated
