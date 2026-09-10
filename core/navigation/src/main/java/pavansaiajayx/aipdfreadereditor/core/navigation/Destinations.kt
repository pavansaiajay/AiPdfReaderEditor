package pavansaiajayx.aipdfreadereditor.core.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data class Viewer(val documentUri: String) : Screen

    @Serializable
    data object Tools : Screen

    @Serializable
    data class Chat(val documentUri: String) : Screen
}
