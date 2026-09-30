package com.v2ray.ang.handler

import com.v2ray.ang.util.Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsSelectorManagerTest {

    @Test
    fun presets_containValidPureIps() {
        val presets = DnsSelectorManager.PRESETS
        assertTrue(presets.isNotEmpty())

        for (preset in presets) {
            assertTrue("Primary IP for ${preset.id} must be pure IP", Utils.isPureIpAddress(preset.primaryIp))
            if (preset.secondaryIp.isNotEmpty()) {
                assertTrue("Secondary IP for ${preset.id} must be pure IP", Utils.isPureIpAddress(preset.secondaryIp))
            }
        }
    }

    @Test
    fun getPresetById_retrievesExpectedPreset() {
        val cloudflare = DnsSelectorManager.getPresetById("cloudflare")
        assertNotNull(cloudflare)
        assertEquals("1.1.1.1", cloudflare?.primaryIp)
        assertEquals("1.0.0.1", cloudflare?.secondaryIp)

        val google = DnsSelectorManager.getPresetById("google")
        assertNotNull(google)
        assertEquals("8.8.8.8", google?.primaryIp)
        assertEquals("8.8.4.4", google?.secondaryIp)
    }

    @Test
    fun presets_containOnlyForeignResolvers() {
        val ids = DnsSelectorManager.PRESETS.map { it.id }
        assertTrue(ids.contains("cloudflare"))
        assertTrue(ids.contains("google"))
        assertTrue(ids.contains("quad9"))
        assertTrue(ids.contains("opendns"))
        assertTrue(ids.contains("adguard"))
        assertTrue(ids.contains("controld"))
        assertTrue(ids.contains("dnswatch"))
        assertTrue(ids.contains("level3"))
        assertTrue(ids.contains("yandex"))
        // Must not contain any Iranian domestic DNS
        assertTrue(!ids.contains("shecan"))
        assertTrue(!ids.contains("begzar"))
        assertTrue(!ids.contains("electro"))
        assertTrue(!ids.contains("radar"))
        assertTrue(!ids.contains("403"))
    }
}
