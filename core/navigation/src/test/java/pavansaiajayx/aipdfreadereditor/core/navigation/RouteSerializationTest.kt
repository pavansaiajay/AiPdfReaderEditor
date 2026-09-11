package pavansaiajayx.aipdfreadereditor.core.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun serializeAndDeserializeOnboardingRoute() {
        val original: Route = Route.Onboarding
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Route>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun serializeAndDeserializeHomeRoute() {
        val original: Route = Route.Home
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Route>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun serializeAndDeserializeViewerRouteWithUri() {
        val original: Route = Route.Viewer(documentUri = "content://media/external/documents/123")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Route>(encoded)

        assertEquals(original, decoded)
        assertTrue(decoded is Route.Viewer)
        assertEquals("content://media/external/documents/123", (decoded as Route.Viewer).documentUri)
    }

    @Test
    fun serializeAndDeserializeChatRouteWithUri() {
        val original: Route = Route.Chat(documentUri = "content://media/external/documents/456")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Route>(encoded)

        assertEquals(original, decoded)
        assertTrue(decoded is Route.Chat)
        assertEquals("content://media/external/documents/456", (decoded as Route.Chat).documentUri)
    }

    @Test
    fun serializeAndDeserializeToolsHubAndSubtoolRoutes() {
        val tools: List<Route> = listOf(
            Route.ToolsHub,
            Route.Merge,
            Route.Split,
            Route.Extract,
            Route.DeletePages,
            Route.Reorder,
            Route.Scanner,
            Route.Compress,
            Route.Encrypt,
            Route.Decrypt,
            Route.Watermark
        )

        for (toolRoute in tools) {
            val encoded = json.encodeToString(toolRoute)
            val decoded = json.decodeFromString<Route>(encoded)
            assertEquals("Route failed serialization roundtrip: $toolRoute", toolRoute, decoded)
        }
    }
}
