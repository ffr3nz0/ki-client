package com.v2ray.ang.dto.entities

data class SubscriptionItem(
    var remarks: String = "",
    var url: String = "",
    var enabled: Boolean = true,
    val addedTime: Long = System.currentTimeMillis(),
    var lastUpdated: Long = -1,
    var autoUpdate: Boolean = true,
    var updateInterval: Long = 60, // in minutes, default to 60 minutes
    var prevProfile: String? = null,
    var nextProfile: String? = null,
    var filter: String? = null,
    var allowInsecureUrl: Boolean = false,
    var userAgent: String? = null,
    var requestHeaders: String? = null,
    var uploadTraffic: Long = 0L,
    var downloadTraffic: Long = 0L,
    var totalTraffic: Long = 0L,
    var expireTime: Long = 0L,
    var webPageUrl: String? = null,
    var supportUrl: String? = null,
    var announceMsg: String? = null,
)

