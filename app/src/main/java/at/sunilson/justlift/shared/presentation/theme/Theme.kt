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

// Premium dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = Primary10,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    secondary = Secondary80,
    onSecondary = Secondary10,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Tertiary10,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary90,
    error = ErrorDark,
    background = SurfaceContainerLowestDark,
    onBackground = Color(0xFFE6E1E5),
    surface = SurfaceContainerDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// Premium light color scheme
private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Primary90,
    onPrimaryContainer = Primary10,
    secondary = Secondary40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary = Tertiary40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Tertiary10,
    error = ErrorLight,
    background = SurfaceContainerLowest,
    onBackground = Color(0xFF1C1B1F),
    surface = SurfaceContainer,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
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
