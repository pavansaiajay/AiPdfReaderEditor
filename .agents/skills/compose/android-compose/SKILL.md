---
name: android-compose
description: Jetpack Compose architectural entry point and router to Chris Banes specialist skills.
---

# Android Compose Architecture & Router

## Local Guidelines:
- Follow Design System tokens from `:core:designsystem` (`MaterialTheme.colorScheme`, `Spacing`, `Shapes`).
- Stateless UI: Composables accept state and emit event lambdas (`onAction: (ViewerAction) -> Unit`).
- Use `rememberSaveable` for ephemeral UI state (scroll offset, zoom scale).

## Specialist Routing:
- State hoisting & state holder design -> Chris Banes `compose-state-hoisting`
- Side effects & lifecycle -> Chris Banes `compose-side-effects`
- Recomposition & performance -> Chris Banes `compose-recomposition-performance`
- Flow & state modeling -> Chris Banes `kotlin-flow-state-event-modeling`
