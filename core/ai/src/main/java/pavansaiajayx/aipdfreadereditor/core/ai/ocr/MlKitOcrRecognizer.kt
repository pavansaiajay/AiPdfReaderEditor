package pavansaiajayx.aipdfreadereditor.core.ai.ocr

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import pavansaiajayx.aipdfreadereditor.core.common.result.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlKitOcrRecognizer @Inject constructor() : OcrRecognizer {
    override suspend fun recognizeText(bitmap: Bitmap): Result<String> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = Tasks.await(recognizer.process(image))
            Result.Success(visionText.text)
        } catch (e: Exception) {
            Result.Error(cause = e)
        }
    }
}
