package com.v2ray.ang.ui.compose

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.handler.AppUpdateInfo
import com.v2ray.ang.handler.AppUpdateManager
import com.v2ray.ang.handler.UpdateDownloadProgress
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun InAppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadProgress by AppUpdateManager.downloadProgress.collectAsStateWithLifecycle()
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isDownloading = downloadProgress is UpdateDownloadProgress.Downloading
    val isCompleted = downloadProgress is UpdateDownloadProgress.Completed || downloadedApk != null

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate && !isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate && !isDownloading,
            dismissOnClickOutside = !updateInfo.isForceUpdate && !isDownloading
        )
    ) {
        LiquidGlassCard(
            shape = IosSquircleCornerLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon with Glass Ring
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(colorFabActive.copy(alpha = 0.25f), Color(0x1000E676))
                            )
                        )
                        .border(1.dp, colorFabActive.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_update_24dp),
                        contentDescription = null,
                        tint = colorFabActive,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = stringResource(R.string.update_new_version_found, updateInfo.latestVersion ?: ""),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Version Badge Pill
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .liquidGlassPill(isActive = true, activeColor = colorFabActive)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v${updateInfo.latestVersion}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorFabActive
                    )
                }

                // Changelog Card
                if (!updateInfo.releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(IosSquircleCornerMedium)
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x22FFFFFF), IosSquircleCornerMedium)
                            .padding(12.dp)
                    ) {
                        val scroll = rememberScrollState()
                        Text(
                            text = updateInfo.releaseNotes,
                            fontSize = 13.sp,
                            color = Color(0xFFD4D4D8),
                            lineHeight = 19.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scroll)
                                .verticalScrollbar(scroll)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Download Progress / Status
                when (val progress = downloadProgress) {
                    is UpdateDownloadProgress.Downloading -> {
                        val animatedPercent by animateFloatAsState(
                            targetValue = progress.percent / 100f,
                            label = "updateProgress"
                        )
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { animatedPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(IosCapsuleShape),
                                color = colorFabActive,
                                trackColor = Color(0x33FFFFFF)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentStr = Formatter.formatFileSize(context, progress.bytesDownloaded)
                                val totalStr = if (progress.totalBytes > 0) Formatter.formatFileSize(context, progress.totalBytes) else ""
                                Text(
                                    text = if (totalStr.isNotEmpty()) "$currentStr / $totalStr" else currentStr,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${progress.percent}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorFabActive
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is UpdateDownloadProgress.Failed -> {
                        Text(
                            text = progress.error,
                            fontSize = 12.sp,
                            color = Color(0xFFFF5252),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    else -> {}
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!updateInfo.isForceUpdate && !isDownloading) {
                        OutlinedButton(
                            onClick = {
                                AppUpdateManager.resetDownloadProgress()
                                onDismiss()
                            },
                            shape = IosSquircleCornerMedium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }

                    Button(
                        onClick = {
                            val apk = downloadedApk ?: (downloadProgress as? UpdateDownloadProgress.Completed)?.apkFile
                            if (apk != null && apk.exists()) {
                                if (!AppUpdateManager.canInstallPackages(context)) {
                                    AppUpdateManager.requestInstallPermission(context)
                                } else {
                                    AppUpdateManager.installApk(context, apk)
                                }
                            } else {
                                updateInfo.downloadUrl?.let { url ->
                                    scope.launch {
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
                                            errorMessage = e.message
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading && !updateInfo.downloadUrl.isNullOrEmpty(),
                        shape = IosSquircleCornerMedium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorFabActive,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_downloading), fontWeight = FontWeight.Bold)
                        } else if (isCompleted) {
                            Text(stringResource(R.string.action_install_now), fontWeight = FontWeight.Bold)
                        } else {
                            Text(stringResource(R.string.update_now), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
