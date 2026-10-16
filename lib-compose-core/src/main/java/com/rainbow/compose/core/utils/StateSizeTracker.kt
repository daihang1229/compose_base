package com.rainbow.compose.core.utils

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.rainbow.compose.core.BuildConfig
import com.rainbow.compose.core.log.ComposeLog

/**
 * Debug 工具：检测大对象状态，超过阈值时主动崩溃以便及早发现问题。
 *
 * 使用方法：在 BaseActivity.onSaveInstanceState 中调用 trackActivity。
 */
object StateSizeTracker {

    private const val TAG = "BigObject"

    private const val CRASH_THRESHOLD = 300 * 1024  // 300KB 崩溃阈值

    /**
     * 追踪 Activity 的状态大小，超过 300KB 时主动崩溃。
     */
    fun trackActivity(activity: FragmentActivity, outState: Bundle) {
        if (!BuildConfig.DEBUG) return

        try {
            val activityName = activity.javaClass.name
            val totalSize = measureBundleSize(outState)

            // 检查是否超过阈值
            if (totalSize > CRASH_THRESHOLD) {
                // 找出问题 Fragment
                val problemFragments = findProblemFragments(activity)

                ComposeLog.e(TAG, "═══════════════════════════════════════════════════════════")
                ComposeLog.e(TAG, "🚨 TransactionTooLargeException 风险检测!")
                ComposeLog.e(TAG, "═══════════════════════════════════════════════════════════")
                ComposeLog.e(TAG, "")
                ComposeLog.e(TAG, "1️⃣ Activity: $activityName")
                ComposeLog.e(TAG, "   总状态大小: ${formatSize(totalSize)} (超过 300KB 阈值)")
                ComposeLog.e(TAG, "")

                // 分析 Fragment 问题
                ComposeLog.e(TAG, "2️⃣ Fragment 分析:")
                if (problemFragments.isNotEmpty()) {
                    for ((index, fragment) in problemFragments.withIndex()) {
                        ComposeLog.e(TAG, "   ${index + 1}. ${fragment.className}")
                        ComposeLog.e(TAG, "      - arguments:      ${formatSize(fragment.argsSize)}")
                        ComposeLog.e(TAG, "      - savedInstance:  ${formatSize(fragment.savedStateSize)}")
                        ComposeLog.e(TAG, "      - viewState:      ${formatSize(fragment.viewStateSize)}")
                        ComposeLog.e(TAG, "      📍 主要问题: ${fragment.mainProblem}")
                        ComposeLog.e(TAG, "")
                    }
                } else {
                    ComposeLog.e(TAG, "   (无超过 50KB 的 Fragment)")
                    ComposeLog.e(TAG, "")
                }

                // 分析 Activity Bundle key
                ComposeLog.e(TAG, "3️⃣ Activity Bundle Key 分析:")
                analyzeBundleKeys(outState)

                ComposeLog.e(TAG, "═══════════════════════════════════════════════════════════")

                // 主动抛出异常
                val mainProblem = if (problemFragments.isNotEmpty())
                    "Fragment: ${problemFragments.first().className}, ${problemFragments.first().mainProblem}"
                else
                    "Activity 自身状态"

                throw BigObjectException(
                    "状态大小 ${formatSize(totalSize)} 超过 300KB!\n" +
                        "Activity: $activityName\n" +
                        "问题来源: $mainProblem"
                )
            }
        } catch (e: BigObjectException) {
            throw e // 重新抛出我们的异常
        } catch (e: Exception) {
            ComposeLog.e(TAG, "追踪失败: ${e.message}")
        }
    }

    /**
     * 找出状态过大的 Fragment
     */
    private fun findProblemFragments(activity: FragmentActivity): List<FragmentProblem> {
        val problems = mutableListOf<FragmentProblem>()
        val fragments = getAllFragments(activity.supportFragmentManager)
        for (fragment in fragments) {
            val problem = analyzeFragment(fragment)
            if (problem.totalSize > 50 * 1024) { // 超过 50KB 的 Fragment 都报告
                problems.add(problem)
            }
        }

        // 按大小排序
        return problems.sortedByDescending { it.totalSize }
    }

    /**
     * 分析单个 Fragment 的状态
     */
    private fun analyzeFragment(fragment: Fragment): FragmentProblem {
        val className = fragment.javaClass.name

        // 测量 arguments 大小
        val argsSize = fragment.arguments?.let { measureBundleSize(it) } ?: 0

        // 测量 savedInstanceState 大小 (通过反射获取)
        var savedStateSize = 0
        try {
            // Fragment 的 mSavedFragmentState 字段包含保存的状态
            val savedStateField = Fragment::class.java.getDeclaredField("mSavedFragmentState")
            savedStateField.isAccessible = true
            val savedState = savedStateField.get(fragment) as? Bundle
            savedStateSize = savedState?.let { measureBundleSize(it) } ?: 0
        } catch (e: Exception) {
            // ignore
        }

        // 测量 view state 大小
        var viewStateSize = 0
        try {
            val viewHierarchyState = SparseArray<Parcelable>()
            fragment.view?.saveHierarchyState(viewHierarchyState)
            viewStateSize = measureSparseArraySize(viewHierarchyState)
        } catch (e: Exception) {
            // ignore
        }

        val totalSize = argsSize + savedStateSize + viewStateSize

        // 确定主要问题来源
        val mainProblem = when {
            argsSize >= savedStateSize && argsSize >= viewStateSize && argsSize > 10 * 1024 ->
                "arguments (${formatSize(argsSize)}) - 建议使用 ViewModel 传递数据"
            savedStateSize >= argsSize && savedStateSize >= viewStateSize && savedStateSize > 10 * 1024 ->
                "savedInstanceState (${formatSize(savedStateSize)}) - 检查 onSaveInstanceState 保存的数据"
            viewStateSize >= argsSize && viewStateSize >= savedStateSize && viewStateSize > 10 * 1024 ->
                "viewState (${formatSize(viewStateSize)}) - 检查 RecyclerView 等 View 的状态保存"
            else -> "未知"
        }

        return FragmentProblem(className, argsSize, savedStateSize, viewStateSize, totalSize, mainProblem)
    }

    /**
     * 获取所有 Fragment（包括嵌套的子 Fragment）
     */
    private fun getAllFragments(fm: androidx.fragment.app.FragmentManager): List<Fragment> {
        val result = mutableListOf<Fragment>()
        for (fragment in fm.fragments) {
            result.add(fragment)
            result.addAll(getAllFragments(fragment.childFragmentManager))
        }
        return result
    }

    private fun measureBundleSize(bundle: Bundle): Int {
        val parcel = Parcel.obtain()
        return try {
            bundle.writeToParcel(parcel, 0)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    private fun measureSparseArraySize(array: SparseArray<Parcelable>): Int {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeSparseArray(array)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    /**
     * 分析 Bundle 中的大 key
     */
    private fun analyzeBundleKeys(bundle: Bundle, indent: String = "   ") {
        val keyInfos = mutableListOf<Triple<String, Int, String>>() // key, size, type

        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            var size = 0
            val typeName = value?.javaClass?.simpleName ?: "null"

            try {
                when (value) {
                    is Bundle -> {
                        size = measureBundleSize(value)
                    }
                    is Parcelable -> {
                        val parcel = Parcel.obtain()
                        value.writeToParcel(parcel, 0)
                        size = parcel.dataSize()
                        parcel.recycle()
                    }
                    is ByteArray -> {
                        size = value.size
                    }
                    is String -> {
                        size = value.length * 2 // UTF-16
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            keyInfos.add(Triple(key, size, typeName))
        }

        // 按大小排序
        keyInfos.sortByDescending { it.second }

        // 打印前 10 个最大的 key
        for ((key, size, typeName) in keyInfos.take(10)) {
            if (size > 1024) { // 只打印大于 1KB 的
                ComposeLog.e(TAG, "$indent- $key: ${formatSize(size)} ($typeName)")
                // 如果是大的 Bundle，递归分析其内容
                val value = bundle.get(key)
                if (value is Bundle && size > 10 * 1024) {
                    analyzeNestedBundle(value, "$indent   ")
                }
            }
        }
    }

    /**
     * 递归分析嵌套的 Bundle（如 BundlableSavedStateRegistry）
     */
    private fun analyzeNestedBundle(bundle: Bundle, indent: String) {
        val keyInfos = mutableListOf<Triple<String, Int, String>>()

        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            var size = 0
            val typeName = value?.javaClass?.simpleName ?: "null"

            try {
                when (value) {
                    is Bundle -> size = measureBundleSize(value)
                    is Parcelable -> {
                        val parcel = Parcel.obtain()
                        value.writeToParcel(parcel, 0)
                        size = parcel.dataSize()
                        parcel.recycle()
                    }
                    is ByteArray -> size = value.size
                    is String -> size = value.length * 2
                }
            } catch (e: Exception) {
                // ignore
            }

            if (size > 1024) {
                keyInfos.add(Triple(key, size, typeName))
            }
        }

        keyInfos.sortByDescending { it.second }

        for ((key, size, typeName) in keyInfos.take(5)) {
            ComposeLog.e(TAG, "$indent└─ $key: ${formatSize(size)} ($typeName)")
            // 继续递归（最多 3 层）
            val value = bundle.get(key)
            if (value is Bundle && size > 10 * 1024 && indent.length < 15) {
                analyzeNestedBundle(value, "$indent   ")
            }
        }
    }

    private fun formatSize(bytes: Int): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    data class FragmentProblem(
        val className: String,
        val argsSize: Int,
        val savedStateSize: Int,
        val viewStateSize: Int,
        val totalSize: Int,
        val mainProblem: String
    )

    /**
     * 自定义异常，用于 Debug 模式下主动崩溃
     */
    class BigObjectException(message: String) : RuntimeException(message)
}
