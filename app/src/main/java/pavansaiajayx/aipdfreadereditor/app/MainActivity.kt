package pavansaiajayx.aipdfreadereditor.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pavansaiajayx.aipdfreadereditor.app.navigation.AppNavHost
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.AiPdfTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        viewModel.handleIntent(intent)
        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value is MainUiState.Loading
        }

        enableEdgeToEdge()

        setContent {
            AiPdfTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                when (val state = uiState) {
                    is MainUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }
                    is MainUiState.Ready -> {
                        val navController = rememberNavController()
                        AppNavHost(
                            navController = navController,
                            startDestination = state.startDestination,
                            modifier = Modifier.fillMaxSize(),
                            initialPdfUri = state.initialPdfUri,
                            onPdfUriHandled = {
                                viewModel.clearDeepLink()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }
}
