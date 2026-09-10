# ADR 0001: Multi-Module Architecture

## Status
Accepted

## Context
The legacy application suffered from 1,000+ line God Composables (PdfToolsScreen.kt, PdfViewerScreen.kt) and circular dependencies between UI and Data layers.

## Decision
Adopt a multi-module architecture dividing the codebase into:
- :feature:* (:feature:home, :feature:viewer, :feature:tools, :feature:chat, :feature:onboarding)
- :core:* (:core:common, :core:model, :core:designsystem, :core:ui, :core:navigation, :core:database, :core:datastore, :core:pdf, :core:ai)

## Consequences
- Fast incremental compilation.
- Enforced unidirectional data flow and strict module isolation.
- Feature modules cannot depend on each other; all navigation is type-safe via :core:navigation.
