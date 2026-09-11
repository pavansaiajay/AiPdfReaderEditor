package pavansaiajayx.aipdfreadereditor.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pavansaiajayx.aipdfreadereditor.core.navigation.Screen
import pavansaiajayx.aipdfreadereditor.feature.chat.ChatRoute
import pavansaiajayx.aipdfreadereditor.feature.home.HomeRoute
import pavansaiajayx.aipdfreadereditor.feature.onboarding.OnboardingRoute
import pavansaiajayx.aipdfreadereditor.feature.tools.ToolsRoute
import pavansaiajayx.aipdfreadereditor.feature.viewer.ViewerRoute

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Screen,
    modifier: Modifier = Modifier,
    initialPdfUri: String? = null,
    onPdfUriHandled: (() -> Unit)? = null
) {
    LaunchedEffect(initialPdfUri) {
        if (!initialPdfUri.isNullOrBlank()) {
            navController.navigate(Screen.Viewer(initialPdfUri)) {
                launchSingleTop = true
            }
            onPdfUriHandled?.invoke()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Screen.Onboarding> {
            OnboardingRoute(
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Home> {
            HomeRoute(
                onNavigateToViewer = { uri ->
                    navController.navigate(Screen.Viewer(uri))
                },
                onNavigateToTools = {
                    navController.navigate(Screen.Tools)
                }
            )
        }

        composable<Screen.Viewer> { backStackEntry ->
            val viewerRoute = backStackEntry.toRoute<Screen.Viewer>()
            ViewerRoute(
                documentUri = viewerRoute.documentUri,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { uri ->
                    navController.navigate(Screen.Chat(uri))
                },
                onNavigateToTools = {
                    navController.navigate(Screen.Tools)
                }
            )
        }

        composable<Screen.Chat> { backStackEntry ->
            val chatRoute = backStackEntry.toRoute<Screen.Chat>()
            val fileName = chatRoute.documentUri.substringAfterLast('/')
            ChatRoute(
                documentUri = chatRoute.documentUri,
                fileName = fileName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.Tools> {
            ToolsRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToViewer = { uri ->
                    navController.navigate(Screen.Viewer(uri))
                }
            )
        }
    }
}
