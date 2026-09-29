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
 * Finds the local IPv4 address of this device (e.g. Wi-Fi IP or Hotspot IP).
 */
private fun findLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        var fallbackIp: String? = null
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            if (iface.isLoopback || !iface.isUp) continue
            val addresses = iface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val host = addr.hostAddress.orEmpty()
                    if (host.startsWith("192.168.43.")) {
                        // Standard Android hotspot IP
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
    var localIp by remember { mutableStateOf("192.168.43.1") }
    val httpPort = remember { SettingsManager.getHttpPort() }
    val socksPort = remember { SettingsManager.getSocksPort() }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        // Automatically enable Allow LAN so local inbound binds to 0.0.0.0
        val previouslyEnabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING, false)
        MmkvManager.encodeSettings(AppConfig.PREF_PROXY_SHARING, true)
        MmkvManager.encodeSettings(AppConfig.PREF_ENABLE_LOCAL_PROXY, true)
        if (!previouslyEnabled) {
            LauncherManager.restartService(context)
        }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF28314A), RoundedCornerShape(24.dp)),
            color = Color(0xFF121624),
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
                        .background(colorFabActive.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = colorFabActive
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.lan_share_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.lan_share_desc),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B2236))
            .border(1.dp, Color(0xFF2C3754), RoundedCornerShape(12.dp))
            .clickable { onCopy() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color(0xFF8E9AB4),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Color.White,
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
                .background(Color(0xFF27314E)),
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
