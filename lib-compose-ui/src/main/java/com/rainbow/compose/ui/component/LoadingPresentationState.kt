package com.rainbow.compose.ui.component

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 刷新或加载更多的展示会话，独立于业务请求状态并使用注入的单调时钟。 */
@Stable
internal class LoadingPresentationState(
    private val nowMillis: () -> Long,
) {
    var session: Session? by mutableStateOf(null)
        private set

    private var isAwaitingRequest = false

    val isActive: Boolean
        get() = session != null

    fun start(): Session {
        isAwaitingRequest = true
        return Session(nowMillis()).also { session = it }
    }

    fun onRequestStarted(): Session {
        val currentSession = session
        if (isAwaitingRequest && currentSession != null) {
            isAwaitingRequest = false
            return currentSession
        }
        return start().also { isAwaitingRequest = false }
    }

    fun remainingMillis(session: Session, minimumDurationMillis: Long): Long {
        if (!isCurrent(session)) return 0L
        val elapsedMillis = (nowMillis() - session.startedAtMillis).coerceAtLeast(0L)
        return (minimumDurationMillis.coerceAtLeast(0L) - elapsedMillis).coerceAtLeast(0L)
    }

    fun isCurrent(session: Session): Boolean = this.session === session

    fun beginSettling(session: Session): Boolean {
        if (!isCurrent(session)) return false
        isAwaitingRequest = false
        return true
    }

    fun finish(session: Session): Boolean {
        if (!isCurrent(session)) return false
        isAwaitingRequest = false
        this.session = null
        return true
    }

    internal class Session(internal val startedAtMillis: Long)
}
