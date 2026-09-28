package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.colorFabActive
import com.v2ray.ang.ui.compose.colorFabInactiveDark
import com.v2ray.ang.ui.compose.colorFabInactiveLight

@Composable
internal fun MainBottomBar(
    displayText: String,
    isRunning: Boolean,
    isDarkTheme: Boolean,
    selectedServer: ServerRowUiModel? = null,
    onAction: (MainAction) -> Unit
) {
    val targetColor = if (isRunning) colorFabActive
    else if (isDarkTheme) colorFabInactiveDark
    else colorFabInactiveLight

    val animatedFabColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "fabContainerColor"
    )

    val dockBorderColor by animateColorAsState(
        targetValue = if (isRunning) colorFabActive.copy(alpha = 0.4f)
        else if (isDarkTheme) Color(0xFF2C2C35)
        else Color(0xFFE5E5EA),
        animationSpec = tween(350),
        label = "dockBorderColor"
    )

    // Infinite transitions for active animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")

    // Radar pulse wave
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Status LED breathing pulse
    val ledBreathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ledPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    onClick = { onAction(MainAction.TestCurrentServer) }
                ),
            shape = RoundedCornerShape(22.dp),
            color = if (isDarkTheme) Color(0xFF191A20) else Color(0xFFF7F7F9),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.2.dp, dockBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Section
                val statusLines = displayText.split("\n")
                val primaryStatus = statusLines.firstOrNull().orEmpty()
                val secondaryStatus = statusLines.getOrNull(1)?.ifBlank { null }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                        .semantics {
                            contentDescription = displayText
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Glowing LED status dot
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRunning) colorFabActive.copy(alpha = ledBreathingAlpha)
                                    else Color(0xFF8E8E93)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        AnimatedContent(
                            targetState = primaryStatus,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                            },
                            label = "statusTextAnim"
                        ) { text ->
                            Text(
                                text = text.ifEmpty {
                                    if (isRunning) stringResource(R.string.connection_connected).substringBefore("\n")
                                    else stringResource(R.string.connection_not_connected)
                                },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = if (isRunning) colorFabActive else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isRunning) {
                            secondaryStatus ?: stringResource(R.string.connection_test_pending)
                        } else {
                            selectedServer?.remarks?.ifBlank { null }
                                ?: stringResource(R.string.title_file_chooser)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Connection Controller Button with Pulse Effect
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    if (isRunning) {
                        // Expanding Pulse Ring
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .border(1.8.dp, colorFabActive.copy(alpha = pulseAlpha), CircleShape)
                        )
                    }

                    // Main Power Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(animatedFabColor)
                            .clickable(
                                onClick = { onAction(MainAction.ToggleService) }
                            )
                            .semantics {
                                role = Role.Button
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isRunning,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, delayMillis = 40)) +
                                        scaleIn(initialScale = 0.7f, animationSpec = tween(220, delayMillis = 40)))
                                    .togetherWith(fadeOut(animationSpec = tween(90)) +
                                            scaleOut(targetScale = 0.7f, animationSpec = tween(90)))
                            },
                            label = "fabIconAnim"
                        ) { running ->
                            Icon(
                                painter = if (running) painterResource(R.drawable.ic_stop_24dp)
                                else painterResource(R.drawable.ic_play_24dp),
                                contentDescription = stringResource(
                                    if (running) R.string.acc_stop else R.string.acc_start
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

