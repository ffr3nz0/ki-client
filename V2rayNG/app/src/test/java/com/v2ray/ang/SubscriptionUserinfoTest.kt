package com.v2ray.ang

import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.ui.subscription.formatTraffic
import com.v2ray.ang.ui.subscription.shouldShowUserinfoCard
import com.v2ray.ang.util.HttpUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionUserinfoTest {

    @Test
    fun testParseSubscriptionUserinfoStandard() {
        val userinfo = "upload=1802586367; download=23664028291; total=0; expire=0"
        val subscription = SubscriptionItem()
        AngConfigManager.parseSubscriptionUserinfo(userinfo, subscription)

        assertEquals(1802586367L, subscription.uploadTraffic)
        assertEquals(23664028291L, subscription.downloadTraffic)
        assertEquals(0L, subscription.totalTraffic)
        assertEquals(0L, subscription.expireTime)
    }

    @Test
    fun testParseSubscriptionUserinfoWithQuotaAndExpiry() {
        val userinfo = "upload=1073741824; download=10737418240; total=53687091200; expire=1780000000"
        val subscription = SubscriptionItem()
        AngConfigManager.parseSubscriptionUserinfo(userinfo, subscription)

        assertEquals(1073741824L, subscription.uploadTraffic) // 1 GB
        assertEquals(10737418240L, subscription.downloadTraffic) // 10 GB
        assertEquals(53687091200L, subscription.totalTraffic) // 50 GB
        assertEquals(1780000000L, subscription.expireTime)
    }

    @Test
    fun testParseSubscriptionUserinfoMalformedAndExtraFields() {
        val userinfo = " UPLOAD = 500 ; random_key = abc ; download = 1500 ; malformed ; expire = "
        val subscription = SubscriptionItem()
        AngConfigManager.parseSubscriptionUserinfo(userinfo, subscription)

        assertEquals(500L, subscription.uploadTraffic)
        assertEquals(1500L, subscription.downloadTraffic)
        assertEquals(0L, subscription.totalTraffic)
        assertEquals(0L, subscription.expireTime)
    }

    @Test
    fun testDecodeHeaderValue() {
        // Plain text remains unchanged
        assertEquals("My Sub Title", HttpUtil.decodeHeaderValue("My Sub Title"))

        // Base64 decoded properly
        assertEquals("SubService", HttpUtil.decodeHeaderValue("base64:U3ViU2VydmljZQ=="))

        // Null / blank returns null
        assertEquals(null, HttpUtil.decodeHeaderValue(null))
        assertEquals(null, HttpUtil.decodeHeaderValue("   "))
    }

    @Test
    fun testFormatTraffic() {
        assertEquals("0 B", formatTraffic(0L))
        assertEquals("500 B", formatTraffic(500L))
        assertEquals("1.00 KB", formatTraffic(1024L))
        assertEquals("1.50 MB", formatTraffic((1.5 * 1024 * 1024).toLong()))
        assertEquals("22.04 GB", formatTraffic(23664028291L))
        assertEquals("1.68 GB", formatTraffic(1802586367L))
        assertEquals("1.00 TB", formatTraffic(1024L * 1024L * 1024L * 1024L))
    }

    @Test
    fun testShouldShowUserinfoCard() {
        assertFalse(shouldShowUserinfoCard(null))
        assertFalse(shouldShowUserinfoCard(SubscriptionItem()))

        assertTrue(shouldShowUserinfoCard(SubscriptionItem(uploadTraffic = 100L)))
        assertTrue(shouldShowUserinfoCard(SubscriptionItem(downloadTraffic = 200L)))
        assertTrue(shouldShowUserinfoCard(SubscriptionItem(totalTraffic = 1000L)))
        assertTrue(shouldShowUserinfoCard(SubscriptionItem(expireTime = 1700000000L)))
        assertTrue(shouldShowUserinfoCard(SubscriptionItem(announceMsg = "Important update!")))
        assertTrue(shouldShowUserinfoCard(SubscriptionItem(supportUrl = "https://t.me/support")))
    }
}
