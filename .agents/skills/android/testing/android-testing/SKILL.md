---
name: android-testing
description: Authoritative guide for testing ViewModels, Repositories, Compose UI, and adversarial test suites.
---

# Android Testing Strategy

## Scope:
1. **ViewModel Tests**: Use `kotlinx-coroutines-test` (`StandardTestDispatcher`, `runTest`) and `Turbine` for observing `UiState` flows.
2. **Repository Tests**: Test with in-memory Room databases and fake data sources.
3. **Adversarial Invariant Tests**:
   - Test that free offline utilities never touch `CreditManager`.
   - Test that AI features gate on `< 1` credit and deduct credits strictly after success.
   - Test that concurrent operations are thread-safe.
