package pavansaiajayx.aipdfreadereditor.core.designsystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class ShapeTokens(
    val none: CornerBasedShape = RoundedCornerShape(0.dp),
    val xs: CornerBasedShape = RoundedCornerShape(4.dp),
    val sm: CornerBasedShape = RoundedCornerShape(8.dp),
    val md: CornerBasedShape = RoundedCornerShape(12.dp),
    val lg: CornerBasedShape = RoundedCornerShape(16.dp),
    val xl: CornerBasedShape = RoundedCornerShape(24.dp),
    val full: CornerBasedShape = RoundedCornerShape(999.dp)
)

object Shapes {
    val none: CornerBasedShape = RoundedCornerShape(0.dp)
    val xs: CornerBasedShape = RoundedCornerShape(4.dp)
    val sm: CornerBasedShape = RoundedCornerShape(8.dp)
    val md: CornerBasedShape = RoundedCornerShape(12.dp)
    val lg: CornerBasedShape = RoundedCornerShape(16.dp)
    val xl: CornerBasedShape = RoundedCornerShape(24.dp)
    val full: CornerBasedShape = RoundedCornerShape(999.dp)
}

val LocalShapes = staticCompositionLocalOf { ShapeTokens() }
