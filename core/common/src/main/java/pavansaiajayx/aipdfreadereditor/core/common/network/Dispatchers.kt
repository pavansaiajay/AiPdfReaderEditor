package pavansaiajayx.aipdfreadereditor.core.common.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Dispatcher(val dispatcher: AppDispatchers)

enum class AppDispatchers {
    Default,
    IO,
    Main
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
