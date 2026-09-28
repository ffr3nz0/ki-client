package com.v2ray.ang.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern stylized Ki App Icon squircle matching user reference images.
 * Features a deep indigo glass squircle housing the 3D folded Ki ribbon symbol.
 */
@Composable
fun KiRibbonLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    hasBackground: Boolean = false
) {
    val cornerRadius = size * 0.28f

    val baseModifier = if (hasBackground) {
        modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color(0xFF818CF8)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1E2548),
                        Color(0xFF111732),
                        Color(0xFF090D1E)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color(0xFF818CF8).copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(size * 0.16f)
    } else {
        modifier.size(size)
    }

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height

            val pillWidth = w * 0.32f
            val pillRadius = CornerRadius(pillWidth / 2f, pillWidth / 2f)

            // 1. Left vertical cyan-to-azure pill
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF00F0FF),
                        Color(0xFF0091FF),
                        Color(0xFF2563EB)
                    )
                ),
                topLeft = Offset(w * 0.02f, h * 0.02f),
                size = Size(pillWidth, h * 0.96f),
                cornerRadius = pillRadius
            )

            // 2. Upper diagonal violet arm
            withTransform({
                rotate(degrees = -38f, pivot = Offset(w * 0.50f, h * 0.40f))
            }) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFA5B4FC),
                            Color(0xFF818CF8),
                            Color(0xFF6366F1)
                        )
                    ),
                    topLeft = Offset(w * 0.44f, h * 0.04f),
                    size = Size(pillWidth * 0.96f, h * 0.55f),
                    cornerRadius = pillRadius
                )
            }

            // 3. Lower diagonal magenta/pink arm
            withTransform({
                rotate(degrees = 38f, pivot = Offset(w * 0.50f, h * 0.60f))
            }) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF818CF8),
                            Color(0xFFC084FC),
                            Color(0xFFE879F9),
                            Color(0xFFF43F5E)
                        )
                    ),
                    topLeft = Offset(w * 0.44f, h * 0.40f),
                    size = Size(pillWidth * 0.96f, h * 0.58f),
                    cornerRadius = pillRadius
                )
            }
        }
    }
}
