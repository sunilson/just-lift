package at.sunilson.justlift.shared.presentation.animations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset

/**
 * Premium animation specifications for award-winning UI/UX.
 * Uses spring physics for natural, fluid motion.
 */
object JustLiftAnimations {

    // ==========================================
    // SPRING SPECIFICATIONS
    // ==========================================

    val bouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val snappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val smoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val gentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow
    )

    val offsetSpring = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    // ==========================================
    // ENTER TRANSITIONS
    // ==========================================

    fun glassEnter(
        durationMillis: Int = 400,
        delayMillis: Int = 0
    ): EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun slideUpEnter(
        initialOffsetY: Int = 100,
        durationMillis: Int = 500,
        delayMillis: Int = 0
    ): EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    ) + slideInVertically(
        initialOffsetY = { initialOffsetY },
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun slideDownEnter(
        initialOffsetY: Int = -100,
        durationMillis: Int = 500,
        delayMillis: Int = 0
    ): EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    ) + slideInVertically(
        initialOffsetY = { initialOffsetY },
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun slideFromLeftEnter(
        initialOffsetX: Int = -200,
        durationMillis: Int = 400,
        delayMillis: Int = 0
    ): EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    ) + slideInHorizontally(
        initialOffsetX = { initialOffsetX },
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun slideFromRightEnter(
        initialOffsetX: Int = 200,
        durationMillis: Int = 400,
        delayMillis: Int = 0
    ): EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    ) + slideInHorizontally(
        initialOffsetX = { initialOffsetX },
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun expandEnter(
        durationMillis: Int = 400,
        delayMillis: Int = 0
    ): EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    ) + expandVertically(
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        )
    )

    // ==========================================
    // EXIT TRANSITIONS
    // ==========================================

    fun glassExit(
        durationMillis: Int = 300
    ): ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    ) + scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun slideDownExit(
        targetOffsetY: Int = 100,
        durationMillis: Int = 300
    ): ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    ) + slideOutVertically(
        targetOffsetY = { targetOffsetY },
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun slideUpExit(
        targetOffsetY: Int = -100,
        durationMillis: Int = 300
    ): ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    ) + slideOutVertically(
        targetOffsetY = { targetOffsetY },
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun slideToLeftExit(
        targetOffsetX: Int = -200,
        durationMillis: Int = 300
    ): ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    ) + slideOutHorizontally(
        targetOffsetX = { targetOffsetX },
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun slideToRightExit(
        targetOffsetX: Int = 200,
        durationMillis: Int = 300
    ): ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    ) + slideOutHorizontally(
        targetOffsetX = { targetOffsetX },
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    )

    fun shrinkExit(
        durationMillis: Int = 300
    ): ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    ) + shrinkVertically(
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    )

    // ==========================================
    // STAGGERED ANIMATION HELPERS
    // ==========================================

    fun staggeredDelay(
        index: Int,
        baseDelay: Int = 100,
        staggerDelay: Int = 50
    ): Int = baseDelay + (index * staggerDelay)

    // ==========================================
    // DURATION CONSTANTS
    // ==========================================

    object Duration {
        const val INSTANT = 100
        const val FAST = 200
        const val MEDIUM = 350
        const val SLOW = 500
        const val VERY_SLOW = 800
    }

    object Delay {
        const val NONE = 0
        const val SHORT = 100
        const val MEDIUM = 200
        const val LONG = 400
    }
}
