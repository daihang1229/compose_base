package com.rainbow.compose.core.utils

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * 持有一个弱引用指向当前栈顶 Activity，避免内存泄漏。
 */
object ActivityUtils {
    private var topActivityRef: WeakReference<Activity>? = null

    fun getTopActivity(): Activity? {
        return topActivityRef?.get()
    }

    fun setTopActivity(activity: Activity) {
        topActivityRef = WeakReference(activity)
    }
}
