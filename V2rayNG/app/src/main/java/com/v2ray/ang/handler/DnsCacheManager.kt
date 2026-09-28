package com.v2ray.ang.handler

import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object DnsCacheManager {
    private const val TAG = "DnsCacheManager"
    private const val ID_DNS_CACHE = "DNS_CACHE"

    // 2 hours: Fresh cache. Within 2 hours, return instantly (0ms) without waiting for network.
    const val FRESH_TTL_MS = 2 * 60 * 60 * 1000L

    // 14 days: Maximum fallback cache duration. If network is weak or DNS fails, fall back to cached IPs.
    const val MAX_STALE_TTL_MS = 14 * 24 * 60 * 60 * 1000L

    // Strict 2500ms timeout for live DNS queries to never block core startup on poor connections.
    const val DEFAULT_TIMEOUT_MS = 2500L

    data class DnsRecord(
        val ips: List<String>,
        val timestamp: Long
    )

    private val storage by lazy {
        try {
            MMKV.mmkvWithID(ID_DNS_CACHE, MMKV.MULTI_PROCESS_MODE)
        } catch (e: Exception) {
            LogUtil.e(TAG, "Failed to init MMKV for DNS_CACHE, falling back to default", e)
            MMKV.defaultMMKV()
        }
    }

    private val memoryCache = ConcurrentHashMap<String, DnsRecord>()

    private val resolverExecutor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "FastDnsResolver").apply { isDaemon = true }
    }

    /**
     * Resolves a host to a list of IP addresses with fast timeout and persistent caching.
     * If the network is weak and resolution times out or fails, returns cached IP addresses.
     */
    fun resolveHost(
        host: String,
        ipv6Preferred: Boolean = false,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): List<String>? {
        if (host.isBlank()) return null
        val cleanHost = host.trim().lowercase()

        // Pure IP addresses need no resolution, return the IP directly
        if (Utils.isPureIpAddress(cleanHost)) {
            return listOf(cleanHost)
        }

        val cachedRecord = getRecord(cleanHost)
        val now = System.currentTimeMillis()

        // 1. Fresh cache hit: return immediately with zero delay (instant core startup)
        if (cachedRecord != null && cachedRecord.ips.isNotEmpty()) {
            val age = now - cachedRecord.timestamp
            if (age < FRESH_TTL_MS) {
                LogUtil.i(TAG, "DNS Cache HIT (Fresh, age=${age / 1000}s) for $cleanHost: ${cachedRecord.ips}")
                return sortIps(cachedRecord.ips, ipv6Preferred)
            }
        }

        // 2. Not fresh or missing: attempt fast network resolution with bounded timeout
        val freshIps = resolveWithTimeout(cleanHost, timeoutMs)
        if (!freshIps.isNullOrEmpty()) {
            saveRecord(cleanHost, freshIps)
            LogUtil.i(TAG, "DNS resolved successfully for $cleanHost: $freshIps")
            return sortIps(freshIps, ipv6Preferred)
        }

        // 3. Fallback on weak network / timeout: use cached record if available
        if (cachedRecord != null && cachedRecord.ips.isNotEmpty()) {
            val age = now - cachedRecord.timestamp
            if (age < MAX_STALE_TTL_MS) {
                LogUtil.w(TAG, "DNS resolution timed out or failed for $cleanHost on weak network. Using cached IPs (age=${age / 1000}s): ${cachedRecord.ips}")
                return sortIps(cachedRecord.ips, ipv6Preferred)
            }
        }

        LogUtil.w(TAG, "DNS resolution failed and no valid cache available for $cleanHost")
        return null
    }

    private fun resolveWithTimeout(host: String, timeoutMs: Long): List<String>? {
        val future = resolverExecutor.submit<List<String>?> {
            try {
                val addresses = InetAddress.getAllByName(host)
                addresses.mapNotNull { it.hostAddress }.filter { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
        }

        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            LogUtil.w(TAG, "DNS query for $host timed out after ${timeoutMs}ms")
            null
        } catch (e: Exception) {
            LogUtil.w(TAG, "DNS query for $host failed: ${e.message}")
            null
        }
    }

    fun getRecord(host: String): DnsRecord? {
        memoryCache[host]?.let { return it }

        val json = storage?.decodeString(host) ?: return null
        return try {
            JsonUtil.fromJsonSafe(json, DnsRecord::class.java)?.also {
                memoryCache[host] = it
            }
        } catch (e: Exception) {
            null
        }
    }

    fun saveRecord(host: String, ips: List<String>) {
        if (ips.isEmpty()) return
        val record = DnsRecord(ips = ips, timestamp = System.currentTimeMillis())
        memoryCache[host] = record
        try {
            storage?.encode(host, JsonUtil.toJson(record))
        } catch (e: Exception) {
            LogUtil.e(TAG, "Failed to save DNS cache in MMKV for $host", e)
        }
    }

    fun sortIps(ips: List<String>, ipv6Preferred: Boolean): List<String> {
        val distinctIps = ips.distinct()
        return if (ipv6Preferred) {
            distinctIps.sortedWith(compareByDescending { it.contains(":") })
        } else {
            distinctIps.sortedWith(compareBy { it.contains(":") })
        }
    }

    /**
     * Pre-resolves a domain in the background without blocking the UI or startup thread.
     */
    fun preResolveAsync(host: String) {
        if (host.isBlank() || Utils.isPureIpAddress(host)) return
        val cleanHost = host.trim().lowercase()
        val record = getRecord(cleanHost)
        if (record != null && (System.currentTimeMillis() - record.timestamp) < FRESH_TTL_MS) {
            return
        }
        resolverExecutor.submit {
            try {
                val ips = InetAddress.getAllByName(cleanHost)
                    .mapNotNull { it.hostAddress }
                    .filter { it.isNotBlank() }
                if (ips.isNotEmpty()) {
                    saveRecord(cleanHost, ips)
                    LogUtil.i(TAG, "Background pre-resolved $cleanHost: $ips")
                }
            } catch (e: Exception) {
                // Ignore background resolve errors
            }
        }
    }

    private var lastToggleTime = 0L
    private var consecutiveToggleCount = 0
    private const val TOGGLE_RESET_WINDOW_MS = 20000L // 20 seconds window
    private const val TOGGLE_THRESHOLD_TO_CLEAR = 3 // 3 consecutive toggles

    /**
     * Records a service toggle event. If the user toggles the connection consecutively
     * (3 times within 20 seconds), clears the DNS cache so fresh IPs are fetched from the network.
     * Returns true if the cache was cleared as a result.
     */
    fun onServiceToggle(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastToggleTime < TOGGLE_RESET_WINDOW_MS) {
            consecutiveToggleCount++
        } else {
            consecutiveToggleCount = 1
        }
        lastToggleTime = now

        if (consecutiveToggleCount >= TOGGLE_THRESHOLD_TO_CLEAR) {
            LogUtil.i(TAG, "Consecutive service toggle threshold ($consecutiveToggleCount) reached. Clearing DNS IP cache!")
            clearCache()
            consecutiveToggleCount = 0
            return true
        }
        return false
    }

    /**
     * Clear all cached DNS entries.
     */
    fun clearCache() {
        memoryCache.clear()
        try {
            storage?.clearAll()
        } catch (e: Exception) {
            LogUtil.e(TAG, "Failed to clear DNS cache", e)
        }
    }
}
