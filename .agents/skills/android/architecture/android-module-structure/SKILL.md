---
name: android-module-structure
description: Rules for multi-module boundary enforcement and dependency direction.
---

# Android Multi-Module Structure

## 1. Module Boundaries
`	ext
:app
:feature:* (:feature:home, :feature:viewer, :feature:tools, :feature:chat, :feature:onboarding)
:core:* (:core:common, :core:model, :core:designsystem, :core:ui, :core:navigation, :core:database, :core:datastore, :core:pdf, :core:ai)
`

## 2. Dependency Rules
- Features NEVER depend on other Features.
- :core:ui depends ONLY on :core:designsystem and :core:common.
- :core:model has zero Android UI dependencies.
- Inter-feature navigation uses type-safe routes from :core:navigation.
