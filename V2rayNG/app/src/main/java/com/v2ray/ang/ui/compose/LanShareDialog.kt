package com.v2ray.ang.ui.compose

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.QRCodeDecoder
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Finds the local IP address assigned to this phone on the active Wi-Fi / Hotspot interface.
 */
private fun findLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "192.168.43.1"
        var fallbackIp: String? = null
        for (networkInterface in interfaces) {
            if (!networkInterface.isUp || networkInterface.isLoopback) continue
            val name = networkInterface.name.lowercase()
            val isHotspotOrWifi = name.contains("wlan") || name.contains("ap") || name.contains("swlan") || name.contains("rndis")
            val addresses = networkInterface.inetAddresses
            for (address in addresses) {
                if (!address.isLoopbackAddress && address is Inet4Address) {
                    val host = address.hostAddress ?: continue
                    if (isHotspotOrWifi) {
                        return host
                    }
                    if (!host.startsWith("127.")) {
                        fallbackIp = host
                    }
                }
            }
        }
        if (fallbackIp != null) return fallbackIp
    } catch (_: Exception) {}
    return "192.168.43.1"
}

@Composable
fun LanShareDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isSharingEnabled by remember {
        mutableStateOf(MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING, false))
    }
    var localIp by remember { mutableStateOf("192.168.43.1") }
    val httpPort = remember { SettingsManager.getHttpPort() }
    val socksPort = remember { SettingsManager.getSocksPort() }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(isSharingEnabled) {
        if (isSharingEnabled) {
            val detectedIp = withContext(Dispatchers.IO) {
                findLocalIpAddress()
            }
            localIp = detectedIp

            // Generate QR code for http proxy info
            val proxyUrl = "http://$detectedIp:$httpPort"
            val qr = withContext(Dispatchers.Default) {
                QRCodeDecoder.createQRCode(proxyUrl, 500)
            }
            qrBitmap = qr
        }
    }

    val isDark = LocalDarkTheme.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, if (isDark) Color(0xFF28314A) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp)),
            color = if (isDark) Color(0xFF121624) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isSharingEnabled) colorFabActive.copy(alpha = 0.15f) else (if (isDark) Color(0xFF1F263B) else Color(0xFFF1F5F9))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (isSharingEnabled) colorFabActive else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.lan_share_title),
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.lan_share_desc),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Switch Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSharingEnabled) colorFabActive.copy(alpha = 0.12f) else (if (isDark) Color(0xFF1B2236) else Color(0xFFF8FAFC)))
                        .border(
                            1.dp,
                            if (isSharingEnabled) colorFabActive.copy(alpha = 0.45f) else (if (isDark) Color(0xFF2C3754) else Color(0xFFE2E8F0)),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.lan_share_switch_title),
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(
                                if (isSharingEnabled) R.string.lan_share_switch_summary_on
                                else R.string.lan_share_switch_summary_off
                            ),
                            color = if (isSharingEnabled) colorFabActive else (if (isDark) Color(0xFF8E9AB4) else Color(0xFF64748B)),
                            fontSize = 11.5.sp
                        )
                    }

                    Switch(
                        checked = isSharingEnabled,
                        onCheckedChange = { enabled ->
                            isSharingEnabled = enabled
                            MmkvManager.encodeSettings(AppConfig.PREF_PROXY_SHARING, enabled)
                            if (enabled) {
                                MmkvManager.encodeSettings(AppConfig.PREF_ENABLE_LOCAL_PROXY, true)
                            }
                            if (CoreServiceManager.isRunning()) {
                                LauncherManager.restartService(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colorFabActive,
                            uncheckedThumbColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            uncheckedTrackColor = if (isDark) Color(0xFF2C3754) else Color(0xFFCBD5E1)
                        )
                    )
                }

                if (isSharingEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Copyable Row: IP Address
                    ShareCopyRow(
                        label = "IP Address",
                        value = localIp,
                        onCopy = {
                            Utils.setClipboard(context, localIp)
                            Toast.makeText(context, context.getString(R.string.lan_share_copied), Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Copyable Row: HTTP Proxy
                    ShareCopyRow(
                        label = "HTTP Proxy",
                        value = "$localIp:$httpPort",
                        subValue = "Port: $httpPort",
                        onCopy = {
                            Utils.setClipboard(context, "$localIp:$httpPort")
                            Toast.makeText(context, context.getString(R.string.lan_share_copied), Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Copyable Row: SOCKS5 Proxy
                    ShareCopyRow(
                        label = "SOCKS5 Proxy",
                        value = "$localIp:$socksPort",
                        subValue = "Port: $socksPort",
                        onCopy = {
                            Utils.setClipboard(context, "$localIp:$socksPort")
                            Toast.makeText(context, context.getString(R.string.lan_share_copied), Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Code
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF161C2C) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDark) Color(0xFF26324A) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.lan_share_disabled_hint),
                            color = if (isDark) Color(0xFF8E9AB4) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = colorFabActive.copy(alpha = 0.15f),
                        contentColor = colorFabActive
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tasker_setting_confirm),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareCopyRow(
    label: String,
    value: String,
    subValue: String? = null,
    onCopy: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF1B2236) else Color(0xFFF8FAFC))
            .border(1.dp, if (isDark) Color(0xFF2C3754) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clickable { onCopy() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (isDark) Color(0xFF8E9AB4) else Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF27314E) else Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_copy),
                contentDescription = "Copy",
                modifier = Modifier.size(16.dp),
                tint = colorFabActive
            )
        }
    }
}
