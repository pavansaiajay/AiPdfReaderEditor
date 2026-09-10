package pavansaiajayx.aipdfreadereditor.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class SpacingTokens(
    val none: Dp = 0.dp,
    val xs: Dp = 4.dp,     // Tight icon-to-text spacing
    val sm: Dp = 8.dp,     // Internal chip/badge padding
    val md: Dp = 12.dp,    // Compact list item padding
    val lg: Dp = 16.dp,    // Standard screen margins & card padding
    val xl: Dp = 24.dp,    // Section header spacing
    val xxl: Dp = 32.dp,   // Major component blocks
    val xxxl: Dp = 48.dp   // Empty state vertical gaps
)

object Spacing {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
}

val LocalSpacing = staticCompositionLocalOf { SpacingTokens() }
