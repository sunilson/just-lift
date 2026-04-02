package at.sunilson.justlift.shared.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated mesh gradient background that creates a fluid, organic motion effect.
 * Perfect as a backdrop for glassmorphism effects.
 */
@Composable
fun AnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    animationDuration: Int = 8000,
    colors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val glassColors = JustLiftTheme.glassColors
    val gradientColors = colors ?: listOf(
        glassColors.meshGradient1Start,
        glassColors.meshGradient1Middle,
        glassColors.meshGradient2Start,
        glassColors.meshGradient2Middle,
        glassColors.meshGradient3Start,
        glassColors.meshGradient3Middle
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mesh_gradient")

    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle1"
    )

    val angle2 by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = 480f,
        animationSpec = infiniteRepeatable(
            animation = tween((animationDuration * 1.3).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle2"
    )

    val angle3 by infiniteTransition.animateFloat(
        initialValue = 240f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween((animationDuration * 0.7).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = minOf(size.width, size.height) * 0.6f

                val pos1 = Offset(
                    centerX + radius * 0.5f * cos(angle1 * PI.toFloat() / 180f),
                    centerY + radius * 0.3f * sin(angle1 * PI.toFloat() / 180f)
                )
                val pos2 = Offset(
                    centerX + radius * 0.4f * cos(angle2 * PI.toFloat() / 180f),
                    centerY + radius * 0.5f * sin(angle2 * PI.toFloat() / 180f)
                )
                val pos3 = Offset(
                    centerX + radius * 0.3f * cos(angle3 * PI.toFloat() / 180f),
                    centerY + radius * 0.4f * sin(angle3 * PI.toFloat() / 180f)
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradientColors[0].copy(alpha = 0.6f),
                            gradientColors[1].copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = pos1,
                        radius = radius * 1.2f
                    )
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradientColors[2].copy(alpha = 0.5f),
                            gradientColors[3].copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = pos2,
                        radius = radius * 1.1f
                    )
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradientColors[4].copy(alpha = 0.4f),
                            gradientColors[5].copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = pos3,
                        radius = radius
                    )
                )
            },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    tint: Color = Color.Transparent,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = JustLiftTheme.glassColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "press_scale"
    )

    val glassBackground = Brush.verticalGradient(
        colors = listOf(
            glassColors.surface.copy(alpha = 0.85f),
            glassColors.surface.copy(alpha = 0.75f)
        )
    )

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            glassColors.highlight,
            glassColors.border.copy(alpha = glassColors.border.alpha * 0.3f)
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = glassColors.shadow,
                spotColor = glassColors.shadow
            )
            .clip(shape)
            .background(glassBackground)
            .background(tint)
            .border(borderWidth, borderBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 0.5.dp,
    tint: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = JustLiftTheme.glassColors

    val glassBackground = Brush.verticalGradient(
        colors = listOf(
            glassColors.surface.copy(alpha = 0.6f),
            glassColors.surface.copy(alpha = 0.4f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassBackground)
            .background(tint)
            .border(borderWidth, glassColors.border, shape),
        content = content
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    glowColor: Color = JustLiftTheme.glassColors.glowPrimary,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = JustLiftTheme.glassColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "button_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.6f else 0f,
        animationSpec = tween(150),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = glowAlpha),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
                    )
                }
            }
            .shadow(if (enabled) 8.dp else 2.dp, shape, ambientColor = glassColors.shadow, spotColor = glassColors.shadow)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        glassColors.surface.copy(alpha = 0.9f),
                        glassColors.surface.copy(alpha = 0.7f)
                    )
                )
            )
            .border(1.dp, glassColors.highlight, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        content = content
    )
}

@Composable
fun Modifier.pulsingGlow(
    color: Color = JustLiftTheme.glassColors.glowPrimary,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    return this.drawBehind {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = size.minDimension * 0.6f
        )
    }
}

@Composable
fun Modifier.animatedGradientBorder(
    borderWidth: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: List<Color> = listOf(
        JustLiftTheme.glassColors.accentPink,
        JustLiftTheme.glassColors.accentPurple,
        JustLiftTheme.glassColors.accentCyan,
        JustLiftTheme.glassColors.accentPink
    ),
    animationDuration: Int = 3000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient_border")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_angle"
    )

    return this.drawWithContent {
        drawContent()
        val brush = Brush.sweepGradient(
            colors = colors,
            center = Offset(size.width / 2, size.height / 2)
        )
        drawRoundRect(
            brush = brush,
            style = androidx.compose.ui.graphics.drawscope.Stroke(borderWidth.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
        )
    }
}

@Composable
fun Modifier.shimmerEffect(
    enabled: Boolean = true,
    colors: List<Color> = listOf(
        Color.Transparent,
        Color.White.copy(alpha = 0.2f),
        Color.Transparent
    )
): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )

    return this.drawWithContent {
        drawContent()
        val shimmerWidth = size.width * 0.5f
        val startX = -shimmerWidth + (size.width + shimmerWidth * 2) * progress

        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(startX, 0f),
                end = Offset(startX + shimmerWidth, size.height)
            )
        )
    }
}

@Composable
fun Modifier.floatingAnimation(
    enabled: Boolean = true,
    amplitude: Float = 8f,
    duration: Int = 3000
): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -amplitude,
        targetValue = amplitude,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    return this.graphicsLayer {
        translationY = offsetY
    }
}

@Composable
fun GradientIconContainer(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = CircleShape,
    colors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val defaultColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(colors ?: defaultColors)
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun ValueChip(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        containerColor,
                        containerColor.copy(alpha = 0.7f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GlassDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0f),
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0f)
                    )
                )
            )
    )
}

@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    shape: Shape = RoundedCornerShape(8.dp),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
