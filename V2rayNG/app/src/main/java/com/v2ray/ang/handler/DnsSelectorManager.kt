package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object DnsSelectorManager {
    private const val TAG = "DnsSelectorManager"
    private var lastScanTimestamp = 0L
    private const val SCAN_COOLDOWN_MS = 3 * 60 * 1000L // 3 minutes cooldown

    data class DnsPreset(
        val id: String,
        val displayName: String,
        val dohUrl: String,
        val primaryIp: String,
        val secondaryIp: String = ""
    )

    // Curated international/foreign DNS resolvers with verified DoH endpoints (over TCP) and pure IPs
    val PRESETS: List<DnsPreset> = listOf(
        DnsPreset(
            id = "cloudflare",
            displayName = "Cloudflare (1.1.1.1 / DoH)",
            dohUrl = "https://cloudflare-dns.com/dns-query",
            primaryIp = "1.1.1.1",
            secondaryIp = "1.0.0.1"
        ),
        DnsPreset(
            id = "google",
            displayName = "Google (8.8.8.8 / DoH)",
            dohUrl = "https://dns.google/dns-query",
            primaryIp = "8.8.8.8",
            secondaryIp = "8.8.4.4"
        ),
        DnsPreset(
            id = "quad9",
            displayName = "Quad9 (9.9.9.9 / Secure)",
            dohUrl = "https://dns.quad9.net/dns-query",
            primaryIp = "9.9.9.9",
            secondaryIp = "149.112.112.112"
        ),
        DnsPreset(
            id = "opendns",
            displayName = "OpenDNS Cisco (208.67.222.222)",
            dohUrl = "https://doh.opendns.com/dns-query",
            primaryIp = "208.67.222.222",
            secondaryIp = "208.67.220.220"
        ),
        DnsPreset(
            id = "adguard",
            displayName = "AdGuard DNS (94.140.14.14)",
            dohUrl = "https://dns.adguard-dns.com/dns-query",
            primaryIp = "94.140.14.14",
            secondaryIp = "94.140.15.15"
        ),
        DnsPreset(
            id = "controld",
            displayName = "Control D (76.76.2.0)",
            dohUrl = "https://freedns.controld.com/p0",
            primaryIp = "76.76.2.0",
            secondaryIp = "76.76.10.0"
        ),
        DnsPreset(
            id = "dnswatch",
            displayName = "DNS.WATCH (84.200.69.80)",
            dohUrl = "84.200.69.80",
            primaryIp = "84.200.69.80",
            secondaryIp = "84.200.70.40"
        ),
        DnsPreset(
            id = "level3",
            displayName = "Level3 / Lumen (4.2.2.4)",
            dohUrl = "4.2.2.4",
            primaryIp = "4.2.2.4",
            secondaryIp = "4.2.2.2"
        ),
        DnsPreset(
            id = "yandex",
            displayName = "Yandex DNS (77.88.8.8)",
            dohUrl = "https://common.dot.dns.yandex.net/dns-query",
            primaryIp = "77.88.8.8",
            secondaryIp = "77.88.8.1"
        )
    )

    fun getPresetById(id: String): DnsPreset? = PRESETS.firstOrNull { it.id == id }

    /**
     * Measures TCP handshake latency to a DNS server.
     * Uses TCP port 443 (DoH port) or port 53 to verify real end-to-end connectivity,
     * avoiding local Iranian ISP UDP 53 DNS poisoning/spoofing.
     * Returns round-trip time in milliseconds, or -1 if unreachable.
     */
    suspend fun benchmarkDns(ip: String, timeoutMs: Int = 1400): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        // 1. Try TCP connect to port 443 (DoH service)
        try {
            Socket().use { tcpSocket ->
                tcpSocket.connect(InetSocketAddress(ip, 443), timeoutMs)
                val elapsed = System.currentTimeMillis() - startTime
                return@withContext elapsed.coerceAtLeast(1L)
            }
        } catch (_: Exception) {
        }

        // 2. Fallback: TCP connect to port 53
        try {
            Socket().use { tcpSocket ->
                tcpSocket.connect(InetSocketAddress(ip, 53), timeoutMs)
                val elapsed = System.currentTimeMillis() - startTime
                return@withContext elapsed.coerceAtLeast(1L)
            }
        } catch (_: Exception) {
            return@withContext -1L
        }
    }

    /**
     * Benchmarks all foreign presets concurrently.
     */
    suspend fun testAllPresets(timeoutMs: Int = 1400): List<Pair<DnsPreset, Long>> = withContext(Dispatchers.IO) {
        PRESETS.map { preset ->
            async {
                val latency = benchmarkDns(preset.primaryIp, timeoutMs)
                preset to latency
            }
        }.awaitAll()
    }

    /**
     * Applies a specific DNS preset to MMKV configurations.
     * Sets remote DNS to the provider's DoH endpoint (over TCP proxy tunnel) and
     * VPN DNS to the pure IP.
     */
    fun applyPreset(context: Context?, preset: DnsPreset) {
        val remoteResolver = if (preset.dohUrl.isNotBlank()) preset.dohUrl else preset.primaryIp
        val vpnIp = preset.primaryIp

        MmkvManager.encodeSettings(AppConfig.PREF_REMOTE_DNS, remoteResolver)
        MmkvManager.encodeSettings(AppConfig.PREF_VPN_DNS, AppConfig.DNS_VPN)
        MmkvManager.encodeSettings(AppConfig.PREF_DOMESTIC_DNS, AppConfig.DNS_DIRECT)
        MmkvManager.encodeSettings(AppConfig.PREF_DNS_SELECTOR_MODE, preset.id)

        LogUtil.i(TAG, "Applied foreign DNS preset '${preset.displayName}': remote=$remoteResolver, vpn=${AppConfig.DNS_VPN}, domestic=${AppConfig.DNS_DIRECT}")

        if (context != null && CoreServiceManager.isRunning()) {
            LauncherManager.restartService(context)
        }
    }

    /**
     * Finds the fastest reachable foreign DNS on the current network and applies it.
     */
    suspend fun autoSelectBestDns(context: Context?): Pair<DnsPreset, Long>? = withContext(Dispatchers.IO) {
        val results = testAllPresets()
        val working = results.filter { it.second > 0L }.sortedBy { it.second }

        val best = working.firstOrNull()
        if (best != null) {
            applyPreset(context, best.first)
            MmkvManager.encodeSettings(AppConfig.PREF_DNS_SELECTOR_MODE, "auto")
            LogUtil.i(TAG, "Auto-selected best foreign DNS '${best.first.displayName}' with ${best.second}ms latency")
        } else {
            LogUtil.w(TAG, "No working foreign DNS server found during auto-select")
        }
        best
    }

    /**
     * Scans foreign DNS presets and applies the fastest one when requested.
     */
    suspend fun scanAndSelectIfDisconnected(context: Context?, force: Boolean = false): Pair<DnsPreset, Long>? = withContext(Dispatchers.IO) {
        val autoEnabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_DNS_ENABLED, false)
        if (!autoEnabled && !force) {
            return@withContext null
        }

        if (CoreServiceManager.isRunning()) {
            return@withContext null
        }

        val now = System.currentTimeMillis()
        if (!force && (now - lastScanTimestamp < SCAN_COOLDOWN_MS)) {
            return@withContext null
        }

        lastScanTimestamp = now
        val best = autoSelectBestDns(context = null)
        best
    }
}
