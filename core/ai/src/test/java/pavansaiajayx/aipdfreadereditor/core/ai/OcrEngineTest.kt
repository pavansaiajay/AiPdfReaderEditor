package pavansaiajayx.aipdfreadereditor.core.ai

import android.graphics.Bitmap
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.ai.ocr.OcrEngine
import pavansaiajayx.aipdfreadereditor.core.ai.ocr.OcrRecognizer
import pavansaiajayx.aipdfreadereditor.core.common.result.Result
import java.io.IOException

private class FakeOcrRecognizer : OcrRecognizer {
    var recognizedTextResult: Result<String> = Result.Success("Extracted OCR Text")
    var callCount = 0

    override suspend fun recognizeText(bitmap: Bitmap): Result<String> {
        callCount++
        return recognizedTextResult
    }
}

class OcrEngineTest {

    private lateinit var fakeRecognizer: FakeOcrRecognizer
    private lateinit var ocrEngine: OcrEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        fakeRecognizer = FakeOcrRecognizer()
        ocrEngine = OcrEngine(
            ocrRecognizer = fakeRecognizer,
            ioDispatcher = testDispatcher
        )
    }

    private fun createDummyBitmap(): Bitmap {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(Bitmap::class.java) as Bitmap
    }

    @Test
    fun extractTextFromBitmapReturnsExtractedTextOnSuccess() = runTest(testDispatcher) {
        val dummyBitmap = createDummyBitmap()
        fakeRecognizer.recognizedTextResult = Result.Success("Chapter 1: The Beginning")

        val result = ocrEngine.extractTextFromBitmap(dummyBitmap)
        assertTrue("Expected Result.Success", result is Result.Success)
        val success = result as Result.Success
        assertEquals("Chapter 1: The Beginning", success.data)
        assertEquals(1, fakeRecognizer.callCount)
    }

    @Test
    fun extractTextFromBitmapPropagatesErrorOnFailure() = runTest(testDispatcher) {
        val dummyBitmap = createDummyBitmap()
        val testException = IOException("ML Kit model not downloaded yet")
        fakeRecognizer.recognizedTextResult = Result.Error(cause = testException)

        val result = ocrEngine.extractTextFromBitmap(dummyBitmap)
        assertTrue("Expected Result.Error", result is Result.Error)
        val error = result as Result.Error
        assertEquals(testException, error.cause)
    }
}
