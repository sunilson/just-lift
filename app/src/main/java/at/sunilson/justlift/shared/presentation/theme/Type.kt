package at.sunilson.justlift.shared.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Premium Typography System for Just Lift
 *
 * Design principles:
 * - Bold headlines for impact and energy
 * - Clean, readable body text
 * - Tight letter spacing for modern feel
 * - Generous line heights for comfort
 */
private val defaultTypography = Typography()

val Typography = Typography(
    // Display styles - Hero numbers, big impact moments
    displayLarge = defaultTypography.displayLarge.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = (-2).sp,
        lineHeight = 64.sp
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp,
        lineHeight = 52.sp
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 44.sp
    ),

    // Headline styles - Section titles, workout names
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 40.sp
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp,
        lineHeight = 36.sp
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    ),

    // Title styles - Card titles, list items, labels
    titleLarge = defaultTypography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    ),

    // Body styles - Content text, descriptions
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp,
        lineHeight = 20.sp
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp
    ),

    // Label styles - Buttons, chips, metadata, UI elements
    labelLarge = defaultTypography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        lineHeight = 14.sp
    )
)
