package com.rainbow.compose.ui.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.rainbow.compose.core.contract.ErrorType
import com.rainbow.compose.core.contract.UiState
import com.rainbow.compose.ui.dimen.Dimens.topBarHeight

/**
 * 状态覆盖层，覆盖在内容之上，不替换内容布局。适合与「内容始终渲染」的模式搭配使用。
 *
 * 用法：
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     MyContent()                     // 内容始终渲染
 *     StatusOverlay(                  // 覆盖层
 *         state = uiState,
 *         onRetry = { vm.load() }
 *     )
 *     DramaTitleBar(..., modifier = Modifier.align(Alignment.TopCenter))
 * }
 * ```
 */
@Composable
fun StatusOverlay(
    state: UiState<*>,
    onRetry: (() -> Unit)? = null,
    loading: @Composable () -> Unit = { DefaultLoadingOverlay() },
    empty: @Composable () -> Unit = { DefaultEmptyPage() },
    error: @Composable (type: ErrorType, message: String) -> Unit = { type, msg ->
        DefaultErrorPage(type = type, message = msg, onRetry = onRetry)
    },
) {
    val topMargin = with(LocalConfiguration.current) {
        screenHeightDp.dp * 0.25f + topBarHeight
    }

    when (state) {
        is UiState.Loading -> {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topMargin + 100.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    loading()
                }
            }
        }
        is UiState.Empty -> {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topMargin))
                Box(Modifier.fillMaxWidth()) {
                    empty()
                }
            }
        }
        is UiState.Error -> {
            val e = state as UiState.Error
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topMargin))
                Box(Modifier.fillMaxWidth()) {
                    error(e.type, e.message)
                }
            }
        }
        is UiState.Success, is UiState.Idle -> {}
    }
}
