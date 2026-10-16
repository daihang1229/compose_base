package com.rainbow.compose.core.contract

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 单 Flow 策略（策略 1）的状态控制器。
 *
 * 封装了 [MutableStateFlow]<[UiState]<T>> 的读写，提供 moveXxx 便捷方法。
 *
 * 用法：
 * ```
 * class MyViewModel : BaseMviViewModel<MyIntent, MyEffect>() {
 *     val stateController = StateController<MyData>()
 *     val uiState: StateFlow<UiState<MyData>> = stateController.state
 *     // ...
 * }
 * ```
 */
class StateController<T>(initial: UiState<T> = UiState.Idle) {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()

    /** 获取当前状态的快照（非 Flow，用于 handleIntent 中判断） */
    val current: UiState<T>
        get() = _state.value

    fun loading() {
        _state.value = UiState.Loading
    }

    fun success(data: T) {
        _state.value = UiState.Success(data)
    }

    fun error(type: ErrorType = ErrorType.UNKNOWN, message: String) {
        _state.value = UiState.Error(type, message)
    }

    fun empty() {
        _state.value = UiState.Empty
    }

    fun idle() {
        _state.value = UiState.Idle
    }
}
