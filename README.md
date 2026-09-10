# 📄 AiPdfReaderEditor

A modern, production-grade, privacy-respecting Android PDF utility and AI intelligence application built with Jetpack Compose, Material 3, and Clean Multi-Module Architecture.

---

## 🏛️ Architecture & Governance Documents

- **[AGENTS.md](./AGENTS.md)**: Primary onboarding & protocol guide for autonomous AI coding agents.
- **[MASTER_ENGINEERING_RULES.md](./MASTER_ENGINEERING_RULES.md)**: The supreme project constitution governing architecture, memory safety, and invariants.
- **[MOBILE_SYSTEM_DESIGN.md](./MOBILE_SYSTEM_DESIGN.md)**: Complete 60-section system design covering multi-module layout, token-metered AI, and data pipelines.
- **[DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md)**: Visual foundations, dual-theme semantic tokens (OLED Dark + Clean Light), 4dp spacing grid, and Compose components.
- **[Architectural Decision Records (ADRs)](./docs/adr/)**: Formal ADRs for multi-module structure, token-cost billing, blended ads, and PDF pool bounds.
- **[Legacy Documentation Archive](./docs/legacy/)**: Historical records, survey benchmarks, and prior milestone notes.

---

## 🛠️ Technology Stack

- **Language & Runtime**: Kotlin 2.4.10, Coroutines, Flow, Java 17
- **UI & Presentation**: Jetpack Compose (BOM 2026.08.00), Material 3 (1.4.0), Type-Safe Navigation Compose
- **Dependency Injection**: Dagger-Hilt 2.60+
- **Persistence & Storage**: Room 3 (`sqlite-bundled`, KSP), Jetpack DataStore Preferences, Android Scoped Storage (`MediaStore` + SAF)
- **Document & AI Engines**: Apache PDFBox Android, Android Native `PdfRendererPool` with `Semaphore(4)`, Firebase Vertex AI (`gemini-3.7-flash`), ML Kit Document Scanner & OCR
- **Tooling & CI**: Google Android CLI (`android`), Gradle Convention Plugins (`build-logic`), GitHub Actions CI

---

## 🤖 Agentic Skill System

The project embeds a comprehensive 53-skill specialist ecosystem under [`.agents/skills/`](./.agents/skills/):
- **`_core/`**: Architectural constitution and project invariants.
- **`android/`**: Data layer, Hilt, Navigation, Android CLI, Gradle convention plugins, Optimization, Performance, Security, Localization, Debugging, and Testing.
- **`compose/`**: Local Compose router + 17 Chris Banes specialist skills.
- **`workflow/`**: Superpowers methodology, Unlazy acceptance gates, ADHD cognitive clarity, and Git workflow.

---

## 🏗️ Build & Verification

```powershell
# Run unit & adversarial invariant test suites
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```
