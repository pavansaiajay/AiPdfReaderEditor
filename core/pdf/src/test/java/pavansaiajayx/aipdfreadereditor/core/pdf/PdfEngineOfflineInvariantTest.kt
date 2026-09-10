package pavansaiajayx.aipdfreadereditor.core.pdf

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.pdf.engine.PdfEngine

class PdfEngineOfflineInvariantTest {

    @Test
    fun verifyPdfEngineNeverInjectsOrReferencesCreditManager() {
        val clazz = PdfEngine::class.java

        // 1. Invariant: Constructor must not take CreditManager
        for (constructor in clazz.constructors) {
            for (param in constructor.parameterTypes) {
                assertFalse(
                    "PdfEngine constructor must not take CreditManager: $param",
                    param.name.contains("CreditManager")
                )
            }
        }

        // 2. Invariant: No fields should be CreditManager
        for (field in clazz.declaredFields) {
            assertFalse(
                "PdfEngine must not have CreditManager field: ${field.name}",
                field.type.name.contains("CreditManager")
            )
        }

        // 3. Invariant: No methods should take CreditManager
        for (method in clazz.declaredMethods) {
            for (param in method.parameterTypes) {
                assertFalse(
                    "PdfEngine method ${method.name} must not take CreditManager",
                    param.name.contains("CreditManager")
                )
            }
        }
    }

    @Test
    fun verifyPdfEngineHasExpectedCoreOperations() {
        val clazz = PdfEngine::class.java
        val methodNames = clazz.declaredMethods.map { it.name }

        assertNotNull(methodNames.find { it.startsWith("mergePdfs") })
        assertNotNull(methodNames.find { it.startsWith("splitPdf") })
        assertNotNull(methodNames.find { it.startsWith("compressPdf") })
        assertNotNull(methodNames.find { it.startsWith("deletePages") })
        assertNotNull(methodNames.find { it.startsWith("rotatePages") })
    }
}
