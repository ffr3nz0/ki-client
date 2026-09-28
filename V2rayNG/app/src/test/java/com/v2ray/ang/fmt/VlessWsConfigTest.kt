package com.v2ray.ang.fmt

import com.tencent.mmkv.MMKV
import com.v2ray.ang.core.CoreOutboundBuilder
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.JsonUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock

import android.util.Log
import org.junit.After
import org.junit.Before
import org.mockito.MockedStatic
import org.mockito.Mockito

class VlessWsConfigTest {

    companion object {
        private val settings: MMKV = mock()

        @BeforeClass
        @JvmStatic
        fun initializeHandles() {
            mockStatic(MMKV::class.java).use {
                it.`when`<MMKV> { MMKV.mmkvWithID(Mockito.anyString(), Mockito.anyInt()) }.thenReturn(settings)
                MmkvManager.decodeSettingsString("test-initialize")
            }
        }
    }

    private lateinit var mockLog: MockedStatic<Log>

    @Before
    fun setUp() {
        mockLog = mockStatic(Log::class.java, Mockito.RETURNS_DEFAULTS)
    }

    @After
    fun tearDown() {
        mockLog.close()
    }

    @Test
    fun testVlessWsOutboundGeneration() {
        val vlessUrl = "vless://a3482e88-686a-4a58-8126-99c9df64b7bf@pop.raynomusic.com:2052?path=%2F&security=none&encryption=none&type=ws#-AmiR"
        val profile = VlessFmt.parse(vlessUrl)
        assertNotNull("Profile should not be null", profile)

        val outbound = CoreOutboundBuilder.convert(profile!!)
        assertNotNull("Outbound should not be null", outbound)

        assertEquals("vless", outbound!!.protocol)
        assertEquals("pop.raynomusic.com", outbound.settings?.address)
        assertEquals(2052, outbound.settings?.port)
        assertEquals("none", outbound.settings?.encryption)
        assertNull(outbound.settings?.flow)

        // WebSocket transport tests
        assertEquals("ws", outbound.streamSettings?.network)
        assertEquals("pop.raynomusic.com", outbound.streamSettings?.wsSettings?.host)
        assertEquals("pop.raynomusic.com", outbound.streamSettings?.wsSettings?.headers?.get("Host"))
        assertEquals("/", outbound.streamSettings?.wsSettings?.path)

        // Security should be null for plain WS
        assertNull(outbound.streamSettings?.security)
        assertNull(outbound.streamSettings?.tlsSettings)

        // tcpFastOpen must not be forced to true
        assertNull(outbound.streamSettings?.sockopt?.tcpFastOpen)
        assertNull(outbound.streamSettings?.sockopt?.TcpNoDelay)

        val json = JsonUtil.toJsonPretty(outbound)
        println("Generated Outbound JSON:\n$json")
    }

    @Test
    fun testPlainVlessPreservesDomainEvenWhenResolveMethodIs2() {
        Mockito.`when`(settings.decodeString(com.v2ray.ang.AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, "1")).thenReturn("2")
        Mockito.`when`(settings.decodeString(com.v2ray.ang.AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD)).thenReturn("2")
        val vlessUrl = "vless://a3482e88-686a-4a58-8126-99c9df64b7bf@pop.raynomusic.com:2052?path=%2F&security=none&encryption=none&type=ws#-AmiR"
        val profile = VlessFmt.parse(vlessUrl)
        assertNotNull(profile)

        val outbound = CoreOutboundBuilder.convert(profile!!)
        assertNotNull(outbound)
        // Domain must never be replaced with public IP for plain VLESS
        assertEquals("pop.raynomusic.com", outbound!!.settings?.address)
    }

    @Test
    fun testPlainVlessWithIpServerUsesHostDomain() {
        val vlessUrl = "vless://a3482e88-686a-4a58-8126-99c9df64b7bf@104.21.50.1:2052?path=%2F&security=none&encryption=none&host=pop.raynomusic.com&type=ws#-AmiR"
        val profile = VlessFmt.parse(vlessUrl)
        assertNotNull(profile)

        val outbound = CoreOutboundBuilder.convert(profile!!)
        assertNotNull(outbound)
        // Outbound address must be set to host domain so Xray does not reject the config
        assertEquals("pop.raynomusic.com", outbound!!.settings?.address)
    }
}
