package com.rainbow.compose.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rainbow.compose.ui.onClickDelay
import com.rainbow.compose.ui.theme.dramaColors

/**
 * 通用条目行：左侧标题 + 右侧值（可选）+ 右侧箭头。
 *
 * 等价原生 ActionItemComponent（左标题右箭头标准条目）。
 *
 * @param title     左侧标题文字
 * @param modifier  Modifier
 * @param value     右侧值文字（如外观的“跟随系统”），传 null 时不显示
 * @param onClick   点击回调（内部已做 onClickDelay 防抖）
 */
@Composable
fun ActionItemRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .onClickDelay(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DramaText(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = dramaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (value != null) {
                DramaText(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = dramaColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = dramaColors.textSecondary
            )
        }
    }
}
