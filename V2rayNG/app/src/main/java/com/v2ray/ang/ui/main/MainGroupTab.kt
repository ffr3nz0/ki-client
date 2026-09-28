package com.v2ray.ang.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.ui.compose.colorFabActive
import kotlinx.coroutines.flow.StateFlow

@Composable
fun GroupTabBar(
    groups: List<GroupMapItem>,
    selectedTabIndex: Int,
    mainViewModel: MainViewModel,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = selectedTabIndex.coerceIn(0, groups.lastIndex)
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in groups.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(
            items = groups,
            key = { _, group -> group.id.ifEmpty { "all" } }
        ) { index, group ->
            val serverFlow = remember(group.id, mainViewModel) {
                mainViewModel.serversForGroup(group.id)
            }
            GroupTabPill(
                group = group,
                selected = index == selectedIndex,
                serverFlow = serverFlow,
                onClick = { onTabClick(index) }
            )
        }
    }
}

@Composable
private fun GroupTabPill(
    group: GroupMapItem,
    selected: Boolean,
    serverFlow: StateFlow<List<ServersCache>>,
    onClick: () -> Unit
) {
    val servers by serverFlow.collectAsStateWithLifecycle()

    val isDark = com.v2ray.ang.ui.compose.LocalDarkTheme.current
    val targetBgColor = if (isDark) {
        if (selected) Color(0xFF0C2216) else Color(0xFF14161F)
    } else {
        if (selected) Color(0xFFD1FAE5) else Color(0xFFF1F5F9)
    }
    val targetBorderColor = if (isDark) {
        if (selected) colorFabActive else Color(0xFF2C2F3A)
    } else {
        if (selected) colorFabActive else Color(0xFFE2E8F0)
    }
    val textColor = if (isDark) {
        if (selected) colorFabActive else Color(0xFFB5B5C3)
    } else {
        if (selected) Color(0xFF047857) else Color(0xFF475569)
    }
    val counterBg = if (isDark) {
        if (selected) colorFabActive.copy(alpha = 0.22f) else Color(0xFF242735)
    } else {
        if (selected) colorFabActive.copy(alpha = 0.25f) else Color(0xFFE2E8F0)
    }
    val counterText = if (isDark) {
        if (selected) colorFabActive else Color(0xFF8E8E93)
    } else {
        if (selected) Color(0xFF047857) else Color(0xFF64748B)
    }
    val iconTint = if (isDark) {
        if (selected) colorFabActive else Color(0xFF7E8299)
    } else {
        if (selected) Color(0xFF047857) else Color(0xFF64748B)
    }

    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "pillBg"
    )

    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "pillBorder"
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (group.id.isNotEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.ic_subscriptions_24dp),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = group.remarks,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(7.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(counterBg)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${servers.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = counterText
                )
            }
        }
    }
}
