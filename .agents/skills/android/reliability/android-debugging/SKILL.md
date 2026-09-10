---
name: android-debugging
description: 8-step systematic debugging process for diagnosing Android crashes, OOMs, and recomposition issues.
---

# Android Systematic Debugging Protocol

## The 8-Step Process:
1. **Reproduce**: Confirm the exact steps to trigger the bug.
2. **Capture Evidence**: Read Logcat stack traces and inspect memory/heap dumps.
3. **Identify Layer**: Isolate whether the failure is UI, ViewModel, Repository, PDF native, or Storage.
4. **Form Hypothesis**: Explain the root cause before touching code.
5. **Minimal Change**: Apply the smallest surgical fix that addresses the root cause.
6. **Test**: Run targeted unit or instrumentation tests to confirm the fix.
7. **Build**: Run `./gradlew assembleDebug` to guarantee zero compilation breaks.
8. **Verify**: Test edge cases and prevent regressions.
