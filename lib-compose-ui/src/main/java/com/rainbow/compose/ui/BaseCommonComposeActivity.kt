package com.rainbow.compose.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.rainbow.compose.core.base.BaseComposeActivity
import com.rainbow.compose.ui.theme.CommonTheme

/**
 * [BaseComposeActivity] 的子类，预集成 [CommonTheme]。
 *
 * 子类实现 [screen] 返回顶层 Composable，可覆盖 [initObservable] 处理旁路副作用流。
 *
 * ```
 * class MyComposeActivity : BaseCommonComposeActivity() {
 *     override fun initObservable() { ... }
 *
 *     @Composable
 *     override fun screen() {
 *         MyScreen()
 *     }
 * }
 * ```
 */
abstract class BaseCommonComposeActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObservable()
        setComposeContent {
            CommonTheme {
                screen()
            }
        }
    }

    @Composable
    abstract fun screen()

    /**
     * 旁路副作用初始化入口（默认空实现），子类可覆盖。
     */
    open fun initObservable() {}
}
