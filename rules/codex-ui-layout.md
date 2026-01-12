## Summary (for Codex)

Apply when generating or modifying **Android XML layouts, UI structure, or screen composition**.

Ignore for business logic or data-only tasks.


## Rules

- Use **single-column vertical layout only**
- Prefer `ConstraintLayout` or `LinearLayout(vertical)`
- Do NOT create multi-column or dashboard layouts
- Use `MaterialCardView` for all major UI blocks
- Do NOT nest CardViews inside CardViews

### Screen Structure

```
Toolbar (optional)
ScrollView / RecyclerView
BottomNavigationView (main only)

```

### Toolbar

- One title only
- Optional back button
- No lists or cards inside Toolbar

### BottomNavigationView

- Present only in main tab screens
- Hide in record/detail screens
