package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager.rememberMmkvBool
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.colorFabActive
import com.v2ray.ang.ui.compose.verticalScrollbar

@Composable
fun MainTopBar(
    isLoading: Boolean,
    showSearch: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onAction: (MainAction) -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit
) {
    var showImportMenu by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val isLanSharingActive by rememberMmkvBool(AppConfig.PREF_PROXY_SHARING, false)
    val importMenuScrollState = rememberScrollState()
    val moreMenuScrollState = rememberScrollState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp - statusBarHeight - navBarHeight - 20.dp

    val appName = stringResource(R.string.app_name)
    val isDark = com.v2ray.ang.ui.compose.LocalDarkTheme.current
    val titleTextColor = if (isDark) Color.White else Color(0xFF1E293B)
    val annotatedTitle = remember(appName, titleTextColor) {
        buildAnnotatedString {
            if (appName.contains("-Client", ignoreCase = true)) {
                val idx = appName.indexOf("-Client", ignoreCase = true)
                withStyle(SpanStyle(color = titleTextColor)) {
                    append(appName.substring(0, idx))
                }
                withStyle(SpanStyle(color = colorFabActive)) {
                    append(appName.substring(idx))
                }
            } else if (appName.contains("Client", ignoreCase = true)) {
                val idx = appName.indexOf("Client", ignoreCase = true)
                withStyle(SpanStyle(color = titleTextColor)) {
                    append(appName.substring(0, idx))
                }
                withStyle(SpanStyle(color = colorFabActive)) {
                    append(appName.substring(idx))
                }
            } else {
                withStyle(SpanStyle(color = titleTextColor)) {
                    append(appName)
                }
            }
        }
    }

    AppTopBar(
        title = appName,
        onBackClick = {},
        isLoading = isLoading,
        isSearchActive = showSearch,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onSearchClose = onSearchClose,
        searchPlaceholder = stringResource(R.string.menu_item_search),
        titleContent = {
            Text(
                text = annotatedTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        },
        navigationIcon = {
            if (showSearch) {
                IconButton(onClick = onSearchClose) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back_24dp),
                        contentDescription = stringResource(R.string.acc_back),
                        tint = Color.White
                    )
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        painterResource(R.drawable.ic_menu_24dp),
                        contentDescription = stringResource(R.string.acc_open_menu),
                        tint = colorFabActive
                    )
                }
            }
        },
        actions = {
            if (!showSearch) {
                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(
                        painterResource(R.drawable.ic_search_24dp),
                        contentDescription = stringResource(R.string.acc_search),
                        tint = Color.White
                    )
                }

                IconButton(onClick = { onAction(MainAction.OpenLanShare) }) {
                    Icon(
                        painterResource(R.drawable.ic_share_24dp),
                        contentDescription = stringResource(R.string.lan_share_title),
                        tint = if (isLanSharingActive) colorFabActive else (if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF475569))
                    )
                }

                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { showImportMenu = true }) {
                        Icon(
                            painterResource(R.drawable.ic_add_24dp),
                            contentDescription = stringResource(R.string.acc_add),
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showImportMenu,
                        onDismissRequest = { showImportMenu = false },
                        modifier = Modifier
                            .heightIn(max = maxMenuHeight)
                            .verticalScrollbar(importMenuScrollState)
                    ) {
                        ImportMenuContent(
                            onAction = { action ->
                                showImportMenu = false
                                onAction(action)
                            }
                        )
                    }
                }

                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            painterResource(R.drawable.ic_more_vert_24dp),
                            contentDescription = stringResource(R.string.acc_more),
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .heightIn(max = maxMenuHeight)
                            .verticalScrollbar(moreMenuScrollState)
                    ) {
                        MoreMenuContent(
                            onSelected = { action ->
                                showMenu = false
                                onMoreMenuAction(action)
                            }
                        )
                    }
                }
            }
        }
    )
}
