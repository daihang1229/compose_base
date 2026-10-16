package com.rainbow.compose.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.rainbow.compose.ui.R

/**
 * 模拟原生 LottieRefreshHeader。
 *
 * - 下拉阶段（progress 0→1）：Lottie 进度跟随手指，预览动画
 * - 刷新阶段（isRefreshing=true）：Lottie 全速循环播放
 *
 * 高度 76dp = 41dp(top) + 24dp(lottie) + 11dp(bottom)。
 */
@Composable
fun ClassicsRefreshIndicator(
    isRefreshing: Boolean,
    progress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.refresh))

    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (isRefreshing) LottieConstants.IterateForever else 1,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(26.dp))
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { if (isRefreshing) lottieProgress else progress },
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(26.dp))
    }
}
