---
name: android-error-handling
description: Standardized error modeling, UiText localization, and user-facing error recovery for AiPdfReaderEditor.
---

# Android Error Handling & Resilience

## Rules:
1. **Hierarchy**: Catch technical exceptions in Data sources -> wrap in typed domain `Result.Error` -> map to `UiState.errorMessage` as `UiText`.
2. **UiText Abstraction**:
   - `UiText.StringResource(resId, vararg args)` for localized strings.
   - `UiText.DynamicString(value)` for server/dynamic messages.
3. **User-Facing Recovery**: Every error state must answer: "What can the user do now?" Provide a retry button or clear fallback path.
