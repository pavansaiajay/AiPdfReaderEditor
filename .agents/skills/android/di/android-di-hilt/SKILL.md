---
name: android-di-hilt
description: Authoritative guide for Dagger-Hilt dependency injection across multi-module Android projects.
---

# Android DI with Dagger-Hilt

## Rules:
1. **Application**: Annotated with `@HiltAndroidApp` in `:app`.
2. **Activities**: Annotated with `@AndroidEntryPoint` in `:app`.
3. **ViewModels**: Annotated with `@HiltViewModel` and `@Inject constructor(...)`.
4. **Modules**:
   - Singletons installed in `SingletonComponent::class`.
   - Use `@Binds` for binding repository interfaces to implementations.
   - Use `@Provides` for third-party libraries (PDFBox, Room, Firebase, DataStore).
   - Use qualifiers (`@IoDispatcher`, `@MainDispatcher`) for coroutine dispatchers.
5. **Multi-Module**: Feature modules access injected dependencies via Hilt without depending on each other.
