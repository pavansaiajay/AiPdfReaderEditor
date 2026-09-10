package pavansaiajayx.aipdfreadereditor.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. Primitive Color Palette
// ==========================================

// Cyan & Teal Brand Primitives
val Cyan50 = Color(0xFFE0F7FA)
val Cyan100 = Color(0xFFB2EBF2)
val Cyan400 = Color(0xFF26C6DA)
val Cyan500 = Color(0xFF00BCD4)
val CyanAccent = Color(0xFF00E5FF)
val CyanDark = Color(0xFF00838F)

// Slate & Neutral Primitives
val Slate950 = Color(0xFF0F172A)
val Slate900 = Color(0xFF121212) // Deep OLED Black
val Slate800 = Color(0xFF1E1E1E) // Surface Dark
val Slate700 = Color(0xFF2C2C2C) // Surface Variant
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8F9FA)  // Clean Light Background

// Feedback Primitives
val Red500 = Color(0xFFEF4444)
val Red400 = Color(0xFFF87171)
val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Amber500 = Color(0xFFF59E0B)
val Amber400 = Color(0xFFFBBF24)

// Legacy compatibility aliases
val OledBackground = Slate900
val OledSurface = Slate800
val OledSurfaceVariant = Slate700
val CleanBackground = Slate50
val CleanSurface = Color(0xFFFFFFFF)
val CleanSurfaceVariant = Slate100

// ==========================================
// 2. Material 3 Color Schemes
// ==========================================

val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Slate900,
    primaryContainer = Slate800,
    onPrimaryContainer = Cyan100,
    secondary = Cyan400,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = Cyan50,
    tertiary = Emerald400,
    onTertiary = Slate900,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate600,
    error = Red400,
    onError = Slate900
)

val LightColorScheme = lightColorScheme(
    primary = CyanDark,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Cyan50,
    onPrimaryContainer = Slate950,
    secondary = Cyan500,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate950,
    tertiary = Emerald500,
    onTertiary = Color(0xFFFFFFFF),
    background = Slate50,
    onBackground = Slate950,
    surface = Color(0xFFFFFFFF),
    onSurface = Slate950,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate200,
    outlineVariant = Slate400,
    error = Red500,
    onError = Color(0xFFFFFFFF)
)
