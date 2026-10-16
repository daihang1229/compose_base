package com.rainbow.compose.core.contract

import androidx.lifecycle.viewModelScope
import com.rainbow.compose.core.base.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MVI ViewModel 基类。
 *
 * 职责：
 * - Intent → handleIntent 的调度入口（[sendIntent]）
 * - 一次性 Effect 的 Channel 投递（[sendEffect] / [effect]）
 *
 * 状态管理由子类自行决定：
 * - 策略 1：子类调用 [StateFlow]<[UiState]<T>>
 * - 策略 2：子类暴露多个 [StateFlow]<[UiState]<T>>
 */
abstract class BaseMviViewModel<I : UiIntent, E : UiEffect>(
    private val effectDelivery: EffectDelivery = EffectDelivery.DELIVER_ALL
) : BaseViewModel() {

    private val _effect: Channel<E> = when (effectDelivery) {
        EffectDelivery.DELIVER_ALL -> Channel(Channel.BUFFERED)
        EffectDelivery.LATEST_ONLY -> Channel(Channel.CONFLATED)
    }
    val effect: Flow<E> = _effect.receiveAsFlow()

    fun sendIntent(intent: I) {
        viewModelScope.launch { handleIntent(intent) }
    }

    abstract suspend fun handleIntent(intent: I)

    /**
     * Effect 发送（一次性副作用）：
     * - LATEST_ONLY：使用 trySend，不阻塞，旧消息被覆盖
     * - DELIVER_ALL：使用 send，阻塞直到 Channel 有空间
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            when (effectDelivery) {
                EffectDelivery.LATEST_ONLY -> _effect.trySend(effect)
                EffectDelivery.DELIVER_ALL -> _effect.send(effect)
            }
        }
    }

    /**
     * 冷流 → 热流（StateFlow），最后一个订阅者消失 5 秒后断开上游。
     *
     * 用途：ViewModel 暴露 Repository 的 Flow 时，避免 VM 存活期间上游一直被收集。
     * 对齐 Now in Android 的 WhileSubscribed(5_000) 策略。
     */
    protected fun <T> Flow<T>.toStateFlowWhileSubscribed(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), initial)
}
