---
name: android-presentation
description: Authoritative guide for modern Android Presentation architecture using MVVM, UDF, and StateFlow.
---

# Android Presentation Architecture: MVVM + UDF

## 1. Core Structure
Every screen must adhere to:
`	ext
ViewModel
   ↓ StateFlow<UiState>
Compose UI
   ↑ onAction(UiAction)
ViewModel
   ↓ Channel<UiEvent>
One-shot side effects (Navigation, Snackbars)
`

## 2. Immutable UiState
`kotlin
data class ScreenUiState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList(),
    val errorMessage: UiText? = null
)
`

## 3. Typed UiActions
`kotlin
sealed interface ScreenAction {
    data class ItemClicked(val id: String) : ScreenAction
    data object Refresh : ScreenAction
    data object DismissError : ScreenAction
}
`

## 4. One-Shot UiEvents
Collect with Lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED).
`kotlin
sealed interface ScreenEvent {
    data class NavigateToDetails(val id: String) : ScreenEvent
    data class ShowToast(val message: UiText) : ScreenEvent
}
`

## 5. Rules
- NEVER create Redux/MVI reducers or event buses unless explicitly required.
- Stateless Composables: accept state object and lambda callbacks.
- State restoration: use 
ememberSaveable for ephemeral UI state (scroll position, expanded flags).
