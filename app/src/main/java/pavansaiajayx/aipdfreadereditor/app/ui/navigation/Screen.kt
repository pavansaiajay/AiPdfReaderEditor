package pavansaiajayx.aipdfreadereditor.app.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data class PdfViewer(val fileUri: String) : Screen

    @Serializable
    data class MultiFormatViewer(val fileUri: String) : Screen

    @Serializable
    data class PdfChat(val fileUri: String) : Screen

    @Serializable
    data object PdfTools : Screen

    @Serializable
    data object MergePdf : Screen

    @Serializable
    data object SplitPdf : Screen

    @Serializable
    data object ExtractPages : Screen

    @Serializable
    data object DeletePages : Screen
}
