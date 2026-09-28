package com.v2ray.ang.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R

/**
 * Standard Apple iOS 26 Squircle Corner Radii ("Rounder corners, more space")
 */
val IosSquircleCornerLarge = RoundedCornerShape(28.dp)
val IosSquircleCornerMedium = RoundedCornerShape(20.dp)
val IosSquircleCornerSmall = RoundedCornerShape(14.dp)
val IosCapsuleShape = RoundedCornerShape(50)
val IosCircleShape = CircleShape

val LocalLiquidGlassEnabled = compositionLocalOf { true }
val LocalLiquidGlassIntensity = compositionLocalOf { "standard" }

/**
 * Resolves intensity factor (0.65 for subtle, 1.0 for standard, 1.45 for high)
 */
@Composable
fun resolveIntensityMultiplier(intensity: String = LocalLiquidGlassIntensity.current): Float {
    return when (intensity.lowercase()) {
        "subtle" -> 0.65f
        "high" -> 1.45f
        else -> 1.0f
    }
}

/**
 * Tactile iOS press animation (spring scale down on touch)
 */
@Composable
fun rememberIosPressInteraction(
    targetScale: Float = 0.93f
): Pair<Modifier, Float> {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iosPressScale"
    )

    val modifier = Modifier.pointerInput(Unit) {
        while (true) {
            awaitPointerEventScope {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
    }
    return modifier to scale
}

/**
 * Full-screen iOS 26 Liquid Glass Fluid Atmospheric Wallpaper.
 * Matches Apple iOS 26 design with deep obsidian base, royal blue fluid waves,
 * electric indigo ambient light, and emerald backlight when connected.
 */
@Composable
fun IosLiquidWallpaper(
    modifier: Modifier = Modifier,
    isGlassEnabled: Boolean = LocalLiquidGlassEnabled.current,
    isConnected: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    if (!isGlassEnabled) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            content = content
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050813),
                        Color(0xFF080C1E),
                        Color(0xFF0A0F26),
                        Color(0xFF050711)
                    )
                )
            )
    ) {
        // Soft diffuse cosmic nebulae (no harsh shapes or ribbons)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Top-right radiant electric sapphire / cyan nebula
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2563EB).copy(alpha = 0.32f),
                        Color(0xFF1D4ED8).copy(alpha = 0.16f),
                        Color(0xFF0284C7).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.95f, h * 0.14f),
                    radius = w * 0.90f
                ),
                center = Offset(w * 0.95f, h * 0.14f),
                radius = w * 0.90f
            )

            // 2. Right-center atmospheric violet/magenta aurora plume
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.24f),
                        Color(0xFF7C3AED).copy(alpha = 0.14f),
                        Color(0xFFA855F7).copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.90f, h * 0.42f),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.90f, h * 0.42f),
                radius = w * 0.75f
            )

            // 3. Center-left deep indigo / cosmic blue nebula
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4F46E5).copy(alpha = 0.25f),
                        Color(0xFF3B82F6).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.05f, h * 0.52f),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.05f, h * 0.52f),
                radius = w * 0.85f
            )

            // 4. Bottom ambient glow
            val bottomGlowColor = if (isConnected) Color(0xFF10B981) else Color(0xFF4F46E5)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bottomGlowColor.copy(alpha = if (isConnected) 0.22f else 0.15f),
                        bottomGlowColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.50f, h * 0.96f),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.50f, h * 0.96f),
                radius = w * 0.85f
            )
        }

        content()
    }
}

/**
 * Liquid Glass Card Composable with Apple continuous squircle and specular rim
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = IosSquircleCornerLarge,
    isActive: Boolean = false,
    activeAccentColor: Color = Color(0xFF34C759),
    isGlassEnabled: Boolean = LocalLiquidGlassEnabled.current,
    intensity: String = LocalLiquidGlassIntensity.current,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val factor = resolveIntensityMultiplier(intensity)

    if (isGlassEnabled) {
        val glassBgBrush = if (isActive) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.16f * factor).coerceIn(0.10f, 0.30f)),
                    Color.White.copy(alpha = (0.10f * factor).coerceIn(0.06f, 0.22f)),
                    Color(0xFF34C759).copy(alpha = (0.08f * factor).coerceIn(0.04f, 0.16f)),
                    Color.White.copy(alpha = (0.07f * factor).coerceIn(0.04f, 0.18f))
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.13f * factor).coerceIn(0.08f, 0.24f)),
                    Color.White.copy(alpha = (0.07f * factor).coerceIn(0.04f, 0.16f)),
                    Color.White.copy(alpha = (0.04f * factor).coerceIn(0.02f, 0.12f)),
                    Color.White.copy(alpha = (0.06f * factor).coerceIn(0.03f, 0.15f))
                )
            )
        }

        val rimBorderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (if (isActive) 0.52f else 0.40f) * factor),
                Color.White.copy(alpha = (if (isActive) 0.22f else 0.14f) * factor),
                Color.Transparent,
                Color.White.copy(alpha = (if (isActive) 0.30f else 0.20f) * factor)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )

        val borderWidth = 1.dp

        var baseModifier = modifier
            .shadow(
                elevation = if (isActive) 16.dp else 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.40f)
            )
            .clip(shape)
            .background(glassBgBrush, shape)
            .border(borderWidth, rimBorderBrush, shape)
            .drawWithContent {
                drawContent()
                // Top-edge horizontal specular refraction sheen
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (0.16f * factor).coerceAtMost(0.35f)),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 32f
                    )
                )
            }

        if (onClick != null) {
            baseModifier = baseModifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        }

        Box(
            modifier = baseModifier,
            content = content
        )
    } else {
        // Fallback solid surface
        val solidBg = if (isActive) Color(0xFF182234) else Color(0xFF181C26)
        val solidBorder = Color.White.copy(alpha = if (isActive) 0.22f else 0.12f)

        var baseModifier = modifier
            .clip(shape)
            .background(solidBg, shape)
            .border(1.dp, solidBorder, shape)

        if (onClick != null) {
            baseModifier = baseModifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        }

        Box(
            modifier = baseModifier,
            content = content
        )
    }
}

/**
 * Liquid Glass Pill / Badge Modifier
 */
@Composable
fun Modifier.liquidGlassPill(
    shape: Shape = IosCapsuleShape,
    borderWidth: Dp = 1.dp,
    isActive: Boolean = false,
    activeColor: Color = Color(0xFF34C759)
): Modifier {
    val isGlassEnabled = LocalLiquidGlassEnabled.current
    val factor = resolveIntensityMultiplier(LocalLiquidGlassIntensity.current)

    return if (isGlassEnabled) {
        val bgBrush = if (isActive) {
            Brush.verticalGradient(
                listOf(
                    activeColor.copy(alpha = 0.28f * factor),
                    activeColor.copy(alpha = 0.14f * factor)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.16f * factor),
                    Color.White.copy(alpha = 0.07f * factor)
                )
            )
        }

        val borderBrush = if (isActive) {
            Brush.linearGradient(
                listOf(
                    activeColor.copy(alpha = 0.80f),
                    Color.White.copy(alpha = 0.45f),
                    activeColor.copy(alpha = 0.50f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.38f * factor),
                    Color.White.copy(alpha = 0.10f * factor),
                    Color.Transparent
                )
            )
        }

        this
            .clip(shape)
            .background(bgBrush, shape)
            .border(borderWidth, borderBrush, shape)
    } else {
        val bg = if (isActive) activeColor.copy(alpha = 0.15f) else Color(0xFF1E212D)
        val border = if (isActive) activeColor.copy(alpha = 0.5f) else Color(0xFF2C3244)
        this
            .clip(shape)
            .background(bg, shape)
            .border(borderWidth, border, shape)
    }
}

/**
 * Apple iOS 26 3D Liquid Lens Power Button.
 * Features 3D glossy convex highlight, neon aura, spring haptic bounce,
 * and pure white icon in all states.
 */
@Composable
fun IosLiquidPowerButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 62.dp
) {
    val (pressModifier, pressScale) = rememberIosPressInteraction(targetScale = 0.91f)

    val infiniteTransition = rememberInfiniteTransition(label = "powerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "powerPulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "powerPulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size + 10.dp)
    ) {
        // Halo beacon when active
        if (isRunning) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF34C759).copy(alpha = pulseAlpha), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(size)
                .scale(pressScale)
                .then(pressModifier)
                .shadow(
                    elevation = if (isRunning) 16.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = if (isRunning) Color(0xFF34C759).copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.35f),
                    spotColor = if (isRunning) Color(0xFF34C759).copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .background(
                    if (isRunning) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF34C759),
                                Color(0xFF28CD41),
                                Color(0xFF1E8E38)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.07f)
                            )
                        )
                    }
                )
                .border(
                    width = 1.2.dp,
                    brush = if (isRunning) {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.85f),
                                Color(0xFF34C759).copy(alpha = 0.5f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        )
                    },
                    shape = CircleShape
                )
                .drawWithContent {
                    drawContent()
                    // Top 3D glossy lens crescent reflection
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isRunning) 0.45f else 0.28f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = this.size.height * 0.55f
                        ),
                        radius = this.size.width * 0.46f,
                        center = Offset(this.size.width * 0.5f, this.size.height * 0.38f)
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_power_24dp),
                contentDescription = stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start),
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
