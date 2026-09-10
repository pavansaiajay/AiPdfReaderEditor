---
name: android-architecture
description: Multi-module architecture guidelines, module dependency boundaries, and UDF state modeling rules for AiPdfReaderEditor.
---

# Android Architecture & Multi-Module Guidelines

## 1. Module Structure
```text
:app
:core
  ├── :core:common        (Dispatchers, formatters, result wrappers)
  ├── :core:model         (Pure Kotlin domain models, zero UI dependencies)
  ├── :core:designsystem  (Tokens: Color, Type, Spacing, Shapes, Theme)
  ├── :core:ui            (Reusable UI: AppButton, AppCard, BlendedAdItem)
  ├── :core:navigation    (Type-safe routes, NavHost builders)
  ├── :core:database      (Room 3 AppDatabase, DocumentDao, migrations)
  ├── :core:datastore     (CreditManager, Preferences)
  ├── :core:pdf           (PdfEngine, PdfRendererPool, thumbnail cache)
  └── :core:ai            (Firebase Vertex AI, token counter, ML Kit)
:feature
  ├── :feature:home       (Dashboard, recent files feed, blended ads)
  ├── :feature:viewer     (Document viewer, annotations, search)
  ├── :feature:tools      (Merge, Split, Delete, Extract, Scan, etc.)
  ├── :feature:chat       (Conversational AI chat with PDF)
  └── :feature:onboarding (First-time onboarding flow)
```

## 2. Dependency Direction
- **Rule 1**: Feature modules NEVER depend on other Feature modules (`:feature:home` ⇏ `:feature:tools`).
- **Rule 2**: Cross-feature navigation is routed strictly via `@Serializable` routes in `:core:navigation`.
- **Rule 3**: Data flows downward: `UI -> ViewModel -> UseCase (when justified) -> Repository -> DataSource`.

## 3. State Management (UDF)
Every feature screen MUST define:
- `data class [Screen]UiState(...)` (Immutable, single source of truth)
- `sealed interface [Screen]Action` (User actions emitted to ViewModel)
- `sealed interface [Screen]Event` (One-shot navigation/toast events collected with `Lifecycle.repeatOnLifecycle`)
