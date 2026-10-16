package com.rainbow.compose.core.contract

/**
 * 错误类型，匹配通用的加载失败分类。
 */
enum class ErrorType {
    NO_NET,
    SERVER_ERROR,
    UNKNOWN,
}

/**
 * 通用异步状态包装。
 *
 * Idle     — 尚未开始加载
 * Loading  — 加载中
 * Success  — 加载成功，携带数据 [T]
 * Error    — 加载失败，携带错误类型和描述
 * Empty    — 加载成功但数据为空（列表页常见）
 *
 * 开发者可按需选择：
 * - 策略 1（单一状态）：ViewModel 暴露一个 [kotlinx.coroutines.flow.StateFlow]<[UiState]<T>>，moveXxx 切换
 * - 策略 2（独立 section）：ViewModel 暴露多个 [kotlinx.coroutines.flow.StateFlow]<[UiState]<T>>，各 section 各自 collect
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val type: ErrorType = ErrorType.UNKNOWN, val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}

interface UiIntent

interface UiEffect
