package pavansaiajayx.aipdfreadereditor.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiTextTest {

    @Test
    fun dynamicStringContainsRawValue() {
        val uiText = UiText.DynamicString("Network timeout occurred")
        assertEquals("Network timeout occurred", uiText.value)
    }

    @Test
    fun stringResourceHoldsResIdAndArguments() {
        val uiText = UiText.StringResource(
            resId = 1001,
            args = listOf("Document.pdf", 5)
        )
        assertEquals(1001, uiText.resId)
        assertEquals(2, uiText.args.size)
        assertEquals("Document.pdf", uiText.args[0])
        assertEquals(5, uiText.args[1])
    }

    @Test
    fun equalityAndHashCodeWorkCorrectly() {
        val text1 = UiText.DynamicString("Same")
        val text2 = UiText.DynamicString("Same")
        assertEquals(text1, text2)
        assertEquals(text1.hashCode(), text2.hashCode())

        val res1 = UiText.StringResource(123, listOf("A"))
        val res2 = UiText.StringResource(123, listOf("A"))
        assertEquals(res1, res2)
        assertEquals(res1.hashCode(), res2.hashCode())
    }
}
