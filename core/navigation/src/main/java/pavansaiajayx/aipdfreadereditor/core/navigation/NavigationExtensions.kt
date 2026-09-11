package pavansaiajayx.aipdfreadereditor.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions

fun NavController.navigateToViewer(documentUri: String, navOptions: NavOptions? = null) {
    navigate(route = Route.Viewer(documentUri), navOptions = navOptions)
}

fun NavController.navigateToChat(documentUri: String, navOptions: NavOptions? = null) {
    navigate(route = Route.Chat(documentUri), navOptions = navOptions)
}

fun NavController.navigateToTool(toolRoute: Route, navOptions: NavOptions? = null) {
    navigate(route = toolRoute, navOptions = navOptions)
}

fun NavController.navigateToHome(popUpToRoute: Route? = null, inclusive: Boolean = false) {
    navigate(
        route = Route.Home,
        navOptions = navOptions {
            launchSingleTop = true
            popUpToRoute?.let {
                popUpTo(it) {
                    this.inclusive = inclusive
                }
            }
        }
    )
}
