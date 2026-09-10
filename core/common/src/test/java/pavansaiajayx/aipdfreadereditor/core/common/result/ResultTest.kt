package pavansaiajayx.aipdfreadereditor.core.common.result

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.common.util.UiText

class ResultTest {

    @Test
    fun flowAsResultEmitsLoadingThenSuccess() = runTest {
        val results = flowOf("Hello").asResult().toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertEquals("Hello", (results[1] as Result.Success).data)
    }

    @Test
    fun flowAsResultCatchesErrorAndWrapsInResultError() = runTest {
        val failingFlow = flow<String> {
            throw IllegalStateException("Disk full")
        }

        val results = failingFlow.asResult().toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Error)

        val error = results[1] as Result.Error
        assertTrue(error.cause is IllegalStateException)
        assertEquals("Disk full", (error.message as? UiText.DynamicString)?.value)
    }

    @Test
    fun resultExtensionsMapSuccess() {
        val success: Result<Int> = Result.Success(42)
        val mapped = success.map { it * 2 }

        assertTrue(mapped is Result.Success)
        assertEquals(84, (mapped as Result.Success).data)
    }

    @Test
    fun resultExtensionsGetOrNullAndGetOrDefault() {
        val success: Result<String> = Result.Success("Data")
        val error: Result<String> = Result.Error(UiText.DynamicString("Fail"))

        assertEquals("Data", success.getOrNull())
        assertNull(error.getOrNull())
        assertEquals("Fallback", error.getOrDefault("Fallback"))
    }
}
