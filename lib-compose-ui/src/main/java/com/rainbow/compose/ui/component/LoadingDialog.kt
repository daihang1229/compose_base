package com.rainbow.compose.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.rainbow.compose.ui.theme.commonColors

/**
 * 浮层 Loading 弹窗：半透明遮罩上的居中转圈，不遮挡整页内容区（区别于全屏 StatusOverlay）。
 *
 * 用于登出 / 提交等阻塞式过程态。通过 [canCancel] 控制是否允许返回键 / 点击外部取消。
 *
 * 显隐由调用方（Screen 层）通过 `if (showLoading) LoadingDialog(...)` 控制。
 */
@Composable
fun LoadingDialog(
    canCancel: Boolean,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {}
) {
    CommonDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = canCancel,
            dismissOnClickOutside = canCancel
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = commonColors.primary)
            }
        }
    }
}
