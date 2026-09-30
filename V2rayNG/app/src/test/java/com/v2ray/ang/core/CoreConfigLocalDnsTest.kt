package com.v2ray.ang.core

import android.content.Context
import android.util.Log
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.CoreConfigContext
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.handler.MmkvManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock

class CoreConfigLocalDnsTest {

    companion object {
        private val settings: MMKV = mock()

        @BeforeClass
        @JvmStatic
        fun initializeHandles() {
            Mockito.mockStatic(MMKV::class.java).use {
                it.`when`<MMKV> { MMKV.mmkvWithID(Mockito.anyString(), Mockito.anyInt()) }.thenReturn(settings)
                MmkvManager.decodeSettingsString("test-initialize")
            }
        }
    }

    private lateinit var mockLog: MockedStatic<Log>

    @Before
    fun setUp() {
        mockLog = Mockito.mockStatic(Log::class.java, Mockito.RETURNS_DEFAULTS)
    }

    @After
    fun tearDown() {
        mockLog.close()
    }

    @Test
    fun configureLocalDns_whenLocalDnsDisabled_doesNotAddDnsOutOrPort53Rule() {
        // Setup mock MMKV: VPN mode enabled, local DNS preference FALSE
        Mockito.`when`(settings.decodeString(AppConfig.PREF_MODE)).thenReturn(AppConfig.VPN)
        Mockito.`when`(settings.decodeBool(AppConfig.PREF_LOCAL_DNS_ENABLED, false)).thenReturn(false)
        Mockito.`when`(settings.decodeBool(AppConfig.PREF_LOCAL_DNS_ENABLED)).thenReturn(false)
        Mockito.`when`(settings.decodeBool(AppConfig.PREF_USE_HEV_TUNNEL, true)).thenReturn(true)

        val v2rayConfig = V2rayConfig(
            log = V2rayConfig.LogBean(),
            inbounds = arrayListOf(),
            outbounds = arrayListOf(),
            routing = V2rayConfig.RoutingBean(domainStrategy = "AsIs", rules = arrayListOf()),
            dns = V2rayConfig.DnsBean(servers = arrayListOf(), hosts = mutableMapOf())
        )

        val dummyContext: Context = mock()
        val configContext = CoreConfigContext(
            context = dummyContext,
            guid = "test-guid"
        )

        val method = CoreConfigManager::class.java.getDeclaredMethod(
            "configureLocalDns",
            CoreConfigContext::class.java,
            V2rayConfig::class.java
        )
        method.isAccessible = true
        method.invoke(CoreConfigManager, configContext, v2rayConfig)

        // Verify that port 53 rule routing to dns-out was NOT added
        val dnsRule = v2rayConfig.routing.rules.firstOrNull { it.outboundTag == "dns-out" && it.port == "53" }
        org.junit.Assert.assertNull("Port 53 rule must not be present when local DNS is disabled", dnsRule)

        // Verify that dns-out outbound was NOT added
        val dnsOutbound = v2rayConfig.outbounds.firstOrNull { it.protocol == "dns" && it.tag == "dns-out" }
        org.junit.Assert.assertNull("dns-out outbound must not be present when local DNS is disabled", dnsOutbound)
    }

    @Test
    fun configureLocalDns_whenLocalDnsEnabled_inVpnMode_hijacksPort53() {
        // Setup mock MMKV: VPN mode enabled, local DNS preference TRUE
        Mockito.`when`(settings.decodeString(AppConfig.PREF_MODE)).thenReturn(AppConfig.VPN)
        Mockito.`when`(settings.decodeBool(AppConfig.PREF_LOCAL_DNS_ENABLED, false)).thenReturn(true)
        Mockito.`when`(settings.decodeBool(AppConfig.PREF_LOCAL_DNS_ENABLED)).thenReturn(true)
        Mockito.`when`(settings.decodeBool(AppConfig.PREF_USE_HEV_TUNNEL, true)).thenReturn(true)

        val v2rayConfig = V2rayConfig(
            log = V2rayConfig.LogBean(),
            inbounds = arrayListOf(),
            outbounds = arrayListOf(),
            routing = V2rayConfig.RoutingBean(domainStrategy = "AsIs", rules = arrayListOf()),
            dns = V2rayConfig.DnsBean(servers = arrayListOf(), hosts = mutableMapOf())
        )

        val dummyContext: Context = mock()
        val configContext = CoreConfigContext(
            context = dummyContext,
            guid = "test-guid"
        )

        val method = CoreConfigManager::class.java.getDeclaredMethod(
            "configureLocalDns",
            CoreConfigContext::class.java,
            V2rayConfig::class.java
        )
        method.isAccessible = true
        method.invoke(CoreConfigManager, configContext, v2rayConfig)

        // Verify that port 53 rule routing to dns-out was added
        val dnsRule = v2rayConfig.routing.rules.firstOrNull { it.outboundTag == "dns-out" && it.port == "53" }
        assertNotNull("Port 53 rule routing to dns-out must be present when local DNS is enabled", dnsRule)
        assertEquals(arrayListOf("socks"), dnsRule?.inboundTag)

        // Verify that dns-out outbound was added
        val dnsOutbound = v2rayConfig.outbounds.firstOrNull { it.protocol == "dns" && it.tag == "dns-out" }
        assertNotNull("dns-out outbound must be present when local DNS is enabled", dnsOutbound)
    }

    @Test
    fun buildDnsHostsFromRoutingRules_containsStaticDoHEndpoints() {
        val dummyContext: Context = mock()
        val configContext = CoreConfigContext(
            context = dummyContext,
            guid = "test-guid"
        )

        val method = CoreConfigManager::class.java.getDeclaredMethod(
            "buildDnsHostsFromRoutingRules",
            CoreConfigContext::class.java
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val hosts = method.invoke(CoreConfigManager, configContext) as Map<String, Any>

        assertTrue("Must contain Cloudflare DoH host", hosts.containsKey(AppConfig.DNS_CLOUDFLARE_DNS_COM_DOMAIN))
        assertTrue("Must contain Google DoH host", hosts.containsKey(AppConfig.DNS_GOOGLE_DOMAIN))
        assertTrue("Must contain Quad9 DoH host", hosts.containsKey(AppConfig.DNS_QUAD9_DOMAIN))
        assertTrue("Must contain AdGuard DoH host", hosts.containsKey("dns.adguard-dns.com"))
        assertTrue("Must contain Control D DoH host", hosts.containsKey("freedns.controld.com"))
        assertTrue("Must contain OpenDNS DoH host", hosts.containsKey("doh.opendns.com"))
    }
}
