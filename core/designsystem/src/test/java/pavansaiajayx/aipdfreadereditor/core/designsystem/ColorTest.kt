package pavansaiajayx.aipdfreadereditor.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.DarkColorScheme
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.LightColorScheme

class ColorTest {

    @Test
    fun darkColorSchemeTokensMatchDesignSystem() {
        assertEquals(Color(0xFF121212), DarkColorScheme.background)
        assertEquals(Color(0xFF1E1E1E), DarkColorScheme.surface)
        assertEquals(Color(0xFF2C2C2C), DarkColorScheme.surfaceVariant)
        assertEquals(Color(0xFF00E5FF), DarkColorScheme.primary)
        assertEquals(Color(0xFF121212), DarkColorScheme.onPrimary)
        assertEquals(Color(0xFFF87171), DarkColorScheme.error)
    }

    @Test
    fun lightColorSchemeTokensMatchDesignSystem() {
        assertEquals(Color(0xFFF8F9FA), LightColorScheme.background)
        assertEquals(Color(0xFFFFFFFF), LightColorScheme.surface)
        assertEquals(Color(0xFFF1F5F9), LightColorScheme.surfaceVariant)
        assertEquals(Color(0xFF00838F), LightColorScheme.primary)
        assertEquals(Color(0xFFFFFFFF), LightColorScheme.onPrimary)
        assertEquals(Color(0xFFEF4444), LightColorScheme.error)
    }
}
