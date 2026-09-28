package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.handler.AngConfigManager

internal data class ServerRowUiModel(
    val guid: String,
    val profile: ProfileItem,
    val remarks: String,
    val statistics: String,
    val typeDescription: String,
    val testDelayMillis: Long,
    val subscriptionBadge: String,
)

internal data class ServerGroupUiState(
    val servers: List<ServersCache> = emptyList(),
    val rows: List<ServerRowUiModel> = emptyList(),
    val subscription: com.v2ray.ang.dto.entities.SubscriptionItem? = null,
)

internal fun buildServerRowUiModel(
    server: ServersCache,
    subscriptionRemarks: String,
): ServerRowUiModel {
    val profile = server.profile
    return ServerRowUiModel(
        guid = server.guid,
        profile = profile,
        remarks = profile.remarks,
        statistics = profile.description.nullIfBlank()
            ?: AngConfigManager.generateDescription(profile),
        typeDescription = serverProtocolDescription(profile),
        testDelayMillis = server.testDelayMillis,
        subscriptionBadge = subscriptionRemarks.firstOrNull()?.toString().orEmpty(),
    )
}

private fun serverProtocolDescription(profile: ProfileItem): String {
    if (profile.configType.isComplexType()) return profile.configType.name
    val parts = mutableListOf(profile.configType.name)
    profile.network?.let { network ->
        if (network.isNotBlank() && !network.equals("tcp", ignoreCase = true)) {
            parts.add(network)
        }
    }
    profile.security?.let { security ->
        if (security.isNotBlank()) {
            parts.add(
                if (profile.insecure == true && security.equals("tls", ignoreCase = true)) {
                    "$security insecure"
                } else {
                    security
                }
            )
        }
    }
    return parts.joinToString(" / ")
}

internal fun extractCountryFlag(remarks: String): String {
    val flagRegex = Regex("[\\uD83C][\\uDDE6-\\uDDFF][\\uD83C][\\uDDE6-\\uDDFF]")
    flagRegex.find(remarks)?.let { return it.value }

    val lower = remarks.lowercase()
    return when {
        "france" in lower || "🇫🇷" in lower || " fr " in lower || lower.startsWith("fr") -> "🇫🇷"
        "germany" in lower || "deutschland" in lower || " de " in lower || lower.startsWith("de") -> "🇩🇪"
        "albania" in lower || " al " in lower -> "🇦🇱"
        "ukraine" in lower || " ua " in lower -> "🇺🇦"
        "kenya" in lower || " ke " in lower -> "🇰🇪"
        "panama" in lower || " pa " in lower -> "🇵🇦"
        "croatia" in lower || " hr " in lower -> "🇭🇷"
        "united states" in lower || "usa" in lower || " us " in lower || lower.startsWith("us") -> "🇺🇸"
        "united kingdom" in lower || "england" in lower || " uk " in lower || " gb " in lower -> "🇬🇧"
        "netherlands" in lower || "amsterdam" in lower || " nl " in lower || lower.startsWith("nl") -> "🇳🇱"
        "finland" in lower || "helsinki" in lower || " fi " in lower -> "🇫🇮"
        "turkey" in lower || "istanbul" in lower || " tr " in lower -> "🇹🇷"
        "iran" in lower || "tehran" in lower || " ir " in lower -> "🇮🇷"
        "canada" in lower || " ca " in lower -> "🇨🇦"
        "singapore" in lower || " sg " in lower -> "🇸🇬"
        "sweden" in lower || " se " in lower -> "🇸🇪"
        "japan" in lower || "tokyo" in lower || " jp " in lower -> "🇯🇵"
        "dubai" in lower || "uae" in lower || " ae " in lower -> "🇦🇪"
        "poland" in lower || " pl " in lower -> "🇵🇱"
        "italy" in lower || " it " in lower -> "🇮🇹"
        "spain" in lower || " es " in lower -> "🇪🇸"
        "russia" in lower || " ru " in lower -> "🇷🇺"
        "switzerland" in lower || " ch " in lower -> "🇨🇭"
        "austria" in lower || " at " in lower -> "🇦🇹"
        "south korea" in lower || "korea" in lower || " kr " in lower -> "🇰🇷"
        else -> "🌐"
    }
}
