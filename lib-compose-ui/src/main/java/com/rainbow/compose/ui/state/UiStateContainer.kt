package com.rainbow.compose.ui.state

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rainbow.compose.core.contract.ErrorType
import com.rainbow.compose.core.contract.UiState

/**
 * 将 [UiState] 自动映射到对应 UI。
 *
 * 默认提供 Loading(Lottie) / Error(图标+重试) / Empty(空状态) 的 UI，
 * 每个阶段都可传入自定义 composable 覆盖。
 *
 * 用法：
 * ```
 * UiStateContainer(
 *     state   = viewModel.uiState,
 *     onRetry = { viewModel.sendIntent(MyIntent.Load) }
 * ) { data ->
 *     MyContent(data)
 * }
 * ```
 */
@Composable
fun <T> UiStateContainer(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    loading: @Composable () -> Unit = { DefaultLoadingOverlay() },
    error: @Composable (type: ErrorType, message: String) -> Unit = { type, msg ->
        DefaultErrorPage(type = type, message = msg, onRetry = onRetry)
    },
    empty: @Composable () -> Unit = { DefaultEmptyPage() },
    success: @Composable (T) -> Unit
) {
    Box(modifier = modifier) {
        when (state) {
            is UiState.Idle -> {}
            is UiState.Loading -> loading()
            is UiState.Error -> error(state.type, state.message)
            is UiState.Empty -> empty()
            is UiState.Success -> success(state.data)
        }
    }
}
