package pavansaiajayx.aipdfreadereditor.core.pdf.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pavansaiajayx.aipdfreadereditor.core.pdf.engine.PdfEngine
import pavansaiajayx.aipdfreadereditor.core.pdf.renderer.PdfRendererPool
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PdfModule {

    @Provides
    @Singleton
    fun providePdfRendererPool(): PdfRendererPool {
        return PdfRendererPool()
    }

    @Provides
    @Singleton
    fun providePdfEngine(
        @ApplicationContext context: Context
    ): PdfEngine {
        return PdfEngine(context)
    }
}
