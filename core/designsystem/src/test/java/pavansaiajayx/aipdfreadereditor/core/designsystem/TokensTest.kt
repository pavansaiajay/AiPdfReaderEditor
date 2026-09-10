package pavansaiajayx.aipdfreadereditor.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.Shapes
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.Spacing

class TokensTest {

    @Test
    fun spacingTokensConformToFourDpGrid() {
        assertEquals(0.dp, Spacing.none)
        assertEquals(4.dp, Spacing.xs)
        assertEquals(8.dp, Spacing.sm)
        assertEquals(12.dp, Spacing.md)
        assertEquals(16.dp, Spacing.lg)
        assertEquals(24.dp, Spacing.xl)
        assertEquals(32.dp, Spacing.xxl)
        assertEquals(48.dp, Spacing.xxxl)
    }

    @Test
    fun shapeTokensMatchDesignSystem() {
        assertEquals(RoundedCornerShape(0.dp), Shapes.none)
        assertEquals(RoundedCornerShape(4.dp), Shapes.xs)
        assertEquals(RoundedCornerShape(8.dp), Shapes.sm)
        assertEquals(RoundedCornerShape(12.dp), Shapes.md)
        assertEquals(RoundedCornerShape(16.dp), Shapes.lg)
        assertEquals(RoundedCornerShape(24.dp), Shapes.xl)
        assertEquals(RoundedCornerShape(999.dp), Shapes.full)
    }
}
