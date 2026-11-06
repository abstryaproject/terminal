# Build fix

Codemagic compilation error:
`Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.`

Cause: `MainActivity.kt` explicitly imported `androidx.compose.foundation.layout.weight`. In Compose, `weight` is a scope-specific member extension available inside `Row`/`Column` content and should not be imported as a top-level symbol.

Fix: removed the explicit `weight` import. Calls such as `Modifier.weight(1f)` remain inside their corresponding `Column`/`Row` scopes.
