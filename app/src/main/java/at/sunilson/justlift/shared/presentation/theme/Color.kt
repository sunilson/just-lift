package at.sunilson.justlift.shared.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Just Lift - Premium Fitness Color System with Glassmorphism Support
 *
 * A carefully crafted color palette that conveys:
 * - Energy and motivation (primary orange/coral)
 * - Trust and calm (secondary deep blue)
 * - Achievement and growth (tertiary teal/green)
 * - Premium feel with rich, saturated colors
 * - Modern glassmorphism aesthetics
 */

// ============================================
// PRIMARY - Coral/Orange - Energy & Action
// ============================================
val Primary10 = Color(0xFF3A0B00)
val Primary20 = Color(0xFF5D1900)
val Primary30 = Color(0xFF832600)
val Primary40 = Color(0xFFAB3500)     // Light theme primary
val Primary50 = Color(0xFFCF4A12)
val Primary60 = Color(0xFFF06030)
val Primary70 = Color(0xFFFF8A65)
val Primary80 = Color(0xFFFFAB91)     // Dark theme primary
val Primary90 = Color(0xFFFFDBCF)
val Primary95 = Color(0xFFFFEDE8)

// ============================================
// SECONDARY - Deep Blue - Trust & Stability
// ============================================
val Secondary10 = Color(0xFF001F2A)
val Secondary20 = Color(0xFF003547)
val Secondary30 = Color(0xFF004D67)
val Secondary40 = Color(0xFF006688)   // Light theme secondary
val Secondary50 = Color(0xFF0081AA)
val Secondary60 = Color(0xFF2E9DC7)
val Secondary70 = Color(0xFF56B8E1)
val Secondary80 = Color(0xFF7FD3FA)   // Dark theme secondary
val Secondary90 = Color(0xFFCAE6FF)
val Secondary95 = Color(0xFFE6F2FF)

// ============================================
// TERTIARY - Teal/Green - Growth & Success
// ============================================
val Tertiary10 = Color(0xFF002019)
val Tertiary20 = Color(0xFF00382D)
val Tertiary30 = Color(0xFF005142)
val Tertiary40 = Color(0xFF006B58)    // Light theme tertiary
val Tertiary50 = Color(0xFF00866F)
val Tertiary60 = Color(0xFF00A387)
val Tertiary70 = Color(0xFF2DBFA0)
val Tertiary80 = Color(0xFF5ADBB9)    // Dark theme tertiary
val Tertiary90 = Color(0xFF9EF7D5)
val Tertiary95 = Color(0xFFCAFFF0)

// ============================================
// SEMANTIC COLORS
// ============================================

// Success - Green
val Success = Color(0xFF4CAF50)
val SuccessLight = Color(0xFF81C784)
val SuccessDark = Color(0xFF388E3C)
val OnSuccess = Color(0xFFFFFFFF)

// Warning - Amber
val Warning = Color(0xFFFFC107)
val WarningLight = Color(0xFFFFD54F)
val WarningDark = Color(0xFFFFA000)
val OnWarning = Color(0xFF000000)

// Error - Red (using Material defaults but defining for reference)
val ErrorLight = Color(0xFFB3261E)
val ErrorDark = Color(0xFFF2B8B5)

// ============================================
// SURFACE & BACKGROUND VARIANTS
// ============================================
val SurfaceDim = Color(0xFFD6D6D6)
val SurfaceBright = Color(0xFFFBFBFB)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF7F7F7)
val SurfaceContainer = Color(0xFFF1F1F1)
val SurfaceContainerHigh = Color(0xFFEBEBEB)
val SurfaceContainerHighest = Color(0xFFE5E5E5)

// Dark surface variants
val SurfaceDimDark = Color(0xFF131313)
val SurfaceBrightDark = Color(0xFF3B3B3B)
val SurfaceContainerLowestDark = Color(0xFF0A0A0F)
val SurfaceContainerLowDark = Color(0xFF121218)
val SurfaceContainerDark = Color(0xFF18181F)
val SurfaceContainerHighDark = Color(0xFF1E1E26)
val SurfaceContainerHighestDark = Color(0xFF252530)

// ============================================
// GRADIENT COLORS for premium effects
// ============================================
val GradientStart = Color(0xFFFF8A65)
val GradientMiddle = Color(0xFFFF6E40)
val GradientEnd = Color(0xFFE64A19)

val GradientStartDark = Color(0xFFFFAB91)
val GradientMiddleDark = Color(0xFFFF8A65)
val GradientEndDark = Color(0xFFFF7043)

// ============================================
// GLASSMORPHISM COLORS
// ============================================

// Glass surface colors - Light theme
val GlassSurfaceLight = Color(0xFFFFFFFF)
val GlassOverlayLight = Color(0x40FFFFFF)  // 25% white overlay
val GlassBorderLight = Color(0x60FFFFFF)   // 37% white border
val GlassHighlightLight = Color(0x80FFFFFF) // 50% white highlight

// Glass surface colors - Dark theme
val GlassSurfaceDark = Color(0xFF1A1A24)
val GlassOverlayDark = Color(0x30FFFFFF)   // 19% white overlay
val GlassBorderDark = Color(0x40FFFFFF)    // 25% white border
val GlassHighlightDark = Color(0x20FFFFFF) // 12% white highlight

// Glass tint colors for liquid glass effect
val GlassTintPrimary = Color(0x15FF6E40)   // Subtle coral tint
val GlassTintSecondary = Color(0x150081AA) // Subtle blue tint
val GlassTintTertiary = Color(0x1500A387)  // Subtle teal tint

// Glass shadow colors
val GlassShadowLight = Color(0x20000000)
val GlassShadowDark = Color(0x40000000)

// ============================================
// ANIMATED MESH GRADIENT COLORS
// ============================================

// Primary mesh gradient (warm energy)
val MeshGradient1Start = Color(0xFFFF6B6B)
val MeshGradient1Middle = Color(0xFFFF8E53)
val MeshGradient1End = Color(0xFFFFA726)

// Secondary mesh gradient (cool trust)
val MeshGradient2Start = Color(0xFF667EEA)
val MeshGradient2Middle = Color(0xFF764BA2)
val MeshGradient2End = Color(0xFF6B8DD6)

// Tertiary mesh gradient (growth)
val MeshGradient3Start = Color(0xFF11998E)
val MeshGradient3Middle = Color(0xFF38EF7D)
val MeshGradient3End = Color(0xFF00D9FF)

// Dark theme mesh gradients (more saturated)
val MeshGradientDark1Start = Color(0xFFFF5252)
val MeshGradientDark1Middle = Color(0xFFFF7043)
val MeshGradientDark1End = Color(0xFFFFAB40)

val MeshGradientDark2Start = Color(0xFF7C4DFF)
val MeshGradientDark2Middle = Color(0xFF536DFE)
val MeshGradientDark2End = Color(0xFF448AFF)

val MeshGradientDark3Start = Color(0xFF00E5FF)
val MeshGradientDark3Middle = Color(0xFF1DE9B6)
val MeshGradientDark3End = Color(0xFF76FF03)

// ============================================
// GLOW COLORS for premium effects
// ============================================
val GlowPrimary = Color(0x40FF6E40)
val GlowSecondary = Color(0x400081AA)
val GlowTertiary = Color(0x4000A387)
val GlowSuccess = Color(0x404CAF50)
val GlowWarning = Color(0x40FFC107)
val GlowError = Color(0x40B3261E)

// ============================================
// ACCENT COLORS for highlights
// ============================================
val AccentPink = Color(0xFFFF6B9D)
val AccentPurple = Color(0xFFB388FF)
val AccentCyan = Color(0xFF18FFFF)
val AccentLime = Color(0xFFB2FF59)
val AccentAmber = Color(0xFFFFD740)

// ============================================
// LEGACY MAPPINGS (for backward compatibility)
// ============================================
val Purple80 = Primary80
val PurpleGrey80 = Secondary80
val Pink80 = Tertiary80

val Purple40 = Primary40
val PurpleGrey40 = Secondary40
val Pink40 = Tertiary40

// Additional semantic mappings
val Orange80 = Primary80
val Orange40 = Primary40
val OrangeGrey80 = Secondary80
val OrangeGrey40 = Secondary40
val Teal80 = Tertiary80
val Teal40 = Tertiary40
