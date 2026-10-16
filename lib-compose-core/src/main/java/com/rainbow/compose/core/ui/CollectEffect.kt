package com.rainbow.compose.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.rainbow.compose.core.contract.UiEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 在生命周期感知下收集 [UiEffect] 一次性副作用。
 *
 * 内部使用 [repeatOnLifecycle] + [Dispatchers.Main.immediate]，避免在后台继续收集。
 */
@Composable
fun <E : UiEffect> CollectEffect(
    flow: Flow<E>,
    handler: suspend (E) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect { handler(it) }
            }
        }
    }
}
