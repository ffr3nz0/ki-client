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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object DnsSelectorManager {
    private const val TAG = "DnsSelectorManager"
    private var lastScanTimestamp = 0L
    private const val SCAN_COOLDOWN_MS = 3 * 60 * 1000L // 3 minutes cooldown

    data class DnsPreset(
        val id: String,
        val displayName: String,
        val primaryIp: String,
        val secondaryIp: String = "",
        val dohUrl: String = "",
        val domesticIp: String = primaryIp
    )

    // Exclusively fast, reliable international/foreign DNS resolvers
    val PRESETS: List<DnsPreset> = listOf(
        DnsPreset(
            id = "cloudflare",
            displayName = "Cloudflare (1.1.1.1 / DoH)",
            primaryIp = "1.1.1.1",
            secondaryIp = "1.0.0.1",
            dohUrl = "https://cloudflare-dns.com/dns-query",
            domesticIp = "1.1.1.1"
        ),
        DnsPreset(
            id = "google",
            displayName = "Google (8.8.8.8 / DoH)",
            primaryIp = "8.8.8.8",
            secondaryIp = "8.8.4.4",
            dohUrl = "https://dns.google/dns-query",
            domesticIp = "8.8.8.8"
        ),
        DnsPreset(
            id = "quad9",
            displayName = "Quad9 (9.9.9.9 / Secure)",
            primaryIp = "9.9.9.9",
            secondaryIp = "149.112.112.112",
            dohUrl = "https://dns.quad9.net/dns-query",
            domesticIp = "9.9.9.9"
        ),
        DnsPreset(
            id = "opendns",
            displayName = "OpenDNS Cisco (208.67.222.222)",
            primaryIp = "208.67.222.222",
            secondaryIp = "208.67.220.220",
            dohUrl = "https://doh.opendns.com/dns-query",
            domesticIp = "208.67.222.222"
        ),
        DnsPreset(
            id = "adguard",
            displayName = "AdGuard DNS (94.140.14.14)",
            primaryIp = "94.140.14.14",
            secondaryIp = "94.140.15.15",
            dohUrl = "https://dns.adguard-dns.com/dns-query",
            domesticIp = "94.140.14.14"
        ),
        DnsPreset(
            id = "controld",
            displayName = "Control D (76.76.2.0)",
            primaryIp = "76.76.2.0",
            secondaryIp = "76.76.10.0",
            dohUrl = "https://freedns.controld.com/p0",
            domesticIp = "76.76.2.0"
        ),
        DnsPreset(
            id = "dnswatch",
            displayName = "DNS.WATCH (84.200.69.80)",
            primaryIp = "84.200.69.80",
            secondaryIp = "84.200.70.40",
            domesticIp = "84.200.69.80"
        ),
        DnsPreset(
            id = "level3",
            displayName = "Level3 / Lumen (4.2.2.4)",
            primaryIp = "4.2.2.4",
            secondaryIp = "4.2.2.2",
            domesticIp = "4.2.2.4"
        ),
        DnsPreset(
            id = "yandex",
            displayName = "Yandex DNS (77.88.8.8)",
            primaryIp = "77.88.8.8",
            secondaryIp = "77.88.8.1",
            dohUrl = "https://common.dot.dns.yandex.net/dns-query",
            domesticIp = "77.88.8.8"
        )
    )

    fun getPresetById(id: String): DnsPreset? = PRESETS.firstOrNull { it.id == id }

    /**
     * Measures latency to a DNS server (IP port 53).
     * First attempts a real UDP DNS query. If UDP times out (e.g. blocked by ISP),
     * falls back to TCP socket connection to verify reachability.
     * Returns round-trip time in milliseconds, or -1 if unreachable/blocked.
     */
    suspend fun benchmarkDns(ip: String, timeoutMs: Int = 1200): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        // 1. Try UDP DNS query
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                val address = InetAddress.getByName(ip)
                // Minimal standard DNS query packet for "cloudflare.com" (Type A, Class IN)
                val dnsQuery = byteArrayOf(
                    0x12.toByte(), 0x34.toByte(), // ID
                    0x01.toByte(), 0x00.toByte(), // Flags: standard recursive query
                    0x00.toByte(), 0x01.toByte(), // QDCOUNT: 1 question
                    0x00.toByte(), 0x00.toByte(), // ANCOUNT: 0
                    0x00.toByte(), 0x00.toByte(), // NSCOUNT: 0
                    0x00.toByte(), 0x00.toByte(), // ARCOUNT: 0
                    // "cloudflare" (10) + "com" (3) + 0
                    0x0a.toByte(),
                    'c'.code.toByte(), 'l'.code.toByte(), 'o'.code.toByte(), 'u'.code.toByte(), 'd'.code.toByte(),
                    'f'.code.toByte(), 'l'.code.toByte(), 'a'.code.toByte(), 'r'.code.toByte(), 'e'.code.toByte(),
                    0x03.toByte(),
                    'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
                    0x00.toByte(),
                    0x00.toByte(), 0x01.toByte(), // QTYPE: A
                    0x00.toByte(), 0x01.toByte()  // QCLASS: IN
                )
                val packet = DatagramPacket(dnsQuery, dnsQuery.size, address, 53)
                socket.send(packet)

                val buffer = ByteArray(512)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)

                val elapsed = System.currentTimeMillis() - startTime
                return@withContext elapsed.coerceAtLeast(1L)
            }
        } catch (_: Exception) {
            // UDP failed or timed out (common ISP filtering/poisoning)
        }

        // 2. Fallback: TCP port 53 probe
        try {
            Socket().use { tcpSocket ->
                tcpSocket.connect(InetSocketAddress(ip, 53), timeoutMs)
                val elapsed = System.currentTimeMillis() - startTime
                return@withContext elapsed.coerceAtLeast(1L)
            }
        } catch (_: Exception) {
            // Both UDP and TCP failed
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
     */
    fun applyPreset(context: Context?, preset: DnsPreset) {
        val remoteVal = if (preset.dohUrl.isNotBlank()) {
            "${preset.dohUrl},${preset.primaryIp}"
        } else if (preset.secondaryIp.isNotBlank()) {
            "${preset.primaryIp},${preset.secondaryIp}"
        } else {
            preset.primaryIp
        }

        val vpnVal = if (preset.secondaryIp.isNotBlank()) {
            "${preset.primaryIp},${preset.secondaryIp}"
        } else {
            preset.primaryIp
        }

        MmkvManager.encodeSettings(AppConfig.PREF_REMOTE_DNS, remoteVal)
        MmkvManager.encodeSettings(AppConfig.PREF_VPN_DNS, vpnVal)
        MmkvManager.encodeSettings(AppConfig.PREF_DOMESTIC_DNS, preset.domesticIp)
        MmkvManager.encodeSettings(AppConfig.PREF_DNS_SELECTOR_MODE, preset.id)

        LogUtil.i(TAG, "Applied foreign DNS preset '${preset.displayName}': remote=$remoteVal, vpn=$vpnVal, domestic=${preset.domesticIp}")

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
     * Scans foreign DNS presets and applies the fastest one, but ONLY when VPN is OFF.
     * When VPN is active, scanning is bypassed so we don't benchmark through the proxy/VPN tunnel.
     */
    suspend fun scanAndSelectIfDisconnected(context: Context?, force: Boolean = false): Pair<DnsPreset, Long>? = withContext(Dispatchers.IO) {
        val autoEnabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_DNS_ENABLED, true)
        if (!autoEnabled) {
            LogUtil.d(TAG, "Auto DNS is disabled, skipping scan.")
            return@withContext null
        }

        if (CoreServiceManager.isRunning()) {
            LogUtil.d(TAG, "VPN is currently running; skipping disconnected DNS scan.")
            return@withContext null
        }

        val now = System.currentTimeMillis()
        if (!force && (now - lastScanTimestamp < SCAN_COOLDOWN_MS)) {
            LogUtil.d(TAG, "Skipping DNS scan due to cooldown (${(now - lastScanTimestamp) / 1000}s ago)")
            return@withContext null
        }

        LogUtil.i(TAG, "Executing foreign DNS scan while VPN is disconnected...")
        lastScanTimestamp = now
        val best = autoSelectBestDns(context = null)
        best
    }

    /**
     * Probes currently configured DNS if auto DNS is enabled, and falls back to a working foreign one if blocked.
     */
    suspend fun ensureDnsHealthy(context: Context) = withContext(Dispatchers.IO) {
        val autoEnabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_DNS_ENABLED, true)
        if (!autoEnabled) return@withContext

        val currentVpnDns = SettingsManager.getVpnDnsServers().firstOrNull() ?: "1.1.1.1"
        val latency = benchmarkDns(currentVpnDns, timeoutMs = 800)
        if (latency <= 0L) {
            LogUtil.w(TAG, "Current DNS $currentVpnDns is unreachable/blocked on this network. Triggering auto-fallback...")
            autoSelectBestDns(null)
        }
    }
}
