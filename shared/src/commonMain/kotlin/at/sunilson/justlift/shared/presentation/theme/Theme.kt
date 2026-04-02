package at.sunilson.justlift.shared.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD8CC),
    onPrimary = Color(0xFF2D0800),
    primaryContainer = Color(0xFF6B2D1A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFD4F5FF),
    onSecondary = Color(0xFF00252E),
    secondaryContainer = Color(0xFF004D67),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF9EF7D5),
    onTertiary = Color(0xFF00251C),
    tertiaryContainer = Color(0xFF005142),
    onTertiaryContainer = Color(0xFFFFFFFF),
    error = Color(0xFFFFDAD6),
    background = Color(0xFF0A0A0F),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF141418),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF252530),
    onSurfaceVariant = Color(0xFFF5F0F7),
    outline = Color(0xFFE0DBE4),
    outlineVariant = Color(0xFFA09AA5)
)

// Premium light color scheme - MAXIMUM accessibility contrast per M3 Expressive
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6B2D1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4DE),
    onPrimaryContainer = Color(0xFF2D0800),
    secondary = Color(0xFF004455),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4F5FF),
    onSecondaryContainer = Color(0xFF001F2A),
    tertiary = Color(0xFF005142),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCAFFF0),
    onTertiaryContainer = Color(0xFF002019),
    error = Color(0xFF9B0000),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF8F5F8),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFEBE4EB),
    onSurfaceVariant = Color(0xFF0F0D12),
    outline = Color(0xFF201D24),
    outlineVariant = Color(0xFF8A858E)
)

@Composable
fun JustLiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val glassColors = if (darkTheme) DarkGlassColors else LightGlassColors

    PlatformThemeEffect(darkTheme)

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
 * Platform-specific theme side effects (e.g. status bar styling on Android).
 * No-op on non-Android platforms.
 */
@Composable
expect fun PlatformThemeEffect(darkTheme: Boolean)

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
