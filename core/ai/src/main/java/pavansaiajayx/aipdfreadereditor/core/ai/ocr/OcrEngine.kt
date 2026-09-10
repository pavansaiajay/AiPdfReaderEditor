package pavansaiajayx.aipdfreadereditor.core.ai.ocr

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pavansaiajayx.aipdfreadereditor.core.common.network.AppDispatchers
import pavansaiajayx.aipdfreadereditor.core.common.network.Dispatcher
import pavansaiajayx.aipdfreadereditor.core.common.result.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrEngine @Inject constructor(
    private val ocrRecognizer: OcrRecognizer,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun extractTextFromBitmap(bitmap: Bitmap): Result<String> = withContext(ioDispatcher) {
        try {
            ocrRecognizer.recognizeText(bitmap)
        } catch (e: Exception) {
            Result.Error(cause = e)
        }
    }
}
