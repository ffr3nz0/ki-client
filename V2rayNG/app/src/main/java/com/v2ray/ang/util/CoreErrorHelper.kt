package com.v2ray.ang.util

import com.v2ray.ang.R

/**
 * Normalizes and localizes raw technical errors emitted by Xray-core / Libv2ray
 * into actionable, user-friendly explanations.
 */
object CoreErrorHelper {

    /**
     * Resolves a known core error into a localized user-friendly string resource ID,
     * or returns null if no specific humanized explanation is defined.
     */
    fun resolveErrorStringRes(rawError: String?): Int? {
        if (rawError.isNullOrBlank()) return null
        val lower = rawError.lowercase()
        return when {
            lower.contains("vless without tls or other encryption is prohibited") ||
            (lower.contains("vless") && lower.contains("without tls") && lower.contains("prohibited")) -> {
                R.string.error_vless_without_tls_prohibited
            }
            lower.contains("trojan without tls is prohibited") ||
            (lower.contains("trojan") && lower.contains("without tls") && lower.contains("prohibited")) -> {
                R.string.error_trojan_without_tls_prohibited
            }
            lower.contains("failed to parse json config") -> {
                R.string.error_config_parse_failed
            }
            else -> null
        }
    }

    /**
     * Formats an error message for service startup failure.
     */
    fun formatServiceStartError(rawError: String?, getString: (Int) -> String): String {
        val resId = resolveErrorStringRes(rawError)
        return when {
            resId != null -> getString(resId)
            !rawError.isNullOrBlank() -> "${getString(R.string.toast_services_failure)}: $rawError"
            else -> getString(R.string.toast_services_failure)
        }
    }

    /**
     * Formats a raw core error message for connection test or diagnostics.
     */
    fun formatCoreErrorMessage(rawError: String, getString: (Int) -> String): String {
        val resId = resolveErrorStringRes(rawError)
        return if (resId != null) getString(resId) else rawError
    }
}
