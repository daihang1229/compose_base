package com.rainbow.compose.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 自定义下拉刷新 / 上拉加载容器，通过 [NestedScrollConnection] 捕获列表到顶/到底后的手势。
 *
 * @param isRefreshing   是否正在刷新
 * @param onRefresh      下拉刷新回调
 * @param isLoadingMore  是否正在加载更多
 * @param onLoadMore     上拉加载更多回调
 * @param hasMore        是否还有更多数据（false 时禁用上拉手势）
 * @param modifier       容器 Modifier
 * @param refreshTrigger 下拉触发阈值，默认 100.dp
 * @param loadMoreTrigger 上拉触发阈值，默认 76.dp（与 indicator 高度一致）
 * @param holdOffset     刷新/加载时指示器保留高度，默认 76.dp
 * @param maxOffset      最大下拉/上拉高度，默认 160.dp
 * @param dragMultiplier 下拉阻尼系数，默认 0.5
 * @param minRefreshDurationMillis 下拉刷新最短展示时长，默认 1200ms；小于等于 0 时关闭
 * @param minLoadMoreDurationMillis 上拉加载最短展示时长，默认 1200ms；小于等于 0 时关闭
 * @param consumeLoadMoreOffset 同步消费底部收起释放的正向滚动像素，并返回列表实际消费距离
 * @param indicator      刷新/加载指示器
 * @param content        内容 composable，接收一个带有 offset 的 Modifier
 */
@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    hasMore: Boolean = true,
    modifier: Modifier = Modifier,
    refreshTrigger: Dp = 100.dp,
    loadMoreTrigger: Dp = 76.dp,
    holdOffset: Dp = 76.dp,
    maxOffset: Dp = 160.dp,
    dragMultiplier: Float = 0.5f,
    minRefreshDurationMillis: Long = 1_200L,
    minLoadMoreDurationMillis: Long = 1_200L,
    consumeLoadMoreOffset: (Float) -> Float = { 0f },
    indicator: @Composable (progress: Float, isRefreshing: Boolean, indicatorModifier: Modifier) -> Unit =
        { progress, refreshing, m ->
            ClassicsRefreshIndicator(isRefreshing = refreshing, progress = progress, modifier = m)
        },
    content: @Composable (contentModifier: Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val triggerPx = with(density) { refreshTrigger.toPx() }
    val loadMoreTriggerPx = with(density) { loadMoreTrigger.toPx() }
    val holdPx = with(density) { holdOffset.toPx() }
    val maxPx = with(density) { maxOffset.toPx() }

    val topOffset = remember { Animatable(0f) }
    val bottomOffset = remember { Animatable(0f) }
    val refreshState = remember { LoadingPresentationState(SystemClock::uptimeMillis) }
    val loadMoreState = remember { LoadingPresentationState(SystemClock::uptimeMillis) }
    val scope = rememberCoroutineScope()
    val isRefreshingRef = rememberUpdatedState(isRefreshing)
    val isLoadingMoreRef = rememberUpdatedState(isLoadingMore)
    val hasMoreRef = rememberUpdatedState(hasMore)
    val onRefreshRef = rememberUpdatedState(onRefresh)
    val onLoadMoreRef = rememberUpdatedState(onLoadMore)

    val scrollConnection = remember {
        object : NestedScrollConnection {
            private fun isBusy(): Boolean = refreshState.isActive || loadMoreState.isActive ||
                isRefreshingRef.value || isLoadingMoreRef.value

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isBusy()) return Offset.Zero
                if (topOffset.isRunning || bottomOffset.isRunning) return Offset.Zero
                if (topOffset.value > 0 && available.y < 0) {
                    val newOffset = (topOffset.value + available.y).coerceAtLeast(0f)
                    scope.launch {
                        if (!isBusy()) topOffset.snapTo(newOffset)
                    }
                    return Offset(0f, available.y)
                }
                if (bottomOffset.value > 0 && available.y > 0) {
                    val newOffset = (bottomOffset.value - available.y).coerceAtLeast(0f)
                    scope.launch {
                        if (!isBusy()) bottomOffset.snapTo(newOffset)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (topOffset.isRunning || bottomOffset.isRunning) return Offset.Zero
                if (isBusy()) return Offset.Zero
                if (source != NestedScrollSource.Drag) return Offset.Zero

                val currentTop = topOffset.value
                val currentBottom = bottomOffset.value

                when {
                    available.y > 0 -> {
                        val newOffset = (currentTop + available.y * dragMultiplier)
                            .coerceIn(0f, maxPx)
                        scope.launch {
                            if (!isBusy()) topOffset.snapTo(newOffset)
                        }
                        return Offset(0f, available.y)
                    }
                    available.y < 0 && currentTop > 0f -> {
                        val newOffset = (currentTop + available.y).coerceAtLeast(0f)
                        scope.launch {
                            if (!isBusy()) topOffset.snapTo(newOffset)
                        }
                        return Offset(0f, available.y)
                    }
                    available.y < 0 && hasMoreRef.value -> {
                        val newOffset = (currentBottom - available.y * dragMultiplier)
                            .coerceIn(0f, maxPx)
                        scope.launch {
                            if (!isBusy()) bottomOffset.snapTo(newOffset)
                        }
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isBusy()) return Velocity.Zero
                when {
                    topOffset.value >= triggerPx -> {
                        refreshState.start()
                        onRefreshRef.value()
                    }
                    bottomOffset.value >= loadMoreTriggerPx && hasMoreRef.value -> {
                        loadMoreState.start()
                        onLoadMoreRef.value()
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (isBusy()) return Velocity.Zero
                if (bottomOffset.value > 0) {
                    bottomOffset.animateTo(0f)
                    return Velocity.Zero
                }
                if (topOffset.value > 0) {
                    topOffset.animateTo(0f)
                }
                return Velocity.Zero
            }
        }
    }

    LoadingPresentationEffect(isRefreshing, refreshState, topOffset, holdPx, minRefreshDurationMillis)
    LoadingPresentationEffect(
        isLoadingMore, loadMoreState, bottomOffset, holdPx, minLoadMoreDurationMillis, consumeLoadMoreOffset,
    )

    val topPx = topOffset.value
    val bottomPx = bottomOffset.value
    val isRefreshVisible = isRefreshing || refreshState.isActive
    val isLoadMoreVisible = isLoadingMore || loadMoreState.isActive
    val pullProgress = (topPx / triggerPx).coerceIn(0f, 1f)
    val loadMoreProgress = (bottomPx / loadMoreTriggerPx).coerceIn(0f, 1f)
    val indicatorHeightPx = with(density) { 76.dp.toPx() }

    Box(modifier = modifier.nestedScroll(scrollConnection).clipToBounds()) {
        content(Modifier.graphicsLayer { translationY = topOffset.value - bottomOffset.value })

        if (topPx > 0 || isRefreshVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = topOffset.value - indicatorHeightPx
                    }
            ) {
                indicator(
                    if (isRefreshVisible) 1f else pullProgress,
                    isRefreshVisible,
                    Modifier
                )
            }
        }

        if (bottomPx > 0 || isLoadMoreVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = indicatorHeightPx - bottomOffset.value
                    }
            ) {
                indicator(
                    if (isLoadMoreVisible) 1f else loadMoreProgress,
                    isLoadMoreVisible,
                    Modifier
                )
            }
        }
    }
}

@Composable
private fun LoadingPresentationEffect(
    isLoading: Boolean,
    state: LoadingPresentationState,
    offset: Animatable<Float, AnimationVector1D>,
    holdPx: Float,
    minimumDurationMillis: Long,
    consumeContentOffset: ((Float) -> Float)? = null,
) {
    val isLoadingRef = rememberUpdatedState(isLoading)
    val consumeContentOffsetRef = rememberUpdatedState(consumeContentOffset)
    LaunchedEffect(isLoading) {
        if (isLoading) state.onRequestStarted()
    }
    LaunchedEffect(isLoading, state.session, minimumDurationMillis, holdPx) {
        val session = state.session ?: return@LaunchedEffect
        offset.snapTo(holdPx)
        if (isLoading) return@LaunchedEffect

        delay(state.remainingMillis(session, minimumDurationMillis))
        if (consumeContentOffsetRef.value != null) withFrameNanos { }
        if (isLoadingRef.value || !state.beginSettling(session)) return@LaunchedEffect
        val compensation = consumeContentOffsetRef.value?.let { LoadMoreScrollCompensation(offset.value) }
        val damping = if (compensation == null) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy
        offset.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = damping,
                stiffness = Spring.StiffnessLow,
            ),
        ) {
            if (!isLoadingRef.value && state.isCurrent(session)) {
                consumeContentOffsetRef.value?.let { compensation?.consume(value, it) }
            }
        }
        if (!isLoadingRef.value) state.finish(session)
    }
}
