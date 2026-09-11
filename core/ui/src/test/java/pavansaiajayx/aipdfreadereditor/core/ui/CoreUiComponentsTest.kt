package pavansaiajayx.aipdfreadereditor.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CoreUiComponentsTest {

    @Test
    fun coreUiComponentClassesExist() {
        val componentClassNames = listOf(
            "pavansaiajayx.aipdfreadereditor.core.ui.components.AppButtonKt",
            "pavansaiajayx.aipdfreadereditor.core.ui.components.AppTextFieldKt",
            "pavansaiajayx.aipdfreadereditor.core.ui.components.DocumentCardKt",
            "pavansaiajayx.aipdfreadereditor.core.ui.components.BlendedAdCardKt",
            "pavansaiajayx.aipdfreadereditor.core.ui.components.InsufficientCreditsDialogKt",
            "pavansaiajayx.aipdfreadereditor.core.ui.components.CreditBadgeKt",
            "pavansaiajayx.aipdfreadereditor.core.ui.components.LoadingOverlayKt",
            "pavansaiajayx.aipdfreadereditor.core.ui.components.EmptyStateKt"
        )

        for (className in componentClassNames) {
            val clazz = Class.forName(className)
            assertNotNull("Class must exist: $className", clazz)
        }
    }

    @Test
    fun noComposableExceeds150LinesInvariant() {
        // Enforce AGENTS.md Invariant: No monolithic composables (max 150 lines)
        val sourceDir = File("src/main/java/pavansaiajayx/aipdfreadereditor/core/ui/components")
        if (!sourceDir.exists()) {
            val altDir = File("core/ui/src/main/java/pavansaiajayx/aipdfreadereditor/core/ui/components")
            assertTrue("Source directory must exist", altDir.exists())
            checkFileLengths(altDir)
        } else {
            checkFileLengths(sourceDir)
        }
    }

    private fun checkFileLengths(directory: File) {
        val ktFiles = directory.listFiles { file -> file.extension == "kt" } ?: emptyArray()
        assertTrue("Component files must exist", ktFiles.isNotEmpty())

        for (file in ktFiles) {
            val lines = file.readLines()
            assertTrue(
                "File ${file.name} has ${lines.size} lines, must not exceed 150 lines per AGENTS.md invariant",
                lines.size <= 150
            )
        }
    }
}
