package com.v2ray.ang.ui.compose

import android.text.format.Formatter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.handler.AppUpdateInfo
import com.v2ray.ang.handler.AppUpdateManager
import com.v2ray.ang.handler.UpdateDownloadProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun InAppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalDarkTheme.current
    val downloadProgress by AppUpdateManager.downloadProgress.collectAsStateWithLifecycle()
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    val isDownloading = downloadProgress is UpdateDownloadProgress.Downloading
    val isCompleted = downloadProgress is UpdateDownloadProgress.Completed || downloadedApk != null

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        AppUpdateManager.resetDownloadProgress()
        errorMessage = null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "updateSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate) {
                cancelDownload()
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate,
            dismissOnClickOutside = !updateInfo.isForceUpdate,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF161922) else Color(0xFFFFFFFF),
            border = BorderStroke(1.2.dp, if (isDark) Color(0xFF2A2E3D) else Color(0xFFE2E8F0)),
            shadowElevation = 16.dp,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon with emerald glowing ring
                val iconBg = if (isDark) Color(0xFF0C2B1D) else Color(0xFFD1FAE5)
                val iconBorder = if (isDark) colorFabActive.copy(alpha = 0.5f) else Color(0xFF10B981)
                val iconTint = if (isDark) colorFabActive else Color(0xFF047857)

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(iconBg)
                        .border(1.5.dp, iconBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isCompleted) R.drawable.ic_action_done
                            else if (isDownloading) R.drawable.ic_cloud_download_24dp
                            else R.drawable.ic_check_update_24dp
                        ),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .size(28.dp)
                            .then(if (isDownloading) Modifier.rotate(spinAngle) else Modifier)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = stringResource(R.string.update_new_version_found, updateInfo.latestVersion ?: ""),
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version upgrade path: Current vX.X.X ➔ New vY.Y.Y
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E2230) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isDark) Color(0xFF2E3448) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF8F94A6) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "➔",
                        fontSize = 11.sp,
                        color = if (isDark) colorFabActive else Color(0xFF047857)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "v${updateInfo.latestVersion ?: ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) colorFabActive else Color(0xFF047857)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Changelog Card or Feature Highlight Banner
                if (!updateInfo.releaseNotes.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF1E2230) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF2E3448) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_description_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isDark) colorFabActive else Color(0xFF047857)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تغییرات نسخه جدید",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val scroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .verticalScroll(scroll)
                        ) {
                            Text(
                                text = updateInfo.releaseNotes,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1A1F2C) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF282F42) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_update_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isDark) colorFabActive else Color(0xFF047857)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بهبود عملکرد، پایداری اتصالات و ارتقای رابط کاربری",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Download Progress
                when (val progress = downloadProgress) {
                    is UpdateDownloadProgress.Downloading -> {
                        val animatedPercent by animateFloatAsState(
                            targetValue = (progress.percent / 100f).coerceIn(0f, 1f),
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            label = "updateProgress"
                        )
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Custom glossy progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isDark) Color(0xFF232738) else Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedPercent)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    colorFabActive.copy(alpha = 0.85f),
                                                    colorFabActive
                                                )
                                            )
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentStr = Formatter.formatFileSize(context, progress.bytesDownloaded)
                                val totalStr = if (progress.totalBytes > 0) Formatter.formatFileSize(context, progress.totalBytes) else ""
                                Text(
                                    text = if (totalStr.isNotEmpty()) "$currentStr / $totalStr" else currentStr,
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${progress.percent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) colorFabActive else Color(0xFF047857),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is UpdateDownloadProgress.Failed -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = progress.error.ifEmpty { "دانلود ناموفق بود" },
                                fontSize = 11.5.sp,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    else -> {}
                }

                if (errorMessage != null && downloadProgress !is UpdateDownloadProgress.Failed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "خطا در دریافت فایل",
                            fontSize = 11.5.sp,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (updateInfo.downloadUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF262117) else Color(0xFFFEF3C7))
                            .border(1.dp, if (isDark) Color(0xFF4D3D1E) else Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "فایل APK هنوز به این ریلیز پیوست نشده است",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel / Later Button
                    if (!updateInfo.isForceUpdate) {
                        OutlinedButton(
                            onClick = {
                                cancelDownload()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF333748) else Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Main Action Button
                    Button(
                        onClick = {
                            if (updateInfo.downloadUrl.isNullOrEmpty()) {
                                val url = updateInfo.releaseUrl ?: "https://github.com/ffr3nz0/ki-client/releases"
                                com.v2ray.ang.util.Utils.openUri(context, url)
                                return@Button
                            }
                            val apk = downloadedApk ?: (downloadProgress as? UpdateDownloadProgress.Completed)?.apkFile
                            if (apk != null && apk.exists()) {
                                if (!AppUpdateManager.canInstallPackages(context)) {
                                    AppUpdateManager.requestInstallPermission(context)
                                } else {
                                    AppUpdateManager.installApk(context, apk)
                                }
                            } else {
                                updateInfo.downloadUrl?.let { url ->
                                    errorMessage = null
                                    downloadJob = scope.launch {
                                        try {
                                            val file = AppUpdateManager.downloadApk(
                                                context = context,
                                                downloadUrl = url,
                                                versionName = updateInfo.latestVersion ?: "latest"
                                            )
                                            downloadedApk = file
                                            if (!AppUpdateManager.canInstallPackages(context)) {
                                                AppUpdateManager.requestInstallPermission(context)
                                            } else {
                                                AppUpdateManager.installApk(context, file)
                                            }
                                        } catch (e: Exception) {
                                            if (e !is kotlinx.coroutines.CancellationException) {
                                                errorMessage = e.message
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorFabActive,
                            contentColor = Color.Black,
                            disabledContainerColor = if (isDark) Color(0xFF13281E) else Color(0xFFD1FAE5),
                            disabledContentColor = if (isDark) colorFabActive else Color(0xFF047857)
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = if (isDark) colorFabActive else Color(0xFF047857),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_downloading),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isCompleted) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_done),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_install_now),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (updateInfo.downloadUrl.isNullOrEmpty()) {
                            Icon(
                                painter = painterResource(R.drawable.ic_github_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "گیت‌هاب",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_cloud_download_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.update_now),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
