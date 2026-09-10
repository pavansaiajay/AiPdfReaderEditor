---
name: android-navigation
description: Authoritative guide for Type-Safe Navigation Compose with Kotlinx Serialization.
---

# Android Navigation Compose: Type-Safe Routing

## Rules:
1. All destinations are defined in `:core:navigation` as `@Serializable` data classes or objects:
   ```kotlin
   @Serializable
   sealed interface Route {
       @Serializable data object Home : Route
       @Serializable data class Viewer(val documentUri: String) : Route
       @Serializable data class Chat(val documentUri: String) : Route
       @Serializable data object ToolsHub : Route
       @Serializable data object Merge : Route
   }
   ```
2. NavHost uses generic destination types: `composable<Route.Home> { HomeScreen(...) }`.
3. Navigation arguments are strongly typed; never pass raw string URLs without proper encoding/decoding.
