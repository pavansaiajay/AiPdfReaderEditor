---
name: git-workflow
description: Modern Git standards for autonomous agents: Conventional Commits, branch naming, safe pull/rebase/push, and pre-commit verification gates.
---

# Modern Git Workflow & Conventional Commits

## 1. Conventional Commits Specification
All commit messages must follow the Conventional Commits format:
```text
<type>(<scope>): <short summary in imperative present tense>

[optional body explaining WHY the change was made, context, and alternatives]

[optional footer: Closes #123, BREAKING CHANGE: ...]
```

### Allowed Types:
- `feat`: A new user-facing feature or capability.
  - Example: `feat(viewer): add high-res pinch-to-zoom support`
- `fix`: A bug fix or crash resolution.
  - Example: `fix(pdf-pool): guard native openPage with Semaphore(4)`
- `refactor`: Code restructuring without functional or bug changes.
  - Example: `refactor(tools): decompose PdfToolsScreen into modular tool sub-composables`
- `perf`: A code change that improves rendering or execution performance.
  - Example: `perf(cache): migrate PdfThumbnailCache to byte-counted LRU with GC eviction`
- `test`: Adding or correcting tests without production code changes.
  - Example: `test(economy): add adversarial concurrency tests for credit deduction`
- `build`: Changes affecting build system, Gradle, dependencies, or convention plugins.
  - Example: `build(deps): migrate Room to 3.0.1 with bundled SQLite driver`
- `chore`: Maintenance tasks, repo configs, CI updates.
  - Example: `chore(ci): add GitHub Actions android-ci workflow`

### Rules for Summaries:
- Use imperative mood: "add", "fix", "refactor" (not "added", "fixing", "refactored").
- No capital first letter in summary; no trailing period.
- Scope indicates the module or subsystem: `(viewer)`, `(tools)`, `(economy)`, `(pdf-pool)`, `(designsystem)`.

## 2. Branch Naming Standard
- Features: `feat/<module>-<short-description>` (e.g. `feat/modular-v2-rebuild`, `feat/core-designsystem`)
- Bugfixes: `fix/<short-description>` (e.g. `fix/pdf-renderer-concurrency`)
- Maintenance/Chores: `chore/<description>` (e.g. `chore/setup-convention-plugins`)

## 3. Pull, Rebase & Push Protocol
1. **Always rebase instead of merge commits**:
   ```powershell
   git pull --rebase origin <branch>
   ```
2. **Pre-Commit Verification Gate**:
   NEVER commit code unless the following check passes:
   ```powershell
   ./gradlew assembleDebug
   ```
3. **Atomic Commits**: Each commit should represent one logical unit of work.
