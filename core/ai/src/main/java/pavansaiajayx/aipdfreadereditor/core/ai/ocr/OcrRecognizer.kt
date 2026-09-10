package pavansaiajayx.aipdfreadereditor.core.ai.ocr

import android.graphics.Bitmap
import pavansaiajayx.aipdfreadereditor.core.common.result.Result

interface OcrRecognizer {
    suspend fun recognizeText(bitmap: Bitmap): Result<String>
}
