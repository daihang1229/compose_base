package com.rainbow.compose.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rainbow.compose.ui.R
import com.rainbow.compose.ui.onClickDelay
import com.rainbow.compose.ui.theme.commonColors

/**
 * 通用标题栏。
 *
 * 高度 44dp，标题居中 17sp Medium，返回图标 touch 区域 44dp，图标 14dp。
 *
 * @param title       标题文字
 * @param modifier    Modifier
 * @param onBack      返回按钮点击，传 null 时不显示返回按钮
 * @param rightText   右侧文字，传 null 时不显示
 * @param onRightText 右侧文字点击
 * @param rightIcon   右侧图标 Painter，传 null 时不显示（与 rightText 互斥，图标优先）
 * @param onRightIcon 右侧图标点击
 * @param leftText    左侧文字（替代返回图标），传 null 时不显示
 * @param onLeftText  左侧文字点击
 * @param bottomDivider 是否显示底部分割线
 * @param onTitleClick     标题单击回调，传 null 时标题不可点击
 * @param onTitleLongClick 标题长按回调，传 null 时标题不响应长按
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommonTitleBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = { },
    rightText: String? = null,
    onRightText: (() -> Unit)? = null,
    rightIcon: Painter? = null,
    onRightIcon: (() -> Unit)? = null,
    leftText: String? = null,
    onLeftText: (() -> Unit)? = null,
    bottomDivider: Boolean = false,
    onTitleClick: (() -> Unit)? = null,
    onTitleLongClick: (() -> Unit)? = null
) {
    val tintColor = commonColors.titleBarIcon

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            if (leftText != null) {
                Text(
                    text = leftText,
                    fontSize = 14.sp,
                    color = tintColor,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .height(44.dp)
                        .onClickDelay(enabled = onLeftText != null) { onLeftText?.invoke() }
                        .padding(start = 16.dp),
                    fontWeight = FontWeight.Normal
                )
            } else if (onBack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(44.dp)
                        .onClickDelay { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "back",
                        modifier = Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(tintColor)
                    )
                }
            }

            // 标题（在父容器中居中）
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = tintColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(
                        if (onTitleClick != null || onTitleLongClick != null) {
                            Modifier.combinedClickable(
                                onClick = { onTitleClick?.invoke() },
                                onLongClick = onTitleLongClick
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 40.dp),
                softWrap = false
            )

            if (rightIcon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(44.dp)
                        .onClickDelay(enabled = onRightIcon != null) { onRightIcon?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rightIcon,
                        contentDescription = "right",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(tintColor)
                    )
                }
            } else if (rightText != null) {
                Text(
                    text = rightText,
                    fontSize = 14.sp,
                    color = tintColor,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(44.dp)
                        .onClickDelay(enabled = onRightText != null) { onRightText?.invoke() }
                        .padding(end = 16.dp),
                    fontWeight = FontWeight.Normal
                )
            }
        }

        if (bottomDivider) {
            HorizontalDivider(
                color = commonColors.divider,
                thickness = 0.5.dp
            )
        }
    }
}

/**
 * 带底部分割线的 [CommonTitleBar]。
 */
@Composable
fun CommonTitleBarWithBack(
    title: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    rightText: String? = null,
    onRightText: (() -> Unit)? = null,
    rightIcon: Painter? = null,
    onRightIcon: (() -> Unit)? = null,
) {
    CommonTitleBar(
        title = title,
        modifier = modifier,
        onBack = onBack,
        rightText = rightText,
        onRightText = onRightText,
        rightIcon = rightIcon,
        onRightIcon = onRightIcon,
        bottomDivider = true
    )
}
