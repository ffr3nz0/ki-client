package com.v2ray.ang.util

import com.v2ray.ang.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoreErrorHelperTest {

    private val fakeStringMap = mapOf(
        R.string.toast_services_failure to "Failed to start service",
        R.string.error_vless_without_tls_prohibited to "VLESS without TLS is prohibited on public networks.",
        R.string.error_trojan_without_tls_prohibited to "Trojan without TLS is prohibited on public networks.",
        R.string.error_config_parse_failed to "Configuration syntax error."
    )

    private val fakeResolver: (Int) -> String = { resId ->
        fakeStringMap[resId] ?: "Unknown string $resId"
    }

    @Test
    fun testResolveVlessWithoutTlsError() {
        val rawError = "config error: infra/conf/serial: failed to parse json config > " +
                "infra/conf: failed to build outbound config with tag proxy > " +
                "infra/conf: vless without TLS or other encryption is prohibited unless the server address is a private IP or domain"

        val resId = CoreErrorHelper.resolveErrorStringRes(rawError)
        assertEquals(R.string.error_vless_without_tls_prohibited, resId)

        val formatted = CoreErrorHelper.formatServiceStartError(rawError, fakeResolver)
        assertEquals("VLESS without TLS is prohibited on public networks.", formatted)
    }

    @Test
    fun testResolveTrojanWithoutTlsError() {
        val rawError = "trojan without TLS is prohibited unless the server address is a private IP or domain"

        val resId = CoreErrorHelper.resolveErrorStringRes(rawError)
        assertEquals(R.string.error_trojan_without_tls_prohibited, resId)

        val formatted = CoreErrorHelper.formatServiceStartError(rawError, fakeResolver)
        assertEquals("Trojan without TLS is prohibited on public networks.", formatted)
    }

    @Test
    fun testResolveJsonConfigParseError() {
        val rawError = "config error: infra/conf/serial: failed to parse json config > unexpected end of JSON input"

        val resId = CoreErrorHelper.resolveErrorStringRes(rawError)
        assertEquals(R.string.error_config_parse_failed, resId)

        val formatted = CoreErrorHelper.formatServiceStartError(rawError, fakeResolver)
        assertEquals("Configuration syntax error.", formatted)
    }

    @Test
    fun testUnknownErrorFallback() {
        val rawError = "connection timed out"

        val resId = CoreErrorHelper.resolveErrorStringRes(rawError)
        assertNull(resId)

        val formatted = CoreErrorHelper.formatServiceStartError(rawError, fakeResolver)
        assertEquals("Failed to start service: connection timed out", formatted)
    }

    @Test
    fun testNullOrBlankError() {
        assertEquals("Failed to start service", CoreErrorHelper.formatServiceStartError(null, fakeResolver))
        assertEquals("Failed to start service", CoreErrorHelper.formatServiceStartError("", fakeResolver))
        assertEquals("Failed to start service", CoreErrorHelper.formatServiceStartError("   ", fakeResolver))
    }

    @Test
    fun testFormatCoreErrorMessage() {
        val vlessError = "infra/conf: vless without TLS or other encryption is prohibited"
        assertEquals(
            "VLESS without TLS is prohibited on public networks.",
            CoreErrorHelper.formatCoreErrorMessage(vlessError, fakeResolver)
        )

        val unknownError = "dial tcp 1.2.3.4:443: i/o timeout"
        assertEquals(
            unknownError,
            CoreErrorHelper.formatCoreErrorMessage(unknownError, fakeResolver)
        )
    }
}
