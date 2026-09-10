---
name: android-localization
description: Universal localization guidelines, string resources, plurals, and Telugu/European support.
---

# Android Localization & Strings Architecture

## Rules:
1. **Zero Hardcoded Strings**: All user-visible copy must reside in strings.xml.
2. **UiText Abstraction**:
   `kotlin
   sealed interface UiText {
       data class DynamicString(val value: String) : UiText
       class StringResource(@StringRes val id: Int, vararg val args: Any) : UiText
   }
   `
3. **Plurals & Formatting**: Use <plurals> and String.format() tokens (%1, %1) rather than string concatenation.
4. **Multilingual Support**: Primary in 
es/values/strings.xml (English), with support for Indic (Telugu alues-te/) and European (Spanish, French, German).
