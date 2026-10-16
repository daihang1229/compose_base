package com.rainbow.compose.core.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable

/**
 * Compose 页面 Activity 基类。
 *
 * 仅做最小职责：基于 [ComponentActivity]，提供 [setComposeContent] 便捷入口，
 * 不耦合任何路由、事件总线、沉浸式或业务埋点逻辑。
 *
 * 继承链：
 *   YourComposeActivity → BaseDramaComposeActivity → BaseComposeActivity → ComponentActivity
 */
abstract class BaseComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * 设置 Compose 内容。子类在 [onCreate] 中调用，例如：
     * ```
     * setComposeContent { MyScreen() }
     * ```
     */
    protected fun setComposeContent(content: @Composable () -> Unit) {
        setContent(content = content)
    }
}
