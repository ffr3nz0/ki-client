package com.v2ray.ang.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.VPN
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.DnsSelectorManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.MmkvManager.rememberMmkvBool
import com.v2ray.ang.handler.MmkvManager.rememberMmkvString
import com.v2ray.ang.root.RootManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.CollapsiblePreferenceGroupHeader
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.SettingsEditItem
import com.v2ray.ang.ui.compose.SettingsListItem
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.ui.checkupdate.CheckUpdateActivity
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.launch

class SettingsActivity : BaseComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshSystemVpnSettingsAvailability()
            }
        }
    }

    private fun openSystemVpnSettings() {
        try {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        } catch (error: ActivityNotFoundException) {
            reportSystemVpnSettingsFailure(error)
        } catch (error: SecurityException) {
            reportSystemVpnSettingsFailure(error)
        }
    }

    private fun reportSystemVpnSettingsFailure(error: RuntimeException) {
        LogUtil.e(AppConfig.TAG, "Cannot open system VPN settings", error)
        toastError(R.string.toast_system_vpn_settings_unavailable)
    }

    @Composable
    override fun ScreenContent() {
        SettingsScreen(
            viewModel = viewModel,
            onBackClick = { finish() },
            onSystemVpnSettingsClicked = ::openSystemVpnSettings,
            onCheckUpdateClicked = { startActivity(Intent(this, CheckUpdateActivity::class.java)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onSystemVpnSettingsClicked: () -> Unit,
    onCheckUpdateClicked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val systemVpnSettingsAvailable by viewModel.systemVpnSettingsAvailable.collectAsStateWithLifecycle()

    var uiSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var vpnSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var dnsSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var coreSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var advancedSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var modeSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var updateSettingsExpanded by rememberSaveable { mutableStateOf(true) }

    // --- UI Settings ---
    var liquidGlassEnabled by rememberMmkvBool(AppConfig.PREF_LIQUID_GLASS_ENABLED, true)
    var liquidGlassIntensity by rememberMmkvString(AppConfig.PREF_LIQUID_GLASS_INTENSITY, "standard")
    var speedEnabled by rememberMmkvBool(AppConfig.PREF_SPEED_ENABLED, false)
    var dynamicColor by rememberMmkvBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    var language by remember {
        mutableStateOf(
            MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE, "auto") ?: "auto"
        )
    }
    var uiModeNight by rememberMmkvString(AppConfig.PREF_UI_MODE_NIGHT, "2")

    // --- VPN Settings ---
    var mode by rememberMmkvString(AppConfig.PREF_MODE, VPN)
    val isVpn = mode == VPN
    var useHevTun by rememberMmkvBool(AppConfig.PREF_USE_HEV_TUNNEL, true)
    var vpnBypassLan by rememberMmkvString(AppConfig.PREF_VPN_BYPASS_LAN, AppConfig.DEFAULT_VPN_BYPASS_LAN)
    var appendHttpProxy by rememberMmkvBool(AppConfig.PREF_APPEND_HTTP_PROXY, false)
    var localDns by rememberMmkvBool(AppConfig.PREF_LOCAL_DNS_ENABLED, false)
    var fakeDns by rememberMmkvBool(AppConfig.PREF_FAKE_DNS_ENABLED, false)

    // --- Smart DNS Selector ---
    var autoDns by rememberMmkvBool(AppConfig.PREF_AUTO_DNS_ENABLED, true)
    var dnsPresetMode by rememberMmkvString(AppConfig.PREF_DNS_SELECTOR_MODE, "auto")
    var remoteDns by rememberMmkvString(AppConfig.PREF_REMOTE_DNS, "")
    var domesticDns by rememberMmkvString(AppConfig.PREF_DOMESTIC_DNS, "")
    var isBenchmarkingDns by remember { mutableStateOf(false) }

    val dnsPresetEntries = listOf(
        stringResource(R.string.dns_preset_auto),
        "Cloudflare (1.1.1.1 / DoH)",
        "Google (8.8.8.8 / DoH)",
        "Quad9 (9.9.9.9 / Secure)",
        "OpenDNS Cisco (208.67.222.222)",
        "AdGuard DNS (94.140.14.14)",
        "Control D (76.76.2.0)",
        "DNS.WATCH (84.200.69.80)",
        "Level3 / Lumen (4.2.2.4)",
        "Yandex DNS (77.88.8.8)",
        stringResource(R.string.dns_preset_custom)
    )
    val dnsPresetValues = listOf(
        "auto",
        "cloudflare",
        "google",
        "quad9",
        "opendns",
        "adguard",
        "controld",
        "dnswatch",
        "level3",
        "yandex",
        "custom"
    )

    // --- Core & Sharing ---
    var sniffingEnabled by rememberMmkvBool(AppConfig.PREF_SNIFFING_ENABLED, true)
    var routeOnlyEnabled by rememberMmkvBool(AppConfig.PREF_ROUTE_ONLY_ENABLED, false)
    var proxySharing by rememberMmkvBool(AppConfig.PREF_PROXY_SHARING, false)
    var socksPort by rememberMmkvString(AppConfig.PREF_SOCKS_PORT, "")

    // --- Advanced ---
    var isBooted by rememberMmkvBool(AppConfig.PREF_IS_BOOTED, false)

    // --- Mode & Root ---
    var enableRootMode by rememberMmkvBool(AppConfig.PREF_ROOT_MODE_ENABLE, false)
    var lanSharing by rememberMmkvBool(AppConfig.PREF_ROOT_LAN_SHARING, false)

    // --- Updates ---
    var autoCheckUpdate by rememberMmkvBool(AppConfig.PREF_AUTO_CHECK_UPDATE, true)

    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    LaunchedEffect(dynamicColorSupported) {
        if (!dynamicColorSupported && dynamicColor) {
            dynamicColor = false
            ThemeManager.setDynamicColorEnabled(false)
        }
    }

    val liquidGlassIntensityEntries = listOf(
        stringResource(R.string.liquid_glass_subtle),
        stringResource(R.string.liquid_glass_standard),
        stringResource(R.string.liquid_glass_high)
    )
    val liquidGlassIntensityValues = listOf("subtle", "standard", "high")
    val languageEntries = stringArrayResource(R.array.language_select).toList()
    val languageValues = stringArrayResource(R.array.language_select_value).toList()
    val uiModeNightEntries = stringArrayResource(R.array.ui_mode_night).toList()
    val uiModeNightValues = stringArrayResource(R.array.ui_mode_night_value).toList()
    val bypassLanEntries = stringArrayResource(R.array.vpn_bypass_lan).toList()
    val bypassLanValues = stringArrayResource(R.array.vpn_bypass_lan_value).toList()
    val modeEntries = stringArrayResource(R.array.mode_entries).toList()
    val modeValues = stringArrayResource(R.array.mode_value).toList()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_settings),
                onBackClick = onBackClick,
                isLoading = isLoading
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScrollbar(scrollState)
                .verticalScroll(scrollState)
        ) {
            // 1. UI Settings
            CollapsiblePreferenceGroupHeader(
                title = stringResource(R.string.title_ui_settings),
                expanded = uiSettingsExpanded,
                onExpandedChange = { uiSettingsExpanded = it }
            )
            if (uiSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_liquid_glass),
                    summary = stringResource(R.string.summary_pref_liquid_glass),
                    checked = liquidGlassEnabled,
                    onCheckedChange = {
                        liquidGlassEnabled = it
                        ThemeManager.setLiquidGlassEnabled(it)
                    }
                )
                if (liquidGlassEnabled) {
                    SettingsListItem(
                        title = stringResource(R.string.title_pref_liquid_glass_intensity),
                        entries = liquidGlassIntensityEntries,
                        values = liquidGlassIntensityValues,
                        selectedValue = liquidGlassIntensity,
                        onSelected = {
                            liquidGlassIntensity = it
                            ThemeManager.setLiquidGlassIntensity(it)
                        }
                    )
                }
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_speed_enabled),
                    summary = stringResource(R.string.summary_pref_speed_enabled),
                    checked = speedEnabled,
                    onCheckedChange = { speedEnabled = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_dynamic_color),
                    summary = stringResource(R.string.summary_pref_dynamic_color),
                    checked = dynamicColor,
                    enabled = dynamicColorSupported,
                    onCheckedChange = {
                        dynamicColor = it
                        ThemeManager.setDynamicColorEnabled(it)
                    }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_language),
                    entries = languageEntries,
                    values = languageValues,
                    selectedValue = language,
                    onSelected = {
                        language = it
                        AppLocaleManager.setApplicationLanguage(it)
                    }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_ui_mode_night),
                    entries = uiModeNightEntries,
                    values = uiModeNightValues,
                    selectedValue = uiModeNight,
                    onSelected = {
                        uiModeNight = it
                        ThemeManager.setThemeMode(it)
                    }
                )
            }

            // 2. VPN Settings
            CollapsiblePreferenceGroupHeader(
                title = stringResource(R.string.title_vpn_settings),
                expanded = vpnSettingsExpanded,
                onExpandedChange = { vpnSettingsExpanded = it }
            )
            if (vpnSettingsExpanded) {
                SettingsListItem(
                    title = stringResource(R.string.title_pref_vpn_bypass_lan),
                    entries = bypassLanEntries,
                    values = bypassLanValues,
                    selectedValue = vpnBypassLan,
                    enabled = isVpn,
                    onSelected = { vpnBypassLan = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_use_hev_tunnel),
                    summary = stringResource(R.string.summary_pref_use_hev_tunnel),
                    checked = useHevTun,
                    enabled = isVpn,
                    onCheckedChange = { useHevTun = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_append_http_proxy),
                    summary = stringResource(R.string.summary_pref_append_http_proxy),
                    checked = appendHttpProxy,
                    enabled = isVpn,
                    onCheckedChange = { appendHttpProxy = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_local_dns_enabled),
                    summary = stringResource(R.string.summary_pref_local_dns_enabled),
                    checked = localDns,
                    enabled = isVpn,
                    onCheckedChange = { localDns = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_fake_dns_enabled),
                    summary = stringResource(R.string.summary_pref_fake_dns_enabled),
                    checked = fakeDns,
                    enabled = isVpn && localDns,
                    onCheckedChange = { fakeDns = it }
                )
            }

            // 3. Smart DNS Selector (Foreign only, automated ping-based)
            CollapsiblePreferenceGroupHeader(
                title = stringResource(R.string.title_dns_selector),
                expanded = dnsSettingsExpanded,
                onExpandedChange = { dnsSettingsExpanded = it }
            )
            if (dnsSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_auto_dns),
                    summary = stringResource(R.string.summary_pref_auto_dns),
                    checked = autoDns,
                    onCheckedChange = { autoDns = it }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_dns_preset),
                    entries = dnsPresetEntries,
                    values = dnsPresetValues,
                    selectedValue = dnsPresetMode,
                    onSelected = { selected ->
                        dnsPresetMode = selected
                        if (selected == "auto") {
                            scope.launch {
                                isBenchmarkingDns = true
                                val best = DnsSelectorManager.autoSelectBestDns(context)
                                isBenchmarkingDns = false
                                if (best != null) {
                                    remoteDns = MmkvManager.decodeSettingsString(AppConfig.PREF_REMOTE_DNS, "") ?: ""
                                    domesticDns = MmkvManager.decodeSettingsString(AppConfig.PREF_DOMESTIC_DNS, "") ?: ""
                                    val msg = context.getString(R.string.dns_test_success, best.first.displayName, best.second)
                                    context.toast(msg)
                                } else {
                                    context.toastError(R.string.dns_test_failed)
                                }
                            }
                        } else if (selected != "custom") {
                            val preset = DnsSelectorManager.getPresetById(selected)
                            if (preset != null) {
                                DnsSelectorManager.applyPreset(context, preset)
                                remoteDns = MmkvManager.decodeSettingsString(AppConfig.PREF_REMOTE_DNS, "") ?: ""
                                domesticDns = MmkvManager.decodeSettingsString(AppConfig.PREF_DOMESTIC_DNS, "") ?: ""
                            }
                        }
                    }
                )
                SettingsMenuItem(
                    title = stringResource(R.string.title_test_and_select_dns),
                    subtitle = if (isBenchmarkingDns) stringResource(R.string.dns_test_in_progress) else stringResource(R.string.summary_test_and_select_dns),
                    onClick = {
                        if (isBenchmarkingDns) return@SettingsMenuItem
                        scope.launch {
                            isBenchmarkingDns = true
                            val best = DnsSelectorManager.autoSelectBestDns(context)
                            isBenchmarkingDns = false
                            if (best != null) {
                                dnsPresetMode = "auto"
                                remoteDns = MmkvManager.decodeSettingsString(AppConfig.PREF_REMOTE_DNS, "") ?: ""
                                domesticDns = MmkvManager.decodeSettingsString(AppConfig.PREF_DOMESTIC_DNS, "") ?: ""
                                val msg = context.getString(R.string.dns_test_success, best.first.displayName, best.second)
                                context.toast(msg)
                            } else {
                                context.toastError(R.string.dns_test_failed)
                            }
                        }
                    }
                )
                if (dnsPresetMode == "custom") {
                    SettingsEditItem(
                        title = stringResource(R.string.title_pref_remote_dns),
                        value = remoteDns,
                        onValueChanged = { remoteDns = it }
                    )
                    SettingsEditItem(
                        title = stringResource(R.string.title_pref_domestic_dns),
                        value = domesticDns,
                        onValueChanged = { domesticDns = it }
                    )
                }
            }

            // 4. Core & LAN Sharing Settings
            CollapsiblePreferenceGroupHeader(
                title = stringResource(R.string.title_core_settings),
                expanded = coreSettingsExpanded,
                onExpandedChange = { coreSettingsExpanded = it }
            )
            if (coreSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_sniffing_enabled),
                    summary = stringResource(R.string.summary_pref_sniffing_enabled),
                    checked = sniffingEnabled,
                    onCheckedChange = { sniffingEnabled = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_route_only_enabled),
                    summary = stringResource(R.string.summary_pref_route_only_enabled),
                    checked = routeOnlyEnabled,
                    onCheckedChange = { routeOnlyEnabled = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_proxy_sharing_enabled),
                    summary = stringResource(R.string.summary_pref_proxy_sharing_enabled),
                    checked = proxySharing,
                    onCheckedChange = { proxySharing = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_socks_port),
                    value = socksPort,
                    keyboardNumber = true,
                    onValueChanged = { socksPort = it }
                )
            }

            // 5. Advanced Settings
            CollapsiblePreferenceGroupHeader(
                title = stringResource(R.string.title_advanced),
                expanded = advancedSettingsExpanded,
                onExpandedChange = { advancedSettingsExpanded = it }
            )
            if (advancedSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_is_booted),
                    summary = stringResource(R.string.summary_pref_is_booted),
                    checked = isBooted,
                    onCheckedChange = { isBooted = it }
                )
                if (systemVpnSettingsAvailable) {
                    SettingsMenuItem(
                        title = stringResource(R.string.title_system_vpn_settings),
                        subtitle = stringResource(R.string.summary_system_vpn_settings),
                        onClick = onSystemVpnSettingsClicked
                    )
                }
            }

            // 6. Mode & Root Settings
            CollapsiblePreferenceGroupHeader(
                title = stringResource(R.string.title_mode_settings),
                expanded = modeSettingsExpanded,
                onExpandedChange = { modeSettingsExpanded = it }
            )
            if (modeSettingsExpanded) {
                SettingsListItem(
                    title = stringResource(R.string.title_mode),
                    entries = modeEntries,
                    values = modeValues,
                    selectedValue = mode,
                    onSelected = { mode = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_root_mode_enabled),
                    summary = stringResource(R.string.summary_root_mode_enabled),
                    checked = enableRootMode,
                    onCheckedChange = { newValue ->
                        if (newValue && !RootManager.cachedRoot()) {
                            viewModel.checkAndRequestRoot {
                                enableRootMode = true
                            }
                        } else {
                            enableRootMode = newValue
                        }
                    }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_root_lan_sharing),
                    summary = stringResource(R.string.summary_root_lan_sharing),
                    checked = lanSharing,
                    onCheckedChange = { newValue ->
                        if (newValue && !RootManager.cachedRoot()) {
                            viewModel.checkAndRequestRoot {
                                lanSharing = true
                            }
                        } else {
                            lanSharing = newValue
                        }
                    }
                )
            }

            // 7. Update Settings
            CollapsiblePreferenceGroupHeader(
                title = stringResource(R.string.title_update_settings),
                expanded = updateSettingsExpanded,
                onExpandedChange = { updateSettingsExpanded = it }
            )
            if (updateSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_auto_check_update),
                    summary = stringResource(R.string.summary_pref_auto_check_update),
                    checked = autoCheckUpdate,
                    onCheckedChange = { autoCheckUpdate = it }
                )
                SettingsMenuItem(
                    title = stringResource(R.string.update_check_for_update),
                    onClick = onCheckUpdateClicked
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            NavigationBarsSpacer()
        }
    }
}
