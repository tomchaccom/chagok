## Summary (for Codex)

Apply when logic depends on **time state (Past / Present / Future)**.

---

## Rules

- State must be explicit (enum or sealed class)

```kotlin
enumclassTimeState {
  PAST, PRESENT, FUTURE
}

```

### Permissions

| State | Allowed |
| --- | --- |
| PRESENT | create/update memory |
| PAST | read only |
| FUTURE | manage goals |
- ViewModel must enforce state rules
- UI must not bypass state validation

❌ No cross-state side effects
