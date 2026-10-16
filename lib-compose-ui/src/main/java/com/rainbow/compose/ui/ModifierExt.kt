package com.rainbow.compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.rainbow.compose.core.log.ComposeLog

/**
 * Compose 点击防抖 Modifier 扩展。
 * 与 View 侧 onClickDelay 行为一致：默认 500ms 间隔内重复点击被忽略。
 *
 * 用法：
 * ```
 * Box(
 *     Modifier.onClickDelay { doSomething() }
 * )
 * ```
 *
 * @param gapMs 防抖间隔(毫秒)，默认 500
 * @param onClick 点击回调(已防抖)
 */
fun Modifier.onClickDelay(
    gapMs: Long = 500,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    this.clickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {
            val now = System.currentTimeMillis()
            if (now - lastClickTime > gapMs || lastClickTime > now) {
                lastClickTime = now
                onClick()
            } else {
                ComposeLog.e("点击时间间距短于${gapMs}ms")
            }
        }
    )
}
