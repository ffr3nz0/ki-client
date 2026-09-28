package com.v2ray.ang.ui.subscription

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.ui.compose.colorFabActive
import com.v2ray.ang.ui.compose.liquidGlassPill
import com.v2ray.ang.util.Utils
import java.util.Locale

/**
 * Checks whether the subscription contains valid userinfo or metadata worth displaying.
 */
fun shouldShowUserinfoCard(subscription: SubscriptionItem?): Boolean {
    if (subscription == null) return false
    return subscription.uploadTraffic > 0L ||
            subscription.downloadTraffic > 0L ||
            subscription.totalTraffic > 0L ||
            subscription.expireTime > 0L ||
            !subscription.announceMsg.isNullOrBlank() ||
            !subscription.supportUrl.isNullOrBlank()
}

/**
 * Formats byte values into readable units (B, KB, MB, GB, TB).
 */
fun formatTraffic(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return if (digitGroups == 0) {
        "$bytes B"
    } else {
        String.format(Locale.US, "%.2f %s", value, units[digitGroups])
    }
}

/**
 * A modern, delightful Compose card presenting subscription consumption, quota,
 * remaining expiration time, announcements, and support links.
 */
@Composable
fun SubscriptionUserinfoCard(
    subscription: SubscriptionItem,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAnnounceDialog by remember { mutableStateOf(false) }
    var refreshRotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = refreshRotationAngle,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "refreshSpin"
    )

    val usedTraffic = subscription.uploadTraffic + subscription.downloadTraffic
    val totalTraffic = subscription.totalTraffic
    val hasTotal = totalTraffic > 0L
    val progress = if (hasTotal) {
        (usedTraffic.toFloat() / totalTraffic.toFloat()).coerceIn(0.01f, 1f)
    } else {
        1f
    }
    val percentage = if (hasTotal) {
        ((usedTraffic.toDouble() / totalTraffic.toDouble()) * 100).coerceIn(0.0, 100.0)
    } else {
        0.0
    }

    // Expiration calculation
    val expireTimeSec = subscription.expireTime
    val hasExpiry = expireTimeSec > 0L
    val now = System.currentTimeMillis()
    val expireMillis = expireTimeSec * 1000L
    val isExpired = hasExpiry && expireMillis <= now
    val diffMillis = if (hasExpiry) (expireMillis - now).coerceAtLeast(0L) else 0L
    val daysLeft = diffMillis / (1000L * 60 * 60 * 24)
    val hoursLeft = (diffMillis / (1000L * 60 * 60)) % 24

    val cardBorderBrush = Brush.linearGradient(
        colors = listOf(
            colorFabActive.copy(alpha = 0.50f),
            Color(0xFF00B0FF).copy(alpha = 0.25f),
            Color(0x22FFFFFF)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(BorderStroke(1.2.dp, cardBorderBrush), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF12141D),
        shadowElevation = 4.dp,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ROW 1: Subscription Name + Expiration Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorFabActive.copy(alpha = 0.12f))
                            .border(1.dp, colorFabActive.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_subscriptions_24dp),
                            contentDescription = null,
                            tint = colorFabActive,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = subscription.remarks.ifBlank { stringResource(R.string.title_sub_setting) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subscription.lastUpdated > 0L) {
                            Text(
                                text = Utils.formatTimestamp(subscription.lastUpdated, "MM/dd HH:mm"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8F94A6),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Expiry Badge Pill
                val expiryText = when {
                    !hasExpiry -> stringResource(R.string.sub_info_no_expiry)
                    isExpired -> stringResource(R.string.sub_info_expired)
                    daysLeft > 0 -> stringResource(R.string.sub_info_days_left, daysLeft)
                    else -> stringResource(R.string.sub_info_hours_left, hoursLeft.coerceAtLeast(1))
                }
                val expiryColor = when {
                    !hasExpiry -> colorFabActive
                    isExpired -> Color(0xFFFF5252)
                    daysLeft < 3 -> Color(0xFFFFB74D)
                    else -> colorFabActive
                }
                val expiryBg = expiryColor.copy(alpha = 0.12f)
                val expiryBorder = expiryColor.copy(alpha = 0.35f)

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(expiryBg)
                        .border(1.dp, expiryBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clock_24dp),
                            contentDescription = null,
                            tint = expiryColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = expiryText,
                            color = expiryColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ROW 2: Hero Traffic Stats (Used / Total + Percentage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.sub_info_used),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8F94A6),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatTraffic(usedTraffic),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 22.sp
                        )
                        Text(
                            text = " / ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF555B6E),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = if (hasTotal) formatTraffic(totalTraffic) else stringResource(R.string.sub_info_unlimited),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasTotal) Color(0xFFB0B4C3) else colorFabActive,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                if (hasTotal) {
                    val percentColor = if (percentage > 90.0) Color(0xFFFF5252) else colorFabActive
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(percentColor.copy(alpha = 0.12f))
                            .border(1.dp, percentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", percentage),
                            color = percentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ROW 3: Progress Bar
            val progressBrush = if (hasTotal) {
                Brush.horizontalGradient(
                    colors = listOf(
                        colorFabActive,
                        Color(0xFF00B0FF)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(
                        colorFabActive.copy(alpha = 0.35f),
                        colorFabActive,
                        colorFabActive.copy(alpha = 0.35f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E2230))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(progressBrush)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ROW 4: Upload & Download Breakdown Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Upload Chip
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1200B0FF))
                        .border(1.dp, Color(0x3000B0FF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_upward_24dp),
                            contentDescription = stringResource(R.string.sub_info_upload),
                            tint = Color(0xFF00B0FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.sub_info_upload),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8F94A6),
                                fontSize = 10.sp
                            )
                            Text(
                                text = formatTraffic(subscription.uploadTraffic),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Download Chip
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1200E676))
                        .border(1.dp, Color(0x3000E676), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_downward_24dp),
                            contentDescription = stringResource(R.string.sub_info_download),
                            tint = colorFabActive,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.sub_info_download),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8F94A6),
                                fontSize = 10.sp
                            )
                            Text(
                                text = formatTraffic(subscription.downloadTraffic),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ROW 5: Quick Action Buttons (Support, Notice, Refresh)
            val supportTarget = subscription.supportUrl?.takeIf { it.isNotBlank() }
                ?: subscription.webPageUrl?.takeIf { it.isNotBlank() }
            val announceTarget = subscription.announceMsg?.takeIf { it.isNotBlank() }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (supportTarget != null) {
                    val isTelegram = supportTarget.contains("t.me", ignoreCase = true) ||
                            supportTarget.contains("telegram", ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x1800B0FF),
                        border = BorderStroke(1.dp, Color(0x4000B0FF)),
                        onClick = { launchUrlSafely(context, supportTarget) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(if (isTelegram) R.drawable.ic_telegram_24dp else R.drawable.ic_feedback_24dp),
                                contentDescription = stringResource(R.string.sub_info_support),
                                tint = Color(0xFF00B0FF),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.sub_info_support),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE1F5FE),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (announceTarget != null) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x18FFB300),
                        border = BorderStroke(1.dp, Color(0x45FFB300)),
                        onClick = { showAnnounceDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_promotion_24dp),
                                contentDescription = stringResource(R.string.sub_info_announcement),
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.sub_info_announcement),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFF8E1),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Refresh Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x1800E676),
                    border = BorderStroke(1.dp, colorFabActive.copy(alpha = 0.40f)),
                    onClick = {
                        refreshRotationAngle += 360f
                        onRefresh()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_restore_24dp),
                            contentDescription = stringResource(R.string.sub_info_refresh),
                            tint = colorFabActive,
                            modifier = Modifier
                                .size(15.dp)
                                .rotate(animatedRotation)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.sub_info_refresh),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE8F5E9),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Announcement Dialog
    if (showAnnounceDialog && !subscription.announceMsg.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showAnnounceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_promotion_24dp),
                        contentDescription = null,
                        tint = colorFabActive,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sub_info_announcement),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Text(
                    text = subscription.announceMsg.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFECEFF1),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showAnnounceDialog = false }) {
                    Text(
                        text = stringResource(R.string.action_close),
                        color = colorFabActive,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = Color(0xFF161924),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

/**
 * Safely launches a URL in an external browser or app.
 */
private fun launchUrlSafely(context: Context, url: String) {
    try {
        val parsed = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, parsed).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
