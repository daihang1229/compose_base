package com.rainbow.compose.core.contract

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * [MutableStateFlow]<[UiState]<T>> 的便捷扩展方法。
 *
 * 策略 1（单 Flow）和策略 2（多 Flow）均可使用。
 *
 * 用法：
 * ```
 * private val _uiState = MutableStateFlow<UiState<MyData>>(UiState.Idle)
 * val uiState: StateFlow<UiState<MyData>> = _uiState.asStateFlow()
 *
 * fun load() {
 *     _uiState.moveLoading()
 *     api.fetch().onSuccess { _uiState.moveSuccess(it) }
 *                .onFailure { _uiState.moveError(ErrorType.SERVER_ERROR, it.message) }
 * }
 * ```
 */

fun <T> MutableStateFlow<UiState<T>>.moveLoading() {
    value = UiState.Loading
}

fun <T> MutableStateFlow<UiState<T>>.moveSuccess(data: T) {
    value = UiState.Success(data)
}

fun <T> MutableStateFlow<UiState<T>>.moveError(
    type: ErrorType = ErrorType.UNKNOWN,
    message: String
) {
    value = UiState.Error(type, message)
}

fun <T> MutableStateFlow<UiState<T>>.moveEmpty() {
    value = UiState.Empty
}

/**
 * [ApiResult] 冷流 → [UiState] StateFlow，附带 WhileSubscribed(5_000) 生命周期管理。
 *
 * 自动映射：
 * - [ApiResult.Loading] → [UiState.Loading]
 * - [ApiResult.Success] (data 非空) → [UiState.Success]
 * - [ApiResult.Success] (data 为空) → [UiState.Empty]
 * - [ApiResult.Error] → [UiState.Error]（按 isServerError 映射 ErrorType）
 *
 * 用途：Repository 化后，ViewModel 直接暴露 Repository Flow 时使用。
 */
fun <T> Flow<ApiResult<T>>.toUiStateFlow(
    scope: CoroutineScope,
    initial: UiState<T> = UiState.Idle,
): StateFlow<UiState<T>> = map { result ->
    when (result) {
        is ApiResult.Loading -> UiState.Loading
        is ApiResult.Success -> result.data?.let { UiState.Success(it) } ?: UiState.Empty
        is ApiResult.Error -> UiState.Error(
            type = if (result.isServerError) ErrorType.SERVER_ERROR else ErrorType.NO_NET,
            message = result.message.ifBlank { "加载失败" },
        )
    }
}.stateIn(scope, SharingStarted.WhileSubscribed(5_000L), initial)
