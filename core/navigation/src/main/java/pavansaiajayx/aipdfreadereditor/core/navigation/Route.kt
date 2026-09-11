package pavansaiajayx.aipdfreadereditor.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Home : Route

    @Serializable
    data class Viewer(val documentUri: String) : Route

    @Serializable
    data class Chat(val documentUri: String) : Route

    @Serializable
    data object ToolsHub : Route

    @Serializable
    data object Merge : Route

    @Serializable
    data object Split : Route

    @Serializable
    data object Extract : Route

    @Serializable
    data object DeletePages : Route

    @Serializable
    data object Reorder : Route

    @Serializable
    data object Scanner : Route

    @Serializable
    data object Compress : Route

    @Serializable
    data object Encrypt : Route

    @Serializable
    data object Decrypt : Route

    @Serializable
    data object Watermark : Route
}
