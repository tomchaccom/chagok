## Summary (for Codex)

Apply when generating **Fragments, Activities, or navigation logic**.

---

## Rules

- Use **Fragment-based navigation**
- Tabs are fixed:
    - PastFragment
    - PresentFragment
    - HighlightFragment
    - FutureFragment

### Additional Screens

- Record screen = `RecordFragment`
- Past detail = `PastDetailFragment`

### Bottom Navigation

- Visible only in tab fragments
- Hidden in RecordFragment and PastDetailFragment

❌ Do NOT make Record screen a tab

❌ Do NOT nest fragments inside fragments
