# 🤖 AGENTS.md: Autonomous Engineering Protocol for AiPdfReaderEditor

Welcome, AI Agent. You are operating in **AiPdfReaderEditor**, a production-grade, multi-module Android application.
Before making any changes or generating code, you MUST adhere to the following protocol.

---

## 1. Constitutional Documents & Authorities
- **[MASTER_ENGINEERING_RULES.md](./MASTER_ENGINEERING_RULES.md)**: The supreme project constitution. Overrules any generic agent assumptions.
- **[MOBILE_SYSTEM_DESIGN.md](./MOBILE_SYSTEM_DESIGN.md)**: Complete 60-section architecture, domain models, data layer, and recovery matrices.
- **[DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md)**: Complete UI tokens (Dual theme, 4dp grid, typography, shapes, and component specifications).
- **[.agents/skills/](./.agents/skills/)**: 53 specialized agent skills categorized by domain (`_core`, `android`, `compose/chrisbanes`, `workflow`).

---

## 2. ADHD / Cognitive Clarity Guidelines (Strict Output Contract)
To prevent cognitive overload, long meandering explanations, and buried answers:
1. **Lead with the Next Action**: State what is being done or what the user needs to do in the first sentence.
2. **Cap Lists at 5 Items**: Group large lists into bite-sized subsets.
3. **No Fluff or Preamble**: Omit generic conversational filler ("Certainly!", "Sure, I can help with that!").
4. **Concrete Code Snippets with File Paths**: Always provide exact file links and line references.

---

## 3. Tooling Authorities & CLI Integration
- **Google Android CLI (`android`)**: Installed and available in PATH.
  - Search official docs: `android docs search "<query>"`
  - Lookup artifact versions: `android studio version-lookup <dependency>`
  - Inspect running UI tree: `android layout --pretty`
  - Capture screen: `android screen capture -o=screenshot.png`
- **Git Workflow**: Always follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `perf:`, `chore:`, `test:`, `build:`). Current branch: `feat/modular-v2-rebuild`.
- **Gradle Build-Logic**: All module build scripts MUST use convention plugins from `:build-logic`.

---

## 4. Skill Routing Hierarchy
Follow this routing order:
```text
User Request
   ↓
Is it a substantial feature/task?
   ├── YES → Activate workflow/superpowers (brainstorming, writing-plans, TDD)
   └── NO  → Direct route to specialist
                 ↓
      Check _core/android-engineering & _core/android-project-context
                 ↓
      Android Documentation / API / Device / Version Lookup?
         └── android/tools/android-cli (`android docs search`, `version-lookup`, `layout`)
                 ↓
      Compose / UI Problem?
         ├── General UI → compose/android-compose
         └── Deep Compose → compose/chrisbanes/* (state-hoisting, performance, effects)
                 ↓
      Data / Storage / Room / MediaStore?
         └── android/data/android-data-layer
                 ↓
      DI / Hilt?
         └── android/di/android-di-hilt
                 ↓
      Navigation?
         └── android/navigation/android-navigation
                 ↓
      Build / Gradle / Convention Plugins / Optimization?
         ├── Convention Plugins → android/build/gradle-convention-plugins
         ├── Optimization & Cache → android/build/gradle-optimization
         └── General Build → android/build/android-gradle
                 ↓
      Git / Commits / Branching?
         └── workflow/git-workflow
                 ↓
      Bug / Crash?
         └── android/reliability/android-debugging
                 ↓
      Completion & Verification?
         └── workflow/unlazy (Depth tree gates, assembleDebug, test passes)
```

---

## 5. Invariant Checklist (Must Never Violate)
- [ ] **Offline Tools are 100% Free**: Never inject or check `CreditManager` in offline tools.
- [ ] **AI Features are Token-Metered**: Deduct credits strictly based on `usageMetadata` token formula after success.
- [ ] **Phase 1 English-Only Strings**: Never hardcode user-facing strings in UI. Strictly use `res/values/strings.xml` and `stringResource(R.string.*)`.
- [ ] **Scoped Storage**: No raw `File` paths on Android 10+. Use MediaStore for single files and SAF for batch.
- [ ] **PdfRenderer Thread Safety**: Never access `PdfRenderer` directly; use `PdfRendererPool` with `Semaphore(4)`.
- [ ] **No Monolithic Composables**: Max 150 lines per Composable function.
- [ ] **Design Tokens**: Never use raw hex colors or arbitrary dp spacing.
- [ ] **Convention Plugins**: Use `:build-logic` convention plugins instead of duplicating module configurations.
- [ ] **No Blind Gradle Upgrades**: Never bump dependency versions without compatibility diagnosis.

---

## 6. Verification Commands
Before declaring any task complete:
```powershell
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
