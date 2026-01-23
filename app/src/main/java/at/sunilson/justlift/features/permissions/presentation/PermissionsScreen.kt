package at.sunilson.justlift.features.permissions.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.shared.presentation.animations.JustLiftAnimations
import at.sunilson.justlift.shared.presentation.components.AnimatedMeshGradientBackground
import at.sunilson.justlift.shared.presentation.components.GlassButton
import at.sunilson.justlift.shared.presentation.components.GlassCard
import at.sunilson.justlift.shared.presentation.components.GlassSurface
import at.sunilson.justlift.shared.presentation.components.floatingAnimation
import at.sunilson.justlift.shared.presentation.components.pulsingGlow
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import kotlinx.coroutines.delay

@Composable
fun PermissionsScreen(
    bluetoothPermissionGiven: Boolean,
    onRequestPermission: () -> Unit,
) {
    var animationStarted by remember { mutableStateOf(false) }
    val extendedColors = JustLiftTheme.extendedColors

    LaunchedEffect(Unit) {
        delay(100)
        animationStarted = true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Animated mesh gradient background
        AnimatedMeshGradientBackground(
            modifier = Modifier.fillMaxSize(),
            animationDuration = 10000
        )

        // Dark overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.12f))

            // Animated logo with floating effect and glow
            AnimatedVisibility(
                visible = animationStarted,
                enter = JustLiftAnimations.glassEnter(durationMillis = 600)
            ) {
                Box(
                    modifier = Modifier
                        .floatingAnimation(amplitude = 6f, duration = 4000)
                        .pulsingGlow(
                            color = extendedColors.gradientMiddle.copy(alpha = 0.4f)
                        )
                ) {
                    GlassCard(
                        modifier = Modifier.size(130.dp),
                        shape = CircleShape,
                        elevation = 16.dp,
                        tint = JustLiftTheme.glassColors.tintPrimary
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            extendedColors.gradientStart.copy(alpha = 0.9f),
                                            extendedColors.gradientMiddle.copy(alpha = 0.7f),
                                            extendedColors.gradientEnd.copy(alpha = 0.5f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Title with staggered animation
            AnimatedVisibility(
                visible = animationStarted,
                enter = JustLiftAnimations.slideUpEnter(
                    durationMillis = 500,
                    delayMillis = JustLiftAnimations.staggeredDelay(0, 200, 100)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Just Lift",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Track your lifts. Crush your goals.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Features list with glass cards and staggered animation
            AnimatedVisibility(
                visible = animationStarted,
                enter = JustLiftAnimations.slideUpEnter(
                    durationMillis = 500,
                    delayMillis = JustLiftAnimations.staggeredDelay(1, 200, 100)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassFeatureItem(
                        icon = Icons.Outlined.Speed,
                        title = "Real-time Tracking",
                        description = "Monitor force, reps, and position live",
                        animationDelay = 0
                    )
                    GlassFeatureItem(
                        icon = Icons.Outlined.Timeline,
                        title = "Progress Analytics",
                        description = "See your strength gains over time",
                        animationDelay = 80
                    )
                    GlassFeatureItem(
                        icon = Icons.Filled.Bluetooth,
                        title = "Bluetooth Connected",
                        description = "Seamless sync with your machine",
                        animationDelay = 160
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Permission request section with glass effect
            if (!bluetoothPermissionGiven) {
                AnimatedVisibility(
                    visible = animationStarted,
                    enter = JustLiftAnimations.slideUpEnter(
                        durationMillis = 600,
                        delayMillis = JustLiftAnimations.staggeredDelay(4, 200, 100)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = 12.dp,
                            tint = JustLiftTheme.glassColors.tintSecondary
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bluetooth,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Bluetooth Permission Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "We need Bluetooth to connect to your fitness machine and track your workouts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Premium glass button with gradient
                        GlassButton(
                            onClick = onRequestPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            glowColor = JustLiftTheme.glassColors.glowPrimary
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                JustLiftTheme.extendedColors.gradientStart,
                                                JustLiftTheme.extendedColors.gradientMiddle,
                                                JustLiftTheme.extendedColors.gradientEnd
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bluetooth,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Enable Bluetooth",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GlassFeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    animationDelay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "feature_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "feature_alpha"
    )

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer { this.alpha = alpha },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
