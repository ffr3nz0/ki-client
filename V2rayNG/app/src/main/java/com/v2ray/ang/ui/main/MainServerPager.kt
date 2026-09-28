package com.v2ray.ang.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.v2ray.ang.ui.compose.LocalDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.LocateTarget
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.ui.compose.ReorderableListItem
import com.v2ray.ang.ui.subscription.SubscriptionUserinfoCard
import com.v2ray.ang.ui.subscription.shouldShowUserinfoCard
import com.v2ray.ang.ui.compose.colorFabActive
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.colorPingRed
import com.v2ray.ang.ui.compose.verticalScrollbar
import java.util.Locale
import kotlin.math.abs
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun GroupPagerPage(
    groupId: String,
    mainViewModel: MainViewModel,
    selectedGuid: String?,
    locateTarget: LocateTarget?,
    doubleColumnDisplay: Boolean,
    searchQuery: String,
    lazyListStates: MutableMap<String, LazyListState>,
    lazyGridStates: MutableMap<String, LazyGridState>,
    onSelectServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String) -> Unit,
    contentPadding: PaddingValues,
    isRunning: Boolean = false,
    status: MainStatus = MainStatus.Disconnected,
    durationSeconds: Long = 0L,
    onToggleService: () -> Unit = {},
    onTestClick: () -> Unit = {}
) {
    val groupStateFlow = remember(groupId) {
        mainViewModel.serverGroupState(groupId)
    }
    val groupState by groupStateFlow.collectAsStateWithLifecycle()
    val canReorder = groupId.isNotEmpty() && searchQuery.isEmpty()
    val actions = remember(
        onSelectServer,
        onEditServer,
        onShareServer,
        onMoreServer,
        onRemoveServer,
        onToggleService,
        onTestClick
    ) {
        ServerRowActions(
            select = onSelectServer,
            edit = onEditServer,
            share = onShareServer,
            more = onMoreServer,
            remove = onRemoveServer,
            toggleService = onToggleService,
            testClick = onTestClick
        )
    }

    val rows = groupState.rows

    ServerListPage(
        rows = rows,
        selectedGuid = selectedGuid,
        locateTarget = locateTarget?.takeIf { it.groupId == groupId },
        canReorder = canReorder,
        doubleColumnDisplay = doubleColumnDisplay,
        groupId = groupId,
        lazyListStates = lazyListStates,
        lazyGridStates = lazyGridStates,
        actions = actions,
        onLocateHandled = { mainViewModel.onAction(MainAction.LocateHandled) },
        onMoveServer = { fromIndex, toIndex ->
            mainViewModel.moveServer(groupId, fromIndex, toIndex)
        },
        contentPadding = contentPadding,
        isRunning = isRunning,
        status = status,
        durationSeconds = durationSeconds,
        subscription = groupState.subscription,
        onRefreshSubscription = { mainViewModel.onAction(MainAction.UpdateSubscription(groupId)) }
    )
}

private class ServerRowActions(
    val select: (String) -> Unit,
    val edit: (String, ProfileItem) -> Unit,
    val share: (String, ProfileItem) -> Unit,
    val more: (String, ProfileItem) -> Unit,
    val remove: (String) -> Unit,
    val toggleService: () -> Unit = {},
    val testClick: () -> Unit = {}
)

@Composable
private fun ServerListPage(
    rows: List<ServerRowUiModel>,
    selectedGuid: String?,
    locateTarget: LocateTarget?,
    canReorder: Boolean,
    doubleColumnDisplay: Boolean,
    groupId: String,
    lazyListStates: MutableMap<String, LazyListState>,
    lazyGridStates: MutableMap<String, LazyGridState>,
    actions: ServerRowActions,
    onLocateHandled: () -> Unit,
    onMoveServer: (Int, Int) -> Unit,
    contentPadding: PaddingValues,
    isRunning: Boolean = false,
    status: MainStatus = MainStatus.Disconnected,
    durationSeconds: Long = 0L,
    subscription: SubscriptionItem? = null,
    onRefreshSubscription: () -> Unit = {}
) {
    val showCard = subscription != null && shouldShowUserinfoCard(subscription)
    val headerOffset = if (showCard) 1 else 0

    if (rows.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (showCard && subscription != null) {
                SubscriptionUserinfoCard(
                    subscription = subscription,
                    onRefresh = onRefreshSubscription,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cloud_download_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.app_tile_first_use),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        return
    }

    if (doubleColumnDisplay) {
        val gridState = remember(groupId) {
            lazyGridStates.getOrPut(groupId) { LazyGridState() }
        }
        val reorderableGridState = if (canReorder) {
            rememberReorderableLazyGridState(gridState) { from, to ->
                val fromPos = from.index - headerOffset
                val toPos = to.index - headerOffset
                if (fromPos in rows.indices && toPos in rows.indices) {
                    onMoveServer(fromPos, toPos)
                }
            }
        } else null

        LocateTargetEffect(locateTarget, rows, gridState, onLocateHandled, headerOffset)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(gridState),
            state = gridState,
            contentPadding = contentPadding
        ) {
            if (showCard && subscription != null) {
                item(
                    key = "sub_userinfo_card",
                    span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                ) {
                    SubscriptionUserinfoCard(
                        subscription = subscription,
                        onRefresh = onRefreshSubscription,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            itemsIndexed(
                items = rows,
                key = { _, item -> item.guid }
            ) { _, row ->
                Column(modifier = Modifier.animateItem()) {
                    if (canReorder && reorderableGridState != null) {
                        ReorderableItem(reorderableGridState, key = row.guid) { isDragging ->
                            ReorderableListItem(this, isDragging) {
                                ServerItemColumn(
                                    row = row,
                                    isSelected = (row.guid == selectedGuid),
                                    doubleColumnDisplay = true,
                                    actions = actions,
                                    isRunning = isRunning,
                                    status = status,
                                    durationSeconds = durationSeconds
                                )
                            }
                        }
                    } else {
                        ServerItemColumn(
                            row = row,
                            isSelected = (row.guid == selectedGuid),
                            doubleColumnDisplay = true,
                            actions = actions,
                            isRunning = isRunning,
                            status = status,
                            durationSeconds = durationSeconds
                        )
                    }
                }
            }
        }
    } else {
        val listState = remember(groupId) {
            lazyListStates.getOrPut(groupId) { LazyListState() }
        }
        val reorderableState = if (canReorder) {
            rememberReorderableLazyListState(listState) { from, to ->
                val fromPos = from.index - headerOffset
                val toPos = to.index - headerOffset
                if (fromPos in rows.indices && toPos in rows.indices) {
                    onMoveServer(fromPos, toPos)
                }
            }
        } else null

        LocateTargetEffect(locateTarget, rows, listState, onLocateHandled, headerOffset)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(listState),
            state = listState,
            contentPadding = contentPadding
        ) {
            if (showCard && subscription != null) {
                item(key = "sub_userinfo_card") {
                    SubscriptionUserinfoCard(
                        subscription = subscription,
                        onRefresh = onRefreshSubscription,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            itemsIndexed(
                items = rows,
                key = { _, item -> item.guid }
            ) { _, row ->
                Column(modifier = Modifier.animateItem()) {
                    if (canReorder && reorderableState != null) {
                        ReorderableItem(reorderableState, key = row.guid) { isDragging ->
                            ReorderableListItem(this, isDragging) {
                                ServerItemRow(
                                    row = row,
                                    isSelected = (row.guid == selectedGuid),
                                    actions = actions,
                                    isRunning = isRunning,
                                    status = status,
                                    durationSeconds = durationSeconds
                                )
                            }
                        }
                    } else {
                        ServerItemRow(
                            row = row,
                            isSelected = (row.guid == selectedGuid),
                            actions = actions,
                            isRunning = isRunning,
                            status = status,
                            durationSeconds = durationSeconds
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocateTargetEffect(
    target: LocateTarget?,
    rows: List<ServerRowUiModel>,
    state: LazyListState,
    onHandled: () -> Unit,
    headerOffset: Int = 0
) {
    if (target != null) {
        LaunchedEffect(target, rows) {
            val index = rows.indexOfFirst { it.guid == target.serverGuid }
            if (index >= 0) {
                state.scrollToItem(index + headerOffset, -state.layoutInfo.viewportSize.height / 3)
            }
            onHandled()
        }
    }
}

@Composable
private fun LocateTargetEffect(
    target: LocateTarget?,
    rows: List<ServerRowUiModel>,
    state: LazyGridState,
    onHandled: () -> Unit,
    headerOffset: Int = 0
) {
    if (target != null) {
        LaunchedEffect(target, rows) {
            val index = rows.indexOfFirst { it.guid == target.serverGuid }
            if (index >= 0) {
                state.scrollToItem(index + headerOffset, -state.layoutInfo.viewportSize.height / 3)
            }
            onHandled()
        }
    }
}

@Composable
private fun ServerItemRow(
    row: ServerRowUiModel,
    isSelected: Boolean,
    actions: ServerRowActions,
    isRunning: Boolean = false,
    status: MainStatus = MainStatus.Disconnected,
    durationSeconds: Long = 0L
) {
    ServerListItem(
        row = row,
        isSelected = isSelected,
        doubleColumnDisplay = false,
        actions = actions,
        isRunning = isRunning,
        status = status,
        durationSeconds = durationSeconds
    )
}

@Composable
private fun ServerItemColumn(
    row: ServerRowUiModel,
    isSelected: Boolean,
    doubleColumnDisplay: Boolean,
    actions: ServerRowActions,
    isRunning: Boolean = false,
    status: MainStatus = MainStatus.Disconnected,
    durationSeconds: Long = 0L
) {
    ServerListItem(
        row = row,
        isSelected = isSelected,
        doubleColumnDisplay = doubleColumnDisplay,
        actions = actions,
        isRunning = isRunning,
        status = status,
        durationSeconds = durationSeconds
    )
}

@Composable
private fun AnimatedLiveWave(
    isRunning: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    if (!isRunning) return
    val transition = rememberInfiniteTransition(label = "liveWave")
    val h1 by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.90f, targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(540, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.30f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(620, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by transition.animateFloat(
        initialValue = 0.85f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )
    val h5 by transition.animateFloat(
        initialValue = 0.40f, targetValue = 0.80f,
        animationSpec = infiniteRepeatable(tween(580, easing = LinearEasing), RepeatMode.Reverse),
        label = "h5"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(h1, h2, h3, h4, h5).forEach { factor ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((13 * factor).dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(tint)
            )
        }
    }
}

@Composable
private fun ServerListItem(
    row: ServerRowUiModel,
    isSelected: Boolean,
    doubleColumnDisplay: Boolean,
    actions: ServerRowActions,
    isRunning: Boolean = false,
    status: MainStatus = MainStatus.Disconnected,
    durationSeconds: Long = 0L
) {
    val isItemRunning = isSelected && isRunning
    val isDark = LocalDarkTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "serverPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val formattedDuration = if (durationSeconds > 0) {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        "00:00:00"
    }

    val selectedStateDescription = if (isSelected) {
        stringResource(R.string.acc_selected_server)
    } else {
        null
    }

    val targetBgColor = if (isDark) {
        if (isItemRunning) Color(0xFF0F1713) else if (isSelected) Color(0xFF131B16) else Color(0xFF13141B)
    } else {
        if (isItemRunning) Color(0xFFE8FDF3) else if (isSelected) Color(0xFFF0FDF4) else Color(0xFFFFFFFF)
    }
    val targetBorderColor = if (isDark) {
        if (isSelected) colorFabActive.copy(alpha = 0.85f) else Color(0xFF232532)
    } else {
        if (isSelected) colorFabActive else Color(0xFFE2E8F0)
    }

    val cardBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "serverCardBg"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "serverCardBorder"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics {
                if (selectedStateDescription != null) {
                    stateDescription = selectedStateDescription
                }
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable { actions.select(row.guid) },
        shape = RoundedCornerShape(18.dp),
        color = cardBgColor,
        shadowElevation = if (isSelected) 3.dp else 0.dp,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        border = BorderStroke(1.2.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (isSelected) {
                // ROW 1: Status & Center Live Wave & Timer / Protocol badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isItemRunning) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colorFabActive),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_fab_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            val notConnectedText = stringResource(R.string.connection_not_connected)
                                .split(".", "\n", "。").firstOrNull()?.trim().orEmpty()
                            Text(
                                text = notConnectedText,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else Color(0xFF64748B),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(colorFabActive.copy(alpha = pulseAlpha))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colorFabActive)
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            val connectedText = stringResource(R.string.connection_connected)
                                .split(".", "\n", "。").firstOrNull()?.trim().orEmpty()
                            Text(
                                text = connectedText,
                                color = colorFabActive,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Center Live Equalizer Wave
                    if (isItemRunning) {
                        AnimatedLiveWave(
                            isRunning = true,
                            tint = colorFabActive,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    // Right Timer Badge or Protocol Chip
                    if (isItemRunning) {
                        val timerBg = if (isDark) Color(0xFF0C2B1D) else Color(0xFFD1FAE5)
                        val timerBorder = if (isDark) colorFabActive.copy(alpha = 0.6f) else Color(0xFF10B981)
                        val timerText = if (isDark) colorFabActive else Color(0xFF047857)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(timerBg)
                                .border(1.dp, timerBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_clock_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = timerText
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formattedDuration,
                                    color = timerText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        val protoBg = if (isDark) Color(0xFF0C2B1D) else Color(0xFFD1FAE5)
                        val protoText = if (isDark) colorFabActive else Color(0xFF047857)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(protoBg)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text(
                                text = row.typeDescription,
                                color = protoText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ROW 2: Remarks, stats/test delay, and circular Power button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = row.remarks,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineBreak = LineBreak.Paragraph
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isItemRunning) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) Color(0xFF0C2B1D) else Color(0xFFD1FAE5))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    Text(
                                        text = row.typeDescription,
                                        color = if (isDark) colorFabActive else Color(0xFF047857),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (row.statistics.isNotBlank()) {
                                Text(
                                    text = row.statistics,
                                    color = if (isDark) Color(0xFF8F94A6) else Color(0xFF64748B),
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        val (testText, testColor) = if (isItemRunning && (status is MainStatus.Testing || status is MainStatus.TestProgress)) {
                            Pair(stringResource(R.string.connection_test_testing), colorFabActive)
                        } else if (isItemRunning && status is MainStatus.ConnectionTest) {
                            val delay = status.result.delayMillis
                            if (delay >= 0) {
                                Pair("$delay ms", colorFabActive)
                            } else {
                                Pair(stringResource(R.string.connection_test_fail), colorPingRed)
                            }
                        } else if (row.testDelayMillis != 0L) {
                            if (row.testDelayMillis < 0) {
                                Pair(stringResource(R.string.connection_test_fail), colorPingRed)
                            } else {
                                Pair("${row.testDelayMillis} ms", colorFabActive)
                            }
                        } else {
                            Pair(stringResource(R.string.connection_test_pending), colorFabActive)
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { actions.testClick() }
                                .padding(horizontal = 2.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_signal_cellular_alt_24dp),
                                contentDescription = stringResource(R.string.connection_test_pending),
                                modifier = Modifier.size(15.dp),
                                tint = testColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = testText,
                                color = testColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Circular Power Button Box with 3D gloss & breathing pulse
                    Box(
                        modifier = Modifier.size(62.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isItemRunning) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .border(2.dp, colorFabActive.copy(alpha = pulseAlpha), CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isItemRunning) colorFabActive
                                    else if (isDark) Color(0xFF222533)
                                    else Color(0xFFF1F5F9)
                                )
                                .border(
                                    width = if (isItemRunning) 0.dp else 1.2.dp,
                                    color = if (isItemRunning) Color.Transparent
                                    else if (isDark) Color(0xFF333748)
                                    else Color(0xFFCBD5E1),
                                    shape = CircleShape
                                )
                                .drawWithContent {
                                    drawContent()
                                    // 3D Glass Lens reflection on top half
                                    drawCircle(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isItemRunning) 0.45f else 0.18f),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY = this.size.height * 0.55f
                                        ),
                                        radius = this.size.width * 0.48f,
                                        center = Offset(this.size.width * 0.5f, this.size.height * 0.35f)
                                    )
                                }
                                .clickable { actions.toggleService() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_power_24dp),
                                contentDescription = stringResource(if (isItemRunning) R.string.acc_stop else R.string.acc_start),
                                modifier = Modifier.size(28.dp),
                                tint = if (isItemRunning) Color.Black else colorFabActive
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ROW 3: Subscription badge & action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (row.subscriptionBadge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colorFabActive.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = row.subscriptionBadge.uppercase(Locale.ROOT),
                                color = if (isDark) colorFabActive else Color(0xFF047857),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val actionIconTint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                        if (doubleColumnDisplay) {
                            IconButton(
                                onClick = { actions.more(row.guid, row.profile) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert_24dp),
                                    contentDescription = stringResource(R.string.acc_more),
                                    modifier = Modifier.size(18.dp),
                                    tint = actionIconTint
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { actions.share(row.guid, row.profile) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_share_24dp),
                                    contentDescription = stringResource(R.string.title_configuration_share),
                                    modifier = Modifier.size(18.dp),
                                    tint = actionIconTint
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { actions.edit(row.guid, row.profile) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit_24dp),
                                    contentDescription = stringResource(R.string.acc_edit),
                                    modifier = Modifier.size(18.dp),
                                    tint = actionIconTint
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { actions.remove(row.guid) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete_24dp),
                                    contentDescription = stringResource(R.string.acc_delete),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            } else {
                // UNSELECTED ITEM
                // ROW 1: Unselected radio circle, remarks, protocol badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isDark) Color(0xFF555869) else Color(0xFFCBD5E1), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = row.remarks,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineBreak = LineBreak.Paragraph
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF0C2B1D) else Color(0xFFD1FAE5))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = row.typeDescription,
                            color = if (isDark) colorFabActive else Color(0xFF047857),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // ROW 2: Statistics and test delay badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.statistics,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 30.dp),
                        color = if (isDark) Color(0xFF8F94A6) else Color(0xFF64748B),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val testDelay = row.testDelayMillis
                    val testDelayText = if (testDelay == 0L) {
                        ""
                    } else {
                        stringResource(R.string.server_test_delay_value, testDelay)
                    }
                    if (testDelayText.isNotEmpty()) {
                        val pingColor = if (testDelay < 0L) colorPingRed else (if (isDark) colorFabActive else Color(0xFF047857))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(pingColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text(
                                text = testDelayText,
                                color = pingColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // BOTTOM ROW: Subscription badge & action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (row.subscriptionBadge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colorFabActive.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = row.subscriptionBadge.uppercase(Locale.ROOT),
                                color = if (isDark) colorFabActive else Color(0xFF047857),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val actionIconTint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                        if (doubleColumnDisplay) {
                            IconButton(
                                onClick = { actions.more(row.guid, row.profile) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert_24dp),
                                    contentDescription = stringResource(R.string.acc_more),
                                    modifier = Modifier.size(18.dp),
                                    tint = actionIconTint
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { actions.share(row.guid, row.profile) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_share_24dp),
                                    contentDescription = stringResource(R.string.title_configuration_share),
                                    modifier = Modifier.size(18.dp),
                                    tint = actionIconTint
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { actions.edit(row.guid, row.profile) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit_24dp),
                                    contentDescription = stringResource(R.string.acc_edit),
                                    modifier = Modifier.size(18.dp),
                                    tint = actionIconTint
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { actions.remove(row.guid) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete_24dp),
                                    contentDescription = stringResource(R.string.acc_delete),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun PagerState.navigateToPageOptimized(targetPage: Int, animateAdjacentPage: Boolean = true) {
    if (pageCount > 0) {
        val target = targetPage.coerceIn(0, pageCount - 1)
        val current = settledPage.coerceIn(0, pageCount - 1)
        if (target != current) {
            if (abs(target - current) == 1 && animateAdjacentPage) {
                animateScrollToPage(target, 0f, tween(350, easing = FastOutSlowInEasing))
            } else {
                scrollToPage(target)
            }
        }
    }
}
