package at.sunilson.justlift.shared.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Extended color palette for semantic colors not in Material3
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val successContainer: Color,
    val onSuccess: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarning: Color,
    val onWarningContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val onError: Color,
    val onErrorContainer: Color,
    val gradientStart: Color,
    val gradientMiddle: Color,
    val gradientEnd: Color
)

/**
 * Glass-specific colors for liquid glass and glassmorphism effects
 */
@Immutable
data class GlassColors(
    val surface: Color,
    val overlay: Color,
    val border: Color,
    val highlight: Color,
    val shadow: Color,
    val tintPrimary: Color,
    val tintSecondary: Color,
    val tintTertiary: Color,
    // Mesh gradient colors for animated backgrounds
    val meshGradient1Start: Color,
    val meshGradient1Middle: Color,
    val meshGradient1End: Color,
    val meshGradient2Start: Color,
    val meshGradient2Middle: Color,
    val meshGradient2End: Color,
    val meshGradient3Start: Color,
    val meshGradient3Middle: Color,
    val meshGradient3End: Color,
    // Glow colors
    val glowPrimary: Color,
    val glowSecondary: Color,
    val glowTertiary: Color,
    val glowSuccess: Color,
    // Accent colors
    val accentPink: Color,
    val accentPurple: Color,
    val accentCyan: Color,
    val accentLime: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = Success,
        successContainer = SuccessLight,
        onSuccess = OnSuccess,
        onSuccessContainer = SuccessDark,
        warning = Warning,
        warningContainer = WarningLight,
        onWarning = OnWarning,
        onWarningContainer = WarningDark,
        error = ErrorLight,
        errorContainer = Color(0xFFFCE4EC),
        onError = Color(0xFFFFFFFF),
        onErrorContainer = Color(0xFF8B0000),
        gradientStart = GradientStart,
        gradientMiddle = GradientMiddle,
        gradientEnd = GradientEnd
    )
}

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        surface = GlassSurfaceLight,
        overlay = GlassOverlayLight,
        border = GlassBorderLight,
        highlight = GlassHighlightLight,
        shadow = GlassShadowLight,
        tintPrimary = GlassTintPrimary,
        tintSecondary = GlassTintSecondary,
        tintTertiary = GlassTintTertiary,
        meshGradient1Start = MeshGradient1Start,
        meshGradient1Middle = MeshGradient1Middle,
        meshGradient1End = MeshGradient1End,
        meshGradient2Start = MeshGradient2Start,
        meshGradient2Middle = MeshGradient2Middle,
        meshGradient2End = MeshGradient2End,
        meshGradient3Start = MeshGradient3Start,
        meshGradient3Middle = MeshGradient3Middle,
        meshGradient3End = MeshGradient3End,
        glowPrimary = GlowPrimary,
        glowSecondary = GlowSecondary,
        glowTertiary = GlowTertiary,
        glowSuccess = GlowSuccess,
        accentPink = AccentPink,
        accentPurple = AccentPurple,
        accentCyan = AccentCyan,
        accentLime = AccentLime
    )
}

// Light theme extended colors
private val LightExtendedColors = ExtendedColors(
    success = Success,
    successContainer = Color(0xFFE8F5E9),
    onSuccess = OnSuccess,
    onSuccessContainer = SuccessDark,
    warning = Warning,
    warningContainer = Color(0xFFFFF8E1),
    onWarning = OnWarning,
    onWarningContainer = WarningDark,
    error = ErrorLight,
    errorContainer = Color(0xFFFCE4EC),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF8B0000),
    gradientStart = GradientStart,
    gradientMiddle = GradientMiddle,
    gradientEnd = GradientEnd
)

// Dark theme extended colors
private val DarkExtendedColors = ExtendedColors(
    success = SuccessLight,
    successContainer = Color(0xFF1B3D1E),
    onSuccess = Color(0xFF003910),
    onSuccessContainer = SuccessLight,
    warning = WarningLight,
    warningContainer = Color(0xFF3D3000),
    onWarning = Color(0xFF3D2E00),
    onWarningContainer = WarningLight,
    error = ErrorDark,
    errorContainer = Color(0xFF3D0000),
    onError = Color(0xFF600000),
    onErrorContainer = ErrorDark,
    gradientStart = GradientStartDark,
    gradientMiddle = GradientMiddleDark,
    gradientEnd = GradientEndDark
)

// Light theme glass colors
private val LightGlassColors = GlassColors(
    surface = GlassSurfaceLight,
    overlay = GlassOverlayLight,
    border = GlassBorderLight,
    highlight = GlassHighlightLight,
    shadow = GlassShadowLight,
    tintPrimary = GlassTintPrimary,
    tintSecondary = GlassTintSecondary,
    tintTertiary = GlassTintTertiary,
    meshGradient1Start = MeshGradient1Start,
    meshGradient1Middle = MeshGradient1Middle,
    meshGradient1End = MeshGradient1End,
    meshGradient2Start = MeshGradient2Start,
    meshGradient2Middle = MeshGradient2Middle,
    meshGradient2End = MeshGradient2End,
    meshGradient3Start = MeshGradient3Start,
    meshGradient3Middle = MeshGradient3Middle,
    meshGradient3End = MeshGradient3End,
    glowPrimary = GlowPrimary,
    glowSecondary = GlowSecondary,
    glowTertiary = GlowTertiary,
    glowSuccess = GlowSuccess,
    accentPink = AccentPink,
    accentPurple = AccentPurple,
    accentCyan = AccentCyan,
    accentLime = AccentLime
)

// Dark theme glass colors
private val DarkGlassColors = GlassColors(
    surface = GlassSurfaceDark,
    overlay = GlassOverlayDark,
    border = GlassBorderDark,
    highlight = GlassHighlightDark,
    shadow = GlassShadowDark,
    tintPrimary = GlassTintPrimary,
    tintSecondary = GlassTintSecondary,
    tintTertiary = GlassTintTertiary,
    meshGradient1Start = MeshGradientDark1Start,
    meshGradient1Middle = MeshGradientDark1Middle,
    meshGradient1End = MeshGradientDark1End,
    meshGradient2Start = MeshGradientDark2Start,
    meshGradient2Middle = MeshGradientDark2Middle,
    meshGradient2End = MeshGradientDark2End,
    meshGradient3Start = MeshGradientDark3Start,
    meshGradient3Middle = MeshGradientDark3Middle,
    meshGradient3End = MeshGradientDark3End,
    glowPrimary = GlowPrimary,
    glowSecondary = GlowSecondary,
    glowTertiary = GlowTertiary,
    glowSuccess = GlowSuccess,
    accentPink = AccentPink,
    accentPurple = AccentPurple,
    accentCyan = AccentCyan,
    accentLime = AccentLime
)

// Premium dark color scheme - MAXIMUM accessibility contrast per M3 Expressive
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD8CC),  // Brighter primary for maximum visibility
    onPrimary = Color(0xFF2D0800),  // Very dark for contrast
    primaryContainer = Color(0xFF6B2D1A),  // Richer container
    onPrimaryContainer = Color(0xFFFFFFFF),  // White for maximum contrast
    secondary = Color(0xFFD4F5FF),  // Brighter secondary
    onSecondary = Color(0xFF00252E),  // Very dark for contrast
    secondaryContainer = Color(0xFF004D67),  // Richer container
    onSecondaryContainer = Color(0xFFFFFFFF),  // White for maximum contrast
    tertiary = Color(0xFF9EF7D5),  // Brighter tertiary
    onTertiary = Color(0xFF00251C),  // Very dark for contrast
    tertiaryContainer = Color(0xFF005142),  // Richer container
    onTertiaryContainer = Color(0xFFFFFFFF),  // White for maximum contrast
    error = Color(0xFFFFDAD6),  // Brighter error
    background = Color(0xFF0A0A0F),  // True dark background
    onBackground = Color(0xFFFFFFFF),  // Pure white for maximum contrast
    surface = Color(0xFF141418),  // Slightly lighter surface
    onSurface = Color(0xFFFFFFFF),  // Pure white for maximum contrast
    surfaceVariant = Color(0xFF252530),  // Clearer distinction
    onSurfaceVariant = Color(0xFFF5F0F7),  // Very bright for maximum contrast
    outline = Color(0xFFE0DBE4),  // Very bright for clear visibility
    outlineVariant = Color(0xFFA09AA5)  // Brighter for better visibility
)

// Premium light color scheme - MAXIMUM accessibility contrast per M3 Expressive
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6B2D1A),  // Darker primary for excellent contrast (7:1+)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4DE),  // Lighter, more visible container
    onPrimaryContainer = Color(0xFF2D0800),  // Very dark for maximum contrast
    secondary = Color(0xFF004455),  // Darker secondary for excellent contrast
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4F5FF),  // Lighter, more visible container
    onSecondaryContainer = Color(0xFF001F2A),  // Very dark for maximum contrast
    tertiary = Color(0xFF005142),  // Darker tertiary
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCAFFF0),  // Lighter container
    onTertiaryContainer = Color(0xFF002019),  // Very dark for maximum contrast
    error = Color(0xFF9B0000),  // Darker error for excellent contrast
    background = Color(0xFFFFFBFF),  // Slightly warm white
    onBackground = Color(0xFF000000),  // Pure black for maximum contrast
    surface = Color(0xFFF8F5F8),  // Warmer surface
    onSurface = Color(0xFF000000),  // Pure black for maximum contrast
    surfaceVariant = Color(0xFFEBE4EB),  // Clearer distinction, warmer
    onSurfaceVariant = Color(0xFF0F0D12),  // Very dark for maximum contrast
    outline = Color(0xFF201D24),  // Very dark for clear visibility
    outlineVariant = Color(0xFF8A858E)  // Better definition
)

@Composable
fun JustLiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to use our premium branded palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val glassColors = if (darkTheme) DarkGlassColors else LightGlassColors

    // Always use dark status bar icons regardless of theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalGlassColors provides glassColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Access extended colors and glass colors from any composable
 */
object JustLiftTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current

    val glassColors: GlassColors
        @Composable
        get() = LocalGlassColors.current
}
