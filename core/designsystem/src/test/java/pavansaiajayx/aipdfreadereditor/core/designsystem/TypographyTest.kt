package pavansaiajayx.aipdfreadereditor.core.designsystem

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.AppTypography

class TypographyTest {

    @Test
    fun typographyOpticalSizesMatchDesignSystem() {
        assertEquals(32.sp, AppTypography.displayLarge.fontSize)
        assertEquals(24.sp, AppTypography.headlineMedium.fontSize)
        assertEquals(20.sp, AppTypography.titleLarge.fontSize)
        assertEquals(16.sp, AppTypography.titleMedium.fontSize)
        assertEquals(16.sp, AppTypography.bodyLarge.fontSize)
        assertEquals(14.sp, AppTypography.bodyMedium.fontSize)
        assertEquals(14.sp, AppTypography.labelLarge.fontSize)
        assertEquals(11.sp, AppTypography.labelSmall.fontSize)
    }
}
