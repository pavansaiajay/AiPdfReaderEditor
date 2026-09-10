package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ToolOperation {
    Merge,
    Split,
    Extract,
    DeletePages,
    Reorder,
    Rotate,
    Compress,
    ImagesToPdf,
    PdfToImages,
    Scan,
    Encrypt,
    Decrypt,
    Watermark,
    Flatten,
    HtmlToPdf,
    TextExtract
}
