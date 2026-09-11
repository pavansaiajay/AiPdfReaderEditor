package pavansaiajayx.aipdfreadereditor.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NavigationExtensionsTest {

    @Test
    fun navigationExtensionFunctionsExist() {
        val navClass = Class.forName("pavansaiajayx.aipdfreadereditor.core.navigation.NavigationExtensionsKt")
        assertNotNull(navClass)

        val methods = navClass.declaredMethods.map { it.name }
        assertEquals(true, methods.any { it.startsWith("navigateToViewer") })
        assertEquals(true, methods.any { it.startsWith("navigateToChat") })
        assertEquals(true, methods.any { it.startsWith("navigateToTool") })
        assertEquals(true, methods.any { it.startsWith("navigateToHome") })
    }
}
