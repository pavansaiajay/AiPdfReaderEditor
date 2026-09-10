package pavansaiajayx.aipdfreadereditor.core.ai.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.ai.AiEngine
import pavansaiajayx.aipdfreadereditor.core.ai.client.FirebaseGeminiClient
import pavansaiajayx.aipdfreadereditor.core.ai.client.GeminiClient
import pavansaiajayx.aipdfreadereditor.core.ai.ocr.MlKitOcrRecognizer
import pavansaiajayx.aipdfreadereditor.core.ai.ocr.OcrEngine
import pavansaiajayx.aipdfreadereditor.core.ai.ocr.OcrRecognizer
import javax.inject.Inject
import javax.inject.Singleton

class AiModuleTest {

    @Test
    fun aiModuleHasModuleAnnotation() {
        val moduleAnnotation = AiModule::class.java.getAnnotation(Module::class.java)
        assertNotNull("AiModule must be annotated with @Module", moduleAnnotation)
    }

    @Test
    fun bindGeminiClientMethodHasProperBindsAndSingleton() {
        val method = AiModule::class.java.declaredMethods.firstOrNull { it.name == "bindGeminiClient" }
        assertNotNull("bindGeminiClient must exist", method)
        assertNotNull("bindGeminiClient must have @Binds", method!!.getAnnotation(Binds::class.java))
        assertNotNull("bindGeminiClient must have @Singleton", method.getAnnotation(Singleton::class.java))
        assertEquals(GeminiClient::class.java, method.returnType)
        assertEquals(FirebaseGeminiClient::class.java, method.parameterTypes.first())
    }

    @Test
    fun bindOcrRecognizerMethodHasProperBindsAndSingleton() {
        val method = AiModule::class.java.declaredMethods.firstOrNull { it.name == "bindOcrRecognizer" }
        assertNotNull("bindOcrRecognizer must exist", method)
        assertNotNull("bindOcrRecognizer must have @Binds", method!!.getAnnotation(Binds::class.java))
        assertNotNull("bindOcrRecognizer must have @Singleton", method.getAnnotation(Singleton::class.java))
        assertEquals(OcrRecognizer::class.java, method.returnType)
        assertEquals(MlKitOcrRecognizer::class.java, method.parameterTypes.first())
    }

    @Test
    fun aiEngineAndOcrEngineAreSingletonInjected() {
        assertNotNull("AiEngine must be @Singleton", AiEngine::class.java.getAnnotation(Singleton::class.java))
        assertNotNull("AiEngine constructor must be @Inject", AiEngine::class.java.constructors.first().getAnnotation(Inject::class.java))

        assertNotNull("OcrEngine must be @Singleton", OcrEngine::class.java.getAnnotation(Singleton::class.java))
        assertNotNull("OcrEngine constructor must be @Inject", OcrEngine::class.java.constructors.first().getAnnotation(Inject::class.java))
    }
}
