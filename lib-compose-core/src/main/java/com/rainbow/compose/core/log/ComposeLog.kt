package com.rainbow.compose.core.log

import android.util.Log

/**
 * 框架统一的日志入口。
 *
 * 默认委托到 [Log]，集中一处便于后续替换为脱敏 / 分级 / 生产关闭的实现。
 */
object ComposeLog {

    /** 全局开关，生产环境可关闭以去除日志开销。 */
    var enabled: Boolean = true

    private const val TAG = "ComposeBase"

    fun v(message: String) = log(Log.VERBOSE, message)
    fun d(message: String) = log(Log.DEBUG, message)
    fun i(message: String) = log(Log.INFO, message)
    fun w(message: String) = log(Log.WARN, message)
    fun e(message: String) = log(Log.ERROR, message)

    fun e(tag: String, message: String) = log(tag, Log.ERROR, message)

    private fun log(priority: Int, message: String) = log(TAG, priority, message)

    private fun log(tag: String, priority: Int, message: String) {
        if (!enabled) return
        Log.println(priority, tag, message)
    }
}
