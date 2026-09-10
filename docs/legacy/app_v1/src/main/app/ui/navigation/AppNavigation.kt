package pavansaiajayx.aipdfreadereditor.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import pavansaiajayx.aipdfreadereditor.app.core.preferences.PreferencesViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.home.HomeScreen
import pavansaiajayx.aipdfreadereditor.app.ui.tools.PdfToolsScreen
import pavansaiajayx.aipdfreadereditor.app.ui.viewer.PdfViewerScreen
import pavansaiajayx.aipdfreadereditor.app.ui.viewer.MultiFormatViewerScreen

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import pavansaiajayx.aipdfreadereditor.app.ui.chat.PdfChatScreen
import pavansaiajayx.aipdfreadereditor.app.ui.onboarding.OnboardingScreen
import pavansaiajayx.aipdfreadereditor.app.ui.tools.MergePdfScreen
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.DeletePagesScreen
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.ExtractPagesScreen
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.SplitPdfScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    preferencesViewModel: PreferencesViewModel = hiltViewModel()
) {
    val preferencesState by preferencesViewModel.uiState.collectAsStateWithLifecycle()

    val targetRoute = remember(preferencesState.isOnboardingCompleted, preferencesState.isLoading) {
        if (preferencesState.isLoading) return@remember null
        if (!preferencesState.isOnboardingCompleted) return@remember OnBoardingGraphRoute
        MainGraphRoute
    }

    LaunchedEffect(targetRoute) {
        targetRoute?.let { route ->
            navController.navigate(route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = RootSplashRoute
    ) {
        composable<RootSplashRoute> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        navigation<OnBoardingGraphRoute>(startDestination = Screen.Onboarding) {
            composable<Screen.Onboarding> {
                OnboardingScreen(
                    onGetStarted = {
                        preferencesViewModel.completeOnboarding()
                    }
                )
            }
        }

        navigation<MainGraphRoute>(startDestination = Screen.Home) {
            composable<Screen.Home> {
                val context = LocalContext.current
                HomeScreen(
                    onNavigateToTools = { navController.navigate(Screen.PdfTools) },
                    onNavigateToViewer = { uri ->
                        val mimeType = context.contentResolver.getType(Uri.parse(uri))
                        if (mimeType == "application/pdf") {
                            navController.navigate(Screen.PdfViewer(uri))
                        } else {
                            navController.navigate(Screen.MultiFormatViewer(uri))
                        }
                    }
                )
            }
            composable<Screen.PdfViewer> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.PdfViewer>()
                PdfViewerScreen(
                    fileUri = args.fileUri,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { uri -> navController.navigate(Screen.PdfChat(uri)) }
                )
            }
            composable<Screen.PdfChat> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.PdfChat>()
                PdfChatScreen(
                    fileUri = args.fileUri,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<Screen.MultiFormatViewer> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.MultiFormatViewer>()
                MultiFormatViewerScreen(
                    fileUri = args.fileUri,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<Screen.PdfTools> {
                PdfToolsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEarnCredits = { navController.popBackStack() },
                    onNavigateToMergePdf = { navController.navigate(Screen.MergePdf) },
                    onNavigateToSplitPdf = {
                        navController.navigate(Screen.SplitPdf)
                    },
                    onNavigateToDeletePdfPage = {
                        navController.navigate(Screen.DeletePages)
                    },
                    onNavigateToExtractPages = {
                        navController.navigate(Screen.ExtractPages)
                    },
                )
            }
            composable<Screen.MergePdf> {
                MergePdfScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPreviewPdf = { uri -> navController.navigate(Screen.PdfViewer(uri)) }
                )
            }
            composable<Screen.SplitPdf> {
                pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.SplitPdfScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<Screen.ExtractPages> {
                pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.ExtractPagesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<Screen.DeletePages> {
                pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.DeletePagesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
