package pavansaiajayx.aipdfreadereditor.core.ai.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pavansaiajayx.aipdfreadereditor.core.ai.client.FirebaseGeminiClient
import pavansaiajayx.aipdfreadereditor.core.ai.client.GeminiClient
import pavansaiajayx.aipdfreadereditor.core.ai.ocr.MlKitOcrRecognizer
import pavansaiajayx.aipdfreadereditor.core.ai.ocr.OcrRecognizer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindGeminiClient(impl: FirebaseGeminiClient): GeminiClient

    @Binds
    @Singleton
    abstract fun bindOcrRecognizer(impl: MlKitOcrRecognizer): OcrRecognizer
}
