package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ViewerTool {
    None,
    Hand,
    Pen,
    Highlighter,
    TextStamp,
    Signature,
    Eraser
}
